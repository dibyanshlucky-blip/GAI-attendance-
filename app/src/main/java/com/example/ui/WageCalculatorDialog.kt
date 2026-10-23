package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
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
import androidx.core.content.FileProvider
import com.example.viewmodel.UserProfile
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modern, mobile-responsive Wage Calculator Popup Modal matching the reference design.
 * Features:
 * - Centered popup over dimmed background
 * - Top header with blue calculator icon, title, and close (×) icon
 * - Employee Name input with user icon
 * - Wage Rate input with placeholder
 * - 3-column responsive attendance selection grid (Present, Half, Absent, 1.5 Day, Double)
 * - Support for single & multi-day attendance entry with reactive multiplier computation
 * - Advance Deduction (₹) input
 * - Instant summary card (Total Worked Days, Total Wage, Advance, Net Payable)
 * - Cyan-to-blue gradient "Generate Report" button with validation & PDF export/sharing
 * - Light-grey "Close" button
 */
@Composable
fun WageCalculatorDialog(
    profile: UserProfile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Input States
    var employeeName by remember { mutableStateOf(profile.name) }
    var wageRate by remember {
        mutableStateOf(if (profile.dailyWage > 0) profile.dailyWage.toInt().toString() else "")
    }
    var advanceDeduction by remember { mutableStateOf("") }

    // Attendance Day Inputs (Direct numeric typing format)
    var isDirectDaysMode by remember { mutableStateOf(false) }
    var directDaysInput by remember { mutableStateOf("") }
    var presentDaysInput by remember { mutableStateOf("") }
    var halfDaysInput by remember { mutableStateOf("") }
    var absentDaysInput by remember { mutableStateOf("") }
    var onePointFiveDaysInput by remember { mutableStateOf("") }
    var doubleDaysInput by remember { mutableStateOf("") }


    // Generated Report Dialog state
    var showReportModal by remember { mutableStateOf(false) }
    var generatedReportText by remember { mutableStateOf("") }

    // Reactive Calculations
    val rate = wageRate.toDoubleOrNull() ?: 0.0
    val advance = advanceDeduction.toDoubleOrNull() ?: 0.0

    val presentDays = presentDaysInput.toDoubleOrNull() ?: 0.0
    val halfDays = halfDaysInput.toDoubleOrNull() ?: 0.0
    val absentDays = absentDaysInput.toDoubleOrNull() ?: 0.0
    val onePointFiveDays = onePointFiveDaysInput.toDoubleOrNull() ?: 0.0
    val doubleDays = doubleDaysInput.toDoubleOrNull() ?: 0.0
    val directDays = directDaysInput.toDoubleOrNull() ?: 0.0

    val totalWorkedDays = if (isDirectDaysMode) {
        directDays
    } else {
        (presentDays * 1.0) +
                (halfDays * 0.5) +
                (absentDays * 0.0) +
                (onePointFiveDays * 1.5) +
                (doubleDays * 2.0)
    }

    val totalWage = totalWorkedDays * rate
    val netPayable = totalWage - advance

    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
            currency = java.util.Currency.getInstance("INR")
        }
    }

    fun formatInr(amount: Double): String {
        return try {
            val formatted = currencyFormat.format(amount)
            // Ensure clean ₹ symbol representation
            if (!formatted.contains("₹")) "₹${String.format(Locale.ENGLISH, "%,.0f", amount)}" else formatted
        } catch (_: Exception) {
            "₹${String.format(Locale.ENGLISH, "%,.0f", amount)}"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // Semi-transparent dimmed background overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // Main Modal Card (Light grey / slate container matching the reference screenshot)
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 440.dp)
                    .padding(vertical = 24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Intercept tap to prevent closing */ }
                    )
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(22.dp),
                        spotColor = Color(0x33000000)
                    ),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF8F9DAA)),
                border = BorderStroke(1.dp, Color(0xFFA0ADB8))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    // -------------------------------------------------------------
                    // 1. POPUP HEADER (Fixed, NEVER scrolls / moves)
                    // -------------------------------------------------------------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "Calculator Icon",
                                tint = Color(0xFF388AF6),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Wage Calculator",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_close_modal_x")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // -------------------------------------------------------------
                    // SCROLLABLE BODY CONTENT
                    // -------------------------------------------------------------
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp)
                    ) {
                        // -------------------------------------------------------------
                        // 2. INPUT 1: EMPLOYEE NAME (with user icon)
                        // -------------------------------------------------------------
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, Color(0xFF388AF6))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "User Icon",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            TextField(
                                value = employeeName,
                                onValueChange = { employeeName = it },
                                placeholder = {
                                    Text(
                                        text = "Employee Name",
                                        fontSize = 15.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = Color(0xFF2563EB),
                                    focusedTextColor = Color(0xFF1E293B),
                                    unfocusedTextColor = Color(0xFF1E293B)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_employee_name")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // -------------------------------------------------------------
                    // 3. INPUT 2: WAGE RATE
                    // -------------------------------------------------------------
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        TextField(
                            value = wageRate,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '.' }) {
                                    wageRate = input
                                }
                            },
                            placeholder = {
                                Text(
                                    text = "Wage Rate (e.g., 500)",
                                    fontSize = 15.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color(0xFF2563EB),
                                focusedTextColor = Color(0xFF1E293B),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                                .testTag("input_wage_rate")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // -------------------------------------------------------------
                    // 4. ATTENDANCE INPUT FIELDS (Direct Form Input Format)
                    // -------------------------------------------------------------
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        // Section Header & Clear Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ATTENDANCE (DAYS)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                letterSpacing = 0.5.sp
                            )

                            if (presentDaysInput.isNotBlank() || halfDaysInput.isNotBlank() ||
                                absentDaysInput.isNotBlank() || onePointFiveDaysInput.isNotBlank() ||
                                doubleDaysInput.isNotBlank() || directDaysInput.isNotBlank()
                            ) {
                                Text(
                                    text = "Clear All",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0072FF),
                                    modifier = Modifier
                                        .clickable {
                                            presentDaysInput = ""
                                            halfDaysInput = ""
                                            absentDaysInput = ""
                                            onePointFiveDaysInput = ""
                                            doubleDaysInput = ""
                                            directDaysInput = ""
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                        .testTag("btn_clear_attendance_inputs")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Mode Selector: Breakdown vs Direct Total Days
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE2E8F0))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isDirectDaysMode) Color.White else Color.Transparent)
                                    .clickable { isDirectDaysMode = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "By Status (P/H/A)",
                                    fontSize = 12.sp,
                                    fontWeight = if (!isDirectDaysMode) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!isDirectDaysMode) Color(0xFF0072FF) else Color(0xFF64748B)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDirectDaysMode) Color.White else Color.Transparent)
                                    .clickable { isDirectDaysMode = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Direct Total Days",
                                    fontSize = 12.sp,
                                    fontWeight = if (isDirectDaysMode) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isDirectDaysMode) Color(0xFF0072FF) else Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isDirectDaysMode) {
                            // Direct Total Days Input Field
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFF0072FF))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Total Worked Days",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Enter total days directly (e.g. 26 or 24.5)",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    BasicTextField(
                                        value = directDaysInput,
                                        onValueChange = { input ->
                                            if (input.all { it.isDigit() || it == '.' }) {
                                                directDaysInput = input
                                            }
                                        },
                                        textStyle = TextStyle(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0072FF),
                                            textAlign = TextAlign.Center
                                        ),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .width(76.dp)
                                            .height(38.dp)
                                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 6.dp, vertical = 8.dp)
                                            .testTag("input_direct_worked_days")
                                    )
                                }
                            }
                        } else {
                            // Status Breakdown Direct Input Fields
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AttendanceInputRow(
                                    label = "Present",
                                    multiplierLabel = "1.0 day rate",
                                    value = presentDaysInput,
                                    onValueChange = { presentDaysInput = it },
                                    onIncrement = {
                                        val v = presentDaysInput.toDoubleOrNull() ?: 0.0
                                        presentDaysInput = (v + 1).toInt().toString()
                                    },
                                    onDecrement = {
                                        val v = presentDaysInput.toDoubleOrNull() ?: 0.0
                                        if (v > 0) {
                                            val nv = (v - 1).coerceAtLeast(0.0)
                                            presentDaysInput = if (nv == 0.0) "" else nv.toInt().toString()
                                        }
                                    },
                                    testTag = "input_days_present"
                                )

                                AttendanceInputRow(
                                    label = "Half Day",
                                    multiplierLabel = "0.5 day rate",
                                    value = halfDaysInput,
                                    onValueChange = { halfDaysInput = it },
                                    onIncrement = {
                                        val v = halfDaysInput.toDoubleOrNull() ?: 0.0
                                        halfDaysInput = (v + 1).toInt().toString()
                                    },
                                    onDecrement = {
                                        val v = halfDaysInput.toDoubleOrNull() ?: 0.0
                                        if (v > 0) {
                                            val nv = (v - 1).coerceAtLeast(0.0)
                                            halfDaysInput = if (nv == 0.0) "" else nv.toInt().toString()
                                        }
                                    },
                                    testTag = "input_days_half"
                                )

                                AttendanceInputRow(
                                    label = "Absent",
                                    multiplierLabel = "0.0 day rate",
                                    value = absentDaysInput,
                                    onValueChange = { absentDaysInput = it },
                                    onIncrement = {
                                        val v = absentDaysInput.toDoubleOrNull() ?: 0.0
                                        absentDaysInput = (v + 1).toInt().toString()
                                    },
                                    onDecrement = {
                                        val v = absentDaysInput.toDoubleOrNull() ?: 0.0
                                        if (v > 0) {
                                            val nv = (v - 1).coerceAtLeast(0.0)
                                            absentDaysInput = if (nv == 0.0) "" else nv.toInt().toString()
                                        }
                                    },
                                    testTag = "input_days_absent"
                                )

                                AttendanceInputRow(
                                    label = "1.5 Day",
                                    multiplierLabel = "1.5x overtime rate",
                                    value = onePointFiveDaysInput,
                                    onValueChange = { onePointFiveDaysInput = it },
                                    onIncrement = {
                                        val v = onePointFiveDaysInput.toDoubleOrNull() ?: 0.0
                                        onePointFiveDaysInput = (v + 1).toInt().toString()
                                    },
                                    onDecrement = {
                                        val v = onePointFiveDaysInput.toDoubleOrNull() ?: 0.0
                                        if (v > 0) {
                                            val nv = (v - 1).coerceAtLeast(0.0)
                                            onePointFiveDaysInput = if (nv == 0.0) "" else nv.toInt().toString()
                                        }
                                    },
                                    testTag = "input_days_one_half"
                                )

                                AttendanceInputRow(
                                    label = "Double Day",
                                    multiplierLabel = "2.0x holiday rate",
                                    value = doubleDaysInput,
                                    onValueChange = { doubleDaysInput = it },
                                    onIncrement = {
                                        val v = doubleDaysInput.toDoubleOrNull() ?: 0.0
                                        doubleDaysInput = (v + 1).toInt().toString()
                                    },
                                    onDecrement = {
                                        val v = doubleDaysInput.toDoubleOrNull() ?: 0.0
                                        if (v > 0) {
                                            val nv = (v - 1).coerceAtLeast(0.0)
                                            doubleDaysInput = if (nv == 0.0) "" else nv.toInt().toString()
                                        }
                                    },
                                    testTag = "input_days_double"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // -------------------------------------------------------------
                    // 5. INPUT 3: ADVANCE DEDUCTION (₹)
                    // -------------------------------------------------------------
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        TextField(
                            value = advanceDeduction,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '.' }) {
                                    advanceDeduction = input
                                }
                            },
                            placeholder = {
                                Text(
                                    text = "Advance Deduction (₹)",
                                    fontSize = 15.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color(0xFF2563EB),
                                focusedTextColor = Color(0xFF1E293B),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                                .testTag("input_advance_deduction")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // -------------------------------------------------------------
                    // 6. CALCULATION SUMMARY CARD
                    // -------------------------------------------------------------
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF7D8C9C),
                        border = BorderStroke(1.dp, Color(0xFF8F9DAA))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            // Row 1: Total Worked Days
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Worked Days:",
                                    fontSize = 15.sp,
                                    color = Color(0xFF243242)
                                )
                                Text(
                                    text = String.format(Locale.ENGLISH, "%.2f", totalWorkedDays),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            // Row 2: Total Wage
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Wage:",
                                    fontSize = 15.sp,
                                    color = Color(0xFF243242)
                                )
                                Text(
                                    text = formatInr(totalWage),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            // Row 3: Advance
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Advance:",
                                    fontSize = 15.sp,
                                    color = Color(0xFF243242)
                                )
                                Text(
                                    text = "- " + formatInr(advance),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color(0xFF5A6978).copy(alpha = 0.5f),
                                thickness = 1.dp
                            )

                            // Row 4: Net Payable
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Net Payable:",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = formatInr(netPayable),
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // AdMob Banner inside Wage Calculator
                    AdMobBanner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // -------------------------------------------------------------
                    // 7. GENERATE REPORT BUTTON (Cyan-to-blue gradient)
                    // -------------------------------------------------------------
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF00C6FF),
                                        Color(0xFF0072FF)
                                    )
                                )
                            )
                            .clickable {
                                // Validation
                                if (employeeName.isBlank()) {
                                    Toast.makeText(context, "Please enter Employee Name", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                if (rate <= 0) {
                                    Toast.makeText(context, "Please enter a valid Wage Rate", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }

                                val timeStamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())
                                val report = buildString {
                                    appendLine("========================================")
                                    appendLine("       WAGE CALCULATION REPORT          ")
                                    appendLine("========================================")
                                    appendLine("Generated: $timeStamp")
                                    appendLine("Employee Name: ${employeeName.trim()}")
                                    appendLine("Daily Wage Rate: ₹${rate.toInt()}")
                                    appendLine("----------------------------------------")
                                    appendLine("ATTENDANCE BREAKDOWN:")
                                    if (isDirectDaysMode) {
                                        appendLine("• Direct Total Days: ${String.format(Locale.ENGLISH, "%.2f", totalWorkedDays)} d")
                                    } else {
                                        if (presentDays > 0) appendLine("• Present: $presentDaysInput days (1.0x) = ${String.format(Locale.ENGLISH, "%.1f", presentDays * 1.0)} d")
                                        if (halfDays > 0) appendLine("• Half Day: $halfDaysInput days (0.5x) = ${String.format(Locale.ENGLISH, "%.1f", halfDays * 0.5)} d")
                                        if (absentDays > 0) appendLine("• Absent: $absentDaysInput days (0.0x) = 0 d")
                                        if (onePointFiveDays > 0) appendLine("• 1.5 Day: $onePointFiveDaysInput days (1.5x) = ${String.format(Locale.ENGLISH, "%.1f", onePointFiveDays * 1.5)} d")
                                        if (doubleDays > 0) appendLine("• Double Day: $doubleDaysInput days (2.0x) = ${String.format(Locale.ENGLISH, "%.1f", doubleDays * 2.0)} d")
                                        if (totalWorkedDays == 0.0) appendLine("• No attendance entered (0 days)")
                                    }
                                    appendLine("----------------------------------------")
                                    appendLine("Total Worked Days: ${String.format(Locale.ENGLISH, "%.2f", totalWorkedDays)}")
                                    appendLine("Total Gross Wage : ${formatInr(totalWage)}")
                                    appendLine("Advance Deduction: - ${formatInr(advance)}")
                                    appendLine("----------------------------------------")
                                    appendLine("NET PAYABLE      : ${formatInr(netPayable)}")
                                    appendLine("========================================")
                                    appendLine("Wage Wige Attendance & Payroll App")
                                }
                                generatedReportText = report
                                showReportModal = true
                            }
                            .testTag("btn_generate_report"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Document Icon",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Generate Report",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // -------------------------------------------------------------
                    // 8. CLOSE BUTTON (Light grey surface with blue text)
                    // -------------------------------------------------------------
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clickable(onClick = onDismiss)
                            .testTag("btn_close_modal_bottom"),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFDCE5EE)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Close",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0072FF)
                            )
                        }
                    }
                }
            }
        }
    }
    }



    // -------------------------------------------------------------------------
    // REPORT PREVIEW & EXPORT DIALOG (Share text, Export PDF, Download)
    // -------------------------------------------------------------------------
    if (showReportModal) {
        Dialog(
            onDismissRequest = { showReportModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 440.dp)
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFF0072FF),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Wage Calculation Report",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                        IconButton(onClick = { showReportModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Text report view
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = generatedReportText,
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons: Share PDF, Share Text, Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Share Text Button
                        OutlinedButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, generatedReportText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Wage Report"))
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Text", fontSize = 13.sp)
                        }

                        // Generate & Share PDF Button
                        Button(
                            onClick = {
                                exportAndSharePdf(
                                    context = context,
                                    employeeName = employeeName,
                                    wageRate = rate,
                                    totalWorkedDays = totalWorkedDays,
                                    totalWage = totalWage,
                                    advance = advance,
                                    netPayable = netPayable,
                                    breakdown = if (isDirectDaysMode) {
                                        mapOf("Direct Total Days" to totalWorkedDays)
                                    } else {
                                        mapOf(
                                            "Present (1.0x)" to presentDays,
                                            "Half Day (0.5x)" to halfDays,
                                            "Absent (0.0x)" to absentDays,
                                            "1.5 Day (1.5x)" to onePointFiveDays,
                                            "Double Day (2.0x)" to doubleDays
                                        )
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0072FF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share PDF", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Form Input Row for each attendance category.
 * Displays category name, multiplier subtitle, minus stepper,
 * direct numeric typing TextField, and plus stepper.
 */
@Composable
fun AttendanceInputRow(
    label: String,
    multiplierLabel: String,
    value: String,
    onValueChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val hasValue = value.isNotBlank() && (value.toDoubleOrNull() ?: 0.0) > 0.0

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(
            1.dp,
            if (hasValue) Color(0xFF0072FF) else Color(0xFFCBD5E1)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = multiplierLabel,
                    fontSize = 11.sp,
                    color = if (hasValue) Color(0xFF0072FF) else Color(0xFF64748B)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Stepper Minus button
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onDecrement() },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "-",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                    }
                }

                // Numeric Direct Input TextField
                BasicTextField(
                    value = value,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) {
                            onValueChange(input)
                        }
                    },
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasValue) Color(0xFF0072FF) else Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .width(58.dp)
                        .height(34.dp)
                        .background(if (hasValue) Color(0xFFEFF6FF) else Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                        .border(
                            1.dp,
                            if (hasValue) Color(0xFF0072FF) else Color(0xFFCBD5E1),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .testTag(testTag)
                )

                // Stepper Plus button
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onIncrement() },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0072FF)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "+",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Attendance Selection Button in the 3-column grid
 * Displays normal white rounded button when count is 0,
 * and highlighted blue border + light-blue background when count > 0.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AttendanceGridButton(
    title: String,
    count: Double,
    multiplier: Double,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isSelected = count > 0

    Surface(
        modifier = modifier
            .height(48.dp)
            .testTag(testTag)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF0072FF) else Color(0xFFE2E8F0)
        ),
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val label = if (count > 0) {
                if (count == count.toInt().toDouble()) {
                    "$title (${count.toInt()})"
                } else {
                    "$title ($count)"
                }
            } else {
                title
            }

            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF0072FF) else Color(0xFF334155),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/**
 * Generates an Android PDF document for the wage calculation and shares it via FileProvider
 */
fun exportAndSharePdf(
    context: Context,
    employeeName: String,
    wageRate: Double,
    totalWorkedDays: Double,
    totalWage: Double,
    advance: Double,
    netPayable: Double,
    breakdown: Map<String, Double>
) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 dimensions in points
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = AndroidColor.rgb(15, 23, 42)
            textSize = 22f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val subtitlePaint = Paint().apply {
            color = AndroidColor.rgb(0, 114, 255)
            textSize = 14f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val textPaint = Paint().apply {
            color = AndroidColor.rgb(51, 65, 85)
            textSize = 12f
        }

        val boldPaint = Paint().apply {
            color = AndroidColor.rgb(15, 23, 42)
            textSize = 13f
            isFakeBoldText = true
        }

        val linePaint = Paint().apply {
            color = AndroidColor.rgb(203, 213, 225)
            strokeWidth = 1f
        }

        var y = 60f

        // Header
        canvas.drawText("WAGE WIGE ATTENDANCE & PAYROLL", 297f, y, titlePaint)
        y += 24f
        canvas.drawText("Wage Calculation Slip / Report", 297f, y, subtitlePaint)
        y += 20f

        val timeStamp = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())
        val datePaint = Paint().apply {
            color = AndroidColor.rgb(100, 116, 139)
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Generated on: $timeStamp", 297f, y, datePaint)
        y += 20f

        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 25f

        // Employee Info Box
        canvas.drawText("Employee Name: ${employeeName.trim()}", 40f, y, boldPaint)
        y += 20f
        canvas.drawText("Daily Wage Rate: ₹${wageRate.toInt()} / day", 40f, y, textPaint)
        y += 25f

        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 25f

        // Breakdown Table
        canvas.drawText("ATTENDANCE BREAKDOWN", 40f, y, boldPaint)
        y += 20f

        breakdown.forEach { (cat, count) ->
            if (count > 0) {
                canvas.drawText("• $cat", 50f, y, textPaint)
                canvas.drawText("${count.toInt()} days", 400f, y, textPaint)
                y += 18f
            }
        }

        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 25f

        // Financial Summary
        canvas.drawText("FINANCIAL SUMMARY", 40f, y, boldPaint)
        y += 22f

        canvas.drawText("Total Worked Days:", 50f, y, textPaint)
        canvas.drawText(String.format(Locale.ENGLISH, "%.2f days", totalWorkedDays), 400f, y, boldPaint)
        y += 20f

        canvas.drawText("Gross Total Wage:", 50f, y, textPaint)
        canvas.drawText("₹${totalWage.toInt()}", 400f, y, boldPaint)
        y += 20f

        canvas.drawText("Advance Deductions:", 50f, y, textPaint)
        canvas.drawText("- ₹${advance.toInt()}", 400f, y, textPaint)
        y += 20f

        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 25f

        val netPaint = Paint().apply {
            color = AndroidColor.rgb(0, 114, 255)
            textSize = 16f
            isFakeBoldText = true
        }
        canvas.drawText("NET PAYABLE AMOUNT:", 50f, y, netPaint)
        canvas.drawText("₹${netPayable.toInt()}", 400f, y, netPaint)

        y += 40f
        val footerPaint = Paint().apply {
            color = AndroidColor.rgb(148, 163, 184)
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("This is a computer-generated wage slip from Wage Wige.", 297f, y, footerPaint)

        pdfDocument.finishPage(page)

        // Write to cache
        val pdfFile = File(context.cacheDir, "Wage_Report_${System.currentTimeMillis()}.pdf")
        val outputStream = FileOutputStream(pdfFile)
        pdfDocument.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDocument.close()

        // Share via FileProvider
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, pdfFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Wage Calculation Report - $employeeName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Wage Report PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not generate PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
