# 🎵 Now Playing IRL

Affiche la musique que tu écoutes **pour de vrai** (Spotify, TIDAL, AIMP, VLC, YouTube…) directement dans Minecraft, avec pochette, artiste, titre et barre de progression.

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.6_--_26.3-brightgreen) ![Fabric](https://img.shields.io/badge/Loader-Fabric-blue) ![Apps](https://img.shields.io/badge/Apps_support%C3%A9es-193-purple) ![Version](https://img.shields.io/badge/Mod-3.1.0-orange)

## 📦 Installation
1. Installe **Fabric Loader** + **Fabric API** pour ta version de Minecraft.
2. Télécharge le `.jar` correspondant à ta version depuis les [Releases](https://github.com/MaxLananas/Now-Playing-IRL/releases) (`now-playing-irl-3.1.0-mc<VERSION>.jar`).
3. Place-le dans ton dossier `mods/`. (ModMenu est optionnel, pour l'écran de config.)

## 🎧 Applications supportées (193 !)
Streaming : Spotify, TIDAL, Deezer, Apple Music, Amazon Music, YouTube Music, SoundCloud, Qobuz, Napster, Pandora, Bandcamp, Mixcloud, Audius, Anghami, Yandex Music, NetEase, QQ Music…
Lecteurs : **AIMP**, foobar2000, VLC, Winamp, MusicBee, MediaMonkey, Windows Media Player, PotPlayer, MPC-HC/BE, mpv, Kodi, Plex/Plexamp, Jellyfin, Clementine, Strawberry, Rhythmbox, Audacious, DeaDBeeF, musikcube, Roon, JRiver…
Navigateurs & web : Chrome, Edge, Firefox, Brave, Opera/Opera GX, Vivaldi, Arc, Safari, Chromium, Zen, LibreWolf… avec détection du service (YouTube, YouTube Music, Spotify Web, Twitch, Vimeo…).
Liste complète et extensible en **une ligne par app** : `src/main/java/com/nowplayingirl/client/media/MusicApps.java`.

## ⌨️ Touches
| Touche | Action |
|---|---|
| `N` | Afficher / masquer le widget |
| `M` | Ouvrir la configuration |
| `,` | Changer de thème |

## ⚙️ Options
12 thèmes, positions (coins + haut/bas centre), taille, opacité, vitesse de rotation de la pochette, barre de progression, mode compact, texte défilant, masquer quand rien ne joue, intervalle de polling, et `disabledApps` (liste d'ids d'apps à ignorer) dans `config/nowplayingirl.json`.

## 🔧 Versions de Minecraft supportées
**1.21.6 → 1.21.11** (Yarn, Java 21) **et 26.1, 26.1.1, 26.1.2, 26.2, 26.3** (mappings Mojang officiels, Java 25, couche dédiée `src26/`) — voir `versions.json`. Chaque version est compilée et publiée automatiquement par GitHub Actions. Pour ajouter une version : une ligne dans `versions.json` + une ligne dans la matrice de `.github/workflows/build.yml`. Sur 26.x, lance le jeu avec **Java 25** et **Fabric Loader 0.20.1+**.

## 🚀 Performance
- Windows : une seule énumération des fenêtres pour toutes les apps, correspondance par nom de processus (corrige AIMP, issue #5).
- Linux : un seul appel `playerctl` pour tous les lecteurs MPRIS. macOS : un seul `osascript`.
- Polling adaptatif : ralentit (jusqu'à 5 s) quand rien ne joue ; timeouts stricts sur les processus externes ; cache de pochettes borné et dédupliqué.

## 🛠️ Build
```bash
./gradlew build                       # version par défaut (gradle.properties)
./gradlew build -Pminecraft_version=1.21.9 -Pyarn_mappings=1.21.9+build.1 -Pfabric_version=0.134.1+1.21.9
./gradlew build -Pminecraft_version=26.3 -Pfabric_version=0.165.0+26.3 -Ploader_version=0.20.1 -Ploom_version=1.19-SNAPSHOT -Pjava_version=25 -Ptarget_api=mojang
```
