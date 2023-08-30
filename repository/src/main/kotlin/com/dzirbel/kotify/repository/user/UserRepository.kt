package com.dzirbel.kotify.repository.user

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Image
import com.dzirbel.kotify.db.model.User
import com.dzirbel.kotify.db.model.UserTable
import com.dzirbel.kotify.db.util.sized
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.PrivateSpotifyUser
import com.dzirbel.kotify.network.model.SpotifyUser
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.repository.CacheState
import com.dzirbel.kotify.repository.DataSource
import com.dzirbel.kotify.repository.DatabaseEntityRepository
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.repository.RequestLog
import com.dzirbel.kotify.repository.savedRepositories
import com.dzirbel.kotify.repository.util.midpointInstantToNow
import com.dzirbel.kotify.repository.util.updateOrInsert
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.time.TimeSource

open class UserRepository internal constructor(
    private val applicationScope: CoroutineScope,
    private val userSessionScope: CoroutineScope,
) : DatabaseEntityRepository<UserViewModel, User, SpotifyUser>(entityClass = User, scope = applicationScope) {

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?>
        get() = _currentUserId

    private val _currentUser = MutableStateFlow<CacheState<UserViewModel>?>(null)
    val currentUser: StateFlow<CacheState<UserViewModel>?>
        get() = _currentUser

    val hasCurrentUserId: Boolean
        get() = _currentUserId.value != null

    val requireCurrentUserId: String
        get() = requireNotNull(_currentUserId.value) { "missing current user ID" }

    override suspend fun fetchFromRemote(id: String) = Spotify.UsersProfile.getUser(userId = id)

    override fun convertToDB(networkModel: SpotifyUser, fetchTime: Instant): User {
        return convertToDB(id = networkModel.id, networkModel = networkModel, fetchTime = fetchTime)
    }

    override fun convertToDB(id: String, networkModel: SpotifyUser, fetchTime: Instant): User {
        return User.updateOrInsert(id = id, networkModel = networkModel, fetchTime = fetchTime) {
            networkModel.images?.let { images ->
                this.images = images
                    .map { Image.findOrCreate(url = it.url, width = it.width, height = it.height) }
                    .sized()
            }

            networkModel.followers?.let {
                followersTotal = it.total
            }

            if (networkModel is PrivateSpotifyUser) {
                fullUpdatedTime = fetchTime
                email = networkModel.email
            }
        }
    }

    override fun convertToVM(databaseModel: User) = UserViewModel(databaseModel)

    fun onConnectToDatabase() {
        _currentUserId.value = UserTable.CurrentUserTable.get()
    }

    // TODO revisit
    fun ensureCurrentUserLoaded() {
        if (currentUser.value?.cachedValue != null) return

        val requestLog = RequestLog(log = mutableLog)
        userSessionScope.launch {
            val dbStart = TimeSource.Monotonic.markNow()
            val cachedUser = currentUserId.value?.let { userId ->
                KotifyDatabase.transaction("load current user") { fetchFromDatabase(userId)?.first }
            }
            requestLog.addDbTime(dbStart.elapsedNow())

            val cachedUpdateTime = cachedUser?.fullUpdatedTime
            if (cachedUpdateTime != null) {
                // TODO use cache strategy for current user
                _currentUser.value = CacheState.Loaded(
                    cachedValue = UserViewModel(cachedUser),
                    cacheTime = cachedUpdateTime,
                )
                requestLog.success("loaded current user from database", DataSource.DATABASE)
            } else {
                _currentUser.value = CacheState.Refreshing()

                val remoteStart = TimeSource.Monotonic.markNow()
                val remoteUser = try {
                    getCurrentUserRemote()
                } catch (cancellationException: CancellationException) {
                    _currentUser.value = CacheState.Error(cancellationException)
                    throw cancellationException
                } catch (throwable: Throwable) {
                    _currentUser.value = CacheState.Error(throwable)
                    requestLog
                        .addRemoteTime(remoteStart.elapsedNow())
                        .error("error loading current user from remote", DataSource.REMOTE, throwable)
                    @Suppress("LabeledExpression")
                    return@launch
                }

                requestLog.addRemoteTime(remoteStart.elapsedNow())

                _currentUserId.value = remoteUser.id.value

                _currentUser.value = CacheState.Loaded(
                    cachedValue = UserViewModel(remoteUser),
                    cacheTime = remoteUser.updatedTime,
                )

                requestLog.success("loaded current user from remote", DataSource.REMOTE)
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
        val start = TimeSource.Monotonic.markNow()
        val user = Spotify.UsersProfile.getCurrentUser()
        val fetchTime = start.midpointInstantToNow()
        return KotifyDatabase.transaction("set current user") {
            UserTable.CurrentUserTable.set(user.id)

            convertToDB(networkModel = user, fetchTime = fetchTime)
        }
    }

    companion object : UserRepository(
        applicationScope = Repository.applicationScope,
        userSessionScope = Repository.userSessionScope,
    )
}
