package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.StringIdTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object TrackRatingTable : IntIdTable() {
    val track: Column<EntityID<String>> = reference("track", TrackTable)
    val rating: Column<Int> = integer("rating")
    val maxRating: Column<Int> = integer("max_rating")
    val rateTime: Column<Instant> = timestamp("rate_time")
    val userId: Column<String> = varchar("user_id", StringIdTable.STRING_ID_LENGTH)
}
