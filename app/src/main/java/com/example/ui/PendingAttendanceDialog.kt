package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.viewmodel.PendingAttendanceSummary
import com.example.viewmodel.PendingDateItem

@Composable
fun PendingAttendanceDialog(
    summary: PendingAttendanceSummary,
    onDismiss: () -> Unit,
    onRemindMeLater: () -> Unit,
    onMarkToday: () -> Unit,
    onMarkDate: (PendingDateItem) -> Unit
) {
    // Blinking animation for current-date subtitle when today's attendance is pending
    val infiniteTransition = rememberInfiniteTransition(label = "PendingAttendanceBlinkTransition")
    val blinkingAlpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CurrentDateBlinkingAlpha"
    )

    // Opacity rule: Blink ONLY if today is pending; if today completed or only previous dates pending, solid 1.0f
    val currentSubtitleAlpha = if (summary.isTodayPending) blinkingAlpha else 1.0f

    // Pending dates to display in 2-column grid in chronological order
    val datesToDisplay = remember(summary.pendingDates) {
        summary.pendingDates
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 76.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .wrapContentHeight()
                    .testTag("dialog_pending_attendance"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    // ---------------------------------------------------------
                    // 1. HEADER ROW: Red Icon + Title + Close Button
                    // ---------------------------------------------------------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EditCalendar,
                                contentDescription = null,
                                tint = Color(0xFFEF4444), // Red/coral accent
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "Pending Attendance",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.testTag("title_pending_attendance")
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_close_pending_popup")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ---------------------------------------------------------
                    // 2. SUBTITLE
                    // ---------------------------------------------------------
                    Text(
                        text = "You have some days pending. Tap to mark attendance.",
                        fontSize = 13.5.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 18.sp
                    )

                    // ---------------------------------------------------------
                    // 3. BLINKING CURRENT-DATE SUBTITLE (When Today is Pending)
                    // ---------------------------------------------------------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onMarkToday)
                            .padding(top = 4.dp, bottom = 12.dp)
                            .alpha(currentSubtitleAlpha)
                            .testTag("subtitle_current_date"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (summary.isTodayPending) Color(0xFFEF4444) else Color(0xFF16A34A),
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (summary.isTodayPending) {
                                "Today — ${summary.todayFormatted} (Pending)"
                            } else {
                                "Today — ${summary.todayFormatted} (Completed)"
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (summary.isTodayPending) Color(0xFFEF4444) else Color(0xFF16A34A)
                        )
                    }

                    // ---------------------------------------------------------
                    // 4. 2-COLUMN GRID OF PENDING DATE BUTTONS
                    // ---------------------------------------------------------
                    if (datesToDisplay.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "All attendance is marked and up to date!",
                                fontSize = 13.sp,
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .testTag("grid_pending_dates")
                        ) {
                            items(datesToDisplay, key = { it.dateString }) { item ->
                                PendingDateButton(
                                    item = item,
                                    onClick = { onMarkDate(item) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // AdMob Banner inside Pending Attendance Dialog
                    AdMobBanner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun PendingDateButton(
    item: PendingDateItem,
    onClick: () -> Unit
) {
    val isToday = item.isToday

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(
            1.dp,
            if (isToday) Color(0xFFEF4444).copy(alpha = 0.5f) else Color(0xFFE2E8F0)
        ),
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("btn_pending_date_${item.dateString}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.buttonFormatted.ifEmpty { "${item.dayOfWeek.take(3)}, ${String.format("%02d", item.dayOfMonth)} Oct" },
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isToday) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(Color(0xFFEF4444), CircleShape)
                )
            }
        }
    }
}
