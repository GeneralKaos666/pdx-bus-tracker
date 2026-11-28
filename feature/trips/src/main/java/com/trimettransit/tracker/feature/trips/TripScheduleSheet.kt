package com.trimettransit.tracker.feature.trips

import com.trimettransit.tracker.ui.components.rememberPressSource
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.domain.ScheduleDayKind
import com.trimettransit.tracker.model.domain.scheduleDayKind
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.theme.m3ContentExpand
import com.trimettransit.tracker.ui.theme.m3ContentShrink
import com.trimettransit.tracker.util.clockTime
import org.joda.time.DateTime
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date

/**
 * Date + time picker for the planner's timed modes. One surface holds both pickers so a
 * trip can be scheduled for a future day without chaining two dialogs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TripScheduleSheet(
    arriveBy: Boolean,
    initialMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val initialDateUtc = remember(initialMillis) { utcMidnightMillis(initialMillis, zone) }
    val initialCalendar = remember(initialMillis) {
        java.util.Calendar.getInstance().apply { timeInMillis = initialMillis }
    }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = initialDateUtc)
    val timeState = rememberTimePickerState(
        initialHour = initialCalendar.get(java.util.Calendar.HOUR_OF_DAY),
        initialMinute = initialCalendar.get(java.util.Calendar.MINUTE),
        // Follow the phone's own 12/24-hour setting instead of forcing 12-hour.
        is24Hour = DateFormat.is24HourFormat(context)
    )
    val chosenMillis = combineDateAndTime(
        selectedDateUtcMillis = dateState.selectedDateMillis,
        hour = timeState.hour,
        minute = timeState.minute,
        zone = zone
    )
    val chosenIsStale = chosenMillis != null && chosenMillis <= System.currentTimeMillis()

    // Quick picks for the times riders actually choose. The date+time pickers stay for
    // everything else, but a common trip should not need a calendar scroll.
    fun applyPreset(minutesAhead: Int) {
        val target = System.currentTimeMillis() + minutesAhead * 60_000L
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = target }
        dateState.selectedDateMillis = utcMidnightMillis(target, zone)
        timeState.hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        timeState.minute = calendar.get(java.util.Calendar.MINUTE)
    }
    val presets = listOf(15, 30, 60, 120)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(
                    if (arriveBy) R.string.arrive_by_time_title else R.string.depart_at_time_title
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { minutes ->
                    AssistChip(
                        onClick = { applyPreset(minutes) },
                        label = {
                            Text(
                                if (minutes < 60) {
                                    stringResource(R.string.trip_preset_minutes_fmt, minutes)
                                } else {
                                    stringResource(R.string.trip_preset_hours_fmt, minutes / 60)
                                }
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            DatePicker(state = dateState, modifier = Modifier.fillMaxWidth())
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            TimePicker(state = timeState, modifier = Modifier.fillMaxWidth())
            AnimatedVisibility(
                visible = chosenIsStale,
                enter = m3ContentExpand(),
                exit = m3ContentShrink()
            ) {
                Text(
                    text = stringResource(R.string.arrive_by_in_past),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                val cancelSource = rememberPressSource()
                TextButton(
                    onClick = onDismiss,
                    interactionSource = cancelSource,
                    modifier = Modifier.pressScale(cancelSource)
                ) {
                    Text(stringResource(R.string.cancel))
                }
                val doneSource = rememberPressSource()
                TextButton(
                    // A time that has already passed cannot be planned for, so it cannot be
                    // confirmed either.
                    enabled = chosenMillis != null && !chosenIsStale,
                    onClick = {
                        chosenMillis?.let(onConfirm)
                        onDismiss()
                    },
                    interactionSource = doneSource,
                    modifier = Modifier.pressScale(doneSource)
                ) {
                    Text(stringResource(R.string.done))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Compact chip label: `Today 4:30 PM`, `Tomorrow 9:05 AM`, or `Wed, Mar 4 9:05 AM`. */
@Composable
internal fun scheduleTimeLabel(millis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val context = LocalContext.current
    val time = clockTime(DateTime(millis))
    val clock = "${time.text} ${time.period}"
    return when (scheduleDayKind(millis, nowMillis)) {
        ScheduleDayKind.TODAY -> stringResource(R.string.trip_today_fmt, clock)
        ScheduleDayKind.TOMORROW -> stringResource(R.string.trip_tomorrow_fmt, clock)
        ScheduleDayKind.LATER -> stringResource(
            R.string.trip_date_time_fmt,
            DateFormat.getMediumDateFormat(context).format(Date(millis)),
            clock
        )
    }
}

/** UTC midnight of the local date containing [millis], the shape Material's DatePicker expects. */
private fun utcMidnightMillis(millis: Long, zone: ZoneId): Long =
    Instant.ofEpochMilli(millis)
        .atZone(zone)
        .toLocalDate()
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

/** Combines DatePicker's UTC date with the TimePicker's wall-clock time in [zone]. */
internal fun combineDateAndTime(
    selectedDateUtcMillis: Long?,
    hour: Int,
    minute: Int,
    zone: ZoneId = ZoneId.systemDefault()
): Long? {
    val dateUtc = selectedDateUtcMillis ?: return null
    val localDate = Instant.ofEpochMilli(dateUtc).atZone(ZoneOffset.UTC).toLocalDate()
    return localDate.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
}
