package com.dzirbel.kotify.db.model

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.and

object ArtistAlbumTable : IntIdTable() {
    val album = reference("album", AlbumTable)
    val artist = reference("artist", ArtistTable)
    val albumGroup = enumeration("album_group", AlbumType::class).nullable()

    init {
        uniqueIndex(album, artist)
    }
}

class ArtistAlbum(id: EntityID<Int>) : IntEntity(id) {
    var albumGroup: AlbumType? by ArtistAlbumTable.albumGroup
    var albumId: EntityID<String> by ArtistAlbumTable.album
    var artistId: EntityID<String> by ArtistAlbumTable.artist

    var artist: Artist by Artist referencedOn ArtistAlbumTable.artist
    var album: Album by Album referencedOn ArtistAlbumTable.album

    companion object : IntEntityClass<ArtistAlbum>(ArtistAlbumTable) {
        fun findOrCreate(artistId: String, albumId: String, albumGroup: AlbumType?): ArtistAlbum {
            return find { (ArtistAlbumTable.album eq albumId) and (ArtistAlbumTable.artist eq artistId) }
                .firstOrNull()
                ?.also { artistAlbum ->
                    if (albumGroup != null && artistAlbum.albumGroup != albumGroup) {
                        artistAlbum.albumGroup = albumGroup
                    }
                }
                ?: ArtistAlbum.new {
                    this.albumId = EntityID(id = albumId, table = AlbumTable)
                    this.artistId = EntityID(id = artistId, table = ArtistTable)
                    this.albumGroup = albumGroup
                }
        }
    }
}
