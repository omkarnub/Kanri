package com.omkarnub.kanri.ui.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.util.rememberKanriHaptics
import com.omkarnub.kanri.widget.core.PixelMotion
import com.omkarnub.kanri.widget.core.PixelTheme
import com.omkarnub.kanri.widget.core.WidgetState
import com.omkarnub.kanri.widget.data.WidgetDataSource
import com.omkarnub.kanri.widget.providers.BudgetWidgetProvider
import com.omkarnub.kanri.widget.providers.DuesWidgetProvider
import com.omkarnub.kanri.widget.providers.GoalsWidgetProvider
import com.omkarnub.kanri.widget.providers.HeatmapWidgetProvider
import com.omkarnub.kanri.widget.providers.MonthDeltaWidgetProvider
import com.omkarnub.kanri.widget.providers.QuickActionsWidgetProvider
import com.omkarnub.kanri.widget.providers.ReviewInboxWidgetProvider
import com.omkarnub.kanri.widget.providers.SafeToSpendWidgetProvider
import com.omkarnub.kanri.widget.providers.SkyWidgetProvider
import com.omkarnub.kanri.widget.providers.StreakWidgetProvider
import com.omkarnub.kanri.widget.providers.TodayWidgetProvider
import com.omkarnub.kanri.widget.providers.WalletsWidgetProvider
import com.omkarnub.kanri.widget.render.WidgetRenderer

data class WidgetInfo(
    val title: String,
    val description: String,
    val category: String,
    val supportedSizes: List<Pair<String, Pair<Dp, Dp>>>,
    val providerClass: Class<*>,
    val stateProvider: suspend (android.content.Context) -> WidgetState
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    BackHandler(onBack = onBack)

    val widgetList = remember {
        listOf(
            // --- MONEY GROUP ---
            WidgetInfo(
                title = "Today Spend",
                description = "Spent today, expense count, and 24h dot timeline",
                category = "Money",
                supportedSizes = listOf("2x2" to Pair(180.dp, 160.dp), "4x2" to Pair(320.dp, 160.dp)),
                providerClass = TodayWidgetProvider::class.java,
                stateProvider = { WidgetDataSource.getTodayState(it) }
            ),
            WidgetInfo(
                title = "Budget",
                description = "Remaining amount, cycle % used, and pacing marker",
                category = "Money",
                supportedSizes = listOf(
                    "2x1" to Pair(180.dp, 90.dp),
                    "2x2" to Pair(180.dp, 160.dp),
                    "4x2" to Pair(320.dp, 160.dp)
                ),
                providerClass = BudgetWidgetProvider::class.java,
                stateProvider = { WidgetDataSource.getBudgetState(it) }
            ),
            WidgetInfo(
                title = "Wallets",
                description = "Cash, bank, and net worth overview with proportion rise",
                category = "Money",
                supportedSizes = listOf("2x2" to Pair(180.dp, 160.dp), "4x2" to Pair(320.dp, 160.dp)),
                providerClass = WalletsWidgetProvider::class.java,
                stateProvider = { WidgetDataSource.getWalletsState(it) }
            ),
            WidgetInfo(
                title = "Savings Goals",
                description = "Target progress, percentage, and milestone tick slider",
                category = "Money",
                supportedSizes = listOf("2x2" to Pair(180.dp, 160.dp), "4x1" to Pair(320.dp, 90.dp)),
                providerClass = GoalsWidgetProvider::class.java,
                stateProvider = { WidgetDataSource.getGoalsState(it) }
            ),
            WidgetInfo(
                title = "Dues (Lend & Borrow)",
                description = "Receivable, payable, net balance, and overdue alerts",
                category = "Money",
                supportedSizes = listOf("2x2" to Pair(180.dp, 160.dp), "4x2" to Pair(320.dp, 160.dp)),
                providerClass = DuesWidgetProvider::class.java,
                stateProvider = { WidgetDataSource.getDuesState(it) }
            ),

            // --- INSIGHTS GROUP ---
            WidgetInfo(
                title = "Financial Sky",
                description = "Live celestial arc, terrain silhouette, and 7-day spend pulse",
                category = "Insights",
                supportedSizes = listOf("4x2" to Pair(320.dp, 160.dp)),
                providerClass = SkyWidgetProvider::class.java,
                stateProvider = { WidgetDataSource.getSkyState(it) }
            ),
            WidgetInfo(
                title = "Month Heatmap",
                description = "Calendar dot grid showing daily spend intensity",
                category = "Insights",
                supportedSizes = listOf("4x3" to Pair(320.dp, 210.dp), "4x2" to Pair(320.dp, 155.dp)),
                providerClass = HeatmapWidgetProvider::class.java,
                stateProvider = { WidgetDataSource.getHeatmapState(it) }
            ),
            WidgetInfo(
                title = "No-Spend Streak",
                description = "Current streak days, best streak, and 30-day dot row",
                category = "Insights",
                supportedSizes = listOf("2x2" to Pair(180.dp, 160.dp), "4x1" to Pair(320.dp, 90.dp)),
                providerClass = StreakWidgetProvider::class.java,
                stateProvider = { WidgetDataSource.getStreakState(it) }
            ),
            WidgetInfo(
                title = "Safe to Spend",
                description = "Daily spending allowance until cycle end vs today spend",
                category = "Insights",
                supportedSizes = listOf("2x2" to Pair(180.dp, 160.dp), "4x1" to Pair(320.dp, 90.dp)),
                providerClass = SafeToSpendWidgetProvider::class.java,
                stateProvider = { WidgetDataSource.getSafeToSpendState(it) }
            ),
            WidgetInfo(
                title = "Month Delta",
                description = "Spend so far vs same day last month with comparison steps",
                category = "Insights",
                supportedSizes = listOf("2x2" to Pair(180.dp, 160.dp), "4x2" to Pair(320.dp, 160.dp)),
                providerClass = MonthDeltaWidgetProvider::class.java,
                stateProvider = { WidgetDataSource.getMonthDeltaState(it) }
            ),

            // --- ACTIONS GROUP ---
            WidgetInfo(
                title = "Review Inbox",
                description = "SMS transactions awaiting review with block visual",
                category = "Actions",
                supportedSizes = listOf("2x1" to Pair(180.dp, 90.dp), "2x2" to Pair(180.dp, 160.dp)),
                providerClass = ReviewInboxWidgetProvider::class.java,
                stateProvider = { WidgetDataSource.getReviewInboxState(it) }
            ),
            WidgetInfo(
                title = "Quick Actions",
                description = "One-tap dot buttons for Expense, Income, and Split Bill",
                category = "Actions",
                supportedSizes = listOf("4x1" to Pair(320.dp, 90.dp), "2x1" to Pair(180.dp, 90.dp)),
                providerClass = QuickActionsWidgetProvider::class.java,
                stateProvider = { WidgetDataSource.getQuickActionsState() }
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pixel DotTiles",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "12 Home Screen Widgets • 1:1 Live Previews",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            val categories = listOf("Money", "Insights", "Actions")
            categories.forEach { cat ->
                val itemsInCat = widgetList.filter { it.category == cat }
                if (itemsInCat.isNotEmpty()) {
                    item(key = "header_$cat") {
                        Text(
                            text = cat.uppercase(),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = PixelTheme.ColorDotMid,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }

                    itemsInCat.forEach { info ->
                        item(key = info.title) {
                            WidgetGalleryCard(info = info)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetGalleryCard(info: WidgetInfo) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    var state by remember { mutableStateOf<WidgetState?>(null) }
    var selectedSizeIndex by remember { mutableIntStateOf(0) }
    var replayCounter by remember { mutableIntStateOf(0) }

    LaunchedEffect(info) {
        state = info.stateProvider(context)
    }

    val currentSizePair = info.supportedSizes.getOrNull(selectedSizeIndex) ?: info.supportedSizes.first()
    val (sizeLabel, dimensions) = currentSizePair
    val (widthDp, heightDp) = dimensions

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111218))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.title,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = info.description,
                    fontFamily = GoogleSansFlex,
                    fontSize = 12.sp,
                    color = PixelTheme.ColorDotMid
                )
            }

            IconButton(
                onClick = {
                    haptics.click()
                    replayCounter++
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Replay Animation",
                    tint = PixelTheme.ColorDotMid
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Faux Home Screen Wallpaper Backdrop with 1:1 Live Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF181B26), Color(0xFF0A0C10))
                    )
                )
                .clickable {
                    haptics.click()
                    replayCounter++
                }
                .padding(vertical = 20.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            val currentState = state
            if (currentState != null) {
                PixelWidgetCanvasPreview(
                    state = currentState,
                    widthDp = widthDp,
                    heightDp = heightDp,
                    replayTrigger = replayCounter
                )
            } else {
                Box(
                    modifier = Modifier.size(widthDp, heightDp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading...", color = PixelTheme.ColorDotMid, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Size Chips & Pin Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Supported size chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                info.supportedSizes.forEachIndexed { index, (label, _) ->
                    FilterChip(
                        selected = (index == selectedSizeIndex),
                        onClick = {
                            haptics.click()
                            selectedSizeIndex = index
                            replayCounter++
                        },
                        label = {
                            Text(
                                text = label,
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0x33FFFFFF),
                            selectedLabelColor = Color.White,
                            containerColor = Color.Transparent,
                            labelColor = PixelTheme.ColorDotMid
                        )
                    )
                }
            }

            // Pin to Home Screen Action
            Button(
                onClick = {
                    haptics.click()
                    pinWidgetToHome(context, info.providerClass)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E222D),
                    contentColor = PixelTheme.ColorDotOn
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = PixelTheme.ColorMint
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Pin",
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun PixelWidgetCanvasPreview(
    state: WidgetState,
    widthDp: Dp,
    heightDp: Dp,
    replayTrigger: Int = 0
) {
    val context = LocalContext.current
    val animProgress = remember { Animatable(0f) }

    val animScale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1.0f
    )
    val animationsEnabled = animScale > 0f

    LaunchedEffect(replayTrigger, state) {
        if (!animationsEnabled) {
            animProgress.snapTo(1f)
        } else {
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = PixelMotion.DURATION_ENTRY_MS,
                    easing = PixelMotion.FastOutSlowIn
                )
            )
        }
    }

    Canvas(
        modifier = Modifier.size(widthDp, heightDp)
    ) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            WidgetRenderer.render(
                canvas = nativeCanvas,
                state = state,
                widthPx = size.width,
                heightPx = size.height,
                progress = animProgress.value,
                density = this.density,
                context = context
            )
        }
    }
}

private fun pinWidgetToHome(context: android.content.Context, providerClass: Class<*>) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
        val component = ComponentName(context, providerClass)
        appWidgetManager.requestPinAppWidget(component, null, null)
        Toast.makeText(context, "Pinned widget request sent to launcher", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(
            context,
            "Touch & hold your home screen, then tap Widgets > Kanri to place this tile.",
            Toast.LENGTH_LONG
        ).show()
    }
}
