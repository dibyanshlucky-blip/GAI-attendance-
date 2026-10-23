package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AttendanceRecord
import com.example.viewmodel.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Supported attendance options for the Attendance Entry screen.
 * Exactly 6 options as specified (NO Holiday option).
 */
enum class AttendanceOption(
    val title: String,
    val subtitle: String,
    val defaultUnits: Double,
    val statusCode: String
) {
    PRESENT_FULL("Present", "Full Day (1)", 1.0, "FULL_DAY"),
    HALF_DAY("Half Day", "0.5 day", 0.5, "HALF_DAY"),
    ABSENT("Absent", "0 day", 0.0, "ABSENT"),
    ONE_AND_HALF("One & Half", "1.5 days", 1.5, "ONE_AND_HALF_DAY"),
    DOUBLE_DAY("Double Day", "2 days", 2.0, "DOUBLE_DAY"),
    CUSTOM("Custom", "Custom value", 1.0, "CUSTOM")
}

@Composable
fun AttendancePopupDialog(
    viewModel: AttendanceViewModel,
    initialDay: Int? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsState()
    val monthName by viewModel.selectedMonthName.collectAsState()
    val year by viewModel.selectedYear.collectAsState()
    val yearMonth by viewModel.selectedYearMonth.collectAsState()
    val records by viewModel.monthRecords.collectAsState()
    val selectedCalendar by viewModel.selectedCalendar.collectAsState()

    // Map existing records for the month by day number
    val recordByDay = remember(records) {
        records.associateBy { it.dayOfMonth }
    }

    // Days in current selected month
    val daysInMonth = remember(year, monthName) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        val monthNames = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val mIndex = monthNames.indexOf(monthName).coerceAtLeast(0)
        cal.set(Calendar.MONTH, mIndex)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // First day of week for current month (1 = Sunday, ..., 7 = Saturday)
    val firstDayOfWeek = remember(year, monthName) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        val monthNames = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val mIndex = monthNames.indexOf(monthName).coerceAtLeast(0)
        cal.set(Calendar.MONTH, mIndex)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.get(Calendar.DAY_OF_WEEK)
    }

    // Maximum days in previous month
    val prevMonthMaxDays = remember(year, monthName) {
        val cal = (selectedCalendar.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Default selected day: current day of month if in current system month, or day 1
    val todayCal = remember { Calendar.getInstance() }
    val isCurrentSystemMonth = remember(year, monthName) {
        todayCal.get(Calendar.YEAR) == year &&
                todayCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH) == monthName
    }
    val defaultDay = if (isCurrentSystemMonth) {
        todayCal.get(Calendar.DAY_OF_MONTH).coerceIn(1, daysInMonth)
    } else {
        1.coerceAtMost(daysInMonth)
    }

    var selectedDayOfMonth by remember(yearMonth, initialDay) {
        mutableIntStateOf(initialDay?.coerceIn(1, daysInMonth) ?: defaultDay)
    }

    // Helper to map record to option
    fun mapRecordToOption(rec: AttendanceRecord?): Pair<AttendanceOption, String> {
        if (rec == null) return Pair(AttendanceOption.PRESENT_FULL, "")
        return when {
            rec.status == "ABSENT" || rec.dayUnits == 0.0 -> Pair(AttendanceOption.ABSENT, "")
            rec.dayUnits == 1.0 && (rec.status == "FULL_DAY" || rec.status == "OVERTIME") -> Pair(AttendanceOption.PRESENT_FULL, "")
            rec.dayUnits == 0.5 -> Pair(AttendanceOption.HALF_DAY, "")
            rec.dayUnits == 1.5 -> Pair(AttendanceOption.ONE_AND_HALF, "")
            rec.dayUnits == 2.0 -> Pair(AttendanceOption.DOUBLE_DAY, "")
            else -> {
                val str = if (rec.dayUnits % 1.0 == 0.0) rec.dayUnits.toInt().toString() else rec.dayUnits.toString()
                Pair(AttendanceOption.CUSTOM, str)
            }
        }
    }

    // Selected attendance option & custom input state
    val currentSavedRecord = recordByDay[selectedDayOfMonth]
    val (initialOpt, initialCustom) = remember(selectedDayOfMonth, currentSavedRecord) {
        mapRecordToOption(currentSavedRecord)
    }

    var selectedOption by remember(selectedDayOfMonth, currentSavedRecord) {
        mutableStateOf(initialOpt)
    }
    var customValueText by remember(selectedDayOfMonth, currentSavedRecord) {
        mutableStateOf(initialCustom)
    }

    // Track unsaved modifications
    val hasUnsavedChanges = remember(selectedOption, customValueText, initialOpt, initialCustom) {
        selectedOption != initialOpt || (selectedOption == AttendanceOption.CUSTOM && customValueText != initialCustom)
    }

    var showDiscardConfirmDialog by remember { mutableStateOf(false) }

    // Safe dismiss handler
    val handleAttemptClose = {
        if (hasUnsavedChanges) {
            showDiscardConfirmDialog = true
        } else {
            onDismiss()
        }
    }

    // Date formatted string: "2026-10-14"
    val selectedDateString = remember(yearMonth, selectedDayOfMonth) {
        String.format(Locale.ENGLISH, "%s-%02d", yearMonth, selectedDayOfMonth)
    }

    // Validation for custom attendance input
    val customValueDouble = customValueText.toDoubleOrNull()
    val isCustomInputValid = remember(selectedOption, customValueText, customValueDouble) {
        if (selectedOption != AttendanceOption.CUSTOM) true
        else customValueDouble != null && customValueDouble >= 0.0
    }

    // Effective attendance value in days
    val effectiveAttendanceUnits: Double? = remember(selectedOption, customValueDouble) {
        when (selectedOption) {
            AttendanceOption.PRESENT_FULL -> 1.0
            AttendanceOption.HALF_DAY -> 0.5
            AttendanceOption.ABSENT -> 0.0
            AttendanceOption.ONE_AND_HALF -> 1.5
            AttendanceOption.DOUBLE_DAY -> 2.0
            AttendanceOption.CUSTOM -> customValueDouble?.takeIf { it >= 0.0 }
        }
    }

    // Wage calculation for this day
    val calculatedDayWage = remember(effectiveAttendanceUnits, profile.dailyWage) {
        if (effectiveAttendanceUnits != null) effectiveAttendanceUnits * profile.dailyWage else 0.0
    }

    // Discard Changes Confirmation Dialog
    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            title = {
                Text(
                    text = "Discard Unsaved Changes?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "You have unsaved changes for $selectedDateString. Do you want to discard them and close?",
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Discard", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmDialog = false }) {
                    Text("Keep Editing", color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Dialog(
        onDismissRequest = handleAttemptClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        // Semi-transparent dark background overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    handleAttemptClose()
                },
            contentAlignment = Alignment.Center
        ) {
            // White popup card with rounded corners and subtle shadow
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .widthIn(max = 440.dp)
                    .heightIn(max = 700.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Prevent click inside card from closing modal
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // -------------------------------------------------------------
                    // 2. POPUP HEADER
                    // -------------------------------------------------------------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFEFF6FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Attendance Entry",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        IconButton(
                            onClick = handleAttemptClose,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFF1F5F9), CircleShape)
                                .testTag("close_attendance_popup")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                    // -------------------------------------------------------------
                    // SCROLLABLE POPUP BODY
                    // -------------------------------------------------------------
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        // ---------------------------------------------------------
                        // 3. CALENDAR DESIGN
                        // ---------------------------------------------------------
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                // Month and Year Header with Nav Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.previousMonth() },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(0xFFF8FAFC), CircleShape)
                                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                                            .testTag("calendar_prev_month")
                                    ) {
                                        Icon(
                                            Icons.Default.ChevronLeft,
                                            contentDescription = "Previous Month",
                                            tint = Color(0xFF334155),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Text(
                                        text = "$monthName $year",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )

                                    IconButton(
                                        onClick = { viewModel.nextMonth() },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(0xFFF8FAFC), CircleShape)
                                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                                            .testTag("calendar_next_month")
                                    ) {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = "Next Month",
                                            tint = Color(0xFF334155),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Weekday Header Row (Sun, Mon, Tue, Wed, Thu, Fri, Sat)
                                val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    weekdays.forEach { dayName ->
                                        Text(
                                            text = dayName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (dayName == "Sun") Color(0xFFDC2626) else Color(0xFF64748B),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(6.dp))

                                // Calendar Dates Grid
                                val startOffset = firstDayOfWeek - 1 // 0 for Sun, 1 for Mon, etc.
                                val totalCells = startOffset + daysInMonth
                                val totalRows = (totalCells + 6) / 7

                                for (row in 0 until totalRows) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        for (col in 0 until 7) {
                                            val cellIndex = row * 7 + col
                                            val dayNum = cellIndex - startOffset + 1

                                            if (cellIndex < startOffset) {
                                                // Adjacent previous month dates in lighter color
                                                val prevDay = prevMonthMaxDays - startOffset + 1 + cellIndex
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .padding(2.dp)
                                                        .clickable {
                                                            viewModel.previousMonth()
                                                            selectedDayOfMonth = prevDay.coerceIn(1, prevMonthMaxDays)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "$prevDay",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFFCBD5E1)
                                                    )
                                                }
                                            } else if (dayNum in 1..daysInMonth) {
                                                // Current month date
                                                val isSelected = (dayNum == selectedDayOfMonth)
                                                val recordForDay = recordByDay[dayNum]

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .padding(2.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            if (isSelected) Color(0xFF2563EB)
                                                            else Color.Transparent
                                                        )
                                                        .border(
                                                            width = if (isSelected) 0.dp else if (recordForDay != null) 1.dp else 0.dp,
                                                            color = if (recordForDay != null) Color(0xFF93C5FD) else Color.Transparent,
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable {
                                                            selectedDayOfMonth = dayNum
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = "$dayNum",
                                                            fontSize = 12.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                            color = if (isSelected) Color.White else Color(0xFF1E293B)
                                                        )
                                                        // Small indicator dot for saved dates when not selected
                                                        if (!isSelected && recordForDay != null) {
                                                            val dotColor = when (recordForDay.status) {
                                                                "FULL_DAY" -> Color(0xFF16A34A)
                                                                "HALF_DAY" -> Color(0xFFEA580C)
                                                                "ABSENT" -> Color(0xFFDC2626)
                                                                "ONE_AND_HALF_DAY" -> Color(0xFF0284C7)
                                                                "DOUBLE_DAY" -> Color(0xFF7C3AED)
                                                                else -> Color(0xFF2563EB)
                                                            }
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.dp)
                                                                    .background(dotColor, CircleShape)
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                // Adjacent next month dates in lighter color
                                                val nextMonthDay = dayNum - daysInMonth
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .padding(2.dp)
                                                        .clickable {
                                                            viewModel.nextMonth()
                                                            selectedDayOfMonth = nextMonthDay
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "$nextMonthDay",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFFCBD5E1)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Selected Date Header info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = formatNiceDate(selectedDateString),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (currentSavedRecord != null) {
                                        "Saved: ${formatStatusName(currentSavedRecord.status)} (${currentSavedRecord.dayUnits}d)"
                                    } else {
                                        "No attendance marked yet"
                                    },
                                    fontSize = 11.sp,
                                    color = if (currentSavedRecord != null) Color(0xFF16A34A) else Color(0xFF64748B)
                                )
                            }

                            // Daily Wage Rate Pill
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Text(
                                    text = "Rate: ${profile.currencySymbol}${profile.dailyWage.toInt()}/d",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ---------------------------------------------------------
                        // 4. ATTENDANCE SELECTION BUTTONS (EXACTLY 6 OPTIONS)
                        // ---------------------------------------------------------
                        Text(
                            text = "Select Attendance Status",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Responsive Grid of 6 options (2 rows of 3 columns)
                        val row1Options = listOf(
                            AttendanceOption.PRESENT_FULL,
                            AttendanceOption.HALF_DAY,
                            AttendanceOption.ABSENT
                        )
                        val row2Options = listOf(
                            AttendanceOption.ONE_AND_HALF,
                            AttendanceOption.DOUBLE_DAY,
                            AttendanceOption.CUSTOM
                        )

                        // Row 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row1Options.forEach { option ->
                                AttendanceOptionButton(
                                    option = option,
                                    isSelected = (selectedOption == option),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedOption = option
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row2Options.forEach { option ->
                                AttendanceOptionButton(
                                    option = option,
                                    isSelected = (selectedOption == option),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedOption = option
                                        if (option == AttendanceOption.CUSTOM && customValueText.isBlank()) {
                                            customValueText = "1.25"
                                        }
                                    }
                                )
                            }
                        }

                        // ---------------------------------------------------------
                        // 5. CUSTOM ATTENDANCE INPUT
                        // ---------------------------------------------------------
                        AnimatedVisibility(visible = selectedOption == AttendanceOption.CUSTOM) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Custom Attendance Value (Days)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A)
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = customValueText,
                                    onValueChange = { input ->
                                        // Allow digits and decimal point
                                        if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                                            customValueText = input
                                        }
                                    },
                                    placeholder = { Text("e.g. 0.25, 0.75, 1.25, 1.75") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    isError = !isCustomInputValid,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("custom_attendance_input"),
                                    trailingIcon = {
                                        Text(
                                            text = "days",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF64748B),
                                            modifier = Modifier.padding(end = 12.dp)
                                        )
                                    }
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                if (!isCustomInputValid) {
                                    Text(
                                        text = "Please enter a valid non-negative number (e.g. 0.75 or 1.25)",
                                        fontSize = 11.sp,
                                        color = Color(0xFFDC2626)
                                    )
                                } else {
                                    Text(
                                        text = "Enter any decimal or whole value: 0.25, 0.75, 1.25, 1.75, etc.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ---------------------------------------------------------
                        // 8. WAGE PREVIEW CARD
                        // ---------------------------------------------------------
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Calculated Day Wage",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = if (effectiveAttendanceUnits != null) {
                                            "${effectiveAttendanceUnits} day(s) × ${profile.currencySymbol}${profile.dailyWage.toInt()}"
                                        } else {
                                            "Invalid attendance value"
                                        },
                                        fontSize = 12.sp,
                                        color = Color(0xFF334155)
                                    )
                                }

                                Text(
                                    text = "${profile.currencySymbol}${calculatedDayWage.toInt()}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selectedOption == AttendanceOption.ABSENT) Color(0xFF94A3B8) else Color(0xFF2563EB)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ---------------------------------------------------------
                        // 6. SAVE ATTENDANCE BUTTON
                        // ---------------------------------------------------------
                        val isSaveEnabled = (effectiveAttendanceUnits != null && isCustomInputValid)

                        Button(
                            onClick = {
                                if (effectiveAttendanceUnits != null) {
                                    val statusString = selectedOption.statusCode
                                    viewModel.markAttendance(
                                        dateString = selectedDateString,
                                        status = statusString,
                                        dayUnits = effectiveAttendanceUnits,
                                        dailyWage = profile.dailyWage,
                                        overtimeHours = 0.0,
                                        overtimeRate = profile.overtimeHourlyRate,
                                        note = null
                                    )
                                    Toast.makeText(
                                        context,
                                        "Attendance saved: ${selectedOption.title} ($selectedDateString)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onDismiss()
                                }
                            },
                            enabled = isSaveEnabled,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_attendance_popup_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color(0xFFCBD5E1)
                            ),
                            contentPadding = PaddingValues()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (isSaveEnabled) {
                                            Modifier.background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF2563EB), Color(0xFF0D9488))
                                                )
                                            )
                                        } else {
                                            Modifier.background(Color(0xFFCBD5E1))
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Save Attendance",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // ---------------------------------------------------------
                        // 7. CANCEL BUTTON
                        // ---------------------------------------------------------
                        FilledTonalButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFEFF6FF),
                                contentColor = Color(0xFF2563EB)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("cancel_attendance_popup_btn")
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2563EB)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // AdMob Banner inside Attendance Dialog
                        AdMobBanner(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Attendance Selection Option Button in the 3-column grid.
 * White background, light-gray border, rounded corners, subtle shadow.
 * Selected: blue border, blue text, light-blue background.
 */
@Composable
fun AttendanceOptionButton(
    option: AttendanceOption,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
        border = BorderStroke(
            width = if (isSelected) 1.8.dp else 1.dp,
            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (isSelected) 2.dp else 1.dp,
        modifier = modifier
            .height(58.dp)
            .testTag("option_${option.statusCode.lowercase()}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = option.title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF1E293B),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = option.subtitle,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Fallback AttendanceScreen if referenced anywhere.
 */
@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    AttendancePopupDialog(
        viewModel = viewModel,
        onDismiss = onBack
    )
}

// -------------------------------------------------------------------------
// Helper functions
// -------------------------------------------------------------------------
private fun formatStatusName(status: String): String {
    return when (status) {
        "FULL_DAY" -> "Present (Full Day)"
        "HALF_DAY" -> "Half Day"
        "ABSENT" -> "Absent"
        "ONE_AND_HALF_DAY" -> "One & Half Day"
        "DOUBLE_DAY" -> "Double Day"
        "CUSTOM" -> "Custom"
        "OVERTIME" -> "Full Day (OT)"
        else -> status
    }
}

private fun formatNiceDate(dateString: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val date = parser.parse(dateString) ?: return dateString
        val formatter = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH)
        formatter.format(date)
    } catch (e: Exception) {
        dateString
    }
}
