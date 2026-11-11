package com.omkarnub.kanri.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.graphics.PathParser
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.ui.theme.Panchang
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Exact Panchang vector glyph path data matching official brand assets
private const val PATH_K = "M32.97,90.72L32.97,77.68L49.09,67.07L75.01,90.72L96.8,90.72L61.88,58.89L95.18,35.48L72.58,35.48L42.69,55.24L32.97,63.1L32.97,35.48L16.52,35.48L16.52,90.72Z"
private const val PATH_A = "M172.45,90.72L192.38,90.72L156.41,35.48L134.54,35.48L98.5,90.72L118.26,90.72L124.99,80.03L165.81,80.03ZM132.11,68.61L145.4,47.39L158.6,68.61Z"
private const val PATH_N = "M212.3,90.72L212.3,52.73L260.17,90.72L278.56,90.72L278.56,35.48L262.11,35.48L262.11,72.82L214.65,35.48L195.85,35.48L195.85,90.72Z"
private const val PATH_R = "M303.75,90.72L303.75,74.68L324.73,74.68L344.57,90.72L365.23,90.72L347.08,76.46L342.71,73.79L346.27,73.71C358.5,72.74 365.71,66.91 365.71,54.76C365.71,40.26 356.24,35.48 342.63,35.48L287.3,35.48L287.3,90.72ZM303.75,49.57L341.01,49.57C346.43,49.57 348.7,51.11 348.7,56.05C348.7,60.59 346.43,62.37 341.01,62.37L303.75,62.37Z"
private const val PATH_I = "M389.37,90.72L389.37,35.48L372.92,35.48L372.92,90.72Z"

// SVG bounding boxes for Panchang wordmark
private const val SVG_TOTAL_MIN_X = 16.52f
private const val SVG_TOTAL_MAX_X = 389.37f
private const val SVG_TOTAL_MIN_Y = 35.48f
private const val SVG_TOTAL_MAX_Y = 90.72f
private const val SVG_TOTAL_WIDTH = SVG_TOTAL_MAX_X - SVG_TOTAL_MIN_X // 372.85f
private const val SVG_TOTAL_HEIGHT = SVG_TOTAL_MAX_Y - SVG_TOTAL_MIN_Y // 55.24f

// Slicing X intervals in SVG coordinate space for clipping each letter
private val SLICE_BOUNDS_X = floatArrayOf(
    16.52f, 97.5f,   // Letter K
    97.5f, 194.0f,   // Letter A
    194.0f, 282.0f,  // Letter N
    282.0f, 369.0f,  // Letter R
    369.0f, 392.0f   // Letter I
)

// Official Kanri SVG Monogram Emblem Path (kanriicon.svg)
private const val MARK_PATH_DATA = "M914,407L1134,407L1134,933L1024,823L914,933Z M914,1641L914,1115L1024,1225L1134,1115L1134,1641Z M197,617L499,617L894,1024L499,1431L197,1431L592,1024Z M1851,617L1549,617L1154,1024L1549,1431L1851,1431L1456,1024Z"

private const val MARK_MIN_X = 197f
private const val MARK_MAX_X = 1851f
private const val MARK_WIDTH = 1654f // 1851 - 197
private const val MARK_MIN_Y = 407f
private const val MARK_MAX_Y = 1641f
private const val MARK_HEIGHT = 1234f // 1641 - 407
private const val MARK_CENTER_X = 1024f
private const val MARK_CENTER_Y = 1024f

private val EaseDecel = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val EaseSettle = CubicBezierEasing(0.25f, 1f, 0.35f, 1f)

/**
 * High-craft Kanri Intro Logo component inspired by ChapterPlayer scene 0.
 *
 * Choreography:
 * 1. Monogram Emblem lands large in the center with exponential ease (scale 1.85 -> 1.50).
 * 2. Settles and extends towards the left lockup position (scale 1.50 -> 1.00, center -> lockup).
 * 3. Staggered Panchang wordmark letters (K - A - N - R - I) emerge beside it in kinetic sequence.
 * 4. Tagline reveals smoothly underneath.
 * 5. Gentle organic breathing keeps the lockup alive with the Shan Shui living scroll.
 */
@Composable
fun KanriIntroLogo(
    isLight: Boolean,
    modifier: Modifier = Modifier
) {
    val markPath = remember {
        try {
            PathParser.createPathFromPathData(MARK_PATH_DATA).asComposePath()
        } catch (_: Exception) {
            Path()
        }
    }

    val letterPaths = remember {
        listOf(PATH_K, PATH_A, PATH_N, PATH_R, PATH_I).map {
            PathParser.createPathFromPathData(it).asComposePath()
        }
    }

    // 1. Monogram Emblem Animations
    val markAlpha = remember { Animatable(0f) }
    val markScaleInitial = remember { Animatable(1.85f) }
    val settleProgress = remember { Animatable(0f) }

    // 2. KANRI Letter Animations
    val letterAlphas = remember { List(5) { Animatable(0f) } }
    val letterOffsets = remember { List(5) { Animatable(16f) } }
    val letterScales = remember { List(5) { Animatable(0.88f) } }

    // 3. Tagline Animations
    val taglineAlpha = remember { Animatable(0f) }
    val taglineOffsetY = remember { Animatable(10f) }

    LaunchedEffect(Unit) {
        // Step 1: Emblem lands large in center (0 to 650ms)
        launch {
            markAlpha.animateTo(1f, tween(360, easing = LinearEasing))
        }
        launch {
            markScaleInitial.animateTo(1.50f, tween(650, easing = EaseDecel))
        }

        // Step 2: Emblem settles and extends towards the left (from 700ms to 1550ms)
        delay(700L)
        launch {
            settleProgress.animateTo(1f, tween(850, easing = EaseSettle))
        }

        // Step 3: Staggered Panchang Letters emerge beside the settling mark (from 1050ms)
        delay(350L)
        letterPaths.indices.forEach { index ->
            launch {
                delay(index * 65L)
                launch {
                    letterAlphas[index].animateTo(1f, tween(320, easing = LinearEasing))
                }
                launch {
                    letterOffsets[index].animateTo(0f, tween(380, easing = EaseDecel))
                }
                launch {
                    letterScales[index].animateTo(1f, tween(380, easing = EaseDecel))
                }
            }
        }

        // Step 4: Tagline reveal (from 1650ms)
        delay(600L)
        launch {
            taglineAlpha.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
        }
        launch {
            taglineOffsetY.animateTo(0f, tween(480, easing = EaseDecel))
        }
    }

    // Subtle living breathing push-in
    val infiniteTransition = rememberInfiniteTransition(label = "logo_breath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    val inkColor = if (isLight) Color(0xFF1E2229) else Color(0xFFF7F5F0)
    val accentRed = Color(0xFFC43B38) // Cinnabar red from Shan Shui aesthetics

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Combined Emblem Mark + KANRI Wordmark Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
            ) {
                val canvasW = size.width
                val canvasH = size.height

                // Lockup dimensions: Emblem on left + Panchang KANRI wordmark on right
                val desiredMarkH = 42.dp.toPx()
                val desiredMarkW = (MARK_WIDTH / MARK_HEIGHT) * desiredMarkH // ~56dp

                val desiredWordH = 30.dp.toPx()
                val desiredWordW = (SVG_TOTAL_WIDTH / SVG_TOTAL_HEIGHT) * desiredWordH // ~202dp
                val gap = 16.dp.toPx()
                val totalLockupW = desiredMarkW + gap + desiredWordW

                val lockupStartX = (canvasW - totalLockupW) / 2f
                val centerY = canvasH / 2f

                // 1. Draw Emblem Mark (moves from center to left lockup as settle progresses)
                val settle = settleProgress.value
                val currentMarkCenterX = lerp(canvasW / 2f, lockupStartX + desiredMarkW / 2f, settle)
                val currentMarkScale = lerp(markScaleInitial.value, 1.0f, settle) * breathScale
                val currentMarkAlpha = markAlpha.value

                if (currentMarkAlpha > 0.01f) {
                    val markUnitScale = desiredMarkH / MARK_HEIGHT
                    scale(
                        scale = currentMarkScale,
                        pivot = Offset(currentMarkCenterX, centerY)
                    ) {
                        translate(
                            left = currentMarkCenterX - (MARK_CENTER_X * markUnitScale),
                            top = centerY - (MARK_CENTER_Y * markUnitScale)
                        ) {
                            scale(
                                scale = markUnitScale,
                                pivot = Offset.Zero
                            ) {
                                drawPath(
                                    path = markPath,
                                    color = accentRed.copy(alpha = currentMarkAlpha)
                                )
                            }
                        }
                    }
                }

                // 2. Draw KANRI letters with individual staggered rise, scale & alpha
                val wordStartX = lockupStartX + desiredMarkW + gap
                val wordScaleX = desiredWordW / SVG_TOTAL_WIDTH
                val wordScaleY = desiredWordH / SVG_TOTAL_HEIGHT

                letterPaths.forEachIndexed { i, path ->
                    val alpha = letterAlphas[i].value
                    val offY = letterOffsets[i].value.dp.toPx()
                    val lScale = letterScales[i].value

                    if (alpha > 0.01f) {
                        val sliceMinX = SLICE_BOUNDS_X[i * 2]
                        val sliceMaxX = SLICE_BOUNDS_X[i * 2 + 1]

                        val clipLeft = wordStartX + (sliceMinX - SVG_TOTAL_MIN_X) * wordScaleX
                        val clipRight = wordStartX + (sliceMaxX - SVG_TOTAL_MIN_X) * wordScaleX
                        val clipTop = centerY - (desiredWordH / 2f) - 30f
                        val clipBottom = centerY + (desiredWordH / 2f) + 30f

                        clipRect(
                            left = clipLeft - 1f,
                            top = clipTop,
                            right = clipRight + 1f,
                            bottom = clipBottom
                        ) {
                            scale(
                                scale = lScale * breathScale,
                                pivot = Offset((clipLeft + clipRight) / 2f, centerY + offY)
                            ) {
                                translate(
                                    left = wordStartX - SVG_TOTAL_MIN_X * wordScaleX,
                                    top = centerY - (desiredWordH / 2f) - SVG_TOTAL_MIN_Y * wordScaleY + offY
                                ) {
                                    scale(
                                        scaleX = wordScaleX,
                                        scaleY = wordScaleY,
                                        pivot = Offset.Zero
                                    ) {
                                        drawPath(
                                            path = path,
                                            color = inkColor.copy(alpha = alpha)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tagline Reveal
        Column(
            modifier = Modifier
                .graphicsLayer {
                    alpha = taglineAlpha.value
                    translationY = taglineOffsetY.value.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "FINANCIAL SERENITY",
                fontFamily = Panchang,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                letterSpacing = 3.2.sp,
                color = accentRed.copy(alpha = 0.90f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "Know where every rupee went.",
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Normal,
                fontSize = 13.5.sp,
                letterSpacing = 0.4.sp,
                color = inkColor.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )
        }
    }
}
