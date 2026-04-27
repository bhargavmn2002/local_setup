package com.signox.player.data.dto

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * CMS / Prisma may emit `speed` as int, double, or occasionally string. Default Gson + Kotlin
 * can leave [ScrollTextConfigDto.speed] null or wrong; this keeps px/sec aligned with the web player.
 */
class ScrollTextConfigDtoDeserializer : JsonDeserializer<ScrollTextConfigDto> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?,
    ): ScrollTextConfigDto {
        val o = json?.takeIf { it.isJsonObject }?.asJsonObject
            ?: return ScrollTextConfigDto()

        fun pickString(key: String, default: String = ""): String {
            val e = o.get(key) ?: return default
            if (e.isJsonNull || !e.isJsonPrimitive) return default
            return e.asJsonPrimitive.asString
        }

        fun pickInt(key: String, default: Int): Int {
            val e = o.get(key) ?: return default
            if (e.isJsonNull) return default
            val p = e.asJsonPrimitive
            return when {
                p.isNumber -> p.asInt
                p.isString -> p.asString.trim().toIntOrNull() ?: default
                else -> default
            }
        }

        fun pickSpeed(): Double? {
            val e = o.get("speed") ?: return null
            if (e.isJsonNull) return null
            val p = e.asJsonPrimitive
            return when {
                p.isNumber -> p.asDouble
                p.isString -> p.asString.trim().toDoubleOrNull()
                else -> null
            }
        }

        return ScrollTextConfigDto(
            text = pickString("text"),
            direction = pickString("direction", "left-to-right"),
            speed = pickSpeed(),
            fontSize = pickInt("fontSize", 24),
            fontWeight = pickString("fontWeight", "normal"),
            textColor = pickString("textColor", "#000000"),
            backgroundColor = pickString("backgroundColor", "transparent"),
        )
    }
}
