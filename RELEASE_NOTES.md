## 🎵 Now Playing IRL 3.1.1 — Correctif de crash (hotfix)

**Choisis le `.jar` qui correspond à ta version de Minecraft** et place-le dans ton dossier `mods/` (Fabric Loader + Fabric API requis).

### 🐛 Corrigé dans la 3.1.1
- **Crash au démarrage sur Minecraft 26.1, 26.1.1, 26.1.2 et 26.2** : la création des touches cherchait la constante `InputConstants.Type.KEYBOARD`, qui n'existe que sur 26.3 (elle s'appelle `KEYSYM` avant). Le mod plantait dès son initialisation. Les deux noms sont maintenant acceptés. (26.3 et 1.21.x n'étaient pas touchés.)
- **Conflit JNA supprimé** : le mod n'embarque plus sa propre copie de JNA 5.14, qui masquait celle, plus récente, de Minecraft 26.x et provoquait des erreurs `oshi ... NoSuchMethodError` dans les logs. Il utilise désormais le JNA fourni par Minecraft. Bonus : le fichier passe de ~4 Mo à quelques centaines de Ko.
- Le thread de détection musicale ne peut plus jamais faire planter le jeu, même en cas d'erreur native.

### Rappel
11 versions supportées : **1.21.6 → 1.21.11** (Java 21) et **26.1 → 26.3** (Java 25, Loader 0.20.1+). 193 applications musicales reconnues (Spotify, TIDAL, AIMP, VLC, YouTube…), 12 thèmes, barre de progression, pochettes d'album.
