package com.dzirbel.kotify.repository.user

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Image
import com.dzirbel.kotify.db.model.User
import com.dzirbel.kotify.db.model.UserTable
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.PrivateSpotifyUser
import com.dzirbel.kotify.network.model.SpotifyUser
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.repository.CacheState
import com.dzirbel.kotify.repository.DatabaseEntityRepository
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.repository.savedRepositories
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant

open class UserRepository internal constructor(
    private val applicationScope: CoroutineScope,
    private val userSessionScope: CoroutineScope,
) : DatabaseEntityRepository<User, SpotifyUser>(entityClass = User, scope = applicationScope) {

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?>
        get() = _currentUserId

    private val _currentUser = MutableStateFlow<CacheState<User>?>(null)
    val currentUser: StateFlow<CacheState<User>?>
        get() = _currentUser

    val hasCurrentUserId: Boolean
        get() = _currentUserId.value != null

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

        userSessionScope.launch {
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

                _currentUserId.value = remoteUser.id.value

                _currentUser.value = CacheState.Loaded(
                    cachedValue = remoteUser,
                    cacheTime = remoteUser.updatedTime,
                )
            }
        }
    }

    fun signOut() {
        applicationScope.launch {
            Repository.userSessionScope.coroutineContext.cancelChildren()

            // clear access token first to immediately show unauthenticated screen
            AccessToken.Cache.clear()

            _currentUserId.value = null
            _currentUser.value = null

            savedRepositories.forEach { it.invalidateUser() }

            // TODO also reset Player state

            KotifyDatabase.transaction("clear current user id") {
                UserTable.CurrentUserTable.clear()
            }
        }
    }

    private suspend fun getCurrentUserRemote(): User {
        val user = Spotify.UsersProfile.getCurrentUser()
        return KotifyDatabase.transaction("set current user") {
            UserTable.CurrentUserTable.set(user.id)

            convert(id = user.id, networkModel = user)
        }
    }

    companion object : UserRepository(
        applicationScope = Repository.applicationScope,
        userSessionScope = Repository.userSessionScope,
    )
}
