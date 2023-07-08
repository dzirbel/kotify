package com.dzirbel.kotify.repository.user

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.ReadWriteCachedProperty
import com.dzirbel.kotify.db.model.User
import com.dzirbel.kotify.db.model.UserTable
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SpotifyUser
import com.dzirbel.kotify.repository.DatabaseRepository

object UserRepository : DatabaseRepository<User, SpotifyUser>(User) {
    val currentUserId: ReadWriteCachedProperty<String?> = ReadWriteCachedProperty(
        getter = { UserTable.CurrentUserTable.get() },
        setter = { id ->
            if (id == null) {
                UserTable.CurrentUserTable.clear()
            } else {
                UserTable.CurrentUserTable.set(id)
            }
        },
    )

    override suspend fun fetch(id: String) = Spotify.UsersProfile.getUser(userId = id)

    suspend fun getCurrentUserCached(): User? {
        return currentUserId.cached
            ?.let { getCached(id = it) }
            ?.takeIf { it.fullUpdatedTime != null }
    }

    suspend fun getCurrentUserRemote(): User? {
        val user = Spotify.UsersProfile.getCurrentUser()
        return KotifyDatabase.transaction("set current user") {
            currentUserId.set(user.id)
            User.from(user)
        }
    }

    suspend fun getCurrentUser(): User? = getCurrentUserCached() ?: getCurrentUserRemote()
}
