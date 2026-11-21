package com.alwinsden.eightparams.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private val SheetBackground = Color(0xFFF2F2F7)
private val FieldBackground = Color(0xFFEFEFF4)

/**
 * Apple Health–style logging sheet (screenshot 1): circular close/confirm
 * buttons, big colored icon, title, and a white card with Date / Time / value.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntrySheet(
    type: MetricType,
    onDismiss: () -> Unit,
    onSave: (value1: Double, value2: Double?, timestampMillis: Long) -> Unit,
) {
    val tz = TimeZone.currentSystemDefault()
    val now = remember { Clock.System.now().toLocalDateTime(tz) }
    var date by remember { mutableStateOf(now.date) }
    var time by remember { mutableStateOf(now.time) }
    var value1Text by remember { mutableStateOf("") }
    var value2Text by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val v1 = value1Text.toDoubleOrNull()
    val v2 = value2Text.toDoubleOrNull()
    val isValid = v1 != null && v1 > 0 && (type != MetricType.BloodPressure || (v2 != null && v2 > 0))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetBackground,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(Icons.Default.Close, "Cancel", onClick = onDismiss)
                Spacer(Modifier.weight(1f))
                CircleIconButton(
                    Icons.Default.Check,
                    "Save",
                    enabled = isValid,
                    onClick = {
                        if (isValid) {
                            val timestamp = date.atTime(time.hour, time.minute).toInstant(tz).toEpochMilliseconds()
                            onSave(v1!!, if (type == MetricType.BloodPressure) v2 else null, timestamp)
                        }
                    },
                )
            }

            Spacer(Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = androidx.compose.foundation.shape.CircleShape, color = Color.White) {
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        MetricGlyph(type = type, tint = metricColor(type), modifier = Modifier.size(46.dp))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                type.title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = HealthColors.TextPrimary,
            )
            Spacer(Modifier.height(22.dp))

            Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
                Column {
                    SheetRow(height = 56.dp) {
                        Text("Date", fontSize = 17.sp, color = HealthColors.TextSecondary)
                        Spacer(Modifier.weight(1f))
                        PillButton(text = formatDateLong(date.atStartOfDayIn(tz).toEpochMilliseconds(), tz)) {
                            showDatePicker = true
                        }
                    }
                    HorizontalDivider(color = HealthColors.Divider, thickness = 0.5.dp)
                    SheetRow(height = 56.dp) {
                        Text("Time", fontSize = 17.sp, color = HealthColors.TextSecondary)
                        Spacer(Modifier.weight(1f))
                        PillButton(text = formatTimeOfDay(time)) { showTimePicker = true }
                    }
                    HorizontalDivider(color = HealthColors.Divider, thickness = 0.5.dp)

                    if (type == MetricType.BloodPressure) {
                        SheetRow(height = 56.dp) {
                            ValueField(
                                value = value1Text,
                                placeholder = "mmHg",
                                onValueChange = { value1Text = sanitizeNumber(it) },
                                modifier = Modifier.weight(1f),
                            )
                            Text(" / ", fontSize = 17.sp, color = HealthColors.TextSecondary)
                            ValueField(
                                value = value2Text,
                                placeholder = "",
                                onValueChange = { value2Text = sanitizeNumber(it) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        SheetRow(height = 56.dp) {
                            ValueField(
                                value = value1Text,
                                placeholder = type.unit,
                                onValueChange = { value1Text = sanitizeNumber(it) },
                                modifier = Modifier.weight(1f),
                            )
                            if (value1Text.isNotEmpty()) {
                                Text(" ${type.unit}", fontSize = 17.sp, color = HealthColors.TextSecondary)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        date = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC).date
                    }
                    showDatePicker = false
                }) { Text("Done", color = metricColor(type)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = HealthColors.TextSecondary) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            val timeState = rememberTimePickerState(
                initialHour = time.hour,
                initialMinute = time.minute,
                is24Hour = false,
            )
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
                Column(modifier = Modifier.padding(18.dp)) {
                    TimePicker(state = timeState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel", color = HealthColors.TextSecondary)
                        }
                        TextButton(onClick = {
                            time = LocalTime(timeState.hour, timeState.minute)
                            showTimePicker = false
                        }) {
                            Text("Done", color = metricColor(type))
                        }
                    }
                }
            }
        }
    }
}

/** 24h→12h "5:44 PM" formatting for the picked time. */
private fun formatTimeOfDay(time: LocalTime): String {
    val amPm = if (time.hour < 12) "AM" else "PM"
    val hour12 = (time.hour % 12).let { if (it == 0) 12 else it }
    return "$hour12:${time.minute.toString().padStart(2, '0')} $amPm"
}

private fun sanitizeNumber(raw: String): String {
    var sawDot = false
    return raw.filter { c ->
        when {
            c.isDigit() -> true
            c == '.' && !sawDot -> { sawDot = true; true }
            else -> false
        }
    }
}

@Composable
private fun SheetRow(height: androidx.compose.ui.unit.Dp, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun PillButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = FieldBackground,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = HealthColors.TextPrimary,
        )
    }
}

@Composable
private fun ValueField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        textStyle = TextStyle(
            fontSize = 17.sp,
            color = HealthColors.TextPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        cursorBrush = SolidColor(metricColor(MetricType.Weight)),
        decorationBox = { innerField ->
            Box(contentAlignment = if (value.isEmpty()) Alignment.CenterStart else Alignment.CenterEnd) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, fontSize = 17.sp, color = HealthColors.TextTertiary)
                }
                innerField()
            }
        },
    )
}
