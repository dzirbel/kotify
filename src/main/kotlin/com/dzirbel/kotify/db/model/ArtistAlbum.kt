package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.ReadWriteCachedProperty
import com.dzirbel.kotify.db.cached
import com.dzirbel.kotify.network.model.SpotifyAlbum
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.and

object ArtistAlbumTable : IntIdTable() {
    val album = reference("album", AlbumTable)
    val artist = reference("artist", ArtistTable)
    val albumGroup = enumeration("album_group", SpotifyAlbum.Type::class).nullable()
}

class ArtistAlbum(id: EntityID<Int>) : IntEntity(id) {
    var albumGroup: SpotifyAlbum.Type? by ArtistAlbumTable.albumGroup
    var albumId: EntityID<String> by ArtistAlbumTable.album
    var artistId: EntityID<String> by ArtistAlbumTable.artist

    val artist: ReadWriteCachedProperty<Artist> by (Artist referencedOn ArtistAlbumTable.artist).cached()
    val album: ReadWriteCachedProperty<Album> by (Album referencedOn ArtistAlbumTable.album).cached()

    companion object : IntEntityClass<ArtistAlbum>(ArtistAlbumTable) {
        private fun recordFor(artistId: String, albumId: String): ArtistAlbum {
            return find {
                @Suppress("UnnecessaryParentheses")
                (ArtistAlbumTable.album eq albumId) and (ArtistAlbumTable.artist eq artistId)
            }
                .firstOrNull()
                ?: ArtistAlbum.new {
                    this.albumId = EntityID(id = albumId, table = AlbumTable)
                    this.artistId = EntityID(id = artistId, table = ArtistTable)
                }
        }

        fun from(artistId: String, albumId: String, albumGroup: SpotifyAlbum.Type?): ArtistAlbum {
            return recordFor(artistId = artistId, albumId = albumId)
                .also { it.albumGroup = albumGroup ?: it.albumGroup }
        }
    }
}
