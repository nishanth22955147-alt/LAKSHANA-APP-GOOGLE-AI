package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateFormatterUtil {
    private val DDMMYY_DASH = SimpleDateFormat("dd-MM-yy", Locale.US)
    private val DDMMYY_SLASH = SimpleDateFormat("dd/MM/yy", Locale.US)
    private val DDMMYY_COMPACT = SimpleDateFormat("ddMMyy", Locale.US)
    private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val HUMAN_FORMAT = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US)

    fun todayDDMMYY(): String = DDMMYY_DASH.format(Date())
    fun todayCompactDDMMYY(): String = DDMMYY_COMPACT.format(Date())

    fun parseToCalendar(dateStr: String): Calendar {
        val cal = Calendar.getInstance()
        val clean = dateStr.trim()
        val formats = listOf(
            DDMMYY_DASH,
            DDMMYY_SLASH,
            DDMMYY_COMPACT,
            ISO_FORMAT,
            SimpleDateFormat("dd-MM-yyyy", Locale.US),
            SimpleDateFormat("dd/MM/yyyy", Locale.US)
        )
        for (sdf in formats) {
            try {
                sdf.isLenient = false
                val parsed = sdf.parse(clean)
                if (parsed != null) {
                    cal.time = parsed
                    return cal
                }
            } catch (_: Exception) {
            }
        }
        return cal
    }

    fun formatDDMMYY(calendar: Calendar): String = DDMMYY_DASH.format(calendar.time)

    fun formatCompactDDMMYY(calendar: Calendar): String = DDMMYY_COMPACT.format(calendar.time)

    fun formatHuman(calendar: Calendar): String = HUMAN_FORMAT.format(calendar.time)

    fun formatIso(calendar: Calendar): String = ISO_FORMAT.format(calendar.time)

    fun getRelativeTag(calendar: Calendar): String {
        val today = Calendar.getInstance()
        val cDay = calendar.get(Calendar.DAY_OF_YEAR)
        val cYear = calendar.get(Calendar.YEAR)
        val tDay = today.get(Calendar.DAY_OF_YEAR)
        val tYear = today.get(Calendar.YEAR)

        return if (cYear == tYear) {
            when (cDay - tDay) {
                0 -> "Today"
                -1 -> "Yesterday"
                1 -> "Tomorrow"
                in -7..-2 -> "${tDay - cDay} days ago"
                in 2..7 -> "In ${cDay - tDay} days"
                else -> ""
            }
        } else {
            ""
        }
    }
}

/**
 * Reusable Compose Component for selecting and stepping dates forward and backward
 * in DDMMYY format (e.g. 27-09-26 / compact 270926).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateForwardBackwardSelector(
    selectedDateStr: String,
    onDateChange: (String) -> Unit,
    label: String = "Date (DDMMYY)",
    modifier: Modifier = Modifier,
    testTagPrefix: String = "date"
) {
    val context = LocalContext.current
    val calendar = remember(selectedDateStr) {
        DateFormatterUtil.parseToCalendar(selectedDateStr)
    }

    val displayDate = DateFormatterUtil.formatDDMMYY(calendar)
    val compactDDMMYY = DateFormatterUtil.formatCompactDDMMYY(calendar)
    val humanDate = DateFormatterUtil.formatHuman(calendar)
    val relativeTag = DateFormatterUtil.getRelativeTag(calendar)

    // Android DatePickerDialog
    val showDatePicker = {
        val dpd = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                onDateChange(DateFormatterUtil.formatDDMMYY(newCal))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dpd.show()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Compact DDMMYY Indicator Badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ) {
                Text(
                    text = "DDMMYY: $compactDDMMYY",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Main Forward / Backward Stepper Bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Backward Button (< -1 Day)
                FilledTonalIconButton(
                    onClick = {
                        val prevCal = (calendar.clone() as Calendar).apply {
                            add(Calendar.DAY_OF_YEAR, -1)
                        }
                        onDateChange(DateFormatterUtil.formatDDMMYY(prevCal))
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("${testTagPrefix}_backward_button")
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous Day (Backward)",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Center Clickable Date Card
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDatePicker() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = "Pick Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                displayDate,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (relativeTag.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (relativeTag) {
                                        "Today" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        "Yesterday" -> Color(0xFFE2E8F0)
                                        else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                    }
                                ) {
                                    Text(
                                        relativeTag,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (relativeTag == "Today") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            humanDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Forward Button (> +1 Day)
                FilledTonalIconButton(
                    onClick = {
                        val nextCal = (calendar.clone() as Calendar).apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                        onDateChange(DateFormatterUtil.formatDDMMYY(nextCal))
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("${testTagPrefix}_forward_button")
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next Day (Forward)",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Preset Chips (Yesterday, Today, Tomorrow, Calendar Picker)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SuggestionChip(
                onClick = {
                    val yestCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                    onDateChange(DateFormatterUtil.formatDDMMYY(yestCal))
                },
                label = { Text("◀ Yesterday", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f)
            )
            SuggestionChip(
                onClick = {
                    val todayCal = Calendar.getInstance()
                    onDateChange(DateFormatterUtil.formatDDMMYY(todayCal))
                },
                label = { Text("Today", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                modifier = Modifier.weight(1f)
            )
            SuggestionChip(
                onClick = {
                    val tomCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                    onDateChange(DateFormatterUtil.formatDDMMYY(tomCal))
                },
                label = { Text("Tomorrow ▶", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { showDatePicker() },
                modifier = Modifier.size(32.dp).align(Alignment.CenterVertically)
            ) {
                Icon(
                    Icons.Default.EditCalendar,
                    contentDescription = "Choose custom date from calendar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
