package com.omkarnub.kanri.ui.common.kokonut

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.shadow
import com.omkarnub.kanri.ui.common.kanriGlass
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.ui.theme.isAppInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.profile.UserProfilePreferences
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * 1:1 Implementation of Kokonut UI Avatar Picker (A8. avatar-picker.tsx)
 *
 * Core Principles & Physics:
 * - Pure deterministic procedural face generation from (displayName, variant) seed
 * - Mulberry32 32-bit PRNG & FNV-1a seed hashing
 * - Continuous shape attribute interpolation enabling real-time face morphing
 * - 3D Coin with 128dp diameter, perspective, depth discs, and 176dp 60-tick rotating bezel
 * - Direct horizontal drag gesture with release momentum / velocity projection
 * - Tap to turn to next face (velocity 900)
 * - Dice shuffle button spinning 5..8 half-turns with Kokonut shuffle spring
 * - 5 Option slots with flying swap preview
 * - Living eyes with pointer-tracking offset & periodic blinking
 * - Real-time name input morphing
 */

const val AVATAR_SLOT_COUNT = 8
const val AVATAR_COIN = 128f
const val AVATAR_BEZEL = 176f
const val AVATAR_TICKS = 60
const val AVATAR_DEG_PER_PX = 180f / AVATAR_COIN
const val AVATAR_MAX_FLICK_STEPS = 10
const val AVATAR_TAP_SLOP = 4f
const val AVATAR_MIN_NAME = 2

val AVATAR_DRAG_SPRING = spring<Float>(stiffness = 140f, dampingRatio = 0.93f)
val AVATAR_SHUFFLE_SPRING = spring<Float>(stiffness = 50f, dampingRatio = 0.78f)
val AVATAR_MORPH_SPRING = spring<Float>(stiffness = 240f, dampingRatio = 0.82f)

data class FaceRgb(val r: Int, val g: Int, val b: Int) {
    fun toColor(alpha: Float = 1f): Color = Color(r, g, b, (alpha * 255).roundToInt().coerceIn(0, 255))
}

data class FaceShape(
    val headW: Float,
    val headH: Float,
    val headSquare: Float,
    val chin: Float,
    val hairVolume: Float,
    val hairFringe: Float,
    val hairPart: Float,
    val hairLength: Float,
    val eyeGap: Float,
    val eyeY: Float,
    val eyeRx: Float,
    val eyeRy: Float,
    val browTilt: Float,
    val browLift: Float,
    val browWeight: Float,
    val mouthW: Float,
    val mouthCurve: Float,
    val mouthOpen: Float,
    val nose: Float,
    val glasses: Float,
    val cheeks: Float,
    val shoulders: Float
)

data class Face(
    val shape: FaceShape,
    val bg: FaceRgb,
    val skin: FaceRgb,
    val hair: FaceRgb,
    val shirt: FaceRgb,
    val description: String
)

val AVATAR_BACKGROUNDS = listOf(
    Pair("gray", FaceRgb(228, 228, 231)),
    Pair("sand", FaceRgb(233, 226, 214)),
    Pair("sage", FaceRgb(217, 225, 213)),
    Pair("slate", FaceRgb(218, 225, 233)),
    Pair("clay", FaceRgb(235, 221, 214)),
    Pair("mist", FaceRgb(226, 223, 233))
)

val AVATAR_SKINS = listOf(
    FaceRgb(246, 233, 222),
    FaceRgb(236, 210, 187),
    FaceRgb(214, 176, 142),
    FaceRgb(176, 128, 96),
    FaceRgb(128, 88, 66)
)

val AVATAR_HAIRS = listOf(
    FaceRgb(39, 39, 42),
    FaceRgb(63, 45, 36),
    FaceRgb(113, 84, 62),
    FaceRgb(161, 161, 170),
    FaceRgb(82, 82, 91)
)

val AVATAR_SHIRTS = listOf(
    FaceRgb(39, 39, 42),
    FaceRgb(82, 82, 91),
    FaceRgb(113, 113, 122),
    FaceRgb(244, 244, 245)
)

val AVATAR_INK = Color(0xFF121212)

fun avatarRound2(v: Double): Float = ((v * 100.0).roundToInt() / 100.0).toFloat()
fun avatarRound2(v: Float): Float = ((v * 100f).roundToInt() / 100f)

fun avatarHashString(input: String): Long {
    var h = 2166136261L.toInt()
    for (i in 0 until input.length) {
        h = (h xor input[i].code) * 16777619
    }
    return h.toLong() and 0xFFFFFFFFL
}

fun avatarMulberry32(seed: Long): () -> Double {
    var a = seed.toInt()
    return {
        a = (a + 0x6d2b79f5.toInt())
        var t = (a xor (a ushr 15)) * (1 or a)
        t = t xor (t + (t xor (t ushr 7)) * (61 or t))
        ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
    }
}

fun <T> avatarPick(list: List<T>, r: Double): T {
    val idx = min(list.size - 1, floor(r * list.size).toInt())
    return list[idx]
}

fun avatarDescribe(shape: FaceShape, bgName: String): String {
    val hair = when {
        shape.hairLength > 0.62f -> "long hair"
        shape.hairVolume < 0.22f -> "cropped hair"
        else -> "short hair"
    }
    val mouth = when {
        shape.mouthOpen > 0.5f -> "an open smile"
        shape.mouthCurve > 3.2f -> "a wide smile"
        shape.mouthCurve < 1f -> "a calm expression"
        else -> "a slight smile"
    }
    val face = if (shape.headSquare > 0.55f) "Square face" else "Round face"
    val glasses = if (shape.glasses > 0.5f) ", glasses" else ""
    return "$face, $hair$glasses, $mouth, on $bgName"
}

fun createAvatarFace(seed: String): Face {
    val r = avatarMulberry32(avatarHashString(seed))
    val bg = avatarPick(AVATAR_BACKGROUNDS, r())

    val shape = FaceShape(
        headW = avatarRound2(18.0 + r() * 5.0),
        headH = avatarRound2(22.0 + r() * 4.0),
        headSquare = avatarRound2(r()),
        chin = avatarRound2(0.75 + r() * 0.25),
        hairVolume = avatarRound2(r()),
        hairFringe = avatarRound2(r()),
        hairPart = avatarRound2(r() * 2.0 - 1.0),
        hairLength = avatarRound2(r().pow(1.6)),
        eyeGap = avatarRound2(7.5 + r() * 3.0),
        eyeY = avatarRound2(-2.0 + r() * 4.0),
        eyeRx = avatarRound2(1.7 + r() * 0.9),
        eyeRy = avatarRound2(1.9 + r() * 1.4),
        browTilt = avatarRound2(-2.0 + r() * 4.0),
        browLift = avatarRound2(4.2 + r() * 2.0),
        browWeight = avatarRound2(if (r() < 0.65) 1.3 + r() * 0.8 else 0.0),
        mouthW = avatarRound2(3.5 + r() * 3.0),
        mouthCurve = avatarRound2(r() * 4.5),
        mouthOpen = avatarRound2(if (r() < 0.3) 0.6 + r() * 0.4 else 0.0),
        nose = avatarRound2(r()),
        glasses = avatarRound2(if (r() < 0.25) 1.0 else 0.0),
        cheeks = avatarRound2(r()),
        shoulders = avatarRound2(r())
    )

    return Face(
        shape = shape,
        bg = bg.second,
        skin = avatarPick(AVATAR_SKINS, r()),
        hair = avatarPick(AVATAR_HAIRS, r()),
        shirt = avatarPick(AVATAR_SHIRTS, r()),
        description = avatarDescribe(shape, bg.first)
    )
}

fun avatarSeed(displayName: String, variant: Int): String {
    return "${displayName.trim().lowercase()}:$variant"
}

fun avatarPad3(v: Int): String {
    val norm = ((v % 1000) + 1000) % 1000
    return String.format(Locale.US, "%03d", norm)
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun lerpRgb(a: FaceRgb, b: FaceRgb, t: Float): FaceRgb {
    val ct = t.coerceIn(0f, 1f)
    return FaceRgb(
        r = (a.r + (b.r - a.r) * ct).roundToInt().coerceIn(0, 255),
        g = (a.g + (b.g - a.g) * ct).roundToInt().coerceIn(0, 255),
        b = (a.b + (b.b - a.b) * ct).roundToInt().coerceIn(0, 255)
    )
}

fun lerpFace(a: Face, b: Face, t: Float): Face {
    val shape = FaceShape(
        headW = lerp(a.shape.headW, b.shape.headW, t),
        headH = lerp(a.shape.headH, b.shape.headH, t),
        headSquare = lerp(a.shape.headSquare, b.shape.headSquare, t),
        chin = lerp(a.shape.chin, b.shape.chin, t),
        hairVolume = lerp(a.shape.hairVolume, b.shape.hairVolume, t),
        hairFringe = lerp(a.shape.hairFringe, b.shape.hairFringe, t),
        hairPart = lerp(a.shape.hairPart, b.shape.hairPart, t),
        hairLength = lerp(a.shape.hairLength, b.shape.hairLength, t),
        eyeGap = lerp(a.shape.eyeGap, b.shape.eyeGap, t),
        eyeY = lerp(a.shape.eyeY, b.shape.eyeY, t),
        eyeRx = lerp(a.shape.eyeRx, b.shape.eyeRx, t),
        eyeRy = lerp(a.shape.eyeRy, b.shape.eyeRy, t),
        browTilt = lerp(a.shape.browTilt, b.shape.browTilt, t),
        browLift = lerp(a.shape.browLift, b.shape.browLift, t),
        browWeight = lerp(a.shape.browWeight, b.shape.browWeight, t),
        mouthW = lerp(a.shape.mouthW, b.shape.mouthW, t),
        mouthCurve = lerp(a.shape.mouthCurve, b.shape.mouthCurve, t),
        mouthOpen = lerp(a.shape.mouthOpen, b.shape.mouthOpen, t),
        nose = lerp(a.shape.nose, b.shape.nose, t),
        glasses = lerp(a.shape.glasses, b.shape.glasses, t),
        cheeks = lerp(a.shape.cheeks, b.shape.cheeks, t),
        shoulders = lerp(a.shape.shoulders, b.shape.shoulders, t)
    )
    val ct = t.coerceIn(0f, 1f)
    return Face(
        shape = shape,
        bg = lerpRgb(a.bg, b.bg, ct),
        skin = lerpRgb(a.skin, b.skin, ct),
        hair = lerpRgb(a.hair, b.hair, ct),
        shirt = lerpRgb(a.shirt, b.shirt, ct),
        description = b.description
    )
}

/** Draws the procedural face art inside any Canvas at size */
fun drawFaceArt(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    face: Face,
    lookX: Float = 0f,
    lookY: Float = 0f,
    blinkScaleY: Float = 1f
) {
    val cx = 50f
    val cy = 50f
    val s = face.shape
    val eyeY = cy + s.eyeY
    val mouthY = cy + s.headH * 0.5f
    val glassR = max(s.eyeRx, s.eyeRy) + 3.2f
    val neckTop = cy + s.headH - 6f
    val shoulderTop = cy + s.headH + 3f
    val sw = 30f + s.shoulders * 8f

    // Kokonut viewBox is (12, 14) with width 76, height 76
    val scale = scope.size.width / 76f
    val offsetX = -12f * scale
    val offsetY = -14f * scale

    scope.drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        nc.save()
        nc.translate(offsetX, offsetY)
        nc.scale(scale, scale)

        // 1. Background Rect
        scope.drawRect(
            color = face.bg.toColor(),
            topLeft = Offset(0f, 0f),
            size = Size(100f, 100f)
        )

        // 2. Long hair blob (hair * 0.9)
        val hairBlobK = 0.552f + s.headSquare * 0.6f * 0.28f
        val hairW = s.headW + 1f + s.hairVolume * 2.5f
        val hairTop = s.headH * 0.85f + s.hairVolume * 4f
        val hairBottom = s.headH * 0.15f + s.hairLength * (s.headH + 14f)
        val hairCy = cy - s.headH * 0.15f

        val hairPath = Path().apply {
            moveTo(cx, hairCy - hairTop)
            cubicTo(cx + hairBlobK * hairW, hairCy - hairTop, cx + hairW, hairCy - hairBlobK * hairTop, cx + hairW, hairCy)
            cubicTo(cx + hairW, hairCy + hairBlobK * hairBottom, cx + hairBlobK * hairW, hairCy + hairBottom, cx, hairCy + hairBottom)
            cubicTo(cx - hairBlobK * hairW, hairCy + hairBottom, cx - hairW, hairCy + hairBlobK * hairBottom, cx - hairW, hairCy)
            cubicTo(cx - hairW, hairCy - hairBlobK * hairTop, cx - hairBlobK * hairW, hairCy - hairTop, cx, hairCy - hairTop)
            close()
        }
        scope.drawPath(hairPath, face.hair.toColor(0.9f))

        // 3. Shirt path
        val shirtPath = Path().apply {
            moveTo(cx - sw, 100f)
            cubicTo(cx - sw, shoulderTop + 6f, cx - sw * 0.55f, shoulderTop, cx, shoulderTop)
            cubicTo(cx + sw * 0.55f, shoulderTop, cx + sw, shoulderTop + 6f, cx + sw, 100f)
            close()
        }
        scope.drawPath(shirtPath, face.shirt.toColor())

        // 4. Neck rect (skin * 0.9)
        scope.drawRoundRect(
            color = face.skin.toColor(0.9f),
            topLeft = Offset(cx - 6f, neckTop),
            size = Size(12f, shoulderTop - neckTop + 3f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
        )

        // 5. Ears (skin * 0.95)
        scope.drawOval(
            color = face.skin.toColor(0.95f),
            topLeft = Offset(cx - s.headW - 3f, cy + 2f - 4.5f),
            size = Size(6f, 9f)
        )
        scope.drawOval(
            color = face.skin.toColor(0.95f),
            topLeft = Offset(cx + s.headW - 3f, cy + 2f - 4.5f),
            size = Size(6f, 9f)
        )

        // 6. Head blob (skin)
        val headK = 0.552f + s.headSquare * 0.28f
        val headKb = headK * s.chin
        val headPath = Path().apply {
            moveTo(cx, cy - s.headH)
            cubicTo(cx + headK * s.headW, cy - s.headH, cx + s.headW, cy - headK * s.headH, cx + s.headW, cy)
            cubicTo(cx + s.headW, cy + headKb * s.headH, cx + headKb * s.headW, cy + s.headH, cx, cy + s.headH)
            cubicTo(cx - headKb * s.headW, cy + s.headH, cx - s.headW, cy + headKb * s.headH, cx - s.headW, cy)
            cubicTo(cx - s.headW, cy - headK * s.headH, cx - headK * s.headW, cy - s.headH, cx, cy - s.headH)
            close()
        }
        scope.drawPath(headPath, face.skin.toColor())

        // 7. Front hair path (hair)
        val temple = cy - s.headH * 0.12f
        val crown = cy - s.headH - 1.5f - s.hairVolume * 4f
        val side = s.headW + 1.5f + s.hairVolume * 2.5f
        val fringeY = cy - s.headH * (0.66f - s.hairFringe * 0.36f)
        val partX = cx + s.hairPart * s.headW * 0.55f

        val frontHairPath = Path().apply {
            moveTo(cx - side, temple)
            cubicTo(cx - side, cy - s.headH * headK - s.hairVolume * 3f, cx - headK * side, crown, cx, crown)
            cubicTo(cx + headK * side, crown, cx + side, cy - s.headH * headK - s.hairVolume * 3f, cx + side, temple)
            lineTo(cx + s.headW - 1f, temple)
            quadraticTo(cx + s.headW * 0.75f, fringeY + 1f - s.hairPart * 5f, partX, fringeY - 2.5f)
            quadraticTo(cx - s.headW * 0.75f, fringeY + 1f + s.hairPart * 5f, cx - s.headW + 1f, temple)
            close()
        }
        scope.drawPath(frontHairPath, face.hair.toColor())

        // 8. Cheeks (INK @ 0.045 * cheeks)
        if (s.cheeks > 0f) {
            val cheekColor = AVATAR_INK.copy(alpha = 0.045f * s.cheeks)
            scope.drawCircle(cheekColor, 3.4f, Offset(cx - s.eyeGap - 3f, eyeY + 7f))
            scope.drawCircle(cheekColor, 3.4f, Offset(cx + s.eyeGap + 3f, eyeY + 7f))
        }

        // 9. Brows (stroke max(0, browWeight), round caps, 0.85 alpha)
        if (s.browWeight > 0f) {
            val browColor = AVATAR_INK.copy(alpha = 0.85f)
            scope.drawLine(
                color = browColor,
                start = Offset(cx - s.eyeGap - 3.2f, eyeY - s.browLift + s.browTilt),
                end = Offset(cx - s.eyeGap + 3.2f, eyeY - s.browLift - s.browTilt),
                strokeWidth = s.browWeight,
                cap = StrokeCap.Round
            )
            scope.drawLine(
                color = browColor,
                start = Offset(cx + s.eyeGap - 3.2f, eyeY - s.browLift - s.browTilt),
                end = Offset(cx + s.eyeGap + 3.2f, eyeY - s.browLift + s.browTilt),
                strokeWidth = s.browWeight,
                cap = StrokeCap.Round
            )
        }

        // 10. Eyes (with look offset & blink scaleY)
        val eyeLeftX = cx - s.eyeGap + lookX
        val eyeRightX = cx + s.eyeGap + lookX
        val eyeCurY = eyeY + lookY
        val eyeRyScaled = s.eyeRy * blinkScaleY

        scope.drawOval(
            color = AVATAR_INK,
            topLeft = Offset(eyeLeftX - s.eyeRx, eyeCurY - eyeRyScaled),
            size = Size(s.eyeRx * 2f, eyeRyScaled * 2f)
        )
        scope.drawOval(
            color = AVATAR_INK,
            topLeft = Offset(eyeRightX - s.eyeRx, eyeCurY - eyeRyScaled),
            size = Size(s.eyeRx * 2f, eyeRyScaled * 2f)
        )

        // 11. Nose (INK @ 0.35, 1.2 stroke)
        val nosePath = Path().apply {
            moveTo(cx - 1.5f - s.nose, eyeY + 8f + s.nose * 1.5f)
            quadraticTo(cx, eyeY + 9.5f + s.nose * 1.5f, cx + 1.5f + s.nose, eyeY + 8f + s.nose * 1.5f)
        }
        scope.drawPath(
            path = nosePath,
            color = AVATAR_INK.copy(alpha = 0.35f),
            style = Stroke(width = 1.2f, cap = StrokeCap.Round)
        )

        // 12. Mouth (INK fill + stroke 1.6, round joins)
        val curve = s.mouthCurve * 2f
        val open = s.mouthOpen * 5f
        val mPath = Path().apply {
            moveTo(cx - s.mouthW, mouthY)
            quadraticTo(cx, mouthY + curve, cx + s.mouthW, mouthY)
            quadraticTo(cx, mouthY + curve + open, cx - s.mouthW, mouthY)
            close()
        }
        scope.drawPath(mPath, AVATAR_INK, style = Fill)
        scope.drawPath(mPath, AVATAR_INK, style = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // 13. Glasses (if glasses > 0)
        if (s.glasses > 0f) {
            val gColor = AVATAR_INK.copy(alpha = s.glasses.coerceIn(0f, 1f))
            scope.drawCircle(gColor, glassR, Offset(cx - s.eyeGap, eyeY), style = Stroke(width = 1.2f))
            scope.drawCircle(gColor, glassR, Offset(cx + s.eyeGap, eyeY), style = Stroke(width = 1.2f))
            val bridgePath = Path().apply {
                moveTo(cx - s.eyeGap + glassR, eyeY)
                quadraticTo(cx, eyeY - 2f, cx + s.eyeGap - glassR, eyeY)
            }
            scope.drawPath(bridgePath, gColor, style = Stroke(width = 1.2f))
        }

        nc.restore()
    }
}

/**
 * Morphing Face Component: smoothly blends features from previous face to new face
 * whenever the seed changes instead of snapping abruptly.
 */
@Composable
fun MorphingFace(
    seed: String,
    morph: Boolean = true,
    lookX: Float = 0f,
    lookY: Float = 0f,
    blinkScaleY: Float = 1f,
    modifier: Modifier = Modifier
) {
    var fromFace by remember { mutableStateOf(createAvatarFace(seed)) }
    var targetFace by remember { mutableStateOf(fromFace) }
    val morphProgress = remember { Animatable(1f) }

    LaunchedEffect(seed) {
        val newTarget = createAvatarFace(seed)
        if (!morph) {
            fromFace = newTarget
            targetFace = newTarget
            morphProgress.snapTo(1f)
            return@LaunchedEffect
        }
        if (newTarget.description != targetFace.description || newTarget.bg != targetFace.bg || newTarget.hair != targetFace.hair) {
            fromFace = lerpFace(fromFace, targetFace, morphProgress.value)
            targetFace = newTarget
            morphProgress.snapTo(0f)
            morphProgress.animateTo(
                targetValue = 1f,
                animationSpec = AVATAR_MORPH_SPRING
            )
        }
    }

    val displayedFace = remember(fromFace, targetFace, morphProgress.value) {
        if (morphProgress.value >= 1f) targetFace else lerpFace(fromFace, targetFace, morphProgress.value)
    }

    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "faceLife")
    val breatheY by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(2400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "breatheY"
    )

    Canvas(
        modifier = modifier.graphicsLayer {
            translationY = if (morph) breatheY else 0f
        }
    ) {
        drawFaceArt(
            scope = this,
            face = displayedFace,
            lookX = lookX,
            lookY = lookY,
            blinkScaleY = blinkScaleY
        )
    }
}

@Composable
fun KokonutAvatarPicker(
    initialName: String = "Robin Vale",
    initialVariant: Int = 0,
    title: String = "Pick your avatar",
    description: String = "Made from your name. Spin it or pick one.",
    submitLabel: String = "Continue",
    onComplete: (displayName: String, variant: Int, photoUri: String?) -> Unit,
    forceLight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dark = if (forceLight) false else isAppInDarkTheme()
    val haptics = rememberKanriHaptics()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val context = LocalContext.current

    var displayName by remember { mutableStateOf(initialName) }
    var selectedSlot by remember { mutableIntStateOf(0) }
    val slots = remember {
        mutableStateListOf<Int>().apply {
            for (i in 0 until AVATAR_SLOT_COUNT) {
                add(initialVariant + i * 7)
            }
        }
    }

    val userPrefs = remember { UserProfilePreferences.getInstance(context) }
    var customPhotoPath by remember {
        mutableStateOf<String?>(userPrefs.profilePhotoPath)
    }
    var photoVersion by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var pendingCropBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        val stream = context.contentResolver.openInputStream(uri)
                        val bmp = BitmapFactory.decodeStream(stream)
                        stream?.close()
                        bmp
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    pendingCropBitmap = bitmap
                }
            }
        }
    }

    val angleAnim = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }

    val currentVariant = slots.getOrNull(selectedSlot) ?: initialVariant
    val shownStep = (angleAnim.value / 180f).roundToInt()
    val shownVariant = currentVariant + shownStep
    val seed = remember(displayName, shownVariant) { avatarSeed(displayName, shownVariant) }

    // Living eyes tracking
    val lookX = remember { Animatable(0f) }
    val lookY = remember { Animatable(0f) }
    var blinkScaleY by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(2600 + Random.nextLong(3400))
            blinkScaleY = 0.1f
            delay(120)
            blinkScaleY = 1f
        }
    }

    fun landSpin(steps: Int) {
        isSpinning = false
        val next = currentVariant + steps
        slots[selectedSlot] = next
        scope.launch { angleAnim.snapTo(0f) }
        haptics.click()
    }

    fun spinBy(steps: Int) {
        customPhotoPath = null
        isSpinning = true
        haptics.click()
        scope.launch {
            angleAnim.animateTo(
                targetValue = steps * 180f,
                animationSpec = AVATAR_SHUFFLE_SPRING
            )
            landSpin(steps)
        }
    }

    fun chooseSlot(idx: Int) {
        customPhotoPath = null
        if (isSpinning) {
            scope.launch { angleAnim.snapTo(0f) }
            isSpinning = false
        }
        selectedSlot = idx
        haptics.click()
    }

    val tokens = LocalKanriGlass.current
    val cardModifier = if (tokens != null && !forceLight) {
        modifier.fillMaxWidth().kanriGlass(level = GlassLevel.Blur, shape = RoundedCornerShape(20.dp))
    } else {
        modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.90f))
            .border(1.dp, Color(0xFF141414).copy(alpha = 0.12f), RoundedCornerShape(20.dp))
    }

    Box(modifier = cardModifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title & Description
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.SemiBold,
                    color = if (dark) Zinc100 else Zinc900,
                    letterSpacing = (-0.02).sp
                )
                Text(
                    text = description,
                    fontSize = 13.5.sp,
                    fontFamily = GoogleSansFlex,
                    color = if (dark) Zinc400 else Zinc500
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bezel Stage (176dp) with rotating ticks and index mark
            val bezelRotation = angleAnim.value / 15f
            val tiltAngle = angleAnim.value - 180f * (angleAnim.value / 180f).roundToInt()

            Box(
                modifier = Modifier
                    .size(AVATAR_BEZEL.dp)
                    .pointerInput(currentVariant, selectedSlot) {
                        var startX = 0f
                        var startAngle = 0f
                        var startTime = 0L
                        var lastX = 0f
                        var lastTime = 0L
                        var moved = false
                        val samples = mutableListOf<Pair<Float, Long>>()

                        detectDragGestures(
                            onDragStart = { offset ->
                                customPhotoPath = null
                                isSpinning = true
                                startX = offset.x
                                lastX = offset.x
                                startAngle = angleAnim.value
                                startTime = System.currentTimeMillis()
                                lastTime = startTime
                                moved = false
                                samples.clear()
                                samples.add(startX to startTime)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                lastX = change.position.x
                                lastTime = System.currentTimeMillis()
                                val dx = lastX - startX
                                if (abs(dx) > AVATAR_TAP_SLOP) {
                                    moved = true
                                }
                                scope.launch {
                                    angleAnim.snapTo(startAngle + dx * AVATAR_DEG_PER_PX)
                                }
                                samples.add(lastX to lastTime)
                                if (samples.size > 6) samples.removeAt(0)
                            },
                            onDragEnd = {
                                val current = angleAnim.value
                                val firstSample = samples.firstOrNull() ?: (lastX to lastTime)
                                val dt = max(16L, lastTime - firstSample.second).toFloat() / 1000f
                                val velocity = ((lastX - firstSample.first) / dt) * AVATAR_DEG_PER_PX
                                val startSteps = (startAngle / 180f).roundToInt()
                                val projected = if (moved) {
                                    val proj = ((current + velocity * 0.28f) / 180f).roundToInt()
                                    proj.coerceIn(startSteps - AVATAR_MAX_FLICK_STEPS, startSteps + AVATAR_MAX_FLICK_STEPS)
                                } else {
                                    (current / 180f).roundToInt() + 1
                                }

                                scope.launch {
                                    angleAnim.animateTo(
                                        targetValue = projected * 180f,
                                        animationSpec = AVATAR_DRAG_SPRING
                                    )
                                    landSpin(projected)
                                }
                            },
                            onDragCancel = {
                                val targetStep = (angleAnim.value / 180f).roundToInt()
                                scope.launch {
                                    angleAnim.animateTo(targetStep * 180f, AVATAR_DRAG_SPRING)
                                    landSpin(targetStep)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Rotating Bezel Ticks
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = bezelRotation }
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val rOuter = size.minDimension / 2f - 4.dp.toPx()
                    val rLongInner = rOuter - 9.dp.toPx()
                    val rShortInner = rOuter - 5.dp.toPx()

                    for (i in 0 until AVATAR_TICKS) {
                        val a = (i.toFloat() / AVATAR_TICKS) * 2f * PI.toFloat()
                        val isLong = (i % 5 == 0)
                        val rInner = if (isLong) rLongInner else rShortInner
                        val p0 = Offset(cx + sin(a) * rInner, cy - cos(a) * rInner)
                        val p1 = Offset(cx + sin(a) * rOuter, cy - cos(a) * rOuter)
                        drawLine(
                            color = if (dark) (if (isLong) Zinc500 else Zinc700) else (if (isLong) Zinc500 else Zinc300),
                            start = p0,
                            end = p1,
                            strokeWidth = (if (isLong) 2.dp else 1.2.dp).toPx(),
                            cap = StrokeCap.Round,
                            alpha = if (isLong) 1f else 0.65f
                        )
                    }
                }

                // Fixed Top Center Index Mark (2dp wide, 12dp tall, rounded)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                        .size(width = 2.dp, height = 12.dp)
                        .clip(CircleShape)
                        .background(if (dark) Zinc100 else Zinc900)
                )

                // 3D Coin (128dp) with depth discs and perspective
                Box(
                    modifier = Modifier
                        .size(AVATAR_COIN.dp)
                        .graphicsLayer {
                            rotationY = tiltAngle
                            cameraDistance = 600f
                        }
                        .shadow(elevation = 6.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .border(1.dp, Color.Black.copy(alpha = 0.10f), CircleShape)
                        .clickable {
                            // Tapping turns to next face
                            spinBy(1)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Depth slices
                    for (z in 6 downTo 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationX = (z * 0.75f) * sin(tiltAngle * PI.toFloat() / 180f)
                                }
                                .clip(CircleShape)
                                .background(if (dark) Zinc700 else Zinc300)
                        )
                    }

                    if (customPhotoPath != null) {
                        val bitmap = remember(customPhotoPath, photoVersion) {
                            BitmapFactory.decodeFile(customPhotoPath)
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Custom profile photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                    } else {
                        MorphingFace(
                            seed = seed,
                            morph = !isSpinning,
                            lookX = lookX.value,
                            lookY = lookY.value,
                            blinkScaleY = blinkScaleY,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Black tilt shadow overlay: |tilt| / 90 * 0.35
                    val shadeAlpha = (abs(tiltAngle) / 90f) * 0.35f
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = shadeAlpha))
                    )
                }
            }

            // Subtitle under stage: [display name 12sp medium] [No. pad3(variant) monospace]
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = displayName.trim().ifEmpty { "Anonymous" },
                    fontSize = 13.sp,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Medium,
                    color = if (dark) Zinc100 else Zinc900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "No. ${avatarPad3(shownVariant)}",
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (dark) Zinc500 else Zinc400
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 8 Slots Toolbar + Shuffle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 8 Slots: 40dp circles in horizontal scroll
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    slots.forEachIndexed { i, slotVariant ->
                        val isSelected = (i == selectedSlot && customPhotoPath == null)
                        val slotSeed = remember(displayName, slotVariant) { avatarSeed(displayName, slotVariant) }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) (if (dark) Zinc700 else Zinc300) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .background(if (isSelected) (if (dark) Zinc900 else Zinc50) else Color.Transparent)
                                .clickable { chooseSlot(i) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (dark) Zinc500 else Zinc400)
                                )
                            } else {
                                MorphingFace(
                                    seed = slotSeed,
                                    morph = false,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Shuffle button: 40dp circle, dice icon rotates angle / 4
                val diceRotation = angleAnim.value / 4f
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.dp, if (dark) Zinc800 else Zinc200, CircleShape)
                        .clickable {
                            val steps = 5 + Random.nextInt(4)
                            spinBy(steps)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Shuffle avatar",
                        tint = if (dark) Zinc300 else Zinc700,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(diceRotation)
                    )
                }
            }

            // Custom Photo Secondary Option: 40dp height full-width button
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, if (dark) Zinc800 else Zinc200, RoundedCornerShape(10.dp))
                    .clickable {
                        if (customPhotoPath != null) {
                            customPhotoPath = null
                            userPrefs.removeProfilePhoto()
                            haptics.click()
                        } else {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (customPhotoPath != null) Icons.Default.Delete else Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = if (dark) Zinc400 else Zinc600,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (customPhotoPath != null) "Remove custom photo" else "Use your own photo",
                        fontSize = 13.5.sp,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Medium,
                        color = if (dark) Zinc300 else Zinc700
                    )
                }
            }

            // Display Name field: height 42, rounded 10, border zinc-200/800
            Spacer(modifier = Modifier.height(18.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Display name",
                    fontSize = 13.sp,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.SemiBold,
                    color = if (dark) Zinc100 else Zinc900
                )

                var isFocused by remember { mutableStateOf(false) }
                BasicTextField(
                    value = displayName,
                    onValueChange = { if (it.length <= 32) displayName = it },
                    singleLine = true,
                    cursorBrush = SolidColor(if (dark) Color(0xFFE5E5E5) else Color(0xFF171717)),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = GoogleSansFlex,
                        color = if (dark) Zinc100 else Zinc900
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = 1.dp,
                            color = if (isFocused) (if (dark) Zinc500 else Zinc400) else (if (dark) Zinc800 else Zinc200),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .background(if (dark) Zinc900.copy(alpha = 0.35f) else Zinc100.copy(alpha = 0.5f))
                        .onFocusChanged { isFocused = it.isFocused }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            // Continue CTA Button: height 44, rounded 10, disabled if name < 2 chars
            Spacer(modifier = Modifier.height(16.dp))
            val isNameValid = displayName.trim().length >= AVATAR_MIN_NAME

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isNameValid) (if (dark) Zinc100 else Zinc900)
                        else (if (dark) Zinc100.copy(alpha = 0.35f) else Zinc900.copy(alpha = 0.35f))
                    )
                    .clickable(enabled = isNameValid) {
                        haptics.click()
                        onComplete(displayName.trim(), shownVariant, customPhotoPath)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = submitLabel,
                        fontSize = 14.sp,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.SemiBold,
                        color = if (dark) Zinc900 else Color.White
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (dark) Zinc900 else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    pendingCropBitmap?.let { bmp ->
        AvatarCropDialog(
            sourceBitmap = bmp,
            onDismiss = { pendingCropBitmap = null },
            onCropSaved = { cropped ->
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        userPrefs.saveBitmapInternal(cropped)
                    }
                    if (ok) {
                        customPhotoPath = userPrefs.profilePhotoPath
                        photoVersion = System.currentTimeMillis()
                        haptics.success()
                    }
                    pendingCropBitmap = null
                }
            }
        )
    }
}

/**
 * KanriUserAvatar: Used across HomeScreen and ProfileScreen for displaying the avatar
 */
@Composable
fun KanriUserAvatar(
    name: String,
    variant: Int,
    photoPath: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!photoPath.isNullOrEmpty()) {
            val fileMod = remember(photoPath) {
                try { java.io.File(photoPath).lastModified() } catch (e: Exception) { 0L }
            }
            val bitmap = remember(photoPath, fileMod) {
                try { BitmapFactory.decodeFile(photoPath) } catch (e: Exception) { null }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                return@Box
            }
        }

        val seed = remember(name, variant) { avatarSeed(name, variant) }
        val face = remember(seed) { createAvatarFace(seed) }
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawFaceArt(
                scope = this,
                face = face,
                lookX = 0f,
                lookY = 0f,
                blinkScaleY = 1f
            )
        }
    }
}
