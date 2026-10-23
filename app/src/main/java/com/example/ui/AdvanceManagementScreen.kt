package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.data.AdvanceRecord
import com.example.viewmodel.AttendanceViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdvanceManagementScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateTab: (Int) -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsState()
    val isMonthLocked by viewModel.isCurrentMonthLocked.collectAsState()
    val monthName by viewModel.selectedMonthName.collectAsState()
    val year by viewModel.selectedYear.collectAsState()
    val monthAdvances by viewModel.monthAdvances.collectAsState()
    val totalAdvance by viewModel.totalAdvance.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // 0: Add Advance, 1: History
    var selectedTab by remember { mutableIntStateOf(0) }

    // Form State
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Date & Time State
    var isAutoDateTime by remember { mutableStateOf(true) }
    var currentCalendar by remember { mutableStateOf(Calendar.getInstance()) }

    // Live clock ticker
    LaunchedEffect(isAutoDateTime) {
        if (isAutoDateTime) {
            while (isActive) {
                currentCalendar = Calendar.getInstance()
                delay(1000L)
            }
        }
    }

    // Formatters
    val clockFormat = remember { SimpleDateFormat("hh : mm : ss  a", Locale.ENGLISH) }
    val fullDateFormat = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH) }
    val dbDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }
    val timeOnlyFormat = remember { SimpleDateFormat("hh:mm:ss a", Locale.ENGLISH) }

    // Dialog state for History edit & delete
    var advanceToEdit by remember { mutableStateOf<AdvanceRecord?>(null) }
    var advanceToDelete by remember { mutableStateOf<AdvanceRecord?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF4F7FC),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AdvanceTopHeader(
                onBack = onNavigateBack
            )
        },
        bottomBar = {
            // Bottom navigation with Tab 3 (Advance) highlighted
            DashboardBottomNavigation(
                selectedIndex = 3,
                isMonthLocked = isMonthLocked,
                onTabSelected = { index ->
                    if (index == 3) {
                        // Already on Advance screen
                    } else if (index == 0) {
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
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // AdMob Banner Ad directly below Header
            AdMobBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // -------------------------------------------------------------
            // 1. EMPLOYEE INFORMATION CARD
            // -------------------------------------------------------------
            EmployeeInfoCard(
                name = profile.name.ifBlank { "Abhimanyu Kumar" },
                dailyRate = profile.dailyWage.toInt(),
                currencySymbol = profile.currencySymbol
            )

            Spacer(modifier = Modifier.height(14.dp))

            // -------------------------------------------------------------
            // 2. ADD ADVANCE / HISTORY SEGMENTED TABS
            // -------------------------------------------------------------
            AdvanceSegmentedTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // -------------------------------------------------------------
            // TAB CONTENT: ADD ADVANCE vs HISTORY
            // -------------------------------------------------------------
            if (selectedTab == 0) {
                // ---------------------------------------------------------
                // 3. ADVANCE AMOUNT CARD (Orange-to-Yellow gradient header)
                // ---------------------------------------------------------
                AdvanceAmountInputCard(
                    amountText = amountText,
                    onAmountChange = {
                        amountText = it
                        amountError = null
                    },
                    currencySymbol = profile.currencySymbol,
                    errorMessage = amountError,
                    noteText = noteText,
                    onNoteChange = { noteText = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ---------------------------------------------------------
                // 4. DATE & TIME SECTION (Live digital clock + Auto toggle)
                // ---------------------------------------------------------
                DateTimeCard(
                    isAutoDateTime = isAutoDateTime,
                    onAutoToggle = { isAutoDateTime = it },
                    calendar = currentCalendar,
                    clockString = clockFormat.format(currentCalendar.time).uppercase(Locale.ENGLISH),
                    dateString = fullDateFormat.format(currentCalendar.time),
                    onPickDate = {
                        val cal = currentCalendar
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = (currentCalendar.clone() as Calendar).apply {
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                                currentCalendar = newCal
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    onPickTime = {
                        val cal = currentCalendar
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                val newCal = (currentCalendar.clone() as Calendar).apply {
                                    set(Calendar.HOUR_OF_DAY, hour)
                                    set(Calendar.MINUTE, minute)
                                    set(Calendar.SECOND, 0)
                                }
                                currentCalendar = newCal
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            false
                        ).show()
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ---------------------------------------------------------
                // 5. ADD ADVANCE BUTTON (Cyan/light-blue gradient)
                // ---------------------------------------------------------
                AddAdvanceButton(
                    isSubmitting = isSubmitting,
                    onClick = {
                        val parsedAmount = amountText.trim().toDoubleOrNull()
                        if (parsedAmount == null || parsedAmount <= 0.0) {
                            amountError = "Please enter a valid amount greater than 0"
                        } else {
                            amountError = null
                            isSubmitting = true
                            val saveDate = dbDateFormat.format(currentCalendar.time)
                            val saveTime = timeOnlyFormat.format(currentCalendar.time)
                            val note = noteText.trim().ifBlank { null }

                            viewModel.addAdvance(
                                amount = parsedAmount,
                                dateString = saveDate,
                                note = note,
                                timeString = saveTime
                            )

                            // Reset input and show confirmation
                            amountText = ""
                            noteText = ""
                            isSubmitting = false

                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Advance of ${profile.currencySymbol}${AttendanceViewModel.formatDays(parsedAmount)} added successfully!",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                )
            } else {
                // ---------------------------------------------------------
                // HISTORY TAB CONTENT
                // ---------------------------------------------------------
                AdvanceHistorySection(
                    viewModel = viewModel,
                    advances = monthAdvances,
                    totalAdvance = totalAdvance,
                    currencySymbol = profile.currencySymbol,
                    monthName = monthName,
                    year = year,
                    onEditAdvance = { advanceToEdit = it },
                    onDeleteAdvance = { advanceToDelete = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ---------------------------------------------------------------------
    // EDIT ADVANCE DIALOG
    // ---------------------------------------------------------------------
    if (advanceToEdit != null) {
        val advance = advanceToEdit!!
        EditAdvanceDialog(
            advance = advance,
            currencySymbol = profile.currencySymbol,
            onDismiss = { advanceToEdit = null },
            onSave = { updatedAmount, updatedDate, updatedTime, updatedNote ->
                viewModel.updateAdvance(
                    id = advance.id,
                    amount = updatedAmount,
                    dateString = updatedDate,
                    timeString = updatedTime,
                    note = updatedNote
                )
                advanceToEdit = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Advance record updated successfully!",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }

    // ---------------------------------------------------------------------
    // DELETE ADVANCE CONFIRMATION DIALOG
    // ---------------------------------------------------------------------
    if (advanceToDelete != null) {
        val advance = advanceToDelete!!
        AlertDialog(
            onDismissRequest = { advanceToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Advance?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete the advance payment of ${profile.currencySymbol}${advance.amount.toInt()} recorded on ${advance.dateString}?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAdvance(advance.id)
                        advanceToDelete = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Advance record deleted",
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { advanceToDelete = null }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }
}

/**
 * Top Header: Back Arrow + Bold Title "Advance Management"
 */
@Composable
fun AdvanceTopHeader(
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color.Transparent,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("advance_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "Advance Management",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                modifier = Modifier.testTag("advance_screen_title")
            )
        }
    }
}

/**
 * 1. Employee Information Card:
 * Displays employee name: Abhimanyu Kumar.
 * Displays daily wage: ₹700 (or ₹750 loaded dynamically).
 * White rounded card with subtle shadow and centered text.
 */
@Composable
fun EmployeeInfoCard(
    name: String,
    dailyRate: Int,
    currencySymbol: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("employee_info_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = name,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Daily Rate: $currencySymbol$dailyRate",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 2. Add Advance / History Tabs:
 * Segmented control with two options: "Add Advance" and "History".
 */
@Composable
fun AdvanceSegmentedTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("advance_segmented_tabs"),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFE2E8F0).copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 0: Add Advance
            Surface(
                onClick = { onTabSelected(0) },
                shape = RoundedCornerShape(10.dp),
                color = if (selectedTab == 0) Color.White else Color.Transparent,
                shadowElevation = if (selectedTab == 0) 1.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add Advance",
                        fontSize = 14.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 0) Color(0xFF0F172A) else Color(0xFF64748B)
                    )
                }
            }

            // Tab 1: History
            Surface(
                onClick = { onTabSelected(1) },
                shape = RoundedCornerShape(10.dp),
                color = if (selectedTab == 1) Color.White else Color.Transparent,
                shadowElevation = if (selectedTab == 1) 1.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "History",
                        fontSize = 14.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 1) Color(0xFF0F172A) else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

/**
 * 3. Advance Amount Card:
 * Orange-to-yellow gradient header with bank/building icon and "Advance Amount".
 * Below: Amount (₹) label, input field with ₹ prefix, placeholder "Enter advance amount".
 */
@Composable
fun AdvanceAmountInputCard(
    amountText: String,
    onAmountChange: (String) -> Unit,
    currencySymbol: String,
    errorMessage: String?,
    noteText: String,
    onNoteChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("advance_amount_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Gradient Header (Orange-to-Yellow / Amber)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFF97316), // Orange
                                Color(0xFFF59E0B)  // Amber
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = "Bank",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Advance Amount",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Card Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "Amount ($currencySymbol)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Input Field matching screenshot
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (errorMessage != null) Color(0xFFEF4444) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currencySymbol,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        TextField(
                            value = amountText,
                            onValueChange = onAmountChange,
                            placeholder = {
                                Text(
                                    text = "Enter advance amount",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 15.sp
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("advance_amount_input")
                        )
                    }
                }

                // Error validation message
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick selection chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(500, 1000, 2000, 5000).forEach { quickVal ->
                        Surface(
                            onClick = { onAmountChange(quickVal.toString()) },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+$currencySymbol$quickVal",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Optional note
                TextField(
                    value = noteText,
                    onValueChange = onNoteChange,
                    placeholder = {
                        Text(
                            text = "Purpose / Note (Optional)",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                )
            }
        }
    }
}

/**
 * 4. Date & Time Section:
 * Separate white rounded card with calendar icon and title "Date & Time".
 * Toggle switch for "Auto Date & Time (Current)" (checked by default).
 * Large live digital clock in 12-hour format: `11 : 14 : 52  AM`
 * Full date underneath: `Thursday, 22 October 2026`.
 */
@Composable
fun DateTimeCard(
    isAutoDateTime: Boolean,
    onAutoToggle: (Boolean) -> Unit,
    calendar: Calendar,
    clockString: String,
    dateString: String,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("date_time_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Title Header with calendar icon
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Date & Time",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Auto Date & Time (Current) Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto Date & Time (Current)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B)
                )

                Switch(
                    checked = isAutoDateTime,
                    onCheckedChange = onAutoToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF3B82F6),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.testTag("auto_date_time_switch")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Big Digital Clock Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Big Clock Display: 11 : 14 : 52  AM
                    Text(
                        text = clockString,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = 1.5.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Date underneath: Thursday, 22 October 2026
                    Text(
                        text = dateString,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    // If manual mode is active, allow tapping to pick date & time
                    if (!isAutoDateTime) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onPickDate,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Change Date", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = onPickTime,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Change Time", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. Add Advance Button:
 * Full-width button with cyan-to-light-blue gradient and rounded corners.
 * Button text: "+ Add Advance"
 */
@Composable
fun AddAdvanceButton(
    isSubmitting: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = !isSubmitting,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("add_advance_submit_button")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF38BDF8), // Cyan
                            Color(0xFF60A5FA)  // Light Blue
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "+ Add Advance",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * HISTORY TAB SECTION:
 * Shows monthly advance summary, total amount, and list of records with edit & delete.
 */
@Composable
fun AdvanceHistorySection(
    viewModel: AttendanceViewModel,
    advances: List<AdvanceRecord>,
    totalAdvance: Double,
    currencySymbol: String,
    monthName: String,
    year: Int,
    onEditAdvance: (AdvanceRecord) -> Unit,
    onDeleteAdvance: (AdvanceRecord) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Monthly Advance Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                // Month navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.previousMonth() },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month", modifier = Modifier.size(18.dp))
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
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary Row
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFF7ED),
                    border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Advance Taken", fontSize = 12.sp, color = Color(0xFFC2410C))
                            Text("${advances.size} entries this month", fontSize = 11.sp, color = Color(0xFFEA580C).copy(alpha = 0.8f))
                        }

                        Text(
                            text = AttendanceViewModel.formatCurrency(totalAdvance, currencySymbol),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC2410C)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History List
        Text(
            text = "Recorded Advances (${advances.size})",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (advances.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No advance payments recorded for $monthName $year.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                advances.forEach { advance ->
                    AdvanceHistoryItemCard(
                        advance = advance,
                        currencySymbol = currencySymbol,
                        onEdit = { onEditAdvance(advance) },
                        onDelete = { onDeleteAdvance(advance) }
                    )
                }
            }
        }
    }
}

/**
 * Individual Advance History Item Card with Edit & Delete options
 */
@Composable
fun AdvanceHistoryItemCard(
    advance: AdvanceRecord,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = AttendanceViewModel.formatCurrency(advance.amount, currencySymbol),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC2410C)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Date & Time
                val timeLabel = advance.timeString ?: ""
                val fullLabel = if (timeLabel.isNotBlank()) "${advance.dateString} • $timeLabel" else advance.dateString
                Text(
                    text = fullLabel,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )

                if (!advance.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = advance.note,
                        fontSize = 12.sp,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Edit and Delete Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Edit Advance Dialog: Allows changing amount, date, time, and note
 */
@Composable
fun EditAdvanceDialog(
    advance: AdvanceRecord,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (Double, String, String?, String?) -> Unit
) {
    val context = LocalContext.current
    var editAmount by remember { mutableStateOf(advance.amount.toString()) }
    var editDate by remember { mutableStateOf(advance.dateString) }
    var editTime by remember { mutableStateOf(advance.timeString ?: "11:00:00 AM") }
    var editNote by remember { mutableStateOf(advance.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Advance", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = editAmount,
                    onValueChange = {
                        editAmount = it
                        error = null
                    },
                    label = { Text("Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(error!!, color = Color.Red, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val parts = editDate.split("-")
                            val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
                            val m = (parts.getOrNull(1)?.toIntOrNull() ?: 10) - 1
                            val d = parts.getOrNull(2)?.toIntOrNull() ?: 22
                            DatePickerDialog(context, { _, year, month, day ->
                                editDate = String.format(Locale.ENGLISH, "%04d-%02d-%02d", year, month + 1, day)
                            }, y, m, d).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(editDate, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(context, { _, hour, minute ->
                                val ampm = if (hour >= 12) "PM" else "AM"
                                val h12 = if (hour % 12 == 0) 12 else hour % 12
                                editTime = String.format(Locale.ENGLISH, "%02d:%02d:00 %s", h12, minute, ampm)
                            }, 11, 0, false).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(editTime, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = editNote,
                    onValueChange = { editNote = it },
                    label = { Text("Note / Purpose") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = editAmount.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        error = "Enter a valid amount"
                    } else {
                        onSave(amt, editDate, editTime, editNote.ifBlank { null })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
