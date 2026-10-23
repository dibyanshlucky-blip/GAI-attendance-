package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.viewmodel.AttendanceViewModel
import com.example.viewmodel.PendingAttendanceSummary
import com.example.viewmodel.PendingDateItem
import com.example.viewmodel.UserProfile
import java.util.*

enum class ActiveDialog {
    NONE,
    MARK_ATTENDANCE,
    MONTHLY_VIEW,
    TOTAL_WAGE,
    ADVANCE,
    WORKED_DAYS,
    NET_PAYABLE,
    NOTES,
    SETTINGS,
    WAGE_CALCULATOR,
    PDF_REPORT,
    LOCK_CONFIRMATION
}

enum class DashboardViewMode {
    DASHBOARD,
    MONTHLY_VIEW,
    ADVANCE,
    PDF_REPORT
}

@Composable
fun DashboardScreen(viewModel: AttendanceViewModel) {
    val profile by viewModel.userProfile.collectAsState()
    val monthName by viewModel.selectedMonthName.collectAsState()
    val year by viewModel.selectedYear.collectAsState()
    val yearMonth by viewModel.selectedYearMonth.collectAsState()
    val records by viewModel.monthRecords.collectAsState()
    val advances by viewModel.monthAdvances.collectAsState()
    val notes by viewModel.monthNotes.collectAsState()
    val isMonthLocked by viewModel.isCurrentMonthLocked.collectAsState()

    val workedDays by viewModel.workedDays.collectAsState()
    val totalWage by viewModel.totalWage.collectAsState()
    val totalAdvance by viewModel.totalAdvance.collectAsState()
    val netPayable by viewModel.netPayable.collectAsState()

    val pendingSummary by viewModel.pendingAttendanceState.collectAsState()
    val isPendingDismissed by viewModel.isPendingPopupDismissedThisSession.collectAsState()

    var activeDialog by remember { mutableStateOf(ActiveDialog.NONE) }
    var activeBottomTab by remember { mutableIntStateOf(0) } // 0: Home, 1: Calendar, 2: Lock Month, 3: Advance, 4: Calculator
    var showAttendancePopup by remember { mutableStateOf(false) }
    var attendanceTargetDay by remember { mutableStateOf<Int?>(null) }
    var showPendingDialog by remember { mutableStateOf(false) }
    var currentViewMode by remember { mutableStateOf(DashboardViewMode.DASHBOARD) }

    // Automatic Pending Attendance Popup:
    // Automatically displays when attendance records are loaded, pending attendance exists,
    // and the user has not temporarily dismissed it for the current session.
    LaunchedEffect(pendingSummary.isDataLoaded, pendingSummary.totalPendingCount, isPendingDismissed) {
        if (pendingSummary.isDataLoaded && pendingSummary.totalPendingCount > 0 && !isPendingDismissed) {
            showPendingDialog = true
        } else if (pendingSummary.totalPendingCount == 0) {
            showPendingDialog = false
        }
    }

    // App Reopened Lifecycle Hook: Rechecks attendance on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAppReopened()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun getActiveTabForCurrentMode(): Int = when (currentViewMode) {
        DashboardViewMode.MONTHLY_VIEW -> 1
        DashboardViewMode.ADVANCE -> 3
        else -> 0
    }

    if (currentViewMode == DashboardViewMode.MONTHLY_VIEW) {
        MonthlyAttendanceViewScreen(
            viewModel = viewModel,
            onNavigateBack = {
                currentViewMode = DashboardViewMode.DASHBOARD
                activeBottomTab = 0
            },
            onNavigateTab = { index ->
                when (index) {
                    0 -> {
                        currentViewMode = DashboardViewMode.DASHBOARD
                        activeBottomTab = 0
                    }
                    1 -> {
                        // Already on Monthly View
                    }
                    2 -> activeDialog = ActiveDialog.LOCK_CONFIRMATION
                    3 -> {
                        currentViewMode = DashboardViewMode.ADVANCE
                        activeBottomTab = 3
                    }
                    4 -> activeDialog = ActiveDialog.WAGE_CALCULATOR
                }
            }
        )
    } else if (currentViewMode == DashboardViewMode.ADVANCE) {
        AdvanceManagementScreen(
            viewModel = viewModel,
            onNavigateBack = {
                currentViewMode = DashboardViewMode.DASHBOARD
                activeBottomTab = 0
            },
            onNavigateTab = { index ->
                when (index) {
                    0 -> {
                        currentViewMode = DashboardViewMode.DASHBOARD
                        activeBottomTab = 0
                    }
                    1 -> {
                        currentViewMode = DashboardViewMode.MONTHLY_VIEW
                        activeBottomTab = 1
                    }
                    2 -> activeDialog = ActiveDialog.LOCK_CONFIRMATION
                    3 -> {
                        // Already on Advance
                    }
                    4 -> activeDialog = ActiveDialog.WAGE_CALCULATOR
                }
            }
        )
    } else if (currentViewMode == DashboardViewMode.PDF_REPORT) {
        PdfReportScreen(
            viewModel = viewModel,
            onNavigateBack = {
                currentViewMode = DashboardViewMode.DASHBOARD
                activeBottomTab = 0
            },
            onNavigateTab = { index ->
                when (index) {
                    0 -> {
                        currentViewMode = DashboardViewMode.DASHBOARD
                        activeBottomTab = 0
                    }
                    1 -> {
                        currentViewMode = DashboardViewMode.MONTHLY_VIEW
                        activeBottomTab = 1
                    }
                    2 -> activeDialog = ActiveDialog.LOCK_CONFIRMATION
                    3 -> {
                        currentViewMode = DashboardViewMode.ADVANCE
                        activeBottomTab = 3
                    }
                    4 -> activeDialog = ActiveDialog.WAGE_CALCULATOR
                }
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFFF4F7FC),
            topBar = {
                // Fixed Transparent Header Section
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        TopHeaderSection(
                            profile = profile,
                            monthName = monthName,
                            year = year,
                            isMonthLocked = isMonthLocked,
                            onPreviousMonth = { viewModel.previousMonth() },
                            onNextMonth = { viewModel.nextMonth() },
                            onProfileClick = { activeDialog = ActiveDialog.SETTINGS }
                        )
                    }
                }
            },
            bottomBar = {
                DashboardBottomNavigation(
                    selectedIndex = activeBottomTab,
                    isMonthLocked = isMonthLocked,
                    onTabSelected = { index ->
                        activeBottomTab = index
                        when (index) {
                            0 -> { /* Home tab, active */ }
                            1 -> {
                                currentViewMode = DashboardViewMode.MONTHLY_VIEW
                            }
                            2 -> activeDialog = ActiveDialog.LOCK_CONFIRMATION
                            3 -> {
                                currentViewMode = DashboardViewMode.ADVANCE
                            }
                            4 -> activeDialog = ActiveDialog.WAGE_CALCULATOR
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // AdMob Banner Ad directly below Header
            AdMobBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // Pending Attendance Banner (shown if pending entries exist and dialogs are closed)
            if (pendingSummary.totalPendingCount > 0 && !showPendingDialog && !showAttendancePopup) {
                Surface(
                    onClick = {
                        viewModel.onAppReopened()
                        showPendingDialog = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("banner_pending_attendance")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EditCalendar,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${pendingSummary.totalPendingCount} Attendance Entries Pending",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB91C1C)
                                )
                                Text(
                                    text = "Tap to review & complete pending days",
                                    fontSize = 11.sp,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ---------------------------------------------------------
            // 2. THE 10 DASHBOARD CARDS (2-Column Grid)
            // ---------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 1: Attendance
                val hasPending = pendingSummary.totalPendingCount > 0
                DashboardCard(
                    icon = Icons.Default.CalendarToday,
                    iconBgColor = Color(0xFF2563EB), // Royal Blue
                    title = "Attendance",
                    subtitle = if (hasPending) "${pendingSummary.totalPendingCount} Pending" else "Mark attendance",
                    subtitleColor = if (hasPending) Color(0xFFDC2626) else Color(0xFF64748B),
                    modifier = Modifier.weight(1f),
                    testTag = "card_attendance",
                    onClick = {
                        showPendingDialog = false
                        attendanceTargetDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                        showAttendancePopup = true
                    }
                )

                // Card 2: Monthly View
                DashboardCard(
                    icon = Icons.Default.CalendarMonth,
                    iconBgColor = Color(0xFF16A34A), // Green
                    title = "Monthly View",
                    subtitle = "View calendar",
                    modifier = Modifier.weight(1f),
                    testTag = "card_monthly_view",
                    onClick = {
                        currentViewMode = DashboardViewMode.MONTHLY_VIEW
                        activeBottomTab = 1
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 3: Total Wage
                DashboardCard(
                    icon = Icons.Default.AccountBalanceWallet,
                    iconBgColor = Color(0xFF9333EA), // Purple
                    title = "Total Wage",
                    subtitle = "View Monthly Summary",
                    value = AttendanceViewModel.formatCurrency(totalWage, profile.currencySymbol),
                    modifier = Modifier.weight(1f),
                    testTag = "card_total_wage",
                    onClick = { activeDialog = ActiveDialog.TOTAL_WAGE }
                )

                // Card 4: Advance
                DashboardCard(
                    icon = Icons.Default.AccountBalance,
                    iconBgColor = Color(0xFFEA580C), // Orange
                    title = "Advance",
                    subtitle = "Add/View/Delete",
                    value = AttendanceViewModel.formatCurrency(totalAdvance, profile.currencySymbol),
                    modifier = Modifier.weight(1f),
                    testTag = "card_advance",
                    onClick = {
                        currentViewMode = DashboardViewMode.ADVANCE
                        activeBottomTab = 3
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 5: Worked Days
                DashboardCard(
                    icon = Icons.Default.Schedule,
                    iconBgColor = Color(0xFF0D9488), // Teal
                    title = "Worked Days",
                    subtitle = "View Monthly Summary",
                    value = AttendanceViewModel.formatDays(workedDays),
                    modifier = Modifier.weight(1f),
                    testTag = "card_worked_days",
                    onClick = { activeDialog = ActiveDialog.WORKED_DAYS }
                )

                // Card 6: Net Payable
                DashboardCard(
                    icon = Icons.Default.Calculate,
                    iconBgColor = Color(0xFF059669), // Emerald
                    title = "Net Payable",
                    subtitle = "Click to View Summary",
                    value = AttendanceViewModel.formatCurrency(netPayable, profile.currencySymbol),
                    modifier = Modifier.weight(1f),
                    testTag = "card_net_payable",
                    onClick = { activeDialog = ActiveDialog.NET_PAYABLE }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 7: Notes
                DashboardCard(
                    icon = Icons.Default.EditNote,
                    iconBgColor = Color(0xFF0284C7), // Sky Blue
                    title = "Notes",
                    subtitle = "Manage your notes",
                    modifier = Modifier.weight(1f),
                    testTag = "card_notes",
                    onClick = { activeDialog = ActiveDialog.NOTES }
                )

                // Card 8: Settings
                DashboardCard(
                    icon = Icons.Default.Settings,
                    iconBgColor = Color(0xFF475569), // Slate Gray
                    title = "Settings",
                    subtitle = "Profile and Data",
                    modifier = Modifier.weight(1f),
                    testTag = "card_settings",
                    onClick = { activeDialog = ActiveDialog.SETTINGS }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 9: Wage Calculator
                DashboardCard(
                    icon = Icons.Default.Calculate,
                    iconBgColor = Color(0xFF4F46E5), // Indigo
                    title = "Wage Calculator",
                    subtitle = "Calculate wages quickly",
                    modifier = Modifier.weight(1f),
                    testTag = "card_wage_calculator",
                    onClick = { activeDialog = ActiveDialog.WAGE_CALCULATOR }
                )

                // Card 10: PDF Report
                DashboardCard(
                    icon = Icons.Default.PictureAsPdf,
                    iconBgColor = Color(0xFFDC2626), // Red
                    title = "PDF Report",
                    subtitle = "Monthly or Custom",
                    modifier = Modifier.weight(1f),
                    testTag = "card_pdf_report",
                    onClick = { currentViewMode = DashboardViewMode.PDF_REPORT }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AdMob Banner Ad on Home Page
            AdMobBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
    }

    // -------------------------------------------------------------------------
    // DIALOGS & ACTION SHEETS
    // -------------------------------------------------------------------------
    if (showPendingDialog && pendingSummary.totalPendingCount > 0) {
        PendingAttendanceDialog(
            summary = pendingSummary,
            onDismiss = {
                viewModel.dismissPendingPopupTemporarily()
                showPendingDialog = false
            },
            onRemindMeLater = {
                viewModel.dismissPendingPopupTemporarily()
                showPendingDialog = false
            },
            onMarkToday = {
                showPendingDialog = false
                val today = pendingSummary.todayItem
                val targetYear = today?.year ?: Calendar.getInstance().get(Calendar.YEAR)
                val targetMonth = today?.monthIndex ?: Calendar.getInstance().get(Calendar.MONTH)
                val targetDay = today?.dayOfMonth ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                viewModel.setMonth(targetYear, targetMonth)
                attendanceTargetDay = targetDay
                showAttendancePopup = true
            },
            onMarkDate = { item ->
                showPendingDialog = false
                viewModel.setMonth(item.year, item.monthIndex)
                attendanceTargetDay = item.dayOfMonth
                showAttendancePopup = true
            }
        )
    }

    if (showAttendancePopup) {
        AttendancePopupDialog(
            viewModel = viewModel,
            initialDay = attendanceTargetDay,
            onDismiss = {
                showAttendancePopup = false
                attendanceTargetDay = null
                activeBottomTab = getActiveTabForCurrentMode()
            }
        )
    }

    when (activeDialog) {
        ActiveDialog.NONE -> { /* No Dialog */ }

        ActiveDialog.MARK_ATTENDANCE -> {
            AttendancePopupDialog(
                viewModel = viewModel,
                onDismiss = {
                    activeDialog = ActiveDialog.NONE
                    activeBottomTab = getActiveTabForCurrentMode()
                }
            )
        }

        ActiveDialog.MONTHLY_VIEW -> {
            currentViewMode = DashboardViewMode.MONTHLY_VIEW
            activeBottomTab = 1
            activeDialog = ActiveDialog.NONE
        }

        ActiveDialog.TOTAL_WAGE -> {
            SummaryDetailDialog(
                title = "Total Wage",
                subtitle = "Monthly Gross Earnings",
                workedDays = workedDays,
                totalWage = totalWage,
                totalAdvance = totalAdvance,
                netPayable = netPayable,
                profile = profile,
                monthName = monthName,
                year = year,
                onDismiss = { activeDialog = ActiveDialog.NONE }
            )
        }

        ActiveDialog.ADVANCE -> {
            AdvanceDialog(
                viewModel = viewModel,
                advances = advances,
                totalAdvance = totalAdvance,
                profile = profile,
                yearMonth = yearMonth,
                onDismiss = {
                    activeDialog = ActiveDialog.NONE
                    activeBottomTab = getActiveTabForCurrentMode()
                }
            )
        }

        ActiveDialog.WORKED_DAYS -> {
            SummaryDetailDialog(
                title = "Worked Days",
                subtitle = "Monthly Attendance Breakdown",
                workedDays = workedDays,
                totalWage = totalWage,
                totalAdvance = totalAdvance,
                netPayable = netPayable,
                profile = profile,
                monthName = monthName,
                year = year,
                onDismiss = { activeDialog = ActiveDialog.NONE }
            )
        }

        ActiveDialog.NET_PAYABLE -> {
            SummaryDetailDialog(
                title = "Net Payable",
                subtitle = "Final Take-Home Wage",
                workedDays = workedDays,
                totalWage = totalWage,
                totalAdvance = totalAdvance,
                netPayable = netPayable,
                profile = profile,
                monthName = monthName,
                year = year,
                onDismiss = { activeDialog = ActiveDialog.NONE }
            )
        }

        ActiveDialog.NOTES -> {
            NotesDialog(
                viewModel = viewModel,
                notes = notes,
                yearMonth = yearMonth,
                onDismiss = { activeDialog = ActiveDialog.NONE }
            )
        }

        ActiveDialog.SETTINGS -> {
            SettingsDialog(
                viewModel = viewModel,
                profile = profile,
                onDismiss = { activeDialog = ActiveDialog.NONE }
            )
        }

        ActiveDialog.WAGE_CALCULATOR -> {
            WageCalculatorDialog(
                profile = profile,
                onDismiss = {
                    activeDialog = ActiveDialog.NONE
                    activeBottomTab = getActiveTabForCurrentMode()
                }
            )
        }

        ActiveDialog.PDF_REPORT -> {
            activeDialog = ActiveDialog.NONE
            currentViewMode = DashboardViewMode.PDF_REPORT
        }

        ActiveDialog.LOCK_CONFIRMATION -> {
            AlertDialog(
                onDismissRequest = {
                    activeDialog = ActiveDialog.NONE
                    activeBottomTab = getActiveTabForCurrentMode()
                },
                icon = {
                    Icon(
                        imageVector = if (isMonthLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isMonthLocked) Color(0xFF16A34A) else Color(0xFF2563EB),
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = if (isMonthLocked) "Unlock $monthName $year?" else "Lock $monthName $year?",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (isMonthLocked) {
                            "Unlocking will allow changes and edits to this month's attendance records and advance payments."
                        } else {
                            "Locking finalizes this month's payroll ($monthName $year). It safeguards records from accidental modifications."
                        }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.toggleLockMonth(yearMonth)
                            activeDialog = ActiveDialog.NONE
                            activeBottomTab = getActiveTabForCurrentMode()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMonthLocked) Color(0xFF16A34A) else Color(0xFF2563EB)
                        )
                    ) {
                        Text(if (isMonthLocked) "Unlock Month" else "Lock Month")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        activeDialog = ActiveDialog.NONE
                        activeBottomTab = getActiveTabForCurrentMode()
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// -------------------------------------------------------------------------
// TOP HEADER COMPOSABLE
// -------------------------------------------------------------------------
@Composable
fun TopHeaderSection(
    profile: UserProfile,
    monthName: String,
    year: Int,
    isMonthLocked: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onProfileClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: User Greeting & Daily Wage
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onProfileClick)
            ) {
                Text(
                    text = "Welcome back,",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = profile.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Daily Wage: ${profile.currencySymbol}${profile.dailyWage.toInt()}",
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Medium
                )
            }

            // Right: Month Navigator (< Month Year >)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color(0xFFF8FAFC), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = monthName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8) // Bold Blue
                            )
                            if (isMonthLocked) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = Color(0xFFEA580C),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Text(
                            text = year.toString(),
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color(0xFFF8FAFC), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// INDIVIDUAL DASHBOARD CARD (WHITE CONTAINER WITH SQUIRCLE ICON)
// -------------------------------------------------------------------------
@Composable
fun DashboardCard(
    icon: ImageVector,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    subtitleColor: Color = Color(0xFF64748B),
    value: String? = null,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .testTag(testTag)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color(0x1A000000),
                ambientColor = Color(0x0D000000)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5ECF6))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Colored Squircle Container for Icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(color = iconBgColor, shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Card Title
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Card Subtitle
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = subtitleColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Prominent Value (if applicable)
            if (value != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.3).sp
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// BOTTOM NAVIGATION BAR (Home, Calendar, Lock Month, Advance, Calculator)
// -------------------------------------------------------------------------
@Composable
fun DashboardBottomNavigation(
    selectedIndex: Int,
    isMonthLocked: Boolean,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = selectedIndex == 0,
                onClick = { onTabSelected(0) }
            )
            BottomNavItem(
                icon = Icons.Default.CalendarMonth,
                label = "Calendar",
                isSelected = selectedIndex == 1,
                onClick = { onTabSelected(1) }
            )
            BottomNavItem(
                icon = if (isMonthLocked) Icons.Default.Lock else Icons.Outlined.Lock,
                label = if (isMonthLocked) "Locked" else "Lock Month",
                isSelected = selectedIndex == 2,
                activeColor = if (isMonthLocked) Color(0xFFEA580C) else Color(0xFF2563EB),
                onClick = { onTabSelected(2) }
            )
            BottomNavItem(
                icon = Icons.Default.AccountBalance,
                label = "Advance",
                isSelected = selectedIndex == 3,
                onClick = { onTabSelected(3) }
            )
            BottomNavItem(
                icon = Icons.Default.Calculate,
                label = "Calculator",
                isSelected = selectedIndex == 4,
                onClick = { onTabSelected(4) }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    activeColor: Color = Color(0xFF2563EB),
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) activeColor.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) activeColor else Color(0xFF475569),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeColor else Color(0xFF475569)
        )
    }
}
