package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.db.SpotifyEntityClass
import com.dzirbel.kotify.db.SpotifyEntityTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

object UserTable : SpotifyEntityTable(name = "users") {
    val followersTotal: Column<Int?> = integer("followers_total").nullable()
    val email: Column<String?> = text("email").nullable()

    object CurrentUserTable : Table(name = "current_user") {
        val userId: Column<String?> = varchar("user_id", STRING_ID_LENGTH).nullable().uniqueIndex()

        fun get(): String? = selectAll().firstOrNull()?.get(userId)

        fun set(userId: String) {
            deleteAll()
            insert { it[CurrentUserTable.userId] = userId }
        }

        fun clear() {
            deleteAll()
        }
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

    var images: SizedIterable<Image> by Image via UserTable.UserImageTable

    companion object : SpotifyEntityClass<User>(UserTable)
}
