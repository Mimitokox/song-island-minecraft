<div align="center">

<img src="media/logo.png" alt="Song Island" width="420">

# Song Island

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-dbd0b4)](https://fabricmc.net)
[![License](https://img.shields.io/badge/License-AGPL--3.0-blue)](LICENSE)

</div>

### Now playing

Title, artist and lyrics

<div align="center">
<img src="media/island.gif" alt="The island" width="620">
</div>

### Text in 3d

3D text with the song lyrics in the world!

<div align="center">
<img src="media/text3d.gif" alt="3D lyrics" width="600">
<img src="media/text3d-f5.gif" alt="3D lyrics in third person" width="600">
</div>

### Manual Map Music

Song has no synced lyrics? No problem!

<div align="center">
<img src="media/manual-map.png" alt="Manual mapping" width="620">
</div>

### Drag

Put the island anywhere on your screen

<div align="center">
<img src="media/drag.gif" alt="Dragging the island" width="620">
</div>

### Supported

**Players** - anything Windows sees as media:

- Spotify
- YouTube Music
- YouTube and other browser tabs (Chrome, Edge)
- SoundCloud

**Lyrics Libs**

1. Your own mappings
2. [LRCLIB](https://lrclib.net)
3. [NetEase](https://music.163.com)

### Config files

All config files: ``%APPDATA% > .minecraft > config > song-island``

(or if you use custom luncher ``<luncher patch/profile> > config > song-island``) ;)

### Settings

Open chat, click the island, then `...`.

| Option | What it does |
| --- | --- |
| Lyric delay | Delays lyrics for this song |
| Manual Map Music | Opens the mapping screen |
| [BETA] Text in 3d | Lyrics in the world |
| Hide bossbar | Hides boss bars |
| Drag | Move the island |

### Requirements

- Minecraft 1.21.11
- Fabric
- Fabric API
- Windows

### Building

```bash
./gradlew build
```

### License

AGPL-3.0. Uses [MediaPlayerInfo](https://github.com/Redstonecrafter0/MediaPlayerInfo) (AGPL-3.0)
and [JNA](https://github.com/java-native-access/jna) (Apache-2.0 / LGPL-2.1)
