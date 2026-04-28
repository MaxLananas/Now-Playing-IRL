package com.nowplayingirl.client.media;

import com.nowplayingirl.NowPlayingIRLMod;

public abstract class MediaDetector {

    public abstract MediaInfo detect();

    public static MediaDetector create() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            NowPlayingIRLMod.LOGGER.info("Using Windows media detector");
            return new WindowsMediaDetector();
        } else if (os.contains("mac")) {
            NowPlayingIRLMod.LOGGER.info("Using macOS media detector");
            return new MacOSMediaDetector();
        } else {
            NowPlayingIRLMod.LOGGER.info("Using Linux media detector");
            return new LinuxMediaDetector();
        }
    }
}

// ─────────────────────────────────────────────
// macOS
// ─────────────────────────────────────────────
class MacOSMediaDetector extends MediaDetector {

    // Liste des apps supportées sur macOS, dans l'ordre de priorité
    private static final String[][] APPS = {
        { "Spotify",     "Spotify"     },
        { "TIDAL",       "Tidal"       },
        { "Music",       "Apple Music" },
        { "iTunes",      "iTunes"      },
        { "Vox",         "Vox"         },
    };

    @Override
    public MediaInfo detect() {
        for (String[] app : APPS) {
            String appName    = app[0]; // Nom du process AppleScript
            String sourceName = app[1]; // Nom affiché dans le HUD

            try {
                // Vérifie si l'app tourne ET joue
                ProcessBuilder pb = new ProcessBuilder(
                    "osascript", "-e",
                    "tell application \"" + appName + "\"\n" +
                    "  if application \"" + appName + "\" is running then\n" +
                    "    if player state is playing then\n" +
                    "      return (get artist of current track) & \"|\" & (get name of current track)\n" +
                    "    end if\n" +
                    "  end if\n" +
                    "end tell"
                );
                Process p = pb.start();
                String result = new String(p.getInputStream().readAllBytes()).trim();

                if (!result.isEmpty() && result.contains("|")) {
                    String[] parts = result.split("\\|", 2);
                    if (parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                        return new MediaInfo(parts[1].trim(), parts[0].trim(), sourceName);
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}

// ─────────────────────────────────────────────
// Linux
// ─────────────────────────────────────────────
class LinuxMediaDetector extends MediaDetector {

    // Noms de players MPRIS reconnus, mappés vers un nom d'affichage
    private static final String[][] KNOWN_PLAYERS = {
        { "spotify",     "Spotify"     },
        { "tidal-hifi",  "Tidal"       },
        { "tidal",       "Tidal"       },
        { "firefox",     "Firefox"     },
        { "chromium",    "Chromium"    },
        { "chrome",      "Chrome"      },
        { "vlc",         "VLC"         },
        { "rhythmbox",   "Rhythmbox"   },
        { "clementine",  "Clementine"  },
    };

    @Override
    public MediaInfo detect() {
        try {
            // playerctl --list-all pour connaître les players actifs
            ProcessBuilder listPb = new ProcessBuilder("playerctl", "--list-all");
            Process listProcess = listPb.start();
            String playerList = new String(listProcess.getInputStream().readAllBytes()).trim();

            if (playerList.isEmpty()) return null;

            // Cherche en priorité les players connus
            for (String[] known : KNOWN_PLAYERS) {
                String playerId  = known[0];
                String sourceName = known[1];

                if (!playerList.toLowerCase().contains(playerId)) continue;

                // Vérifie le statut
                ProcessBuilder statusPb = new ProcessBuilder(
                    "playerctl", "--player=" + playerId, "status"
                );
                Process statusProcess = statusPb.start();
                String status = new String(statusProcess.getInputStream().readAllBytes()).trim();

                if (!status.equalsIgnoreCase("Playing")) continue;

                // Récupère les métadonnées
                ProcessBuilder metaPb = new ProcessBuilder(
                    "playerctl", "--player=" + playerId,
                    "metadata", "--format", "{{artist}}|{{title}}"
                );
                Process metaProcess = metaPb.start();
                String result = new String(metaProcess.getInputStream().readAllBytes()).trim();

                if (!result.isEmpty() && result.contains("|")) {
                    String[] parts = result.split("\\|", 2);
                    if (parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                        return new MediaInfo(parts[1].trim(), parts[0].trim(), sourceName);
                    }
                }
            }

            // Fallback : n'importe quel player actif
            ProcessBuilder pb = new ProcessBuilder(
                "playerctl", "metadata", "--format", "{{artist}}|{{title}}|{{playerName}}"
            );
            Process p = pb.start();
            String result = new String(p.getInputStream().readAllBytes()).trim();

            if (!result.isEmpty() && result.contains("|")) {
                String[] parts = result.split("\\|", 3);
                if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                    String source = parts.length > 2 ? capitalize(parts[2].trim()) : "Unknown";
                    return new MediaInfo(parts[1].trim(), parts[0].trim(), source);
                }
            }

        } catch (Exception ignored) {}
        return null;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
