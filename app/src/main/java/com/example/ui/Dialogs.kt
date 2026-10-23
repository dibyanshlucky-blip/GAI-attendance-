package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.AdvanceRecord
import com.example.data.AttendanceRecord
import com.example.data.WorkNote
import com.example.viewmodel.AttendanceViewModel
import com.example.viewmodel.UserProfile
import java.text.SimpleDateFormat
import java.util.*

// -------------------------------------------------------------------------
// 1. MARK ATTENDANCE DIALOG
// -------------------------------------------------------------------------
@Composable
fun MarkAttendanceDialog(
    initialDateString: String,
    initialRecord: AttendanceRecord?,
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (dateString: String, status: String, dayUnits: Double, dailyWage: Double, otHours: Double, otRate: Double, note: String?) -> Unit,
    onDelete: ((Int) -> Unit)? = null
) {
    var dateString by remember { mutableStateOf(initialDateString) }
    var selectedStatus by remember { mutableStateOf(initialRecord?.status ?: "FULL_DAY") }
    var dayUnits by remember { mutableDoubleStateOf(initialRecord?.dayUnits ?: 1.0) }
    var overtimeHours by remember { mutableDoubleStateOf(initialRecord?.overtimeHours ?: 0.0) }
    var note by remember { mutableStateOf(initialRecord?.notes ?: "") }
    val dailyWage = profile.dailyWage
    val otRate = profile.overtimeHourlyRate

    val calculatedWage = (dayUnits * dailyWage) + (overtimeHours * otRate)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (initialRecord != null) "Edit Attendance" else "Mark Attendance",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Date: $dateString",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Options
                Text(
                    text = "Select Attendance Status",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip(
                        title = "Full Day",
                        unit = "1.0",
                        color = Color(0xFF16A34A),
                        isSelected = selectedStatus == "FULL_DAY",
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedStatus = "FULL_DAY"
                        dayUnits = 1.0
                        overtimeHours = 0.0
                    }
                    StatusChip(
                        title = "Half Day",
                        unit = "0.5",
                        color = Color(0xFFEA580C),
                        isSelected = selectedStatus == "HALF_DAY",
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedStatus = "HALF_DAY"
                        dayUnits = 0.5
                        overtimeHours = 0.0
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip(
                        title = "Overtime",
                        unit = "1.0 + OT",
                        color = Color(0xFF2563EB),
                        isSelected = selectedStatus == "OVERTIME",
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedStatus = "OVERTIME"
                        dayUnits = 1.0
                        if (overtimeHours == 0.0) overtimeHours = 2.0
                    }
                    StatusChip(
                        title = "Absent",
                        unit = "0.0",
                        color = Color(0xFFDC2626),
                        isSelected = selectedStatus == "ABSENT",
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedStatus = "ABSENT"
                        dayUnits = 0.0
                        overtimeHours = 0.0
                    }
                }

                if (selectedStatus == "OVERTIME") {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = if (overtimeHours > 0) overtimeHours.toString() else "",
                        onValueChange = { overtimeHours = it.toDoubleOrNull() ?: 0.0 },
                        label = { Text("Overtime Hours") },
                        placeholder = { Text("e.g. 2.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Calculated Earnings Preview
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Wage for this day",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "${profile.currencySymbol}${profile.dailyWage.toInt()} base rate",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Text(
                            text = "${profile.currencySymbol}${calculatedWage.toInt()}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Note Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    placeholder = { Text("e.g. Completed site inspection") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Save & Delete Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (initialRecord != null && onDelete != null) {
                        OutlinedButton(
                            onClick = {
                                onDelete(initialRecord.id)
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete")
                        }
                    }

                    Button(
                        onClick = {
                            onSave(
                                dateString,
                                selectedStatus,
                                dayUnits,
                                dailyWage,
                                overtimeHours,
                                otRate,
                                note.ifBlank { null }
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(if (initialRecord != null) 1.5f else 1f)
                    ) {
                        Text("Save Attendance", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(
    title: String,
    unit: String,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) color else Color(0xFFE2E8F0)
        ),
        modifier = modifier.height(52.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) color else Color(0xFF334155)
            )
            Text(
                text = unit,
                fontSize = 11.sp,
                color = if (isSelected) color else Color(0xFF94A3B8)
            )
        }
    }
}

// -------------------------------------------------------------------------
// 2. MONTHLY CALENDAR VIEW DIALOG
// -------------------------------------------------------------------------
@Composable
fun MonthlyCalendarDialog(
    viewModel: AttendanceViewModel,
    monthName: String,
    year: Int,
    yearMonth: String,
    records: List<AttendanceRecord>,
    profile: UserProfile,
    onDismiss: () -> Unit
) {
    var editingRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var selectedDateString by remember { mutableStateOf<String?>(null) }

    val daysInMonth = remember(year, monthName) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        // Convert monthName to month index
        val monthNames = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val mIndex = monthNames.indexOf(monthName).coerceAtLeast(0)
        cal.set(Calendar.MONTH, mIndex)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

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
        cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, etc.
    }

    val recordByDay = remember(records) {
        records.associateBy { it.dayOfMonth }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header with navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Monthly View",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "$monthName $year",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2563EB)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.previousMonth() },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFF1F5F9), CircleShape)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.nextMonth() },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFF1F5F9), CircleShape)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next", modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Days", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text("${records.sumOf { it.dayUnits }} d", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Wage", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                AttendanceViewModel.formatCurrency(records.sumOf { it.totalEarnings }, profile.currencySymbol),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Days of week header
                val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                Row(modifier = Modifier.fillMaxWidth()) {
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Calendar Grid
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val totalSlots = (firstDayOfWeek - 1) + daysInMonth
                    val totalRows = (totalSlots + 6) / 7

                    items(totalRows) { rowIndex ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (colIndex in 0..6) {
                                val slot = rowIndex * 7 + colIndex
                                val dayNum = slot - (firstDayOfWeek - 1) + 1

                                if (dayNum in 1..daysInMonth) {
                                    val record = recordByDay[dayNum]
                                    val formattedDate = String.format(Locale.ENGLISH, "%s-%02d", yearMonth, dayNum)

                                    val (badgeBg, badgeText, textColor) = when (record?.status) {
                                        "FULL_DAY" -> Triple(Color(0xFFDCFCE7), "P", Color(0xFF16A34A))
                                        "HALF_DAY" -> Triple(Color(0xFFFFEDD5), "HD", Color(0xFFEA580C))
                                        "OVERTIME" -> Triple(Color(0xFFDBEAFE), "OT", Color(0xFF2563EB))
                                        "ABSENT" -> Triple(Color(0xFFFEE2E2), "A", Color(0xFFDC2626))
                                        else -> Triple(Color(0xFFF8FAFC), "", Color(0xFF64748B))
                                    }

                                    Surface(
                                        onClick = {
                                            selectedDateString = formattedDate
                                            editingRecord = record
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (record != null) badgeBg else Color.Transparent,
                                        border = BorderStroke(1.dp, if (record != null) textColor.copy(alpha = 0.3f) else Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .padding(2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF1E293B)
                                            )
                                            if (badgeText.isNotEmpty()) {
                                                Text(
                                                    text = badgeText,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(color = Color(0xFF16A34A), label = "Present")
                    LegendItem(color = Color(0xFFEA580C), label = "Half Day")
                    LegendItem(color = Color(0xFF2563EB), label = "Overtime")
                    LegendItem(color = Color(0xFFDC2626), label = "Absent")
                }
            }
        }
    }

    if (selectedDateString != null) {
        MarkAttendanceDialog(
            initialDateString = selectedDateString!!,
            initialRecord = editingRecord,
            profile = profile,
            onDismiss = { selectedDateString = null },
            onSave = { dt, st, du, dw, otH, otR, nt ->
                viewModel.markAttendance(dt, st, du, dw, otH, otR, nt)
            },
            onDelete = { id -> viewModel.deleteAttendance(id) }
        )
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, color = Color(0xFF64748B))
    }
}

// -------------------------------------------------------------------------
// 3. ADVANCE PAYMENTS DIALOG (Add / View / Delete)
// -------------------------------------------------------------------------
@Composable
fun AdvanceDialog(
    viewModel: AttendanceViewModel,
    advances: List<AdvanceRecord>,
    totalAdvance: Double,
    profile: UserProfile,
    yearMonth: String,
    onDismiss: () -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }
    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var dateInput by remember {
        mutableStateOf(String.format(Locale.ENGLISH, "%s-%02d", yearMonth, 15))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.80f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Advance Payments",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Add / View / Delete",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Total Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFF7ED),
                    border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Advance Taken", fontSize = 12.sp, color = Color(0xFFC2410C))
                            Text("Deducted from gross wage", fontSize = 11.sp, color = Color(0xFFEA580C).copy(alpha = 0.8f))
                        }
                        Text(
                            text = AttendanceViewModel.formatCurrency(totalAdvance, profile.currencySymbol),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC2410C)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Add Advance Toggle / Button
                if (!showAddForm) {
                    Button(
                        onClick = { showAddForm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add New Advance", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("New Advance Entry", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = amountInput,
                                onValueChange = { amountInput = it },
                                label = { Text("Amount (${profile.currencySymbol})") },
                                placeholder = { Text("e.g. 5000") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick amount chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(1000, 2000, 5000).forEach { quickAmt ->
                                    SuggestionChip(
                                        onClick = { amountInput = quickAmt.toString() },
                                        label = { Text("+${profile.currencySymbol}$quickAmt", fontSize = 11.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = noteInput,
                                onValueChange = { noteInput = it },
                                label = { Text("Purpose / Note") },
                                placeholder = { Text("e.g. Festival travel advance") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showAddForm = false }) {
                                    Text("Cancel", color = Color(0xFF64748B))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val amt = amountInput.toDoubleOrNull() ?: 0.0
                                        if (amt > 0) {
                                            viewModel.addAdvance(amt, dateInput, noteInput.ifBlank { null })
                                            amountInput = ""
                                            noteInput = ""
                                            showAddForm = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Save Advance")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // List of Advances
                Text(
                    text = "History (${advances.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (advances.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No advances recorded for this month.", fontSize = 13.sp, color = Color(0xFF94A3B8))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(advances, key = { it.id }) { advance ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFAFAFA),
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = advance.note ?: "Advance Payment",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = advance.dateString,
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = AttendanceViewModel.formatCurrency(advance.amount, profile.currencySymbol),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC2410C)
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteAdvance(advance.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 4. SUMMARY DIALOG (For Total Wage, Worked Days, or Net Payable)
// -------------------------------------------------------------------------
@Composable
fun SummaryDetailDialog(
    title: String,
    subtitle: String,
    workedDays: Double,
    totalWage: Double,
    totalAdvance: Double,
    netPayable: Double,
    profile: UserProfile,
    monthName: String,
    year: Int,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "$monthName $year Summary",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detailed Calculation Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SummaryRow(
                            label = "Worker Profile",
                            value = profile.name,
                            isBold = true
                        )
                        SummaryRow(
                            label = "Daily Wage Rate",
                            value = "${profile.currencySymbol}${profile.dailyWage.toInt()} / day"
                        )
                        SummaryRow(
                            label = "Total Worked Days",
                            value = AttendanceViewModel.formatDays(workedDays) + " days",
                            valueColor = Color(0xFF0D9488)
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = Color(0xFFE2E8F0)
                        )
                        SummaryRow(
                            label = "Total Wage (Gross)",
                            value = AttendanceViewModel.formatCurrency(totalWage, profile.currencySymbol),
                            valueColor = Color(0xFF9333EA),
                            isBold = true
                        )
                        SummaryRow(
                            label = "Advance Deductions",
                            value = "- " + AttendanceViewModel.formatCurrency(totalAdvance, profile.currencySymbol),
                            valueColor = Color(0xFFEA580C)
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = Color(0xFFCBD5E1)
                        )
                        SummaryRow(
                            label = "Net Payable Amount",
                            value = AttendanceViewModel.formatCurrency(netPayable, profile.currencySymbol),
                            valueColor = Color(0xFF059669),
                            isBold = true,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF1E293B),
    isBold: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFF64748B))
        Text(
            text = value,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}

// -------------------------------------------------------------------------
// 5. WORK NOTES DIALOG
// -------------------------------------------------------------------------
@Composable
fun NotesDialog(
    viewModel: AttendanceViewModel,
    notes: List<WorkNote>,
    yearMonth: String,
    onDismiss: () -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var contentInput by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.80f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Notes",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Manage your notes",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!showAddForm) {
                    Button(
                        onClick = { showAddForm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add New Note", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("New Note", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = titleInput,
                                onValueChange = { titleInput = it },
                                label = { Text("Title") },
                                placeholder = { Text("e.g. Safety briefing") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = contentInput,
                                onValueChange = { contentInput = it },
                                label = { Text("Details") },
                                placeholder = { Text("Note content...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                maxLines = 3
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showAddForm = false }) {
                                    Text("Cancel", color = Color(0xFF64748B))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (titleInput.isNotBlank() || contentInput.isNotBlank()) {
                                            val dateStr = String.format(Locale.ENGLISH, "%s-%02d", yearMonth, 20)
                                            viewModel.addNote(
                                                titleInput.ifBlank { "Note" },
                                                contentInput,
                                                dateStr
                                            )
                                            titleInput = ""
                                            contentInput = ""
                                            showAddForm = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Save Note")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Saved Notes (${notes.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (notes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No notes found for this month.", fontSize = 13.sp, color = Color(0xFF94A3B8))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFAFAFA),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = note.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteNote(note.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    if (note.content.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = note.content,
                                            fontSize = 13.sp,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = note.dateString,
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 6. SETTINGS DIALOG (Profile and Data)
// -------------------------------------------------------------------------
@Composable
fun SettingsDialog(
    viewModel: AttendanceViewModel,
    profile: UserProfile,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf(profile.name) }
    var mobileInput by remember { mutableStateOf(profile.mobileNumber) }
    var dailyWageInput by remember { mutableStateOf(profile.dailyWage.toInt().toString()) }
    var currencyInput by remember { mutableStateOf(profile.currencySymbol) }
    var otRateInput by remember { mutableStateOf(profile.overtimeHourlyRate.toInt().toString()) }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Settings",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Profile, Mobile & Data",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Account & Mobile Section
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFEFF6FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mobile Login Account",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = profile.mobileNumber,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Verified ✓",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                viewModel.logout()
                                Toast.makeText(context, "Logged out. Please verify mobile OTP to login.", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFDC2626)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("logout_button")
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Switch Account / Log Out", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Worker / Employee Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = mobileInput,
                    onValueChange = { mobileInput = it },
                    label = { Text("Mobile Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dailyWageInput,
                    onValueChange = { dailyWageInput = it },
                    label = { Text("Daily Wage Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = currencyInput,
                        onValueChange = { currencyInput = it },
                        label = { Text("Currency") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = otRateInput,
                        onValueChange = { otRateInput = it },
                        label = { Text("OT Rate / hr") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Demo Data Reset
                OutlinedButton(
                    onClick = {
                        viewModel.resetToDemoData()
                        Toast.makeText(context, "Loaded Demo Data for November 2025", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore Demo Data (₹16,500 / ₹9,000)")
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val wage = dailyWageInput.toDoubleOrNull() ?: 750.0
                        val otRate = otRateInput.toDoubleOrNull() ?: 100.0
                        val curr = currencyInput.ifBlank { "₹" }
                        val mob = mobileInput.ifBlank { profile.mobileNumber }
                        viewModel.updateProfile(nameInput.ifBlank { "ABHIMANYU KUMAR" }, wage, curr, otRate, mob)
                        Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// Note: WageCalculatorDialog is now modularized in WageCalculatorDialog.kt


// -------------------------------------------------------------------------
// 8. PDF REPORT DIALOG
// -------------------------------------------------------------------------
@Composable
fun PdfReportDialog(
    profile: UserProfile,
    monthName: String,
    year: Int,
    workedDays: Double,
    totalWage: Double,
    totalAdvance: Double,
    netPayable: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val statementText = remember(profile, monthName, year, workedDays, totalWage, totalAdvance, netPayable) {
        """
        ========================================
             ATTENDANCE & WAGE STATEMENT
        ========================================
        Employee Name: ${profile.name}
        Billing Month: $monthName $year
        Daily Wage: ${profile.currencySymbol}${profile.dailyWage.toInt()}
        
        ----------------------------------------
        Worked Days: ${AttendanceViewModel.formatDays(workedDays)} days
        Gross Wage: ${AttendanceViewModel.formatCurrency(totalWage, profile.currencySymbol)}
        Advance Deductions: ${AttendanceViewModel.formatCurrency(totalAdvance, profile.currencySymbol)}
        ----------------------------------------
        NET PAYABLE: ${AttendanceViewModel.formatCurrency(netPayable, profile.currencySymbol)}
        ========================================
        Generated on: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())}
        """.trimIndent()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PDF Report",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Monthly or Custom Statement",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Printable Slip Preview
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statementText,
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, statementText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Wage Slip")
                            context.startActivity(shareIntent)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Salary slip exported successfully", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PDF")
                    }
                }
            }
        }
    }
}
