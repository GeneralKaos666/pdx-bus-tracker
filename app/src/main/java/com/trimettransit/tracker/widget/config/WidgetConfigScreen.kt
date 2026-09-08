package com.trimettransit.tracker.widget.config

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.R
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.FavoritesRepository
import com.trimettransit.tracker.widget.WidgetConfig
import com.trimettransit.tracker.widget.WidgetThemeOption

/**
 * Per-widget configuration editor. An empty [WidgetConfig.selectedStopIds] means "all
 * favorites" — it is never persisted as a blank-widget selection. [selectedStopIds] and
 * [reorder] are the reorder seam the drag layer (requiring [androidx.compose.foundation.gestures.detectDragGestures])
 * builds on.
 */
@Composable
fun WidgetConfigScreen(
    initial: WidgetConfig,
    favoritesRepository: FavoritesRepository,
    onDone: (WidgetConfig) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler { onCancel() }

    val selectedStopIds = remember { mutableStateOf(initial.selectedStopIds.distinct()) }
    val arrivalsPerStop = remember { mutableIntStateOf(initial.arrivalsPerStop) }
    val showClockTime = remember { mutableStateOf(initial.showClockTime) }
    val theme = remember { mutableStateOf(initial.theme) }
    val compactRows = remember { mutableStateOf(initial.compactRows) }
    val titleText = remember { mutableStateOf(initial.titleText.orEmpty()) }
    val hideTitle = remember { mutableStateOf(initial.hideTitle) }
    val favorites = remember { mutableStateOf(listOf<Stop>()) }
    LaunchedEffect(Unit) { favorites.value = favoritesRepository.getFavorites() }

    fun toggleStop(stop: Stop) {
        val id = stop.locId.toString()
        selectedStopIds.value = if (id in selectedStopIds.value) {
            selectedStopIds.value - id
        } else {
            selectedStopIds.value + id
        }
    }

    /**
     * Moves the selected stop at [from] to index [to] within [selectedStopIds],
     * preserving order of the rest and never introducing duplicates.
     */
    fun reorder(from: Int, to: Int) {
        val current = selectedStopIds.value
        if (from !in current.indices || to !in current.indices) return
        val reordered = current.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        selectedStopIds.value = reordered
    }

    fun buildConfig(): WidgetConfig = WidgetConfig(
        selectedStopIds = selectedStopIds.value.distinct(),
        arrivalsPerStop = arrivalsPerStop.value,
        showClockTime = showClockTime.value,
        theme = theme.value,
        compactRows = compactRows.value,
        titleText = titleText.value.trim().takeIf { it.isNotBlank() },
        hideTitle = hideTitle.value
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.widget_config_cancel))
                    }
                    Button(
                        onClick = { onDone(buildConfig()) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.widget_config_done))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.widget_config_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            SectionHeader(R.string.widget_config_stops)
            Text(
                text = stringResource(R.string.widget_config_stops_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (favorites.value.isEmpty()) {
                Text(
                    text = stringResource(R.string.widget_config_no_favorites),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                favorites.value.forEach { stop ->
                    FavoriteStopRow(
                        stop = stop,
                        selected = stop.locId.toString() in selectedStopIds.value,
                        onToggle = { toggleStop(stop) },
                        modifier = Modifier
                    )
                }
            }

            SectionHeader(R.string.widget_config_arrivals)
            Text(
                text = stringResource(R.string.widget_config_arrivals_per_stop),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ChoiceSegmentedRow(
                selected = arrivalsPerStop.value,
                onSelect = { arrivalsPerStop.value = it },
                options = listOf(1, 2, 3),
                labelFor = { it.toString() }
            )

            SectionHeader(R.string.widget_config_time)
            ChoiceSegmentedRow(
                selected = showClockTime.value,
                onSelect = { showClockTime.value = it },
                options = listOf(false, true),
                labelFor = {
                    if (it) stringResource(R.string.widget_config_clock)
                    else stringResource(R.string.widget_config_countdown)
                }
            )

            SectionHeader(R.string.widget_config_theme)
            ChoiceSegmentedRow(
                selected = theme.value,
                onSelect = { theme.value = it },
                options = WidgetThemeOption.entries,
                labelFor = { option ->
                    when (option) {
                        WidgetThemeOption.SYSTEM -> stringResource(R.string.widget_config_theme_system)
                        WidgetThemeOption.LIGHT -> stringResource(R.string.widget_config_theme_light)
                        WidgetThemeOption.DARK -> stringResource(R.string.widget_config_theme_dark)
                    }
                }
            )

            SectionHeader(R.string.widget_config_rows)
            ChoiceSegmentedRow(
                selected = compactRows.value,
                onSelect = { compactRows.value = it },
                options = listOf(false, true),
                labelFor = {
                    if (it) stringResource(R.string.widget_config_compact)
                    else stringResource(R.string.widget_config_detailed)
                }
            )

            SectionHeader(R.string.widget_config_title_field)
            OutlinedTextField(
                value = titleText.value,
                onValueChange = { titleText.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.next_arrivals_widget_label)) },
                singleLine = true
            )
            HideTitleRow(
                checked = hideTitle.value,
                onCheckedChange = { hideTitle.value = it }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(@StringRes res: Int) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

/** Single-choice segmented control; [labelFor] renders each option's visible label. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceSegmentedRow(
    selected: T,
    onSelect: (T) -> Unit,
    options: List<T>,
    labelFor: @Composable (T) -> String
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(text = labelFor(option))
            }
        }
    }
}

/** A favorite stop row: checkbox reflecting membership plus a name/id subtitle. */
@Composable
private fun FavoriteStopRow(
    stop: Stop,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onToggle
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = selected, onCheckedChange = null)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stop.desc,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stopSubtitle(stop),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun stopSubtitle(stop: Stop): String {
    val id = "#${stop.locId}"
    return if (stop.dirDesc.isNotBlank()) "$id \u00b7 ${stop.dirDesc}" else id
}

@Composable
private fun HideTitleRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.widget_config_hide_title),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}