package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String = "",       // e.g. "2025-11-23"
    val yearMonth: String = "",        // e.g. "2025-11"
    val dayOfMonth: Int = 1,
    val status: String = "FULL_DAY",   // "FULL_DAY", "HALF_DAY", "ABSENT", "OVERTIME"
    val dayUnits: Double = 1.0,        // 1.0 (Full), 0.5 (Half), 0.0 (Absent)
    val dailyWage: Double = 750.0,
    val overtimeHours: Double = 0.0,
    val overtimeRate: Double = 100.0,
    val totalEarnings: Double = 750.0,
    val checkInTime: Long = 0L,
    val checkOutTime: Long? = null,
    val hourlyRate: Double = 15.0,
    val notes: String? = null,
    val overtimeThreshold: Double = 8.0,
    val overtimeMultiplier: Double = 1.5
) {
    val durationHours: Double
        get() {
            if (checkInTime == 0L) return dayUnits * 8.0
            val end = checkOutTime ?: System.currentTimeMillis()
            val diffMs = end - checkInTime
            return diffMs.toDouble() / (1000.0 * 60.0 * 60.0)
        }

    val earnings: Double
        get() = totalEarnings
}
