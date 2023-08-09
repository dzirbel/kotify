package com.dzirbel.kotify.db.model

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object GlobalUpdateTimesTable : Table(name = "global_update_times") {
    val key: Column<String> = varchar(name = "key", length = 64).uniqueIndex()
    val updateTime: Column<Instant> = timestamp(name = "update_time")
    override val primaryKey = PrimaryKey(key)
}
