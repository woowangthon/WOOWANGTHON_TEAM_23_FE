package com.woowang.scrollmate.data

import androidx.room.TypeConverter
import org.json.JSONArray

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = JSONArray(value).toString()

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val array = JSONArray(value)
        return List(array.length()) { array.getString(it) }
    }
}
