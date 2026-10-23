package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    // Attendance Records
    @Query("SELECT * FROM attendance_records ORDER BY dateString DESC, id DESC")
    fun getAllRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE yearMonth = :yearMonth ORDER BY dayOfMonth ASC")
    fun getRecordsForMonth(yearMonth: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE dateString = :dateString LIMIT 1")
    suspend fun getRecordForDate(dateString: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE checkOutTime IS NULL AND checkInTime > 0 LIMIT 1")
    fun getActiveRecord(): Flow<AttendanceRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecord>)

    @Update
    suspend fun updateRecord(record: AttendanceRecord)

    @Delete
    suspend fun deleteRecord(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteRecordById(id: Int)

    @Query("DELETE FROM attendance_records WHERE dateString = :dateString")
    suspend fun deleteRecordByDate(dateString: String)

    @Query("DELETE FROM attendance_records WHERE yearMonth = :yearMonth")
    suspend fun deleteRecordsForMonth(yearMonth: String)

    // Advance Records
    @Query("SELECT * FROM advance_records ORDER BY dateString DESC, id DESC")
    fun getAllAdvances(): Flow<List<AdvanceRecord>>

    @Query("SELECT * FROM advance_records WHERE yearMonth = :yearMonth ORDER BY dateString ASC")
    fun getAdvancesForMonth(yearMonth: String): Flow<List<AdvanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdvance(advance: AdvanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdvances(advances: List<AdvanceRecord>)

    @Update
    suspend fun updateAdvance(advance: AdvanceRecord)

    @Delete
    suspend fun deleteAdvance(advance: AdvanceRecord)

    @Query("DELETE FROM advance_records WHERE id = :id")
    suspend fun deleteAdvanceById(id: Int)

    @Query("DELETE FROM advance_records WHERE yearMonth = :yearMonth")
    suspend fun deleteAdvancesForMonth(yearMonth: String)

    // Work Notes
    @Query("SELECT * FROM work_notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<WorkNote>>

    @Query("SELECT * FROM work_notes WHERE yearMonth = :yearMonth ORDER BY createdAt DESC")
    fun getNotesForMonth(yearMonth: String): Flow<List<WorkNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: WorkNote): Long

    @Delete
    suspend fun deleteNote(note: WorkNote)

    @Query("DELETE FROM work_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    // User Settings
    @Query("SELECT * FROM user_settings")
    fun getAllSettingsFlow(): Flow<List<UserSetting>>

    @Query("SELECT value FROM user_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: UserSetting)
}
