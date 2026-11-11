package com.omkarnub.kanri.ui.common

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.PathMeasure as AndroidPathMeasure
import android.graphics.RectF
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.omkarnub.kanri.R
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.launch

/**
 * Individual contour of a character's vector path for animated segment stroke drawing.
 */
private data class ContourSegment(
    val length: Float,
    val measure: AndroidPathMeasure
)

/**
 * Parsed vector data for each character glyph in the signature.
 */
private data class GlyphVectorData(
    val char: Char,
    val fullComposePath: Path,
    val contours: List<ContourSegment>,
    val totalLength: Float
)

/**
 * Pre-calculated layout geometry containing all glyph paths and the tight bounding box.
 */
private data class SignatureLayout(
    val glyphs: List<GlyphVectorData>,
    val bounds: RectF,
    val totalAdvanceWidth: Float
)

/**
 * SignatureAnimationText
 *
 * Implements a dynamic cursive pen-writing signature animation in Jetpack Compose,
 * translating opentype.js font glyph outline extraction into native Android Skia vector paths.
 *
 * Features:
 * - Automatically triggers the handwriting animation whenever scrolled into view.
 * - Displays "Made by" directly introducing the developer signature.
 * - Dynamic Responsive Scaling: Automatically scales to fit any screen size (from 320dp to tablets).
 * - Optical Kerning Engine: Eliminates abnormal font advance gaps between '@', 'O', 'a', and cursive letters.
 * - Progressive Stroke Handwriting: Animates pen strokes across characters with staggered cubic-bezier timing.
 * - Solid Calligraphy Cross-Fade: Rich ink fills seamlessly as strokes complete.
 * - Single-tap: Replays handwriting animation with haptic feedback.
 * - Double-tap: Launches developer GitHub profile (https://github.com/Omkarnub).
 */
@Composable
fun SignatureAnimationText(
    modifier: Modifier = Modifier,
    text: String = "@Omkarnub",
    fontSize: TextUnit = 38.sp,
    strokeWidth: Dp = 1.8.dp,
    color: Color? = null,
    totalDurationMs: Int = 1800,
    githubUrl: String = "https://github.com/Omkarnub",
    label: String = "Made by",
    isVisible: Boolean? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = rememberKanriHaptics()
    val scope = rememberCoroutineScope()
    val glassTokens = LocalKanriGlass.current

    // Resolve color based on theme
    val resolvedColor = color
        ?: glassTokens?.textPrimary
        ?: MaterialTheme.colorScheme.onSurface

    val fontSizePx = with(density) { fontSize.toPx() }

    // Load typeface from font resources
    val typeface = remember(context) {
        try {
            ResourcesCompat.getFont(context, R.font.lastoria_bold_regular)
        } catch (_: Exception) {
            null
        }
    }

    // Extract vector paths and measure geometry with optical kerning
    val layout = remember(typeface, fontSizePx, text) {
        if (typeface == null) return@remember null

        val paint = Paint().apply {
            this.typeface = typeface
            this.textSize = fontSizePx
            this.isAntiAlias = true
        }

        val widths = FloatArray(text.length)
        paint.getTextWidths(text, widths)

        val glyphList = mutableListOf<GlyphVectorData>()
        val overallBounds = RectF()
        var currentX = 0f

        for (i in text.indices) {
            val charStr = text[i].toString()
            val charAndroidPath = android.graphics.Path()

            // Paint glyph at currentX with baseline at 0f (bounds will normalize later)
            paint.getTextPath(charStr, 0, 1, currentX, 0f, charAndroidPath)

            val charBounds = RectF()
            charAndroidPath.computeBounds(charBounds, true)
            if (!charBounds.isEmpty) {
                overallBounds.union(charBounds)
            }

            // Decompose character path into individual contour curves
            val contours = mutableListOf<ContourSegment>()
            val measure = AndroidPathMeasure(charAndroidPath, false)
            var totalLen = 0f

            do {
                val len = measure.length
                if (len > 0f) {
                    val contourPath = android.graphics.Path()
                    measure.getSegment(0f, len, contourPath, true)
                    val cMeasure = AndroidPathMeasure(contourPath, false)
                    contours.add(ContourSegment(len, cMeasure))
                    totalLen += len
                }
            } while (measure.nextContour())

            glyphList.add(
                GlyphVectorData(
                    char = text[i],
                    fullComposePath = charAndroidPath.asComposePath(),
                    contours = contours,
                    totalLength = totalLen
                )
            )

            // OPTICAL KERNING:
            // 1. '@' advance scaled to 0.76f (tucks naturally beside 'O')
            // 2. 'O' advance scaled to 0.62f (connects 'O' directly to 'm')
            // 3. 'a' advance scaled to 0.46f (completely closes the gap between 'a' and 'r')
            // 4. 'k' advance scaled to 0.95f (smooth cursive transition)
            val rawAdv = widths[i]
            val effectiveAdvance = when (text[i]) {
                '@' -> rawAdv * 0.76f
                'O' -> rawAdv * 0.62f
                'a' -> rawAdv * 0.46f
                'k' -> rawAdv * 0.95f
                else -> rawAdv
            }
            currentX += effectiveAdvance
        }

        // Ensure bounds are non-empty
        if (overallBounds.isEmpty) {
            overallBounds.set(0f, -fontSizePx, currentX.coerceAtLeast(fontSizePx), fontSizePx * 0.2f)
        }

        SignatureLayout(
            glyphs = glyphList,
            bounds = overallBounds,
            totalAdvanceWidth = currentX
        )
    }

    // Coordinate-based viewport detection
    var inViewCoordinates by remember { mutableStateOf(false) }
    val effectiveInView = isVisible ?: inViewCoordinates

    // Animation progress (0f = initial, 1f = fully drawn and filled)
    val animProgress = remember { Animatable(0f) }

    // Automatically start / replay the animation whenever user scrolls down to it
    LaunchedEffect(effectiveInView) {
        if (effectiveInView) {
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = totalDurationMs,
                    easing = LinearEasing
                )
            )
        }
    }

    fun openGithubProfile() {
        haptics.success()
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun replayAnimation() {
        haptics.tick()
        scope.launch {
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = totalDurationMs,
                    easing = LinearEasing
                )
            )
        }
    }

    // Easing curve for natural handwriting feel
    val handwritingEasing = remember { CubicBezierEasing(0.35f, 0f, 0.15f, 1f) }

    val windowHeightPx = LocalWindowInfo.current.containerSize.height
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                inViewCoordinates = bounds.top < windowHeightPx * 0.95f && bounds.bottom > 50
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        replayAnimation()
                    },
                    onDoubleTap = {
                        openGithubProfile()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val availableWidthPx = constraints.maxWidth.toFloat()
        val paddingPx = fontSizePx * 0.15f

        val naturalWidthPx = if (layout != null) layout.bounds.width() + paddingPx * 2f else 300f
        val naturalHeightPx = if (layout != null) layout.bounds.height() + paddingPx * 2f else 80f

        // DYNAMIC RESPONSIVE SCALING:
        // Automatically adapts to any mobile screen size (320dp small phone up to tablets).
        // Calculates a scale factor so the signature occupies an optimal ~70-76% of screen width,
        // preventing overflowing, clipping, or abnormal whitespace across all devices.
        val maxAllowedWidth = (availableWidthPx - with(density) { 40.dp.toPx() }).coerceAtLeast(120f)
        val responsiveScale = if (naturalWidthPx > 0f) {
            (maxAllowedWidth / naturalWidthPx).coerceAtMost(1.0f).coerceAtLeast(0.50f)
        } else 1f

        val displayWidthDp = with(density) { (naturalWidthPx * responsiveScale).toDp() }
        val displayHeightDp = with(density) { (naturalHeightPx * responsiveScale).toDp() }

        // Stroke width compensated for scale so physical stroke remains consistent
        val scaledStrokeWidthPx = with(density) { strokeWidth.toPx() } / responsiveScale

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // "Made by" header
            Text(
                text = label,
                fontFamily = GoogleSansFlex,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.6.sp,
                color = resolvedColor.copy(alpha = 0.50f),
                modifier = Modifier.padding(bottom = 2.dp)
            )

            if (layout != null && layout.glyphs.isNotEmpty()) {
                val glyphCount = layout.glyphs.size
                val progress = animProgress.value

                Canvas(
                    modifier = Modifier.size(width = displayWidthDp, height = displayHeightDp)
                ) {
                    // Apply dynamic scale and center within padding
                    scale(scale = responsiveScale, pivot = Offset.Zero) {
                        val offsetX = -layout.bounds.left + paddingPx
                        val offsetY = -layout.bounds.top + paddingPx

                        translate(left = offsetX, top = offsetY) {
                            layout.glyphs.forEachIndexed { i, glyph ->
                                // Calculate staggered local progress for each character
                                val staggerFraction = 0.55f
                                val charWindow = 0.45f
                                val charStart = (i.toFloat() / glyphCount) * staggerFraction
                                val rawCharProgress = ((progress - charStart) / charWindow).coerceIn(0f, 1f)
                                val charProgress = handwritingEasing.transform(rawCharProgress)

                                if (charProgress > 0f) {
                                    // 1. Draw animated pen stroke contours
                                    glyph.contours.forEach { contour ->
                                        val segmentLen = contour.length * charProgress
                                        if (segmentLen > 0f) {
                                            val segmentAndroidPath = android.graphics.Path()
                                            contour.measure.getSegment(0f, segmentLen, segmentAndroidPath, true)
                                            drawPath(
                                                path = segmentAndroidPath.asComposePath(),
                                                color = resolvedColor,
                                                style = Stroke(
                                                    width = scaledStrokeWidthPx,
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                )
                                            )
                                        }
                                    }

                                    // 2. Cross-fade rich solid calligraphy fill as stroke completes
                                    if (charProgress > 0.45f) {
                                        val fillAlpha = ((charProgress - 0.45f) / 0.55f).coerceIn(0f, 1f)
                                        drawPath(
                                            path = glyph.fullComposePath,
                                            color = resolvedColor.copy(alpha = fillAlpha),
                                            style = Fill
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Graceful fallback if typeface loading fails
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = resolvedColor
                )
            }
        }
    }
}
