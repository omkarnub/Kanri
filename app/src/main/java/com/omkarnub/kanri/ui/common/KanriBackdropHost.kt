package com.omkarnub.kanri.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun KanriBackdropHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val glassTokens = LocalKanriGlass.current
    if (glassTokens != null) {
        val hazeState = remember { HazeState() }
        Box(modifier = modifier.fillMaxSize()) {
            GlassBackdrop(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
            )
            CompositionLocalProvider(LocalHazeState provides hazeState) {
                content()
            }
        }
    } else {
        content()
    }
}
