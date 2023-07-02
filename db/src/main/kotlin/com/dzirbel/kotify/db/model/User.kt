package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.ReadOnlyCachedProperty
import com.dzirbel.kotify.db.ReadWriteCachedProperty
import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.db.SpotifyEntityClass
import com.dzirbel.kotify.db.SpotifyEntityTable
import com.dzirbel.kotify.db.cachedAsList
import com.dzirbel.kotify.db.cachedReadOnly
import com.dzirbel.kotify.db.util.smallest
import com.dzirbel.kotify.network.model.PrivateSpotifyUser
import com.dzirbel.kotify.network.model.SpotifyUser
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import java.time.Instant

object UserTable : SpotifyEntityTable(name = "users") {
    val followersTotal: Column<Int?> = integer("followers_total").nullable()
    val email: Column<String?> = text("email").nullable()

    object CurrentUserTable : Table(name = "current_user") {
        val userId: Column<String?> = varchar("user_id", STRING_ID_LENGTH).nullable().uniqueIndex()
    }

    object UserImageTable : Table() {
        val user = reference("user", UserTable)
        val image = reference("image", ImageTable)
        override val primaryKey = PrimaryKey(user, image)
    }
}

class User(id: EntityID<String>) : SpotifyEntity(id = id, table = UserTable) {
    var followersTotal: Int? by UserTable.followersTotal
    var email: String? by UserTable.email

    val images: ReadWriteCachedProperty<List<Image>> by (Image via UserTable.UserImageTable).cachedAsList()
    val thumbnailImage: ReadOnlyCachedProperty<Image?> by (Image via UserTable.UserImageTable)
        .cachedReadOnly { it.smallest() }

    companion object : SpotifyEntityClass<User, SpotifyUser>(UserTable) {
        override fun User.update(networkModel: SpotifyUser) {
            networkModel.images?.let { images ->
                this.images.set(images.map { Image.from(it) })
            }

            networkModel.followers?.let {
                followersTotal = it.total
            }

            if (networkModel is PrivateSpotifyUser) {
                fullUpdatedTime = Instant.now()
                email = networkModel.email
            }
        }
    }
}
