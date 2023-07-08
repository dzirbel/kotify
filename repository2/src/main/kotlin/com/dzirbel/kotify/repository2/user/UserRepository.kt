package com.dzirbel.kotify.repository2.user

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.User
import com.dzirbel.kotify.db.model.UserTable
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SpotifyUser
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.repository.savedRepositories
import com.dzirbel.kotify.repository2.CacheState
import com.dzirbel.kotify.repository2.DatabaseRepository
import com.dzirbel.kotify.repository2.Repository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll

object UserRepository : Repository<User> by object : DatabaseRepository<User, SpotifyUser>(User) {
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?>
        get() = _currentUserId

    private val _currentUser = MutableStateFlow<CacheState<User>?>(null)
    val currentUser: StateFlow<CacheState<User>?>
        get() = _currentUser

    val requireCurrentUserId: String
        get() = requireNotNull(_currentUserId.value) { "missing current user ID" }

    fun ensureCurrentUserLoaded() {
        Repository.scope.launch {
            val userId = getCachedCurrentUserId()
            if (userId != null) {
                val cachedUser = getCached(id = userId)
                val cachedUpdateTime = cachedUser?.fullUpdatedTime
                if (cachedUpdateTime != null) {
                    _currentUser.value = CacheState.Loaded(cachedValue = cachedUser, cacheTime = cachedUpdateTime)
                } else {
                    _currentUser.value = CacheState.Refreshing()

                    val cacheState = try {
                        val remoteUser = getCurrentUserRemote()
                        if (remoteUser == null) {
                            CacheState.NotFound()
                        } else {
                            CacheState.Loaded(
                                cachedValue = remoteUser,
                                cacheTime = remoteUser.updatedTime,
                            )
                        }
                    } catch (cancellationException: CancellationException) {
                        _currentUser.value = CacheState.Error(cancellationException)
                        throw cancellationException
                    } catch (throwable: Throwable) {
                        CacheState.Error(throwable)
                    }

                    _currentUser.value = cacheState
                }
            }
        }
    }

    fun signOut() {
        Repository.scope.launch {
            // TODO ordering?
            KotifyDatabase.transaction("clear current user id") {
                UserTable.CurrentUserTable.deleteAll()
            }
            _currentUserId.value = null
            _currentUser.value = null
            savedRepositories.forEach { it.invalidateAll() }
            AccessToken.Cache.clear()
        }
    }

    private suspend fun getCachedCurrentUserId(): String? {
        return _currentUserId.value
            ?: KotifyDatabase.transaction(name = "load current user id") {
                UserTable.CurrentUserTable.selectAll().firstOrNull()?.get(UserTable.CurrentUserTable.userId)
            }
                .also { _currentUserId.value = it }
    }

    private suspend fun getCurrentUserRemote(): User? {
        val user = Spotify.UsersProfile.getCurrentUser()
        return KotifyDatabase.transaction("set current user") { User.from(user) }
    }

    override suspend fun fetch(id: String) = Spotify.UsersProfile.getUser(userId = id)
}
