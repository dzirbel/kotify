package com.dzirbel.kotify.repository2.player

/**
 * Represents the state of the playing position in a currently playing track.
 */
sealed interface TrackPosition {
    /**
     * Fetches the live position in the track, in milliseconds, based on the current system time (i.e. values may change
     * for different reads).
     */
    val currentPositionMs: Int

    /**
     * The state when the track is in the process of being seeked to [positionMs].
     */
    data class Seeking(val positionMs: Int) : TrackPosition {
        override val currentPositionMs: Int
            get() = positionMs
    }

    /**
     * The state of the track at a particular time when it was fetched from the remote source.
     *
     * In particular, this state is not updated "live" (although reads to [currentPositionMs] are), it only captures the
     * moment in time (specifically, [fetchedPositionMs], an estimate or returned Unix time when the state was read) and
     * the play position at that time, [fetchedPositionMs].
     */
    data class Fetched(val fetchedTimestamp: Long, val fetchedPositionMs: Int, val playing: Boolean) : TrackPosition {
        // TODO verify
        override val currentPositionMs: Int
            get() = positionRelativeTo(System.currentTimeMillis())

        private fun positionRelativeTo(timestamp: Long): Int {
            return if (playing) {
                (timestamp - fetchedTimestamp).toInt() + fetchedPositionMs
            } else {
                fetchedPositionMs
            }
        }

        fun play(playTimestamp: Long): TrackPosition {
            return Fetched(
                fetchedTimestamp = playTimestamp,
                fetchedPositionMs = positionRelativeTo(playTimestamp),
                playing = false,
            )
        }

        fun pause(pauseTimestamp: Long): TrackPosition {
            return Fetched(
                fetchedTimestamp = pauseTimestamp,
                fetchedPositionMs = positionRelativeTo(pauseTimestamp),
                playing = false,
            )
        }
    }
}
