package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSetting(
    @PrimaryKey val key: String,
    val value: String
)
