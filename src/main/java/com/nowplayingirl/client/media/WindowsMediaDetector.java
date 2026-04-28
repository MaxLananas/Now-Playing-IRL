package com.nowplayingirl.client.media;

import com.nowplayingirl.NowPlayingIRLMod;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WindowsMediaDetector extends MediaDetector {

    private static final Map<String, Pattern> WINDOW_PATTERNS = new HashMap<>();

    static {
        WINDOW_PATTERNS.put("Spotify", Pattern.compile("^(.+?)\\s*[-–—]\\s*(.+)$"));
        WINDOW_PATTERNS.put("Tidal", Pattern.compile("^(.+?)\\s*[-–—]\\s*(.+)$"));
        WINDOW_PATTERNS.put("VLC", Pattern.compile("^(.+?)\\s*[-–—]\\s*VLC media player$"));
        WINDOW_PATTERNS.put("YouTube", Pattern.compile("^(.+?)\\s*[-–—]\\s*YouTube.*$"));
        WINDOW_PATTERNS.put("foobar2000", Pattern.compile("^\\[(.+?)\\]\\s*(.+?)\\s*[-–—]\\s*(.+)$"));
    }

    @Override
    public MediaInfo detect() {
        try {
            MediaInfo spotify = detectSpotify();
            if (spotify != null) return spotify;

            MediaInfo tidal = detectTidal();
            if (tidal != null) return tidal;

            MediaInfo other = detectOtherPlayers();
            if (other != null) return other;

            return detectFromWindows();

        } catch (Exception e) {
            NowPlayingIRLMod.LOGGER.debug("Detection error: {}", e.getMessage());
            return null;
        }
    }

    private MediaInfo detectSpotify() {
        final String[] title = {null};

        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            char[] windowText = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, windowText, 512);
            String text = Native.toString(windowText).trim();

            if (text.isEmpty()) return true;

            char[] className = new char[256];
            User32.INSTANCE.GetClassName(hwnd, className, 256);
            String classStr = Native.toString(className);

            if (classStr.contains("Chrome_WidgetWin") || classStr.equals("SpotifyMainWindow")) {
                IntByReference pid = new IntByReference();
                User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);

                if (isProcessName(pid.getValue(), "spotify") && text.contains(" - ") &&
                    !text.equals("Spotify") && !text.equals("Spotify Free") &&
                    !text.equals("Spotify Premium")) {
                    title[0] = text;
                    return false;
                }
            }
            return true;
        }, null);

        if (title[0] != null) {
            return parseTitleArtist(title[0], "Spotify");
        }
        return null;
    }

    private MediaInfo detectTidal() {
        final String[] title = {null};

        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            if (!User32.INSTANCE.IsWindowVisible(hwnd)) return true;

            char[] windowText = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, windowText, 512);
            String text = Native.toString(windowText).trim();

            if (text.isEmpty()) return true;

            char[] className = new char[256];
            User32.INSTANCE.GetClassName(hwnd, className, 256);
            String classStr = Native.toString(className);

            IntByReference pid = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);

            boolean isTidalProcess = isProcessName(pid.getValue(), "tidal");
            boolean isTidalClass = classStr.contains("Chrome_WidgetWin") ||
                                   classStr.contains("TIDAL") ||
                                   classStr.contains("tidal");

            if (isTidalProcess && isTidalClass && text.contains(" - ") && !text.equalsIgnoreCase("tidal")) {
                title[0] = text;
                return false;
            }

            return true;
        }, null);

        if (title[0] != null) {
            return parseTitleArtist(title[0], "Tidal");
        }
        return null;
    }

    private boolean isProcessName(int pid, String name) {
        try {
            WinNT.HANDLE process = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_QUERY_LIMITED_INFORMATION, false, pid);
            if (process != null) {
                char[] path = new char[1024];
                IntByReference size = new IntByReference(1024);
                if (Kernel32.INSTANCE.QueryFullProcessImageName(process, 0, path, size)) {
                    String processPath = Native.toString(path).toLowerCase();
                    Kernel32.INSTANCE.CloseHandle(process);
                    return processPath.contains(name.toLowerCase());
                }
                Kernel32.INSTANCE.CloseHandle(process);
            }
        } catch (Exception ignored) {}
        return false;
    }

    private MediaInfo parseTitleArtist(String title, String source) {
        Pattern pattern = WINDOW_PATTERNS.get(source);
        if (pattern == null) pattern = WINDOW_PATTERNS.get("Spotify");
        Matcher matcher = pattern.matcher(title);
        if (matcher.matches()) {
            String artist = matcher.group(1).trim();
            String track = matcher.group(2).trim();
            return new MediaInfo(track, artist, source);
        }
        return null;
    }

    private MediaInfo detectOtherPlayers() {
        final MediaInfo[] result = {null};

        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            if (!User32.INSTANCE.IsWindowVisible(hwnd)) return true;

            char[] windowText = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, windowText, 512);
            String text = Native.toString(windowText).trim();

            if (text.isEmpty()) return true;

            if (text.contains("VLC media player") && !text.equals("VLC media player")) {
                Matcher m = WINDOW_PATTERNS.get("VLC").matcher(text);
                if (m.matches()) {
                    result[0] = new MediaInfo(m.group(1).trim(), "Unknown Artist", "VLC");
                    return false;
                }
            }

            if (text.contains("YouTube") && !text.equals("YouTube")) {
                Matcher m = WINDOW_PATTERNS.get("YouTube").matcher(text);
                if (m.matches()) {
                    String videoTitle = m.group(1).trim();
                    if (videoTitle.contains(" - ")) {
                        String[] parts = videoTitle.split(" - ", 2);
                        result[0] = new MediaInfo(parts[1].trim(), parts[0].trim(), "YouTube");
                    } else {
                        result[0] = new MediaInfo(videoTitle, "YouTube", "YouTube");
                    }
                    return false;
                }
            }

            return true;
        }, null);

        return result[0];
    }

    private MediaInfo detectFromWindows() {
        final MediaInfo[] result = {null};
        final String[] players = {"AIMP", "Winamp", "foobar2000", "MusicBee", "iTunes"};

        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            if (!User32.INSTANCE.IsWindowVisible(hwnd)) return true;

            char[] windowText = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, windowText, 512);
            String text = Native.toString(windowText).trim();

            for (String player : players) {
                if (text.toLowerCase().contains(player.toLowerCase()) && text.contains(" - ")) {
                    String clean = text.replaceAll("(?i)" + player, "").trim();
                    clean = clean.replaceAll("^[\\s\\-–—]+|[\\s\\-–—]+$", "");

                    if (clean.contains(" - ")) {
                        String[] parts = clean.split(" - ", 2);
                        result[0] = new MediaInfo(parts[1].trim(), parts[0].trim(), player);
                        return false;
                    }
                }
            }
            return true;
        }, null);

        return result[0];
    }
}
