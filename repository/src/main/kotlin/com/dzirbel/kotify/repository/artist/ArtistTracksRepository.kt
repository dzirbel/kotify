package com.dzirbel.kotify.repository.artist

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.db.DB
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.TrackTable
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.repository.util.SynchronizedWeakStateFlowMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * A local-only repository mapping artist IDs to the set of track IDs for that artist.
 *
 * This is not a full [Repository] because it does not implement fetching from a remote source (since there is no
 * endpoint to get the set of tracks by an artist).
 *
 * TODO make it regular Repository to allow reuse of extensions
 */
@Stable
open class ArtistTracksRepository internal constructor(private val scope: CoroutineScope) {

    // map from artist ID to the set of track IDs for that artist
    private val artistTrackStates = SynchronizedWeakStateFlowMap<String, Set<String>>()

    /**
     * Sets the artist [artistIds] for the track with the given [trackId].
     */
    fun setTrackArtists(trackId: String, artistIds: Iterable<String>) {
        TrackTable.TrackArtistTable.setTrackArtists(trackId, artistIds)

        // note: does not remove trackId from other artists which may have had it previously, but transferring a track
        // to a new artist should generally not happen
        for (artistId in artistIds) {
            artistTrackStates.updateValue(artistId) { it?.plus(trackId) }
        }
    }

    /**
     * Returns a [StateFlow] representing the live state of the tracks for the artist with the given [artistId].
     */
    fun artistTracksStateOf(artistId: String): StateFlow<Set<String>?> {
        Repository.checkEnabled()
        return artistTrackStates.getOrCreateStateFlow(artistId) {
            scope.launch {
                val trackIds = KotifyDatabase[DB.CACHE].transaction(name = "load artist $artistId tracks") {
                    TrackTable.TrackArtistTable.trackIdsForArtist(artistId = artistId)
                }

                artistTrackStates.updateValue(artistId, trackIds)
            }
        }
    }

    /**
     * Returns a batch of [StateFlow]s representing the live states of the tracks for the artists with the given
     * [artistIds].
     */
    fun artistTracksStatesOf(artistIds: Iterable<String>): List<StateFlow<Set<String>?>> {
        Repository.checkEnabled()
        return artistTrackStates.getOrCreateStateFlows(keys = artistIds) { createdIds ->
            scope.launch {
                val tracksByArtist = KotifyDatabase[DB.CACHE].transaction(
                    name = "load tracks for ${createdIds.size} artists",
                ) {
                    createdIds.mapValues { (artistId, _) ->
                        TrackTable.TrackArtistTable.trackIdsForArtist(artistId = artistId)
                    }
                }

                tracksByArtist.forEach { (artistId, trackIds) -> artistTrackStates.updateValue(artistId, trackIds) }
            }
        }
    }

    companion object : ArtistTracksRepository(scope = Repository.applicationScope)
}
