package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceRecord
import com.example.viewmodel.AttendanceViewModel
import java.util.*

@Composable
fun MonthlyAttendanceViewScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateTab: (Int) -> Unit
) {
    BackHandler { onNavigateBack() }

    val profile by viewModel.userProfile.collectAsState()
    val monthName by viewModel.selectedMonthName.collectAsState()
    val year by viewModel.selectedYear.collectAsState()
    val records by viewModel.monthRecords.collectAsState()
    val advances by viewModel.monthAdvances.collectAsState()
    val isMonthLocked by viewModel.isCurrentMonthLocked.collectAsState()
    val selectedCalendar by viewModel.selectedCalendar.collectAsState()

    // State for Attendance Entry Popup
    var showAttendancePopup by remember { mutableStateOf(false) }
    var selectedDayForPopup by remember { mutableStateOf<Int?>(null) }

    // Map existing attendance records by day of month
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

    // First day of week for current month (1 = Sunday, 2 = Monday, ..., 7 = Saturday)
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

    // Leading empty cells (Sun=0, Mon=1, ..., Sat=6)
    val leadingEmptyCells = firstDayOfWeek - 1

    // Calculations for the 5 summary categories (NO Holiday)
    val presentCount = remember(records) {
        records.count { it.dayUnits == 1.0 && it.status != "ABSENT" }
    }
    val halfDayCount = remember(records) {
        records.count { it.dayUnits == 0.5 }
    }
    val absentCount = remember(records) {
        records.count { it.dayUnits == 0.0 || it.status == "ABSENT" }
    }
    val oneAndHalfCount = remember(records) {
        records.count { it.dayUnits == 1.5 }
    }
    val doubleDayCount = remember(records) {
        records.count { it.dayUnits == 2.0 }
    }

    // Financial & Worked totals
    val totalWorkedDays = remember(records) {
        records.sumOf { it.dayUnits }
    }
    val totalWages = remember(records) {
        records.sumOf { it.totalEarnings }
    }
    val totalAdvance = remember(advances) {
        advances.sumOf { it.amount }
    }
    val netPayable = remember(totalWages, totalAdvance) {
        totalWages - totalAdvance
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF4F7FC),
        topBar = {
            // 1. TOP HEADER - Light-blue gradient header with subtle shadow & back arrow
            MonthlyViewHeader(
                onBack = onNavigateBack
            )
        },
        bottomBar = {
            // 8. FIXED BOTTOM NAVIGATION BAR - Tab 1 (Calendar) selected
            DashboardBottomNavigation(
                selectedIndex = 1,
                isMonthLocked = isMonthLocked,
                onTabSelected = { index ->
                    if (index == 0) {
                        onNavigateBack()
                    } else {
                        onNavigateTab(index)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // 2. MONTHLY ATTENDANCE CALENDAR CARD
            MonthlyCalendarCard(
                monthName = monthName,
                year = year,
                daysInMonth = daysInMonth,
                leadingEmptyCells = leadingEmptyCells,
                recordByDay = recordByDay,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() },
                onDateSelected = { day ->
                    selectedDayForPopup = day
                    showAttendancePopup = true
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. MONTHLY SUMMARY CARD (5 Categories: Present, Half Day, Absent, 1.5x Day, 2x Day)
            // Plus 4. Total Worked Days, 5. Total Wages, 6. Total Advance, 7. Net Payable
            MonthlySummaryAndPayrollCard(
                presentCount = presentCount,
                halfDayCount = halfDayCount,
                absentCount = absentCount,
                oneAndHalfCount = oneAndHalfCount,
                doubleDayCount = doubleDayCount,
                totalWorkedDays = totalWorkedDays,
                totalWages = totalWages,
                totalAdvance = totalAdvance,
                netPayable = netPayable,
                currencySymbol = profile.currencySymbol
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Attendance Entry Popup Modal when tapped on any date
    if (showAttendancePopup) {
        AttendancePopupDialog(
            viewModel = viewModel,
            initialDay = selectedDayForPopup,
            onDismiss = {
                showAttendancePopup = false
                selectedDayForPopup = null
            }
        )
    }
}

/**
 * 1. Top Header: Light-blue gradient header with subtle shadow, dark bold text, and back arrow.
 */
@Composable
fun MonthlyViewHeader(
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color.Transparent,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("monthly_view_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = Color(0xFF0F172A)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "Monthly Attendance View",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.testTag("monthly_view_title")
                )
            }
        }
    }
}

/**
 * 2. Monthly Attendance Calendar Card:
 * White calendar card, rounded corners, subtle shadow.
 * Month navigation (< Month Year >), 7-column weekday headers (Sun..Sat),
 * date numbers, and circular colored attendance indicators below date numbers.
 */
@Composable
fun MonthlyCalendarCard(
    monthName: String,
    year: Int,
    daysInMonth: Int,
    leadingEmptyCells: Int,
    recordByDay: Map<Int, AttendanceRecord>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("monthly_calendar_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Month & Navigation Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF1F5F9), CircleShape)
                        .testTag("calendar_prev_month")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Month",
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "$monthName $year",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF1F5F9), CircleShape)
                        .testTag("calendar_next_month")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Month",
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Weekday Headers: Sun, Mon, Tue, Wed, Thu, Fri, Sat
            val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weekdays.forEach { dayName ->
                    Text(
                        text = dayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (dayName == "Sun") Color(0xFFDC2626) else Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 7-Column Grid of Dates
            val totalCells = leadingEmptyCells + daysInMonth
            val totalRows = (totalCells + 6) / 7

            for (rowIndex in 0 until totalRows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (colIndex in 0 until 7) {
                        val cellIndex = rowIndex * 7 + colIndex
                        val dayNumber = cellIndex - leadingEmptyCells + 1

                        if (dayNumber in 1..daysInMonth) {
                            val record = recordByDay[dayNumber]
                            CalendarDateCell(
                                dayNumber = dayNumber,
                                record = record,
                                modifier = Modifier.weight(1f),
                                onClick = { onDateSelected(dayNumber) }
                            )
                        } else {
                            // Empty spacing cell
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single Date Cell in Calendar:
 * Displays date number in dark text, and circular attendance badge below it if attendance is recorded.
 */
@Composable
fun CalendarDateCell(
    dayNumber: Int,
    record: AttendanceRecord?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        modifier = modifier
            .padding(1.dp)
            .height(52.dp)
            .testTag("calendar_day_$dayNumber")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Date number
            Text(
                text = dayNumber.toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )

            // Circular Attendance Badge (when attendance is recorded)
            if (record != null) {
                AttendanceBadge(record = record)
            } else {
                // Placeholder to preserve vertical height symmetry
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

/**
 * Attendance Badge Indicator:
 * - Present: Green circle with white "P"
 * - Absent: Red circle with white "A"
 * - Half Day: Orange indicator with white "0.5"
 * - One & Half Day: Blue indicator with "1.5"
 * - Double Day: Purple indicator with "2x"
 * - Custom: Appropriate indicator with decimal value or custom badge
 */
@Composable
fun AttendanceBadge(record: AttendanceRecord) {
    val (bgColor, label) = when {
        record.status == "ABSENT" || record.dayUnits == 0.0 -> {
            Pair(Color(0xFFDC2626), "A")
        }
        record.dayUnits == 0.5 || record.status == "HALF_DAY" -> {
            Pair(Color(0xFFEA580C), "0.5")
        }
        record.dayUnits == 1.0 && (record.status == "FULL_DAY" || record.status == "OVERTIME" || record.status == "PRESENT") -> {
            Pair(Color(0xFF16A34A), "P")
        }
        record.dayUnits == 1.5 || record.status == "ONE_AND_HALF_DAY" -> {
            Pair(Color(0xFF2563EB), "1.5")
        }
        record.dayUnits == 2.0 || record.status == "DOUBLE_DAY" -> {
            Pair(Color(0xFF9333EA), "2x")
        }
        else -> {
            val formatted = if (record.dayUnits % 1.0 == 0.0) {
                record.dayUnits.toInt().toString()
            } else {
                record.dayUnits.toString()
            }
            Pair(Color(0xFF0D9488), formatted)
        }
    }

    Box(
        modifier = Modifier
            .size(19.dp)
            .background(bgColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 3. Monthly Summary Card:
 * Displays exactly 5 categories (NO Holiday):
 * 1. Present (Green checkmark)
 * 2. Half Day (Orange indicator)
 * 3. Absent (Red cross)
 * 4. 1.5x Day (Blue badge/award icon)
 * 5. 2x Day (Purple double-arrow icon)
 *
 * Followed by Divider and:
 * 4. Total Worked Days
 * 5. Total Wages
 * 6. Total Advance
 * 7. Net Payable
 */
@Composable
fun MonthlySummaryAndPayrollCard(
    presentCount: Int,
    halfDayCount: Int,
    absentCount: Int,
    oneAndHalfCount: Int,
    doubleDayCount: Int,
    totalWorkedDays: Double,
    totalWages: Double,
    totalAdvance: Double,
    netPayable: Double,
    currencySymbol: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("monthly_summary_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Card Title
            Text(
                text = "Monthly Summary",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Two-column responsive grid with exactly 5 summary categories
            // Row 1: Present & Half Day
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCategoryItem(
                    label = "Present",
                    count = presentCount,
                    icon = Icons.Default.CheckCircle,
                    iconColor = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f)
                )
                SummaryCategoryItem(
                    label = "Half Day",
                    count = halfDayCount,
                    icon = Icons.Default.Schedule,
                    iconColor = Color(0xFFEA580C),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Absent & 1.5x Day
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCategoryItem(
                    label = "Absent",
                    count = absentCount,
                    icon = Icons.Default.Cancel,
                    iconColor = Color(0xFFDC2626),
                    modifier = Modifier.weight(1f)
                )
                SummaryCategoryItem(
                    label = "1.5x Day",
                    count = oneAndHalfCount,
                    icon = Icons.Default.MilitaryTech,
                    iconColor = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: 2x Day
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCategoryItem(
                    label = "2x Day",
                    count = doubleDayCount,
                    icon = Icons.Default.KeyboardDoubleArrowUp,
                    iconColor = Color(0xFF9333EA),
                    modifier = Modifier.weight(1f)
                )
                // Spacer for balanced 2-column layout
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // 4. TOTAL WORKED DAYS (Formatted to 2 decimal places, e.g. 25.00)
            PayrollMetricRow(
                icon = Icons.Default.DateRange,
                iconColor = Color(0xFF0284C7),
                iconBgColor = Color(0xFFE0F2FE),
                label = "Total Worked Days",
                value = String.format(Locale.ENGLISH, "%.2f Days", totalWorkedDays),
                valueColor = Color(0xFF0F172A),
                isBold = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5. TOTAL WAGES (Indian Rupee formatting)
            PayrollMetricRow(
                icon = Icons.Default.AccountBalanceWallet,
                iconColor = Color(0xFF7C3AED),
                iconBgColor = Color(0xFFF3E8FF),
                label = "Total Wages",
                value = AttendanceViewModel.formatCurrency(totalWages, currencySymbol),
                valueColor = Color(0xFF1E293B),
                isBold = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 6. TOTAL ADVANCE (Indian Rupee formatting)
            PayrollMetricRow(
                icon = Icons.Default.AccountBalance,
                iconColor = Color(0xFFEA580C),
                iconBgColor = Color(0xFFFFEDD5),
                label = "Total Advance",
                value = AttendanceViewModel.formatCurrency(totalAdvance, currencySymbol),
                valueColor = Color(0xFFDC2626),
                isBold = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 7. NET PAYABLE (Total Wages - Total Advance, in bold with Indian Rupee formatting)
            PayrollMetricRow(
                icon = Icons.Default.Calculate,
                iconColor = Color(0xFF16A34A),
                iconBgColor = Color(0xFFDCFCE7),
                label = "Net Payable",
                value = AttendanceViewModel.formatCurrency(netPayable, currencySymbol),
                valueColor = Color(0xFF15803D),
                isBold = true,
                isHighlight = true
            )
        }
    }
}

/**
 * Individual Summary Category Item inside the 2-column grid.
 * Light gray background, rounded corners, colored icon, category label, dynamic count.
 */
@Composable
fun SummaryCategoryItem(
    label: String,
    count: Int,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .height(58.dp)
            .testTag("summary_${label.lowercase().replace(" ", "_").replace(".", "_")}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(iconColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$count",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Full-width row for Payroll Metrics (Total Worked Days, Total Wages, Total Advance, Net Payable).
 */
@Composable
fun PayrollMetricRow(
    icon: ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    label: String,
    value: String,
    valueColor: Color,
    isBold: Boolean = false,
    isHighlight: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isHighlight) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
        border = BorderStroke(
            1.dp,
            if (isHighlight) Color(0xFFBBF7D0) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(iconBgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
            }

            Text(
                text = value,
                fontSize = if (isHighlight) 16.sp else 14.sp,
                fontWeight = if (isBold || isHighlight) FontWeight.Bold else FontWeight.Medium,
                color = valueColor,
                textAlign = TextAlign.End
            )
        }
    }
}
