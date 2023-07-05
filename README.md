# Kotify

[![Build status](https://github.com/dzirbel/kotify/workflows/Build/badge.svg)](https://github.com/dzirbel/kotify/actions/workflows/build.yml)
[![codecov](https://codecov.io/gh/dzirbel/kotify/branch/master/graph/badge.svg?token=RZU5D35M5E)](https://codecov.io/gh/dzirbel/kotify)

Kotify is a multiplatform desktop client for [Spotify](https://www.spotify.com/) focused on library
organization for power users.

Kotify is in early development, and does not yet have feature parity with Spotify's desktop client,
much less additional features. Planned features include:
* organize music by genre, rating, play count, and more
* tools to maintain your library - see playlists a song is in, new releases not yet added, etc
* edit song metadata or add custom fields
* custom auto-generated playlists
* deduplication of identical albums (see for example [this issue](https://community.spotify.com/t5/iOS-iPhone-iPad/Duplicates-of-the-same-albums/td-p/4542505))
* auto-skip songs by rating, type (instrumental, live, etc), and more
* shuffle with priority (higher rated songs more likely to be played sooner)

### How does it work?

Kotify uses Spotify's [web API](https://developer.spotify.com/documentation/web-api/) to retrieve
artist/album/song information, manage your library, and play music.

After installing, the application will request permission from Spotify to read and modify your
library. This one-time process will open Spotify in your web browser, and uses the
[OAuth protocol](https://developer.spotify.com/documentation/general/guides/authorization-guide/).
You may need to log in to Spotify in your web browser to grant permission, but you do not need to
provide your Spotify password to Kotify.

Once installed and authorized, Kotify can replace using Spotify's desktop client. However, Kotify
cannot directly play music from Spotify. You'll need to keep Spotify's client (or another device,
like a phone) running in the background through which Kotify can play music.

### Installation

Kotify isn't ready for general use yet. It can be built from source by running

```
./gradlew run
```

in the root directory. JDK 18 is required.

### Why "Kotify"?

The name Kotify is a play on Spotify and [Kotlin](https://kotlinlang.org/), the language this
project is written in. It also likens to "codify", meaning to systematize.
