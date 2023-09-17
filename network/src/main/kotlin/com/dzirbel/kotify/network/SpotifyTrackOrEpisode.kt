package com.dzirbel.kotify.network

import com.dzirbel.kotify.Runtime
import com.dzirbel.kotify.network.model.FullSpotifyEpisode
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SimplifiedSpotifyEpisode
import com.dzirbel.kotify.network.model.SimplifiedSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyObject
import com.dzirbel.kotify.network.util.json
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable(SimplifiedSpotifyTrackOrEpisode.Serializer::class)
interface SimplifiedSpotifyTrackOrEpisode : SpotifyObject {
    val durationMs: Long

    object Serializer : KSerializer<SimplifiedSpotifyTrackOrEpisode> {
        override val descriptor = JsonElement.serializer().descriptor

        override fun serialize(encoder: Encoder, value: SimplifiedSpotifyTrackOrEpisode) {
            when (value) {
                is SimplifiedSpotifyTrack ->
                    encoder.encodeSerializableValue(SimplifiedSpotifyTrack.serializer(), value)

                is SimplifiedSpotifyEpisode ->
                    encoder.encodeSerializableValue(SimplifiedSpotifyEpisode.serializer(), value)

                else -> throw SerializationException("unexpected type ${value::class}")
            }
        }

        override fun deserialize(decoder: Decoder): SimplifiedSpotifyTrackOrEpisode {
            val jsonElement = decoder.decodeSerializableValue(JsonElement.serializer())
            if (jsonElement !is JsonObject) throw SerializationException("expected JsonObject")

            val json = Runtime.json

            // hack: in playlists, the spotify API often returns a track object for podcast episodes, with its "type" as
            // "episode" but the fields matching the track model; use these "track"/"episode" fields to override "type"
            // since they appear to be more accurate
            if (jsonElement.getPrimitiveContent("track") == "true") {
                return json.decodeFromJsonElement<SimplifiedSpotifyTrack>(jsonElement)
            }
            if (jsonElement.getPrimitiveContent("episode") == "true") {
                return json.decodeFromJsonElement<SimplifiedSpotifyEpisode>(jsonElement)
            }

            return when (val type = jsonElement.getPrimitiveContent("type")) {
                "track" -> json.decodeFromJsonElement<SimplifiedSpotifyTrack>(jsonElement)
                "episode" -> json.decodeFromJsonElement<SimplifiedSpotifyEpisode>(jsonElement)
                else -> throw SerializationException("unexpected type $type")
            }
        }
    }
}

@Serializable(FullSpotifyTrackOrEpisode.Serializer::class)
interface FullSpotifyTrackOrEpisode : SpotifyObject {
    val durationMs: Long

    object Serializer : KSerializer<FullSpotifyTrackOrEpisode> {
        override val descriptor = JsonElement.serializer().descriptor

        override fun serialize(encoder: Encoder, value: FullSpotifyTrackOrEpisode) {
            when (value) {
                is FullSpotifyTrack ->
                    encoder.encodeSerializableValue(FullSpotifyTrack.serializer(), value)

                is FullSpotifyEpisode ->
                    encoder.encodeSerializableValue(FullSpotifyEpisode.serializer(), value)

                else -> throw SerializationException("unexpected type ${value::class}")
            }
        }

        override fun deserialize(decoder: Decoder): FullSpotifyTrackOrEpisode {
            val jsonElement = decoder.decodeSerializableValue(JsonElement.serializer())
            if (jsonElement !is JsonObject) throw SerializationException("expected JsonObject")

            val json = Runtime.json

            return when (val type = jsonElement.getPrimitiveContent("type")) {
                "track" -> json.decodeFromJsonElement<FullSpotifyTrack>(jsonElement)
                "episode" -> json.decodeFromJsonElement<FullSpotifyEpisode>(jsonElement)
                else -> throw SerializationException("unexpected type $type")
            }
        }
    }
}

private fun JsonObject.getPrimitiveContent(key: String): String? {
    val value = this[key] ?: return null
    val primitive = value as? JsonPrimitive ?: throw SerializationException("expected primitive, was $value")
    return primitive.content
}
