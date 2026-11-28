package com.trimettransit.tracker.feature.settings

import androidx.compose.runtime.Composable
import com.trimettransit.tracker.ui.components.SectionHeader
import com.trimettransit.tracker.ui.components.SectionHeaderStyle

@Composable
internal fun SettingsGroupHeader(label: String) {
    SectionHeader(title = label, style = SectionHeaderStyle.Group)
}
