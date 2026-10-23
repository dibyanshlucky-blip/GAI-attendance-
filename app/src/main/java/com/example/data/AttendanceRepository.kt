package com.example.data

import kotlinx.coroutines.flow.Flow

class AttendanceRepository(private val attendanceDao: AttendanceDao) {
    val allRecords: Flow<List<AttendanceRecord>> = attendanceDao.getAllRecords()
    val activeRecord: Flow<AttendanceRecord?> = attendanceDao.getActiveRecord()
    val allSettings: Flow<List<UserSetting>> = attendanceDao.getAllSettingsFlow()
    val allAdvances: Flow<List<AdvanceRecord>> = attendanceDao.getAllAdvances()
    val allNotes: Flow<List<WorkNote>> = attendanceDao.getAllNotes()

    fun getRecordsForMonth(yearMonth: String): Flow<List<AttendanceRecord>> {
        return attendanceDao.getRecordsForMonth(yearMonth)
    }

    suspend fun getRecordForDate(dateString: String): AttendanceRecord? {
        return attendanceDao.getRecordForDate(dateString)
    }

    suspend fun insertRecord(record: AttendanceRecord): Long {
        return attendanceDao.insertRecord(record)
    }

    suspend fun insertRecords(records: List<AttendanceRecord>) {
        attendanceDao.insertRecords(records)
    }

    suspend fun updateRecord(record: AttendanceRecord) {
        attendanceDao.updateRecord(record)
    }

    suspend fun deleteRecord(record: AttendanceRecord) {
        attendanceDao.deleteRecord(record)
    }

    suspend fun deleteRecordById(id: Int) {
        attendanceDao.deleteRecordById(id)
    }

    suspend fun deleteRecordByDate(dateString: String) {
        attendanceDao.deleteRecordByDate(dateString)
    }

    suspend fun deleteRecordsForMonth(yearMonth: String) {
        attendanceDao.deleteRecordsForMonth(yearMonth)
    }

    // Advances
    fun getAdvancesForMonth(yearMonth: String): Flow<List<AdvanceRecord>> {
        return attendanceDao.getAdvancesForMonth(yearMonth)
    }

    suspend fun insertAdvance(advance: AdvanceRecord): Long {
        return attendanceDao.insertAdvance(advance)
    }

    suspend fun insertAdvances(advances: List<AdvanceRecord>) {
        attendanceDao.insertAdvances(advances)
    }

    suspend fun updateAdvance(advance: AdvanceRecord) {
        attendanceDao.updateAdvance(advance)
    }

    suspend fun deleteAdvanceById(id: Int) {
        attendanceDao.deleteAdvanceById(id)
    }

    suspend fun deleteAdvancesForMonth(yearMonth: String) {
        attendanceDao.deleteAdvancesForMonth(yearMonth)
    }

    // Notes
    fun getNotesForMonth(yearMonth: String): Flow<List<WorkNote>> {
        return attendanceDao.getNotesForMonth(yearMonth)
    }

    suspend fun insertNote(note: WorkNote): Long {
        return attendanceDao.insertNote(note)
    }

    suspend fun deleteNoteById(id: Int) {
        attendanceDao.deleteNoteById(id)
    }

    // Settings
    suspend fun getSettingValue(key: String): String? {
        return attendanceDao.getSettingValue(key)
    }

    suspend fun saveSetting(key: String, value: String) {
        attendanceDao.insertSetting(UserSetting(key, value))
    }
}
