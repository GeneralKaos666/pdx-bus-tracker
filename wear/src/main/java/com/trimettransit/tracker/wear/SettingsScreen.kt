package com.trimettransit.tracker.wear

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.trimettransit.tracker.R
import com.trimettransit.tracker.wear.tile.TileScheduler

/**
 * Watch settings: arrivals filter, tile refresh interval, and About (version,
 * license, third-party notices). All prefs live in [WearPrefs].
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var onlyShowRoute by remember { mutableStateOf(WearPrefs.onlyShowSelectedRoute(context)) }
    var refreshMinutes by remember { mutableIntStateOf(WearPrefs.refreshIntervalMinutes(context)) }
    var licensesExpanded by remember { mutableStateOf(false) }
    val version = watchVersionName(context)
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = listState,
        scrollIndicator = { ScrollIndicator(listState) }
    ) { contentPadding ->
        WearContentEntrance(modifier = Modifier.fillMaxSize()) {
            TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
                item {
                    ListHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) { Text(stringResource(R.string.settings)) }
                }

                item {
                    ListSubHeader { Text(stringResource(R.string.section_arrivals)) }
                }
                item {
                    SwitchButton(
                        checked = onlyShowRoute,
                        onCheckedChange = {
                            onlyShowRoute = it
                            WearPrefs.setOnlyShowSelectedRoute(context, it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        label = {
                            Text(
                                text = stringResource(R.string.only_show_selected_route),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }

                item {
                    ListSubHeader { Text(stringResource(R.string.section_tile)) }
                }
                items(WearPrefs.TILE_REFRESH_OPTIONS) { optionMinutes ->
                    RadioButton(
                        selected = optionMinutes == refreshMinutes,
                        onSelect = {
                            refreshMinutes = optionMinutes
                            WearPrefs.setRefreshIntervalMinutes(context, optionMinutes)
                            TileScheduler.schedulePeriodic(context, optionMinutes)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        label = {
                            Text(
                                text = stringResource(R.string.tile_refresh_interval_format, optionMinutes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }

                item {
                    ListSubHeader { Text(stringResource(R.string.section_about)) }
                }
                item {
                    Text(
                        text = stringResource(R.string.version_format, version),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.about_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.mit_license),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                item {
                    Button(
                        onClick = { licensesExpanded = !licensesExpanded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Text(stringResource(R.string.third_party_licenses))
                    }
                }
                if (licensesExpanded) {
                    item {
                        Text(
                            text = stringResource(R.string.third_party_notices),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}