package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_notes")
data class WorkNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val dateString: String,      // "2025-11-23"
    val yearMonth: String,       // "2025-11"
    val createdAt: Long = System.currentTimeMillis()
)
