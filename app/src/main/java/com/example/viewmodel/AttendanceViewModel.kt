package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AdvanceRecord
import com.example.data.AttendanceRecord
import com.example.data.AttendanceRepository
import com.example.data.WorkNote
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

data class PendingDateItem(
    val dateString: String = "",
    val year: Int = 2026,
    val monthIndex: Int = 9,
    val dayOfMonth: Int = 1,
    val dayOfWeek: String = "",
    val formattedDisplay: String = "",
    val buttonFormatted: String = "",
    val isToday: Boolean = false,
    val isPending: Boolean = false,
    val statusLabel: String = "Pending",
    val record: AttendanceRecord? = null
)

data class PendingAttendanceSummary(
    val totalPendingCount: Int = 0,
    val isTodayPending: Boolean = false,
    val todayDateString: String = "",
    val todayFormatted: String = "",
    val todayItem: PendingDateItem? = null,
    val pendingDates: List<PendingDateItem> = emptyList(),
    val previousPendingDates: List<PendingDateItem> = emptyList(),
    val allEvaluatedDates: List<PendingDateItem> = emptyList(),
    val isDataLoaded: Boolean = false
)

data class UserProfile(
    val name: String = "ABHIMANYU KUMAR",
    val dailyWage: Double = 750.0,
    val currencySymbol: String = "₹",
    val overtimeHourlyRate: Double = 100.0,
    val mobileNumber: String = "+91 98765 43210"
)

data class OtpAuthState(
    val phoneNumber: String = "",
    val countryCode: String = "+91",
    val workerName: String = "ABHIMANYU KUMAR",
    val isOtpSent: Boolean = false,
    val generatedOtp: String = "",
    val enteredOtp: String = "",
    val countdownSeconds: Int = 0,
    val isVerifying: Boolean = false,
    val errorMessage: String? = null,
    val incomingSmsMessage: String? = null
)

class AttendanceViewModel(private val repository: AttendanceRepository) : ViewModel() {

    // Auth & OTP State
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isAuthReady = MutableStateFlow(false)
    val isAuthReady: StateFlow<Boolean> = _isAuthReady.asStateFlow()

    private val _otpAuthState = MutableStateFlow(OtpAuthState())
    val otpAuthState: StateFlow<OtpAuthState> = _otpAuthState.asStateFlow()

    private var countdownJob: kotlinx.coroutines.Job? = null

    // Current selected Year & Month (Defaults to November 2025 to match user's dashboard view)
    private val _selectedCalendar = MutableStateFlow(
        Calendar.getInstance().apply {
            set(Calendar.YEAR, 2025)
            set(Calendar.MONTH, Calendar.NOVEMBER) // 0-indexed: Calendar.NOVEMBER is 10
            set(Calendar.DAY_OF_MONTH, 1)
        }
    )
    val selectedCalendar: StateFlow<Calendar> = _selectedCalendar.asStateFlow()

    val selectedYearMonth: StateFlow<String> = _selectedCalendar.map { cal ->
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        String.format(Locale.ENGLISH, "%04d-%02d", year, month)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = "2025-11"
    )

    val selectedMonthName: StateFlow<String> = _selectedCalendar.map { cal ->
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        monthNames[cal.get(Calendar.MONTH)]
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = "November"
    )

    val selectedYear: StateFlow<Int> = _selectedCalendar.map { cal ->
        cal.get(Calendar.YEAR)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 2025
    )

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _lockedMonths = MutableStateFlow<Set<String>>(emptySet())
    val lockedMonths: StateFlow<Set<String>> = _lockedMonths.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthRecords: StateFlow<List<AttendanceRecord>> = selectedYearMonth
        .flatMapLatest { ym -> repository.getRecordsForMonth(ym) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthAdvances: StateFlow<List<AdvanceRecord>> = selectedYearMonth
        .flatMapLatest { ym -> repository.getAdvancesForMonth(ym) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allAdvances: StateFlow<List<AdvanceRecord>> = repository.allAdvances
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthNotes: StateFlow<List<WorkNote>> = selectedYearMonth
        .flatMapLatest { ym -> repository.getNotesForMonth(ym) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isCurrentMonthLocked: StateFlow<Boolean> = combine(selectedYearMonth, _lockedMonths) { ym, lockedSet ->
        lockedSet.contains(ym)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    // Summary Computations
    val workedDays: StateFlow<Double> = monthRecords.map { list ->
        list.sumOf { it.dayUnits }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 22.0
    )

    val totalWage: StateFlow<Double> = monthRecords.map { list ->
        list.sumOf { it.totalEarnings }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 16500.0
    )

    val totalAdvance: StateFlow<Double> = monthAdvances.map { list ->
        list.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 9000.0
    )

    val netPayable: StateFlow<Double> = combine(totalWage, totalAdvance) { wage, advance ->
        wage - advance
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 7500.0
    )

    // Pending Attendance Tracking
    private val _isPendingPopupDismissedThisSession = MutableStateFlow(false)
    val isPendingPopupDismissedThisSession: StateFlow<Boolean> = _isPendingPopupDismissedThisSession.asStateFlow()

    fun dismissPendingPopupTemporarily() {
        _isPendingPopupDismissedThisSession.value = true
    }

    fun onAppReopened() {
        _isPendingPopupDismissedThisSession.value = false
    }

    val pendingAttendanceState: StateFlow<PendingAttendanceSummary> = combine(
        repository.allRecords,
        _isAuthReady
    ) { records, authReady ->
        calculatePendingSummary(records, isDataLoaded = authReady)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PendingAttendanceSummary()
    )

    private fun calculatePendingSummary(records: List<AttendanceRecord>, isDataLoaded: Boolean): PendingAttendanceSummary {
        if (!isDataLoaded) {
            return PendingAttendanceSummary(isDataLoaded = false)
        }

        val todayCal = Calendar.getInstance()
        val todayYear = todayCal.get(Calendar.YEAR)
        val todayMonth = todayCal.get(Calendar.MONTH) // 0-indexed
        val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
        val todayDateStr = String.format(Locale.ENGLISH, "%04d-%02d-%02d", todayYear, todayMonth + 1, todayDay)
        val todayDisplayStr = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(todayCal.time)
        val todayDayOfWeekStr = SimpleDateFormat("EEEE", Locale.ENGLISH).format(todayCal.time)

        val recordsMap = records.associateBy { it.dateString }

        // Evaluate days in current month from 1 up to todayDay (strictly no future dates)
        val evaluatedList = mutableListOf<PendingDateItem>()
        val pendingChronological = mutableListOf<PendingDateItem>()

        for (d in 1..todayDay) {
            val cal = Calendar.getInstance().apply {
                set(todayYear, todayMonth, d)
            }
            val dateStr = String.format(Locale.ENGLISH, "%04d-%02d-%02d", todayYear, todayMonth + 1, d)
            val displayStr = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(cal.time)
            val dayName = SimpleDateFormat("EEEE", Locale.ENGLISH).format(cal.time)
            val btnStr = SimpleDateFormat("EEE, dd MMM", Locale.ENGLISH).format(cal.time)
            val rec = recordsMap[dateStr]
            val isToday = (d == todayDay)

            val isPending = (rec == null)
            val statusLabel = when {
                rec == null -> "Pending"
                rec.status == "ABSENT" || rec.dayUnits == 0.0 -> "Absent"
                rec.dayUnits == 0.5 -> "Half Day"
                rec.dayUnits == 1.5 -> "1.5 Day"
                rec.dayUnits == 2.0 -> "Double Day"
                rec.dayUnits == 1.0 -> "Full Day"
                else -> "Custom (${rec.dayUnits}d)"
            }

            val item = PendingDateItem(
                dateString = dateStr,
                year = todayYear,
                monthIndex = todayMonth,
                dayOfMonth = d,
                dayOfWeek = dayName,
                formattedDisplay = displayStr,
                buttonFormatted = btnStr,
                isToday = isToday,
                isPending = isPending,
                statusLabel = statusLabel,
                record = rec
            )

            evaluatedList.add(item)
            if (isPending) {
                pendingChronological.add(item)
            }
        }

        val todayRec = recordsMap[todayDateStr]
        val isTodayPending = (todayRec == null)

        val todayItem = evaluatedList.find { it.isToday } ?: PendingDateItem(
            dateString = todayDateStr,
            year = todayYear,
            monthIndex = todayMonth,
            dayOfMonth = todayDay,
            dayOfWeek = todayDayOfWeekStr,
            formattedDisplay = todayDisplayStr,
            buttonFormatted = SimpleDateFormat("EEE, dd MMM", Locale.ENGLISH).format(todayCal.time),
            isToday = true,
            isPending = isTodayPending,
            statusLabel = if (todayRec != null) "Full Day" else "Pending",
            record = todayRec
        )

        val previousPending = evaluatedList
            .filter { !it.isToday && it.isPending }
            .sortedByDescending { it.dayOfMonth }

        val totalPending = pendingChronological.size

        return PendingAttendanceSummary(
            totalPendingCount = totalPending,
            isTodayPending = isTodayPending,
            todayDateString = todayDateStr,
            todayFormatted = todayDisplayStr,
            todayItem = todayItem,
            pendingDates = pendingChronological,
            previousPendingDates = previousPending,
            allEvaluatedDates = evaluatedList.sortedByDescending { it.dayOfMonth },
            isDataLoaded = true
        )
    }

    init {
        // Load Settings and check if initial sample data needs to be populated
        viewModelScope.launch {
            repository.allSettings.collect { list ->
                var name = "ABHIMANYU KUMAR"
                var wage = 750.0
                var currency = "₹"
                var otRate = 100.0
                var lockedStr = ""
                var mobile = "+91 98765 43210"
                var loggedIn = false

                list.forEach { setting ->
                    when (setting.key) {
                        "user_name" -> name = setting.value
                        "daily_wage" -> wage = setting.value.toDoubleOrNull() ?: 750.0
                        "currency" -> currency = setting.value
                        "overtime_rate" -> otRate = setting.value.toDoubleOrNull() ?: 100.0
                        "locked_months" -> lockedStr = setting.value
                        "auth_phone" -> mobile = setting.value
                        "auth_logged_in" -> loggedIn = (setting.value == "true")
                    }
                }
                _userProfile.value = UserProfile(name, wage, currency, otRate, mobile)
                _isLoggedIn.value = loggedIn
                _isAuthReady.value = true
                if (lockedStr.isNotEmpty()) {
                    _lockedMonths.value = lockedStr.split(",").filter { it.isNotBlank() }.toSet()
                }

                // If user_name setting wasn't present, populate initial sample data
                val isInitialized = list.any { it.key == "initialized" }
                if (!isInitialized) {
                    populateInitialSampleData()
                }
            }
        }
    }

    private suspend fun populateInitialSampleData() {
        repository.saveSetting("initialized", "true")
        repository.saveSetting("user_name", "ABHIMANYU KUMAR")
        repository.saveSetting("daily_wage", "750")
        repository.saveSetting("currency", "₹")
        repository.saveSetting("overtime_rate", "100")

        // Populate 22 working days for November 2025
        val workingDays = listOf(
            1, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 15, 17, 18, 19, 20, 21, 22, 24, 25, 26
        )
        val initialRecords = workingDays.map { day ->
            AttendanceRecord(
                dateString = String.format(Locale.ENGLISH, "2025-11-%02d", day),
                yearMonth = "2025-11",
                dayOfMonth = day,
                status = "FULL_DAY",
                dayUnits = 1.0,
                dailyWage = 750.0,
                overtimeHours = 0.0,
                overtimeRate = 100.0,
                totalEarnings = 750.0,
                notes = if (day == 12) "Material delivery verified" else null
            )
        }
        repository.insertRecords(initialRecords)

        // Populate Advances for November 2025 (Total: ₹9,000)
        val initialAdvances = listOf(
            AdvanceRecord(
                amount = 5000.0,
                dateString = "2025-11-05",
                yearMonth = "2025-11",
                note = "Festival family advance"
            ),
            AdvanceRecord(
                amount = 4000.0,
                dateString = "2025-11-18",
                yearMonth = "2025-11",
                note = "Personal medical & travel"
            )
        )
        repository.insertAdvances(initialAdvances)

        // Populate a sample work note
        repository.insertNote(
            WorkNote(
                title = "Monthly Target Achieved",
                content = "Completed 22 shifts for November. All site safety standards adhered to.",
                dateString = "2025-11-26",
                yearMonth = "2025-11"
            )
        )
    }

    // Month Navigation
    fun previousMonth() {
        val current = _selectedCalendar.value.clone() as Calendar
        current.add(Calendar.MONTH, -1)
        _selectedCalendar.value = current
    }

    fun nextMonth() {
        val current = _selectedCalendar.value.clone() as Calendar
        current.add(Calendar.MONTH, 1)
        _selectedCalendar.value = current
    }

    fun setMonth(year: Int, monthIndex: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        _selectedCalendar.value = cal
    }

    // Attendance Operations
    fun markAttendance(
        dateString: String,
        status: String,
        dayUnits: Double,
        dailyWage: Double,
        overtimeHours: Double = 0.0,
        overtimeRate: Double = 100.0,
        note: String? = null
    ) {
        viewModelScope.launch {
            val parts = dateString.split("-")
            val yearMonth = if (parts.size >= 2) "${parts[0]}-${parts[1]}" else selectedYearMonth.value
            val dayOfMonth = if (parts.size >= 3) parts[2].toIntOrNull() ?: 1 else 1

            val baseEarnings = dayUnits * dailyWage
            val overtimeEarnings = overtimeHours * overtimeRate
            val totalEarnings = baseEarnings + overtimeEarnings

            val existing = repository.getRecordForDate(dateString)
            val record = AttendanceRecord(
                id = existing?.id ?: 0,
                dateString = dateString,
                yearMonth = yearMonth,
                dayOfMonth = dayOfMonth,
                status = status,
                dayUnits = dayUnits,
                dailyWage = dailyWage,
                overtimeHours = overtimeHours,
                overtimeRate = overtimeRate,
                totalEarnings = totalEarnings,
                notes = note
            )
            repository.insertRecord(record)
        }
    }

    fun deleteAttendance(recordId: Int) {
        viewModelScope.launch {
            repository.deleteRecordById(recordId)
        }
    }

    fun deleteAttendanceForDate(dateString: String) {
        viewModelScope.launch {
            val record = repository.getRecordForDate(dateString)
            if (record != null) {
                repository.deleteRecordById(record.id)
            }
        }
    }

    // Advance Operations
    fun addAdvance(amount: Double, dateString: String, note: String?, timeString: String? = null) {
        viewModelScope.launch {
            val parts = dateString.split("-")
            val yearMonth = if (parts.size >= 2) "${parts[0]}-${parts[1]}" else selectedYearMonth.value
            val advance = AdvanceRecord(
                amount = amount,
                dateString = dateString,
                yearMonth = yearMonth,
                timeString = timeString,
                note = note
            )
            repository.insertAdvance(advance)
        }
    }

    fun updateAdvance(id: Int, amount: Double, dateString: String, timeString: String?, note: String?) {
        viewModelScope.launch {
            val parts = dateString.split("-")
            val yearMonth = if (parts.size >= 2) "${parts[0]}-${parts[1]}" else selectedYearMonth.value
            val advance = AdvanceRecord(
                id = id,
                amount = amount,
                dateString = dateString,
                yearMonth = yearMonth,
                timeString = timeString,
                note = note
            )
            repository.updateAdvance(advance)
        }
    }

    fun deleteAdvance(id: Int) {
        viewModelScope.launch {
            repository.deleteAdvanceById(id)
        }
    }

    // Note Operations
    fun addNote(title: String, content: String, dateString: String) {
        viewModelScope.launch {
            val parts = dateString.split("-")
            val yearMonth = if (parts.size >= 2) "${parts[0]}-${parts[1]}" else selectedYearMonth.value
            val note = WorkNote(
                title = title,
                content = content,
                dateString = dateString,
                yearMonth = yearMonth
            )
            repository.insertNote(note)
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            repository.deleteNoteById(id)
        }
    }

    // Settings & Profile Operations
    fun updateProfile(
        name: String,
        dailyWage: Double,
        currency: String,
        otRate: Double,
        mobileNumber: String = _userProfile.value.mobileNumber
    ) {
        viewModelScope.launch {
            repository.saveSetting("user_name", name)
            repository.saveSetting("daily_wage", dailyWage.toString())
            repository.saveSetting("currency", currency)
            repository.saveSetting("overtime_rate", otRate.toString())
            repository.saveSetting("auth_phone", mobileNumber)
            _userProfile.value = UserProfile(name, dailyWage, currency, otRate, mobileNumber)
        }
    }

    // -------------------------------------------------------------------------
    // OTP AUTHENTICATION OPERATIONS
    // -------------------------------------------------------------------------
    fun onPhoneChange(phone: String) {
        val cleaned = phone.filter { it.isDigit() }.take(10)
        _otpAuthState.update { it.copy(phoneNumber = cleaned, errorMessage = null) }
    }

    fun onCountryCodeChange(code: String) {
        _otpAuthState.update { it.copy(countryCode = code) }
    }

    fun onWorkerNameChange(name: String) {
        _otpAuthState.update { it.copy(workerName = name) }
    }

    fun onEnteredOtpChange(otp: String) {
        val digitsOnly = otp.filter { it.isDigit() }.take(6)
        _otpAuthState.update { it.copy(enteredOtp = digitsOnly, errorMessage = null) }
    }

    fun sendOtp() {
        val phone = _otpAuthState.value.phoneNumber.trim()
        if (phone.length < 10) {
            _otpAuthState.update { it.copy(errorMessage = "Please enter a valid 10-digit mobile number") }
            return
        }
        val generated = String.format(Locale.ENGLISH, "%06d", (100000..999999).random())
        _otpAuthState.update {
            it.copy(
                isOtpSent = true,
                generatedOtp = generated,
                enteredOtp = "",
                errorMessage = null,
                incomingSmsMessage = "Wage Tracker Login: Your OTP is $generated. Valid for 5 minutes."
            )
        }
        startResendCountdown()
    }

    fun resendOtp() {
        val generated = String.format(Locale.ENGLISH, "%06d", (100000..999999).random())
        _otpAuthState.update {
            it.copy(
                generatedOtp = generated,
                enteredOtp = "",
                errorMessage = null,
                incomingSmsMessage = "Wage Tracker Login: New OTP is $generated. Valid for 5 minutes."
            )
        }
        startResendCountdown()
    }

    private fun startResendCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var seconds = 30
            while (seconds > 0) {
                _otpAuthState.update { it.copy(countdownSeconds = seconds) }
                kotlinx.coroutines.delay(1000L)
                seconds--
            }
            _otpAuthState.update { it.copy(countdownSeconds = 0) }
        }
    }

    fun autoFillOtp() {
        val code = _otpAuthState.value.generatedOtp
        if (code.isNotEmpty()) {
            _otpAuthState.update { it.copy(enteredOtp = code, errorMessage = null) }
        }
    }

    fun verifyOtp(onSuccess: (() -> Unit)? = null) {
        val current = _otpAuthState.value
        val entered = current.enteredOtp.trim()
        val targetOtp = if (current.generatedOtp.isNotEmpty()) current.generatedOtp else "123456"
        
        // If empty, auto-use targetOtp so the user is never stuck
        val effectiveOtp = if (entered.isBlank()) targetOtp else entered

        // Accept target OTP, 123456, or any valid 6-digit number to guarantee login
        if (effectiveOtp == targetOtp || effectiveOtp == "123456" || effectiveOtp.length == 6) {
            viewModelScope.launch {
                val phone = current.phoneNumber.ifBlank { "9876543210" }
                val fullMobile = "${current.countryCode} $phone"
                val name = current.workerName.trim().ifBlank { "ABHIMANYU KUMAR" }
                repository.saveSetting("auth_logged_in", "true")
                repository.saveSetting("auth_phone", fullMobile)
                repository.saveSetting("user_name", name)
                _userProfile.update { it.copy(name = name, mobileNumber = fullMobile) }
                _isLoggedIn.value = true
                _otpAuthState.update { it.copy(errorMessage = null, isVerifying = false) }
                onSuccess?.invoke()
            }
        } else {
            _otpAuthState.update {
                it.copy(
                    errorMessage = "Please enter 6 digits, or tap 'Auto-fill OTP (${current.generatedOtp})' below."
                )
            }
        }
    }

    fun directVerifyWithoutOtp() {
        val current = _otpAuthState.value
        val phone = current.phoneNumber.trim().ifBlank { "9876543210" }
        val fullMobile = "${current.countryCode} $phone"
        val name = current.workerName.trim().ifBlank { "ABHIMANYU KUMAR" }
        viewModelScope.launch {
            repository.saveSetting("auth_logged_in", "true")
            repository.saveSetting("auth_phone", fullMobile)
            repository.saveSetting("user_name", name)
            _userProfile.update { it.copy(name = name, mobileNumber = fullMobile) }
            _isLoggedIn.value = true
            _otpAuthState.update { it.copy(errorMessage = null, isVerifying = false) }
        }
    }

    fun resetOtpStep() {
        countdownJob?.cancel()
        _otpAuthState.update {
            it.copy(
                isOtpSent = false,
                enteredOtp = "",
                errorMessage = null,
                incomingSmsMessage = null,
                countdownSeconds = 0
            )
        }
    }

    fun demoLogin() {
        viewModelScope.launch {
            repository.saveSetting("auth_logged_in", "true")
            repository.saveSetting("auth_phone", "+91 98765 43210")
            repository.saveSetting("user_name", "ABHIMANYU KUMAR")
            _userProfile.update { it.copy(name = "ABHIMANYU KUMAR", mobileNumber = "+91 98765 43210") }
            _isLoggedIn.value = true
        }
    }

    fun logout() {
        viewModelScope.launch {
            countdownJob?.cancel()
            repository.saveSetting("auth_logged_in", "false")
            _isLoggedIn.value = false
            _otpAuthState.value = OtpAuthState()
        }
    }

    fun toggleLockMonth(yearMonth: String) {
        viewModelScope.launch {
            val current = _lockedMonths.value.toMutableSet()
            if (current.contains(yearMonth)) {
                current.remove(yearMonth)
            } else {
                current.add(yearMonth)
            }
            _lockedMonths.value = current
            repository.saveSetting("locked_months", current.joinToString(","))
        }
    }

    fun resetToDemoData() {
        viewModelScope.launch {
            val ym = selectedYearMonth.value
            repository.deleteRecordsForMonth(ym)
            repository.deleteAdvancesForMonth(ym)
            populateInitialSampleData()
        }
    }

    companion object {
        private val currencyFormatter = DecimalFormat("#,##,##0")
        private val decimalFormatter = DecimalFormat("0.00")

        fun formatCurrency(value: Double, symbol: String = "₹"): String {
            return "$symbol${currencyFormatter.format(value.toLong())}"
        }

        fun formatDays(value: Double): String {
            return decimalFormatter.format(value)
        }
    }
}

class AttendanceViewModelFactory(private val repository: AttendanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AttendanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
