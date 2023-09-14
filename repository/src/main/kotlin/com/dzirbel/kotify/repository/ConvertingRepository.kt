package com.dzirbel.kotify.repository

import com.dzirbel.kotify.network.model.SpotifyObject
import java.time.Instant

interface ConvertingRepository<Database, Network> {

    /**
     * Converts the given [networkModel] with the given [id] into a [Database], saving it in the local database.
     *
     * Must be called from within a database transaction.
     */
    fun convertToDB(id: String, networkModel: Network, fetchTime: Instant): Database
}

fun <Database, Network : SpotifyObject> ConvertingRepository<Database, Network>.convertToDB(
    networkModel: Network,
    fetchTime: Instant,
): Database? {
    return networkModel.id?.let { convertToDB(id = it, networkModel = networkModel, fetchTime = fetchTime) }
}
