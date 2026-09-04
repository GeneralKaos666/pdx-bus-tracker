package com.trimettransit.tracker.wear

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.trimettransit.tracker.R
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

@Composable
fun HomeScreen(
    onOpenFavorites: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenRoutes: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    WearContentEntrance(modifier = Modifier.fillMaxSize()) {
        ScreenScaffold(scrollState = listState) { contentPadding ->
            TransformingLazyColumn(
                state = listState,
                contentPadding = contentPadding
            ) {
                item {
                    ListHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) {
                        Text(stringResource(R.string.app_name))
                    }
                }
                item {
                    val interactionSource = remember { MutableInteractionSource() }
                    Button(
                        onClick = onOpenFavorites,
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .wearPressScale(interactionSource),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) {
                        Text(stringResource(R.string.favorites))
                    }
                }
                item {
                    val interactionSource = remember { MutableInteractionSource() }
                    Button(
                        onClick = onOpenRecent,
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .wearPressScale(interactionSource),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) {
                        Text(stringResource(R.string.recent_stops))
                    }
                }
                item {
                    val interactionSource = remember { MutableInteractionSource() }
                    Button(
                        onClick = onOpenRoutes,
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .wearPressScale(interactionSource),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) {
                        Text(stringResource(R.string.routes))
                    }
                }
                item {
                    val interactionSource = remember { MutableInteractionSource() }
                    Button(
                        onClick = onOpenAbout,
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .wearPressScale(interactionSource),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) {
                        Text(stringResource(R.string.about))
                    }
                }
            }
        }
    }
}
