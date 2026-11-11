package com.omkarnub.kanri.ui.widget

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Astronomical and weather constants ported from the Weather recipe specification.
 */
object FinancialSkyMath {
    const val SUNRISE = 7.5f
    const val SUNSET = 19.5f
    const val SYNODIC = 29.530589
    const val MS_PER_DAY = 86400000.0

    // Reference full moon timestamp (UTC 2026-09-26 16:49)
    private val FULL_MOON_REF = 1790441340000L

    /**
     * Calculates the astronomical moon age from 0.0 (new) to 0.5 (full) to 1.0 (waning new).
     */
    fun moonAge(date: Date = Date()): Float {
        val diff = (date.time - FULL_MOON_REF).toDouble() / MS_PER_DAY
        val age = ((diff / SYNODIC) + 0.5) % 1.0
        return (if (age < 0.0) age + 1.0 else age).toFloat()
    }

    /**
     * Returns true if a dot at (dx, dy) on a disc of radius r is lit for the given moon age.
     */
    fun isMoonLit(dx: Float, r: Float, age: Float): Boolean {
        val lit = ((1f - cos(age * 2f * PI.toFloat())) / 2f)
        val nx = dx / r
        return if (age < 0.5f) {
            nx >= (1f - 2f * lit)
        } else {
            nx <= -(1f - 2f * lit)
        }
    }

    fun wrap(v: Int, n: Int): Int = ((v % n) + n) % n
}

/**
 * 7-Day Spending Record replacing weather forecast.
 */
data class FinancialDayRecord(
    val dayName: String,
    val spendAmount: Double,
    val budgetTarget: Double,
    val isToday: Boolean = false
)

/**
 * Interactive Dot-Matrix Sky Well that renders astronomical day/night cycles,
 * celestial paths, clouds, terrain, and live financial conditions.
 */
@Composable
fun FinancialSkyWell(
    todaySpent: Double,
    safeToSpendDaily: Double,
    modifier: Modifier = Modifier,
    hour: Float = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + Calendar.getInstance().get(Calendar.MINUTE) / 60f,
    cols: Int = 42,
    rows: Int = 24,
    horizon: Int = 18,
    pitchDp: Dp = 6.8.dp,
    dotSizeDp: Dp = 5.2.dp,
    replayTrigger: Int = 0
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sky_tick")
    val tickFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sky_tick"
    )
    val tick = (tickFloat + replayTrigger * 3).toInt()

    // Financial climate state resolver
    val isNight = hour < FinancialSkyMath.SUNRISE || hour > FinancialSkyMath.SUNSET
    val isOverBudget = todaySpent > safeToSpendDaily * 1.5 && safeToSpendDaily > 0
    val isModerateSpend = todaySpent > safeToSpendDaily && !isOverBudget
    val moonAgeVal = remember { FinancialSkyMath.moonAge() }

    val conditionText = when {
        isOverBudget -> "Stormy Outflow"
        isModerateSpend -> "Partly Cloudy"
        isNight -> "Starry Night"
        hour in 7.0f..9.0f -> "Golden Sunrise"
        hour in 17.5f..19.5f -> "Amber Sunset"
        else -> "Clear Horizons"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF040507))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Dot-matrix Sky Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((rows * 4.8).dp)
            ) {
                val canvasW = size.width
                val canvasH = size.height
                val dotPitch = canvasW / cols
                val dotSize = dotPitch * 0.78f
                val dotRadius = dotSize / 2f

                val grid = HashMap<String, Color>()

                val day = hour in FinancialSkyMath.SUNRISE..FinancialSkyMath.SUNSET
                val td = ((hour - FinancialSkyMath.SUNRISE) / (FinancialSkyMath.SUNSET - FinancialSkyMath.SUNRISE)).coerceIn(0f, 1f)
                val tn = (FinancialSkyMath.wrap(((hour - FinancialSkyMath.SUNSET).toInt()), 24).toFloat() / (24f - (FinancialSkyMath.SUNSET - FinancialSkyMath.SUNRISE))).coerceIn(0f, 1f)

                // Sun or Moon Arc calculation
                fun arcPos(t: Float): Pair<Float, Float> {
                    val x = 3f + t * (cols - 7)
                    val y = horizon - 2f - sin(PI.toFloat() * t) * (horizon - 7f)
                    return Pair(x, y)
                }

                // 1. Dawn / Dusk Horizon Glow
                if (day && (td < 0.15f || td > 0.85f)) {
                    for (y in (horizon - 4) until horizon) {
                        for (x in 0 until cols) {
                            if ((x + y) % 2 == 0 && (y >= horizon - 2 || x % 4 == 0)) {
                                grid["$x,$y"] = Color(0x66FFA726)
                            }
                        }
                    }
                }

                // 2. Stars at night
                if (!day) {
                    for (s in 0 until 14) {
                        val sx = FinancialSkyMath.wrap(s * 17 + 5, cols)
                        val sy = FinancialSkyMath.wrap(s * 7 + 1, horizon - 4)
                        if (FinancialSkyMath.wrap(tick + s * 3, 7) != 0) {
                            grid["$sx,$sy"] = Color(0xCCFFFFFF)
                        }
                    }
                }

                // 3. Sun or Moon
                if (day) {
                    // Sun disc
                    val (px, py) = arcPos(td)
                    val sx = px.roundToInt()
                    val sy = py.roundToInt()
                    val r = 2.8f

                    for (dx in -4..4) {
                        for (dy in -4..4) {
                            if (dx * dx + dy * dy <= r * r + 0.3f) {
                                val gx = sx + dx
                                val gy = sy + dy
                                if (gx in 0 until cols && gy in 0 until horizon) {
                                    grid["$gx,$gy"] = Color(0xFFFFD54F)
                                }
                            }
                        }
                    }

                    // Stepping Sun Rays
                    val dirs = if (FinancialSkyMath.wrap(tick, 4) < 2) {
                        listOf(Pair(0, -1), Pair(0, 1), Pair(-1, 0), Pair(1, 0))
                    } else {
                        listOf(Pair(-1, -1), Pair(1, -1), Pair(-1, 1), Pair(1, 1))
                    }
                    for (dir in dirs) {
                        val rx = sx + dir.first * 4
                        val ry = sy + dir.second * 4
                        if (rx in 0 until cols && ry in 0 until horizon) {
                            grid["$rx,$ry"] = Color(0x88FFD54F)
                        }
                    }

                    // Birds flying across sky
                    if (!isOverBudget) {
                        val bx = FinancialSkyMath.wrap((cols * 0.55f).toInt() + (tick / 2), cols + 4) - 2
                        val by = 5
                        val up = if (FinancialSkyMath.wrap(tick, 2) == 0) 1 else 0
                        grid["${bx - 1},${by - up}"] = Color(0xFF90A4AE)
                        grid["$bx,$by"] = Color(0xFF90A4AE)
                        grid["${bx + 1},${by - up}"] = Color(0xFF90A4AE)
                    }
                } else {
                    // Moon in its phase
                    val (qx, qy) = arcPos(tn)
                    val mx = qx.roundToInt()
                    val my = qy.roundToInt()
                    val r = 2.4f

                    for (dx in -3..3) {
                        for (dy in -3..3) {
                            if (dx * dx + dy * dy <= r * r + 0.35f) {
                                val gx = mx + dx
                                val gy = my + dy
                                if (gx in 0 until cols && gy in 0 until horizon) {
                                    val isLit = FinancialSkyMath.isMoonLit(dx.toFloat(), r, moonAgeVal)
                                    grid["$gx,$gy"] = if (isLit) Color.White else Color(0x33B0BEC5)
                                }
                            }
                        }
                    }
                }

                // 4. Clouds (when moderate or stormy)
                if (isModerateSpend || isOverBudget) {
                    val drift = (tick * 0.3f).toInt()
                    val cloudCenters = listOf(
                        Triple(FinancialSkyMath.wrap(12 + drift, cols), 6, 4),
                        Triple(FinancialSkyMath.wrap(28 + drift, cols), 8, 5)
                    )
                    for (cloud in cloudCenters) {
                        val cx = cloud.first
                        val cy = cloud.second
                        val cw = cloud.third
                        val cloudColor = if (isOverBudget) Color(0xFF546E7A) else Color(0xFFB0BEC5)
                        for (dx in -cw..cw) {
                            val gx = cx + dx
                            if (gx in 0 until cols) {
                                grid["$gx,$cy"] = cloudColor
                                grid["$gx,${cy - 1}"] = cloudColor
                            }
                        }
                    }
                }

                // 5. Rain & Lightning if Over Budget
                if (isOverBudget) {
                    for (x in 2 until cols step 3) {
                        val ry = FinancialSkyMath.wrap(tick * 2 + x * 2, horizon - 8) + 8
                        grid["$x,$ry"] = Color(0xFF4FC3F7)
                    }
                    if (FinancialSkyMath.wrap(tick, 16) < 2) {
                        // Lightning bolt
                        val lx = 20
                        for (y in 7..horizon) {
                            grid["$lx,$y"] = Color(0xFFFFEB3B)
                            grid["${lx + 1},$y"] = Color(0xFFFFEB3B)
                        }
                    }
                }

                // 6. Horizon Hills
                for (x in 0 until cols) {
                    val hill = max(0, (3f - abs(x - cols * 0.78f) / 2.6f).roundToInt()) +
                            max(0, (1.8f - abs(x - cols * 0.15f) / 2.4f).roundToInt())
                    for (y in (horizon - hill) until horizon) {
                        grid["$x,$y"] = Color(0xFF263238)
                    }
                    grid["$x,$horizon"] = Color(0x5537474F)
                }

                // Draw all dots
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val dotColor = grid["$c,$r"] ?: Color(0x12FFFFFF)
                        val cx = c * dotPitch + dotPitch / 2f
                        val cy = r * dotPitch + dotPitch / 2f
                        drawCircle(
                            color = dotColor,
                            radius = dotRadius,
                            center = Offset(cx, cy)
                        )
                    }
                }
            }

            // Ground info overlay below horizon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = CurrencyUtils.formatCurrency(todaySpent),
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White
                    )
                    Text(
                        text = "spent today",
                        fontFamily = GoogleSansFlex,
                        fontSize = 11.sp,
                        color = Color(0x88FFFFFF)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = conditionText,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = when {
                            isOverBudget -> Color(0xFFFF5252)
                            isModerateSpend -> Color(0xFFFFB74D)
                            isNight -> Color(0xFF64B5F6)
                            else -> Color(0xFF00E676)
                        }
                    )
                    Text(
                        text = "Safe " + CurrencyUtils.formatCompactCurrency(safeToSpendDaily) + "/day",
                        fontFamily = GoogleSansFlex,
                        fontSize = 11.sp,
                        color = Color(0xB3FFFFFF)
                    )
                }
            }
        }
    }
}

/**
 * The 7-Day Spending Pulse: draws each day's spend as lit dots on one shared scale.
 */
@Composable
fun FinancialWeekPulse(
    days: List<FinancialDayRecord>,
    modifier: Modifier = Modifier,
    maxScale: Double = 3000.0
) {
    val totalDots = 20

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("7-Day Spend Pulse", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0x88FFFFFF))
            Text(CurrencyUtils.formatCurrency(0.0, includeDecimals = false) + " — " + CurrencyUtils.formatCompactCurrency(maxScale), fontSize = 10.sp, color = Color(0x66FFFFFF))
        }

        // Each Day's Row
        days.take(7).forEach { day ->
            val ratio = (day.spendAmount / maxScale).coerceIn(0.0, 1.0).toFloat()
            val litDots = (ratio * totalDots).toInt().coerceIn(1, totalDots)
            val isOver = day.spendAmount > day.budgetTarget && day.budgetTarget > 0
            val rowColor = when {
                isOver -> Color(0xFFFF5252)
                day.spendAmount > day.budgetTarget * 0.75 -> Color(0xFFFFB74D)
                else -> Color(0xFF00E676)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Day name
                Text(
                    text = day.dayName,
                    fontFamily = GoogleSansFlex,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                    color = if (day.isToday) Color.White else Color(0x99FFFFFF),
                    modifier = Modifier.width(38.dp)
                )

                // Dot scale bar
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                ) {
                    val step = size.width / totalDots
                    val dotRadius = 2.5f

                    for (i in 0 until totalDots) {
                        val cx = i * step + step / 2f
                        val cy = size.height / 2f
                        val isLit = i < litDots

                        drawCircle(
                            color = if (isLit) rowColor else Color(0x18FFFFFF),
                            radius = if (isLit) dotRadius * 1.1f else dotRadius * 0.8f,
                            center = Offset(cx, cy)
                        )
                    }
                }

                // Amount
                Text(
                    text = CurrencyUtils.formatCompactCurrency(day.spendAmount),
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.width(55.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}

/**
 * Complete Financial Sky Weather Widget:
 * Root Slab > Header (Title, Summary, Live LED, Clock)
 *           > Sky Well (Dot Matrix Celestial Scene & Condition)
 *           > 7-Day Spending Pulse (Shared dot-matrix scale)
 */
@Composable
fun FinancialWeatherWidget(
    todaySpent: Double,
    monthBudget: Double,
    monthSpent: Double,
    safeToSpendDaily: Double,
    weekRecords: List<FinancialDayRecord>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    var replayCounter by remember { mutableIntStateOf(0) }
    val now = Calendar.getInstance()
    val timeStr = String.format(java.util.Locale.US, "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
    val monthName = SimpleDateFormat("MMMM", java.util.Locale.US).format(now.time)
    val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF090A0C))
            .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(26.dp))
            .clickable {
                replayCounter++
                onClick?.invoke()
            }
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header Row: Place, summary, Live LED, Clock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FINANCIAL SKY",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.08.sp,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Text(
                        text = "$monthName Horizon • Day $dayOfMonth",
                        fontFamily = GoogleSansFlex,
                        fontSize = 11.sp,
                        color = Color(0x88FFFFFF)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                    )
                    Text(
                        text = "Live",
                        fontFamily = GoogleSansFlex,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xCCFFFFFF)
                    )
                    Text(
                        text = timeStr,
                        fontFamily = GoogleSansFlex,
                        fontSize = 11.sp,
                        color = Color(0x88FFFFFF)
                    )
                }
            }

            // Dot-Matrix Sky Well with Sun/Moon/Stars & Spend State
            FinancialSkyWell(
                todaySpent = todaySpent,
                safeToSpendDaily = safeToSpendDaily,
                replayTrigger = replayCounter
            )

            // 7-Day Spending Pulse
            FinancialWeekPulse(
                days = weekRecords,
                maxScale = max(safeToSpendDaily * 2.5, 3000.0)
            )
        }
    }
}

