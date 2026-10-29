package com.hanan.mysavings.ui.navigation

import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.util.Base64

/**
 * Lets a `@Serializable` class be a type-safe navigation argument. Navigation only knows
 * primitives, strings, enums and their lists; anything richer is stored as JSON.
 */
inline fun <reified T : Any> serializableNavType(): NavType<T> = SerializableNavType(serializer())

class SerializableNavType<T : Any>(
    private val serializer: KSerializer<T>,
) : NavType<T>(isNullableAllowed = false) {

    override fun get(bundle: Bundle, key: String): T? =
        bundle.getString(key)?.let { Json.decodeFromString(serializer, it) }

    override fun put(bundle: Bundle, key: String, value: T) =
        bundle.putString(key, Json.encodeToString(serializer, value))

    /**
     * The value is embedded in the route string. URL-safe Base64 (A-Z, a-z, 0-9, '-', '_')
     * is unaffected by URL encoding and decoding, so user text such as "Kopi & Roti 100%"
     * cannot be corrupted however many times Navigation decodes the route.
     */
    override fun serializeAsValue(value: T): String =
        base64Encoder.encodeToString(Json.encodeToString(serializer, value).toByteArray())

    override fun parseValue(value: String): T =
        Json.decodeFromString(serializer, String(base64Decoder.decode(value)))

    private companion object {
        val base64Encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
        val base64Decoder: Base64.Decoder = Base64.getUrlDecoder()
    }
}
