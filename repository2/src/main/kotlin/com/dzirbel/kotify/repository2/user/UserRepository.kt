package com.dzirbel.kotify.repository2.user

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Image
import com.dzirbel.kotify.db.model.User
import com.dzirbel.kotify.db.model.UserTable
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.PrivateSpotifyUser
import com.dzirbel.kotify.network.model.SpotifyUser
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.repository.savedRepositories
import com.dzirbel.kotify.repository.user.UserRepository
import com.dzirbel.kotify.repository2.CacheState
import com.dzirbel.kotify.repository2.DatabaseEntityRepository
import com.dzirbel.kotify.repository2.Repository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant

object UserRepository : DatabaseEntityRepository<User, SpotifyUser>(User) {
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?>
        get() = _currentUserId

    private val _currentUser = MutableStateFlow<CacheState<User>?>(null)
    val currentUser: StateFlow<CacheState<User>?>
        get() = _currentUser

    val requireCurrentUserId: String
        get() = requireNotNull(_currentUserId.value) { "missing current user ID" }

    override suspend fun fetchFromRemote(id: String) = Spotify.UsersProfile.getUser(userId = id)

    override fun convert(id: String, networkModel: SpotifyUser): User {
        return User.updateOrInsert(id = id, networkModel = networkModel) {
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

    fun onConnectToDatabase() {
        _currentUserId.value = UserTable.CurrentUserTable.get()
    }

    // TODO revisit
    fun ensureCurrentUserLoaded() {
        if (currentUser.value?.cachedValue != null) return

        Repository.scope.launch {
            val cachedUser = currentUserId.value?.let { userId ->
                KotifyDatabase.transaction("load current user") { fetchFromDatabase(userId)?.first }
            }

            val cachedUpdateTime = cachedUser?.fullUpdatedTime
            if (cachedUpdateTime != null) {
                _currentUser.value = CacheState.Loaded(cachedValue = cachedUser, cacheTime = cachedUpdateTime)
            } else {
                _currentUser.value = CacheState.Refreshing()

                val remoteUser = try {
                    getCurrentUserRemote()
                } catch (cancellationException: CancellationException) {
                    _currentUser.value = CacheState.Error(cancellationException)
                    throw cancellationException
                } catch (throwable: Throwable) {
                    _currentUser.value = CacheState.Error(throwable)
                    @Suppress("LabeledExpression")
                    return@launch
                }

                _currentUser.value = if (remoteUser == null) {
                    CacheState.NotFound()
                } else {
                    _currentUserId.value = remoteUser.id.value

                    CacheState.Loaded(
                        cachedValue = remoteUser,
                        cacheTime = remoteUser.updatedTime,
                    )
                }
            }
        }
    }

    fun signOut() {
        Repository.scope.launch {
            // clear access token first to immediately show unauthenticated screen
            AccessToken.Cache.clear()

            _currentUserId.value = null
            _currentUser.value = null

            savedRepositories.forEach { it.invalidateAll() }

            KotifyDatabase.transaction("clear current user id") {
                UserTable.CurrentUserTable.clear()
            }
        }
    }

    private suspend fun getCurrentUserRemote(): User? {
        val user = Spotify.UsersProfile.getCurrentUser()
        return KotifyDatabase.transaction("set current user") {
            UserTable.CurrentUserTable.set(user.id)

            // ensure legacy repository has updated value
            UserRepository.currentUserId.loadToCache()

            User.from(user)
        }
    }
}
