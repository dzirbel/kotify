package com.dzirbel.kotify.db.model

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
