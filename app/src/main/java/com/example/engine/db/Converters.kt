package com.example.engine.db

import androidx.room.TypeConverter
import com.example.ui.chat.MessageRole

class Converters {
    @TypeConverter
    fun fromMessageRole(value: MessageRole): String = value.name

    @TypeConverter
    fun toMessageRole(value: String): MessageRole = MessageRole.valueOf(value)
}
