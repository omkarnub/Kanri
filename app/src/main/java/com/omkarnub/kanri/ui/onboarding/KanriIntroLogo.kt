package com.omkarnub.kanri.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.core.graphics.PathParser
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.ui.theme.Panchang
import kotlin.math.pow

// ChapterPlayer exact math helpers (Louis Studios)
private fun clamp01(x: Float): Float = x.coerceIn(0f, 1f)
private fun seg(t: Float, a: Float, b: Float): Float = clamp01((t - a) / (b - a))
private fun eExpo(x: Float): Float = if (x >= 1f) 1f else 1f - 2f.pow(-10f * clamp01(x))
private fun eInOut(x: Float): Float {
    val c = clamp01(x)
    return if (c < 0.5f) 4f * c * c * c else 1f - (-2f * c + 2f).pow(3) / 2f
}

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

/**
 * 100% Exact Translation of Louis Studios ChapterPlayer Scene 0 Intro Animation.
 *
 * Sequence:
 * 1. lt = 0.0s..0.65s: Kanri emblem lands in the center at 1.55x with eExpo deceleration.
 * 2. lt = 0.85s..1.70s: Emblem settles from center to left lockup with eInOut, scaling from 1.55x down to 1.0x.
 * 3. lt = 1.30s..2.10s: Panchang KANRI letters emerge beside it (stagger = 35ms, dur = 700ms, eExpo rise & sharpen).
 * 4. lt = 2.15s..2.95s: Subtitle reveals behind mask.
 * 5. Continuous slow push-in (push = 1 + 0.03 * eInOut(seg(lt, 0, 5))) keeps the frame alive.
 * 6. On Exit: dy = -eInOut(out) * 14px, alpha = 1 - eInOut(out) (duration 320ms).
 */
@Composable
fun KanriIntroLogo(
    isLight: Boolean,
    startAnimation: Boolean,
    isExiting: Boolean,
    onIntroReadyForButton: () -> Unit = {},
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

    val ltAnim = remember { Animatable(0f) }
    val exitAnim = remember { Animatable(0f) }

    // Intro playback driven by exact ChapterPlayer timeline (5.0 seconds total)
    LaunchedEffect(startAnimation) {
        if (startAnimation) {
            ltAnim.animateTo(
                targetValue = 5.0f,
                animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
            )
        }
    }

    LaunchedEffect(ltAnim.value) {
        if (ltAnim.value >= 2.95f) {
            onIntroReadyForButton()
        }
    }

    LaunchedEffect(isExiting) {
        if (isExiting) {
            exitAnim.animateTo(1f, tween(320, easing = LinearEasing))
        } else {
            exitAnim.snapTo(0f)
        }
    }

    val lt = ltAnim.value
    val exitP = exitAnim.value

    val inkColor = Color(0xFF000000) // Pure black for both emblem mark and Panchang text logo
    val cinnabar = Color(0xFFC43B38) // Cinnabar Red from living scroll aesthetics

    // Exit transforms (ChapterPlayer formula: alpha = 1 - eInOut(out), dy = -eInOut(out) * 14)
    val out = seg(exitP, 0f, 1f)
    val exitAlpha = 1f - eInOut(out)
    val exitDyPx = -eInOut(out) * 14f

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Combined Emblem Mark + Panchang KANRI Wordmark Canvas
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
                if (lt <= 0.001f || exitAlpha <= 0.001f) return@Canvas

                val canvasW = size.width
                val canvasH = size.height
                val cy = canvasH * 0.47f

                // ChapterPlayer slow living push-in
                val push = 1f + 0.03f * eInOut(seg(lt, 0f, 5f))

                // Lockup dimensions: Emblem + Panchang Wordmark
                val desiredMarkH = 44.dp.toPx()
                val desiredMarkW = (MARK_WIDTH / MARK_HEIGHT) * desiredMarkH // ~59dp

                val desiredWordH = 28.dp.toPx()
                val desiredWordW = (SVG_TOTAL_WIDTH / SVG_TOTAL_HEIGHT) * desiredWordH // ~189dp
                val gap = 16.dp.toPx()
                val totalLockupW = desiredMarkW + gap + desiredWordW

                // ChapterPlayer Mark Landing & Settle (lt: 0.0s to 1.70s)
                val mp = eExpo(seg(lt, 0f, 0.60f))
                val settle = eInOut(seg(lt, 0.85f, 1.70f))
                val sc = (0.9f + 0.1f * mp) * (1.55f - 0.55f * settle) * push
                val mx = canvasW / 2f + (-totalLockupW / 2f + desiredMarkW / 2f) * settle
                val markAlpha = clamp01(mp * 1.6f) * exitAlpha

                // 1. Draw Emblem Mark
                if (markAlpha > 0.01f) {
                    val markUnitScale = desiredMarkH / MARK_HEIGHT
                    scale(
                        scale = sc,
                        pivot = Offset(mx, (cy - 6f) + exitDyPx * density)
                    ) {
                        translate(
                            left = mx - (MARK_CENTER_X * markUnitScale),
                            top = (cy - 6f) + exitDyPx * density - (MARK_CENTER_Y * markUnitScale)
                        ) {
                            scale(scale = markUnitScale, pivot = Offset.Zero) {
                                drawPath(
                                    path = markPath,
                                    color = cinnabar.copy(alpha = markAlpha)
                                )
                            }
                        }
                    }
                }

                // 2. Kinetic Characters ("KANRI") beside settling emblem
                val wordStartX = canvasW / 2f - totalLockupW / 2f + desiredMarkW + gap
                val wordScaleX = desiredWordW / SVG_TOTAL_WIDTH
                val wordScaleY = desiredWordH / SVG_TOTAL_HEIGHT
                val letterTime = lt - 1.30f

                letterPaths.forEachIndexed { i, path ->
                    val p = seg(letterTime, i * 0.035f, i * 0.035f + 0.70f)
                    if (p > 0f) {
                        val e = eExpo(p)
                        val lAlpha = clamp01(p * 1.8f) * exitAlpha
                        val dy = (1f - e) * desiredWordH * 0.55f + exitDyPx * density

                        val sliceMinX = SLICE_BOUNDS_X[i * 2]
                        val sliceMaxX = SLICE_BOUNDS_X[i * 2 + 1]

                        val clipLeft = wordStartX + (sliceMinX - SVG_TOTAL_MIN_X) * wordScaleX
                        val clipRight = wordStartX + (sliceMaxX - SVG_TOTAL_MIN_X) * wordScaleX
                        val clipTop = cy - (desiredWordH / 2f) - 30f + exitDyPx * density
                        val clipBottom = cy + (desiredWordH / 2f) + 30f + exitDyPx * density

                        clipRect(
                            left = clipLeft - 1f,
                            top = clipTop,
                            right = clipRight + 1f,
                            bottom = clipBottom
                        ) {
                            scale(
                                scale = push,
                                pivot = Offset(clipLeft, cy + dy)
                            ) {
                                translate(
                                    left = wordStartX - SVG_TOTAL_MIN_X * wordScaleX,
                                    top = cy - (desiredWordH / 2f) - SVG_TOTAL_MIN_Y * wordScaleY + dy
                                ) {
                                    scale(
                                        scaleX = wordScaleX,
                                        scaleY = wordScaleY,
                                        pivot = Offset.Zero
                                    ) {
                                        drawPath(
                                            path = path,
                                            color = inkColor.copy(alpha = lAlpha)
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

        // Subtitle line reveals behind mask (revealLine formula: p = seg(lt, 2.15, 2.95))
        val pSub = seg(lt, 2.15f, 2.95f)
        val subAlpha = clamp01(pSub * 2.5f) * exitAlpha
        val subDy = (1f - eExpo(pSub)) * 16f + exitDyPx

        if (subAlpha > 0.01f) {
            Column(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = subAlpha
                        translationY = subDy * density
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FINANCIAL SERENITY",
                    fontFamily = Panchang,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    letterSpacing = 3.2.sp,
                    color = cinnabar.copy(alpha = 0.90f),
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
}
