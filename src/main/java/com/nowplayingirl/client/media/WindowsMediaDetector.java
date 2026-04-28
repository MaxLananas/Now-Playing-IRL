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
    
    // Patterns pour parser les titres de fenêtres
    private static final Map<String, Pattern> WINDOW_PATTERNS = new HashMap<>();
    
    static {
        // Spotify: "Artist - Title"
        WINDOW_PATTERNS.put("Spotify", Pattern.compile("^(.+?)\\s*[-–—]\\s*(.+)$"));
        // VLC: "Title - VLC media player"
        WINDOW_PATTERNS.put("VLC", Pattern.compile("^(.+?)\\s*[-–—]\\s*VLC media player$"));
        // Chrome/Edge avec YouTube: "Title - YouTube"
        WINDOW_PATTERNS.put("YouTube", Pattern.compile("^(.+?)\\s*[-–—]\\s*YouTube.*$"));
        // foobar2000
        WINDOW_PATTERNS.put("foobar2000", Pattern.compile("^\\[(.+?)\\]\\s*(.+?)\\s*[-–—]\\s*(.+)$"));
    }
    
    private String lastDetectedTitle = "";
    private MediaInfo cachedMedia = null;
    
    @Override
    public MediaInfo detect() {
        try {
            // 1. Essayer Spotify d'abord (le plus courant)
            MediaInfo spotify = detectSpotify();
            if (spotify != null) return spotify;
            
            // 2. Essayer les autres lecteurs
            MediaInfo other = detectOtherPlayers();
            if (other != null) return other;
            
            // 3. Essayer de détecter via les fenêtres
            return detectFromWindows();
            
        } catch (Exception e) {
            NowPlayingIRLMod.LOGGER.debug("Detection error: {}", e.getMessage());
            return null;
        }
    }
    
    private MediaInfo detectSpotify() {
        // Chercher la fenêtre Spotify
        final String[] title = {null};
        
        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            char[] windowText = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, windowText, 512);
            String text = Native.toString(windowText).trim();
            
            if (text.isEmpty()) return true;
            
            char[] className = new char[256];
            User32.INSTANCE.GetClassName(hwnd, className, 256);
            String classStr = Native.toString(className);
            
            // Spotify a une classe spécifique
            if (classStr.contains("Chrome_WidgetWin") || classStr.equals("SpotifyMainWindow")) {
                // Vérifier le processus
                IntByReference pid = new IntByReference();
                User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);
                
                if (isSpotifyProcess(pid.getValue()) && text.contains(" - ") && 
                    !text.equals("Spotify") && !text.equals("Spotify Free") && 
                    !text.equals("Spotify Premium")) {
                    title[0] = text;
                    return false;
                }
            }
            return true;
        }, null);
        
        if (title[0] != null) {
            return parseSpotifyTitle(title[0]);
        }
        return null;
    }
    
    private boolean isSpotifyProcess(int pid) {
        try {
            WinNT.HANDLE process = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_QUERY_LIMITED_INFORMATION, false, pid);
            if (process != null) {
                char[] path = new char[1024];
                IntByReference size = new IntByReference(1024);
                if (Kernel32.INSTANCE.QueryFullProcessImageName(process, 0, path, size)) {
                    String processPath = Native.toString(path).toLowerCase();
                    Kernel32.INSTANCE.CloseHandle(process);
                    return processPath.contains("spotify");
                }
                Kernel32.INSTANCE.CloseHandle(process);
            }
        } catch (Exception ignored) {}
        return false;
    }
    
    private MediaInfo parseSpotifyTitle(String title) {
        Pattern pattern = WINDOW_PATTERNS.get("Spotify");
        Matcher matcher = pattern.matcher(title);
        if (matcher.matches()) {
            String artist = matcher.group(1).trim();
            String track = matcher.group(2).trim();
            return new MediaInfo(track, artist, "Spotify");
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
            
            // VLC
            if (text.contains("VLC media player") && !text.equals("VLC media player")) {
                Matcher m = WINDOW_PATTERNS.get("VLC").matcher(text);
                if (m.matches()) {
                    result[0] = new MediaInfo(m.group(1).trim(), "Unknown Artist", "VLC");
                    return false;
                }
            }
            
            // YouTube (Chrome/Edge/Firefox)
            if (text.contains("YouTube") && !text.equals("YouTube")) {
                Matcher m = WINDOW_PATTERNS.get("YouTube").matcher(text);
                if (m.matches()) {
                    String videoTitle = m.group(1).trim();
                    // Essayer de parser "Artist - Title" du titre YouTube
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
        // Fallback: chercher n'importe quelle fenêtre avec un pattern "Artist - Title"
        final MediaInfo[] result = {null};
        final String[] players = {"AIMP", "Winamp", "foobar2000", "MusicBee", "iTunes"};
        
        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            if (!User32.INSTANCE.IsWindowVisible(hwnd)) return true;
            
            char[] windowText = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, windowText, 512);
            String text = Native.toString(windowText).trim();
            
            for (String player : players) {
                if (text.toLowerCase().contains(player.toLowerCase()) && text.contains(" - ")) {
                    // Parser le titre
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