package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "advance_records")
data class AdvanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val dateString: String,      // "2026-10-22"
    val yearMonth: String,       // "2026-10"
    val timeString: String? = null, // "11:14:52 AM"
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

