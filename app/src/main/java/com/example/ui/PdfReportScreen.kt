package com.example.ui

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.AdvanceRecord
import com.example.data.AttendanceRecord
import com.example.viewmodel.AttendanceViewModel
import com.example.viewmodel.UserProfile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class ReportViewMode {
    CONFIG,
    PREVIEW
}

enum class ReportPeriodType {
    MONTHLY,
    CUSTOM_RANGE
}

@Composable
fun PdfReportScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateTab: (Int) -> Unit
) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsState()
    val isMonthLocked by viewModel.isCurrentMonthLocked.collectAsState()

    var viewMode by remember { mutableStateOf(ReportViewMode.CONFIG) }
    var periodType by remember { mutableStateOf(ReportPeriodType.MONTHLY) }

    // Selected month calendar for reporting
    var reportCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            // Default to December 2026 to match screenshot, or current selected
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 1)
        })
    }

    // Custom date range state
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH) }
    var customStartDate by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 1)
        })
    }
    var customEndDate by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 31)
        })
    }

    // Month & Year display strings
    val monthNames = remember {
        listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
    }
    val currentMonthName = monthNames[reportCalendar.get(Calendar.MONTH)]
    val currentYear = reportCalendar.get(Calendar.YEAR)
    val periodTitle = remember(periodType, reportCalendar, customStartDate, customEndDate) {
        if (periodType == ReportPeriodType.MONTHLY) {
            "$currentMonthName $currentYear"
        } else {
            "${dateFormatter.format(customStartDate.time)} - ${dateFormatter.format(customEndDate.time)}"
        }
    }

    // Query records for the period
    val allMonthRecords by viewModel.monthRecords.collectAsState()
    val allMonthAdvances by viewModel.monthAdvances.collectAsState()

    // Calculated metrics
    val presentCount = remember(allMonthRecords) {
        allMonthRecords.count { it.dayUnits == 1.0 && it.status != "ABSENT" }
    }
    val halfDayCount = remember(allMonthRecords) {
        allMonthRecords.count { it.dayUnits == 0.5 }
    }
    val holidayCount = remember(allMonthRecords) {
        allMonthRecords.count { it.status == "HOLIDAY" }
    }
    val oneAndHalfCount = remember(allMonthRecords) {
        allMonthRecords.count { it.dayUnits == 1.5 }
    }
    val doubleDayCount = remember(allMonthRecords) {
        allMonthRecords.count { it.dayUnits == 2.0 }
    }
    val absentCount = remember(allMonthRecords) {
        allMonthRecords.count { it.dayUnits == 0.0 || it.status == "ABSENT" }
    }

    val totalWorkedDays = remember(allMonthRecords) {
        allMonthRecords.sumOf { it.dayUnits }
    }
    val totalEarnedWages = remember(allMonthRecords, profile.dailyWage) {
        if (allMonthRecords.isNotEmpty()) {
            allMonthRecords.sumOf { it.totalEarnings }
        } else {
            totalWorkedDays * profile.dailyWage
        }
    }
    val totalAdvanceTaken = remember(allMonthAdvances) {
        allMonthAdvances.sumOf { it.amount }
    }
    val netPayableAmount = remember(totalEarnedWages, totalAdvanceTaken) {
        (totalEarnedWages - totalAdvanceTaken).coerceAtLeast(0.0)
    }

    // Intercept hardware/system back button
    BackHandler {
        if (viewMode == ReportViewMode.PREVIEW) {
            viewMode = ReportViewMode.CONFIG
        } else {
            onNavigateBack()
        }
    }

    if (viewMode == ReportViewMode.CONFIG) {
        // =====================================================================
        // SCREEN 1: PDF REPORT GENERATION (Report Configuration)
        // =====================================================================
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFFF4F7FC),
            topBar = {
                Surface(
                    color = Color.Transparent,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("btn_back_pdf_config")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Text(
                            text = "PDF Report Generation",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 40.dp)
                                .testTag("title_pdf_report_gen")
                        )
                    }
                }
            },
            bottomBar = {
                DashboardBottomNavigation(
                    selectedIndex = 0,
                    isMonthLocked = isMonthLocked,
                    onTabSelected = onNavigateTab
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 1. Employee Header Card (Abhimanyu Kumar, Daily Rate: ₹700)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(18.dp), spotColor = Color(0x1A000000)),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = profile.name.ifBlank { "Abhimanyu Kumar" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Daily Rate: ₹${profile.dailyWage.toInt()}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Report Configuration Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(18.dp), spotColor = Color(0x1A000000)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Blue Card Header with Settings Icon
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF0099FF),
                                            Color(0xFF0072FF)
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Report Configuration",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Configuration Body
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            // Report Type Label
                            Text(
                                text = "Report Type",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Radio Buttons: Monthly Report vs Custom Date Range
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { periodType = ReportPeriodType.MONTHLY }
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = periodType == ReportPeriodType.MONTHLY,
                                        onClick = { periodType = ReportPeriodType.MONTHLY },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0072FF))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Monthly Report",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1E293B)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { periodType = ReportPeriodType.CUSTOM_RANGE }
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = periodType == ReportPeriodType.CUSTOM_RANGE,
                                        onClick = { periodType = ReportPeriodType.CUSTOM_RANGE },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0072FF))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Custom Date Range",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (periodType == ReportPeriodType.MONTHLY) {
                                // Select Month Section
                                Text(
                                    text = "Select Month",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Month Selector Card (< December 2026 >)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val newCal = reportCalendar.clone() as Calendar
                                                newCal.add(Calendar.MONTH, -1)
                                                reportCalendar = newCal
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronLeft,
                                                contentDescription = "Previous Month",
                                                tint = Color(0xFF0F172A),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Text(
                                            text = "$currentMonthName $currentYear",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )

                                        Surface(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clickable {
                                                    val newCal = reportCalendar.clone() as Calendar
                                                    newCal.add(Calendar.MONTH, 1)
                                                    reportCalendar = newCal
                                                },
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFE2E8F0)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = "Next Month",
                                                    tint = Color(0xFF0072FF),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Custom Date Range Pickers
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Start Date",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF475569)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .clickable {
                                                    DatePickerDialog(
                                                        context,
                                                        { _, y, m, d ->
                                                            val c = Calendar.getInstance()
                                                            c.set(y, m, d)
                                                            customStartDate = c
                                                        },
                                                        customStartDate.get(Calendar.YEAR),
                                                        customStartDate.get(Calendar.MONTH),
                                                        customStartDate.get(Calendar.DAY_OF_MONTH)
                                                    ).show()
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFF8FAFC),
                                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(dateFormatter.format(customStartDate.time), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "End Date",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF475569)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .clickable {
                                                    DatePickerDialog(
                                                        context,
                                                        { _, y, m, d ->
                                                            val c = Calendar.getInstance()
                                                            c.set(y, m, d)
                                                            customEndDate = c
                                                        },
                                                        customEndDate.get(Calendar.YEAR),
                                                        customEndDate.get(Calendar.MONTH),
                                                        customEndDate.get(Calendar.DAY_OF_MONTH)
                                                    ).show()
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFF8FAFC),
                                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(dateFormatter.format(customEndDate.time), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // AdMob Banner Ad (matching reference screenshot layout)
                AdMobBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Generate Report Preview Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0099FF),
                                    Color(0xFF0072FF)
                                )
                            )
                        )
                        .clickable {
                            val activity = context as? Activity
                            if (activity != null) {
                                AdMobManager.showInterstitial(activity) {
                                    viewMode = ReportViewMode.PREVIEW
                                }
                            } else {
                                viewMode = ReportViewMode.PREVIEW
                            }
                        }
                        .testTag("btn_generate_report_preview"),
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
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Generate Report Preview",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    } else {
        // =====================================================================
        // SCREEN 2 & 3: PAYROLL REPORT PREVIEW (Sharable to WhatsApp)
        // =====================================================================
        val generatedTimeStamp = remember {
            SimpleDateFormat("MMMM d'th', yyyy hh:mm a", Locale.ENGLISH).format(Date())
        }

        // Action helper to export PDF and trigger WhatsApp or system share
        val onShareReport: (Boolean) -> Unit = { toWhatsAppOnly ->
            val pdfFile = generatePayrollPdfDocument(
                context = context,
                employeeName = profile.name.ifBlank { "Abhimanyu Kumar" },
                dailyRate = profile.dailyWage,
                periodTitle = periodTitle,
                presentCount = presentCount,
                halfDayCount = halfDayCount,
                holidayCount = holidayCount,
                oneAndHalfCount = oneAndHalfCount,
                doubleDayCount = doubleDayCount,
                absentCount = absentCount,
                totalWorkedDays = totalWorkedDays,
                totalEarnedWages = totalEarnedWages,
                totalAdvanceTaken = totalAdvanceTaken,
                netPayableAmount = netPayableAmount,
                generatedTime = generatedTimeStamp
            )

            val textSlip = buildString {
                appendLine("📄 *PAYROLL REPORT - $periodTitle*")
                appendLine("👤 *Employee*: ${profile.name.ifBlank { "Abhimanyu Kumar" }}")
                appendLine("💰 *Daily Rate*: ₹${profile.dailyWage.toInt()}")
                appendLine("----------------------------------------")
                appendLine("📊 *ATTENDANCE SUMMARY*")
                appendLine("• Present Days: $presentCount")
                appendLine("• Half Days: $halfDayCount")
                appendLine("• Holidays: $holidayCount")
                appendLine("• One & Half Days: $oneAndHalfCount")
                appendLine("• Double Days: $doubleDayCount")
                appendLine("• Absent Days: $absentCount")
                appendLine("----------------------------------------")
                appendLine("⏳ *Total Worked Days*: ${String.format(Locale.ENGLISH, "%.2f", totalWorkedDays)}")
                appendLine("")
                appendLine("💵 *FINANCIAL SUMMARY*")
                appendLine("• Total Earned Wages: ₹${totalEarnedWages.toInt()}")
                appendLine("• Total Advance Taken: ₹${totalAdvanceTaken.toInt()}")
                appendLine("----------------------------------------")
                appendLine("✅ *NET PAYABLE AMOUNT*: *₹${netPayableAmount.toInt()}*")
                appendLine("========================================")
                appendLine("Generated on $generatedTimeStamp")
                appendLine("Wage Book - Simplified Attendance & Payroll Management")
            }

            if (pdfFile != null) {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdfFile
                )
                sharePayrollReport(context, uri, textSlip, toWhatsAppOnly)
            } else {
                // Share as text slip fallback
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, textSlip)
                    if (toWhatsAppOnly) setPackage("com.whatsapp")
                }
                try {
                    context.startActivity(sendIntent)
                } catch (_: Exception) {
                    context.startActivity(Intent.createChooser(sendIntent, "Share Payroll Report"))
                }
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFFF4F7FC),
            topBar = {
                Surface(
                    color = Color.Transparent,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewMode = ReportViewMode.CONFIG },
                                modifier = Modifier.testTag("btn_back_preview")
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
                                text = "Payroll Report",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        // Top-Right Cyan/Blue Rounded Share Button matching Screenshot 2
                        Surface(
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { onShareReport(false) }
                                .testTag("btn_top_share_report"),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0099FF),
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Report",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // -------------------------------------------------------------
                // CARD 1: MAIN REPORT PAPER CARD
                // -------------------------------------------------------------
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(22.dp), spotColor = Color(0x1A000000)),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Title: Payroll Report (Blue)
                        Text(
                            text = "Payroll Report",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0080FF),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Subtitle: December 2026 (or custom date range)
                        Text(
                            text = periodTitle,
                            fontSize = 15.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Inner Employee Banner Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = profile.name.ifBlank { "Abhimanyu Kumar" },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Daily Rate: ₹${profile.dailyWage.toInt()}",
                                    fontSize = 14.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Section Header: Attendance Summary
                        Text(
                            text = "Attendance Summary",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Rows matching Screenshot 2 & 3
                        ReportIconRow(
                            icon = Icons.Default.CheckCircle,
                            iconColor = Color(0xFF10B981), // Green
                            label = "Present Days",
                            value = presentCount.toString()
                        )

                        ReportIconRow(
                            icon = Icons.AutoMirrored.Filled.Redo,
                            iconColor = Color(0xFFF59E0B), // Orange / Amber
                            label = "Half Days",
                            value = halfDayCount.toString()
                        )

                        ReportIconRow(
                            icon = Icons.Default.CardGiftcard,
                            iconColor = Color(0xFF06B6D4), // Cyan
                            label = "Holidays",
                            value = holidayCount.toString()
                        )

                        ReportIconRow(
                            icon = Icons.Default.MilitaryTech,
                            iconColor = Color(0xFF3B82F6), // Blue
                            label = "One & Half Days",
                            value = oneAndHalfCount.toString()
                        )

                        ReportIconRow(
                            icon = Icons.Default.KeyboardDoubleArrowRight,
                            iconColor = Color(0xFFA855F7), // Purple
                            label = "Double Days",
                            value = doubleDayCount.toString()
                        )

                        ReportIconRow(
                            icon = Icons.Default.Cancel,
                            iconColor = Color(0xFFEF4444), // Red
                            label = "Absent Days",
                            value = absentCount.toString()
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color(0xFFF1F5F9),
                            thickness = 1.dp
                        )

                        // Total Worked Days
                        ReportIconRow(
                            icon = Icons.Default.Schedule,
                            iconColor = Color(0xFF0D9488), // Teal
                            label = "Total Worked Days",
                            value = String.format(Locale.ENGLISH, "%.2f", totalWorkedDays),
                            isBoldValue = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // -------------------------------------------------------------
                // CARD 2: FINANCIAL SUMMARY CARD
                // -------------------------------------------------------------
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(22.dp), spotColor = Color(0x1A000000)),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Financial Summary",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Total Earned Wages
                        ReportIconRow(
                            icon = Icons.Default.AccountBalanceWallet,
                            iconColor = Color(0xFF8B5CF6), // Purple
                            label = "Total Earned Wages",
                            value = "₹${totalEarnedWages.toInt()}"
                        )

                        // Total Advance Taken
                        ReportIconRow(
                            icon = Icons.Default.AccountBalance,
                            iconColor = Color(0xFFF97316), // Orange
                            label = "Total Advance Taken",
                            value = "₹${totalAdvanceTaken.toInt()}"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color(0xFFF1F5F9),
                            thickness = 1.dp
                        )

                        // Net Payable Amount
                        ReportIconRow(
                            icon = Icons.Default.Calculate,
                            iconColor = Color(0xFF10B981), // Green
                            label = "Net Payable Amount",
                            value = "₹${netPayableAmount.toInt()}",
                            isBoldValue = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // -------------------------------------------------------------
                // WHATSAPP SHARING DIRECT BUTTON
                // -------------------------------------------------------------
                Button(
                    onClick = { onShareReport(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_share_to_whatsapp"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), // Official WhatsApp Green
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "WhatsApp",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Share to WhatsApp (PDF + Slip)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // AdMob Banner Ad in Preview Mode
                AdMobBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Footer (Matching Screenshot 3)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Generated on $generatedTimeStamp",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Wage Book - Simplified Attendance & Payroll Management",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Clean reusable row with icon, label, and right-aligned value.
 */
@Composable
fun ReportIconRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    isBoldValue: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                color = Color(0xFF334155),
                fontWeight = FontWeight.Normal
            )
        }

        Text(
            text = value,
            fontSize = if (isBoldValue) 16.sp else 15.sp,
            fontWeight = if (isBoldValue) FontWeight.Bold else FontWeight.Normal,
            color = Color(0xFF0F172A)
        )
    }
}

/**
 * Generates an A4 PDF document matching the exact layout of the Payroll Report
 */
fun generatePayrollPdfDocument(
    context: Context,
    employeeName: String,
    dailyRate: Double,
    periodTitle: String,
    presentCount: Int,
    halfDayCount: Int,
    holidayCount: Int,
    oneAndHalfCount: Int,
    doubleDayCount: Int,
    absentCount: Int,
    totalWorkedDays: Double,
    totalEarnedWages: Double,
    totalAdvanceTaken: Double,
    netPayableAmount: Double,
    generatedTime: String
): File? {
    return try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 in points
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paintTitle = Paint().apply {
            color = AndroidColor.rgb(0, 128, 255)
            textSize = 24f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val paintSubtitle = Paint().apply {
            color = AndroidColor.rgb(100, 116, 139)
            textSize = 14f
            textAlign = Paint.Align.CENTER
        }

        val paintSectionHeading = Paint().apply {
            color = AndroidColor.rgb(15, 23, 42)
            textSize = 15f
            isFakeBoldText = true
        }

        val paintLabel = Paint().apply {
            color = AndroidColor.rgb(51, 65, 85)
            textSize = 13f
        }

        val paintValue = Paint().apply {
            color = AndroidColor.rgb(15, 23, 42)
            textSize = 13f
            textAlign = Paint.Align.RIGHT
        }

        val paintBoldValue = Paint().apply {
            color = AndroidColor.rgb(15, 23, 42)
            textSize = 14f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }

        val paintLine = Paint().apply {
            color = AndroidColor.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        val paintFooter = Paint().apply {
            color = AndroidColor.rgb(148, 163, 184)
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }

        var y = 50f

        // Document Title & Subtitle
        canvas.drawText("Payroll Report", 297.5f, y, paintTitle)
        y += 20f
        canvas.drawText(periodTitle, 297.5f, y, paintSubtitle)
        y += 30f

        // Employee Info Box
        val boxPaint = Paint().apply {
            color = AndroidColor.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        val boxStroke = Paint().apply {
            color = AndroidColor.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(40f, y, 555f, y + 54f, 12f, 12f, boxPaint)
        canvas.drawRoundRect(40f, y, 555f, y + 54f, 12f, 12f, boxStroke)

        val empNamePaint = Paint().apply {
            color = AndroidColor.rgb(15, 23, 42)
            textSize = 16f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(employeeName, 297.5f, y + 24f, empNamePaint)
        canvas.drawText("Daily Rate: ₹${dailyRate.toInt()}", 297.5f, y + 42f, paintSubtitle)
        y += 80f

        // Attendance Summary Section
        canvas.drawText("Attendance Summary", 40f, y, paintSectionHeading)
        y += 24f

        fun drawPdfRow(label: String, value: String, isBold: Boolean = false) {
            canvas.drawText(label, 40f, y, paintLabel)
            canvas.drawText(value, 555f, y, if (isBold) paintBoldValue else paintValue)
            y += 22f
        }

        drawPdfRow("• Present Days", presentCount.toString())
        drawPdfRow("• Half Days", halfDayCount.toString())
        drawPdfRow("• Holidays", holidayCount.toString())
        drawPdfRow("• One & Half Days", oneAndHalfCount.toString())
        drawPdfRow("• Double Days", doubleDayCount.toString())
        drawPdfRow("• Absent Days", absentCount.toString())

        y += 4f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 18f
        drawPdfRow("Total Worked Days", String.format(Locale.ENGLISH, "%.2f", totalWorkedDays), isBold = true)

        y += 30f

        // Financial Summary Section
        canvas.drawText("Financial Summary", 40f, y, paintSectionHeading)
        y += 24f

        drawPdfRow("• Total Earned Wages", "₹${totalEarnedWages.toInt()}")
        drawPdfRow("• Total Advance Taken", "₹${totalAdvanceTaken.toInt()}")

        y += 4f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 18f
        drawPdfRow("Net Payable Amount", "₹${netPayableAmount.toInt()}", isBold = true)

        y += 60f

        // Footer
        canvas.drawText("Generated on $generatedTime", 297.5f, y, paintFooter)
        y += 14f
        canvas.drawText("Wage Book - Simplified Attendance & Payroll Management", 297.5f, y, paintFooter)

        pdfDocument.finishPage(page)

        // Save PDF to cache directory
        val reportDir = File(context.cacheDir, "payroll_reports")
        if (!reportDir.exists()) reportDir.mkdirs()
        val file = File(reportDir, "Payroll_Report_${periodTitle.replace(" ", "_")}.pdf")
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.close()
        pdfDocument.close()

        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Triggers WhatsApp share or general Android chooser with PDF attachment and text summary
 */
fun sharePayrollReport(
    context: Context,
    pdfUri: Uri,
    textSummary: String,
    toWhatsAppOnly: Boolean
) {
    if (toWhatsAppOnly) {
        val waIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_TEXT, textSummary)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(waIntent)
            return
        } catch (_: Exception) {
            // Try WhatsApp Business if normal WhatsApp is not installed
            try {
                val waBusinessIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                    putExtra(Intent.EXTRA_TEXT, textSummary)
                    setPackage("com.whatsapp.w4b")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(waBusinessIntent)
                return
            } catch (_: Exception) {
                Toast.makeText(context, "WhatsApp is not installed on this device. Opening share options...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Universal Android Chooser
    val chooserIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, pdfUri)
        putExtra(Intent.EXTRA_TEXT, textSummary)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(chooserIntent, "Share Payroll Report"))
}
