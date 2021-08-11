package dev.brella.blaseball.finally

import kotlinx.serialization.json.*
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline operator fun JsonObjectBuilder.set(key: String, value: Int) = put(key, value)
inline operator fun JsonObjectBuilder.set(key: String, value: Long) = put(key, value)
inline operator fun JsonObjectBuilder.set(key: String, value: String) = put(key, value)
inline operator fun JsonObjectBuilder.set(key: String, value: Boolean) = put(key, value)
inline operator fun JsonObjectBuilder.set(key: String, value: JsonElement) = put(key, value)

public inline fun JsonObject.getJsonObject(key: String) =
    getValue(key).jsonObject

public inline fun JsonObject.getJsonArray(key: String) =
    getValue(key).jsonArray

public inline fun JsonObject.getJsonPrimitive(key: String) =
    getValue(key).jsonPrimitive

public inline fun JsonObject.getString(key: String) =
    getValue(key).jsonPrimitive.content

public inline fun JsonObject.getInt(key: String) =
    getValue(key).jsonPrimitive.int

public inline fun JsonObject.getLong(key: String) =
    getValue(key).jsonPrimitive.long


public inline fun JsonObject.getJsonObjectOrNull(key: String) =
    get(key) as? JsonObject

public inline fun JsonObject.getJsonArrayOrNull(key: String) =
    get(key) as? JsonArray

public inline fun JsonObject.getJsonPrimitiveOrNull(key: String) =
    (get(key) as? JsonPrimitive)

public inline fun JsonObject.getStringOrNull(key: String) =
    (get(key) as? JsonPrimitive)?.contentOrNull

public inline fun JsonObject.getIntOrNull(key: String) =
    (get(key) as? JsonPrimitive)?.intOrNull

public inline fun JsonObject.getLongOrNull(key: String) =
    (get(key) as? JsonPrimitive)?.longOrNull

public inline fun JsonObject.getDoubleOrNull(key: String) =
    (get(key) as? JsonPrimitive)?.doubleOrNull

public inline fun JsonObject.getDoubleOrNull(vararg keys: String) =
    keys.firstNotNullOfOrNull { (get(it) as? JsonPrimitive)?.doubleOrNull }

public inline fun JsonObject.getBooleanOrNull(key: String) =
    (get(key) as? JsonPrimitive)?.booleanOrNull

inline fun string(map: Map<String, JsonElement>): ReadOnlyProperty<Any?, String> =
    ReadOnlyProperty { thisRef, property -> map.getValue(property.name).jsonPrimitive.content }

inline fun string(map: MutableMap<String, JsonElement>): ReadWriteProperty<Any?, String> =
    object: ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): String =
            map.getValue(property.name).jsonPrimitive.content

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            map[property.name] = JsonPrimitive(value)
        }
    }

inline fun int(map: Map<String, JsonElement>): ReadOnlyProperty<Any?, Int> =
    ReadOnlyProperty { thisRef, property -> map.getValue(property.name).jsonPrimitive.int }

inline fun int(map: MutableMap<String, JsonElement>): ReadWriteProperty<Any?, Int> =
    object: ReadWriteProperty<Any?, Int> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Int =
            map.getValue(property.name).jsonPrimitive.int

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            map[property.name] = JsonPrimitive(value)
        }
    }

inline fun boolean(map: MutableMap<String, JsonElement>): ReadWriteProperty<Any?, Boolean> =
    object: ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
            map.getValue(property.name).jsonPrimitive.boolean

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            map[property.name] = JsonPrimitive(value)
        }
    }