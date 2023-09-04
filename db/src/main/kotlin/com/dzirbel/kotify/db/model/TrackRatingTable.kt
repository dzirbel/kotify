package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.StringIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object TrackRatingTable : Table("track_rating") {
    val track: Column<String> = varchar("track", StringIdTable.STRING_ID_LENGTH)
    val rating: Column<Int> = integer("rating")
    val maxRating: Column<Int> = integer("max_rating")
    val rateTime: Column<Instant> = timestamp("rate_time")
    val userId: Column<String> = varchar("user_id", StringIdTable.STRING_ID_LENGTH)

    init {
        index(isUnique = false, track, userId)
    }
}
