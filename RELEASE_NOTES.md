## 🎵 Now Playing IRL 3.0.0 — MC 1.21.6 → 1.21.11

**Un seul téléchargement par version de Minecraft** : choisis le `.jar` qui correspond à ta version (1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10 ou 1.21.11) et place-le dans ton dossier `mods/` (Fabric Loader + Fabric API requis).

### ✨ Nouveautés 3.0.0
- **193 applications musicales reconnues** (contre une dizaine) : Spotify, TIDAL, Deezer, Apple Music, YouTube Music, SoundCloud, Qobuz, **AIMP (correctif #5)**, foobar2000, VLC, Winamp, MusicBee, Plex/Plexamp, Kodi, Jellyfin, les navigateurs (Chrome, Edge, Firefox, Brave, Opera GX, Vivaldi, Arc, Zen…) et les services web (YouTube, Twitch, Bandcamp, Mixcloud…).
- **Portage multi-versions** : 1.21.6 → 1.21.11 depuis un seul code source.
- **Optimisations** : 1 seul passage d'énumération des fenêtres sous Windows (4 avant), 1 seul processus `playerctl` sous Linux (3+ par lecteur avant), 1 seul `osascript` sous macOS, polling adaptatif (ralentit jusqu'à 5 s quand rien ne joue), timeouts stricts, cache de pochettes borné (LRU 48) et dédupliqué.
- **Plus beau, plus d'options** : 12 thèmes, barre de progression, mode compact, texte défilant, masquage quand rien ne joue, positions haut/bas-centre, intervalle de polling réglable, liste d'apps ignorées dans la config.
- Pochettes : URL fournie par le lecteur (MPRIS) utilisée en priorité, sinon iTunes puis Deezer.

> ⚠️ Ne pas utiliser le fichier `-sources.jar` (développeurs uniquement) s'il est présent.
