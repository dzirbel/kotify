package com.dzirbel.kotify.db.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameAs
import com.dzirbel.kotify.db.DatabaseExtension
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.blockingTransaction
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.sql.SQLException

@ExtendWith(DatabaseExtension::class)
class ArtistAlbumTest {
    @Test
    fun `findOrCreate returns the same entity`() {
        KotifyDatabase.blockingTransaction {
            val artistAlbum = ArtistAlbum.findOrCreate(
                artistId = "artist",
                albumId = "album",
                albumGroup = AlbumType.ALBUM,
            )

            assertThat(artistAlbum.albumGroup).isEqualTo(AlbumType.ALBUM)

            assertThat(ArtistAlbum.findOrCreate(artistId = "artist", albumId = "album", albumGroup = null))
                .isSameAs(artistAlbum)

            // album group is not updated when null is provided
            assertThat(artistAlbum.albumGroup).isEqualTo(AlbumType.ALBUM)

            ArtistAlbum.findOrCreate(artistId = "artist", albumId = "album", albumGroup = AlbumType.APPEARS_ON)

            assertThat(artistAlbum.albumGroup).isEqualTo(AlbumType.APPEARS_ON)
        }
    }

    @Test
    fun `album and artist must be unique`() {
        KotifyDatabase.blockingTransaction {
            ArtistAlbumTable.insert { statement ->
                statement[album] = EntityID("album", AlbumTable)
                statement[artist] = EntityID("artist", ArtistTable)
                statement[albumGroup] = AlbumType.ALBUM
            }

            assertThrows<SQLException> {
                ArtistAlbumTable.insert { statement ->
                    statement[album] = EntityID("album", AlbumTable)
                    statement[artist] = EntityID("artist", ArtistTable)
                    statement[albumGroup] = AlbumType.APPEARS_ON
                }
            }
        }
    }
}
