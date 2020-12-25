package com.manylogs.logging

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.util.zip.CRC32

inline fun String.crc32(): Long {
    val b = toByteArray()
    return CRC32().apply { update(b, 0, b.size) }.value
}


// Gson
val genericGson = GsonBuilder().setPrettyPrinting()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

inline fun <reified T : Any> T.json(): String = genericGson.toJson(this, T::class.java)

inline fun <reified T : Any> String.fromJson(): T = genericGson.fromJson(this, T::class.java)
inline fun <reified T : Any> String.fromJsonList(): T =
    genericGson.fromJson(this, object : TypeToken<T>() {}.type)


///////////////////////////////////////////////////////////////////////////
// Extensions
///////////////////////////////////////////////////////////////////////////

/**
 * Generic, non user specific, app settings.
 */
internal fun getPreferences(context: Context): SharedPreferences =
    context.getSharedPreferences("manylogs_app_settings", Context.MODE_PRIVATE)

inline fun <reified T> SharedPreferences.get(key: String, defaultValue: T): T {
    when (T::class) {
        Boolean::class -> return this.getBoolean(key, defaultValue as Boolean) as T
        Float::class -> return this.getFloat(key, defaultValue as Float) as T
        Int::class -> return this.getInt(key, defaultValue as Int) as T
        Long::class -> return this.getLong(key, defaultValue as Long) as T
        String::class -> return this.getString(key, defaultValue as String) as T
        else -> {
            if (defaultValue is Set<*>) {
                return this.getStringSet(key, defaultValue as Set<String>) as T
            }
        }
    }

    return defaultValue
}

inline fun <reified T> SharedPreferences.put(key: String, value: T) {
    val editor = this.edit()

    when (T::class) {
        Boolean::class -> editor.putBoolean(key, value as Boolean)
        Float::class -> editor.putFloat(key, value as Float)
        Int::class -> editor.putInt(key, value as Int)
        Long::class -> editor.putLong(key, value as Long)
        String::class -> editor.putString(key, value as String)
        else -> {
            if (value is Set<*>) {
                editor.putStringSet(key, value as Set<String>)
            }
        }
    }

    editor.apply()
}

fun SharedPreferences.clear() {
    edit().clear().apply()
}

fun SharedPreferences.remove(key: String) {
    edit().remove(key).apply()
}