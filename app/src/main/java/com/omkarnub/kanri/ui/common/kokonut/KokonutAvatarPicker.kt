package com.omkarnub.kanri.ui.common.kokonut

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.data.profile.UserProfilePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.floor
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * 1:1 Port of Kokonut UI Avatar Picker (A8. avatar-picker.tsx)
 *
 * Exact constants:
 * SLOT_COUNT = 5, COIN = 128, BEZEL = 176, TICKS = 60
 * DEG_PER_PX = 180 / 128
 * MAX_FLICK_STEPS = 10, TAP_SLOP = 4
 * SPRINGS:
 * - DRAG_SPRING: stiffness 140, damping 22 -> dampingRatio = 22 / (2 * sqrt(140)) = 0.9296f
 * - SHUFFLE_SPRING: stiffness 50, damping 11 -> dampingRatio = 11 / (2 * sqrt(50)) = 0.7778f
 * - MORPH: bounce 0.22, visualDuration 0.42
 * - FLY: bounce 0.18, visualDuration 0.45
 * - EYES: stiffness 180, damping 18 -> dampingRatio = 18 / (2 * sqrt(180)) = 0.6708f
 */

const val AVATAR_COIN_SIZE = 128f
const val AVATAR_BEZEL_SIZE = 176f
const val AVATAR_TICKS = 60
const val AVATAR_DEG_PER_PX = 180f / 128f
const val AVATAR_MAX_FLICK_STEPS = 10
const val AVATAR_TAP_SLOP = 4f
const val AVATAR_SLOT_COUNT = 5

val AVATAR_DRAG_SPRING = spring<Float>(stiffness = 140f, dampingRatio = 0.93f)
val AVATAR_SHUFFLE_SPRING = spring<Float>(stiffness = 50f, dampingRatio = 0.778f)
val AVATAR_MORPH_SPRING = spring<Float>(stiffness = 190f, dampingRatio = 0.78f)

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

val AVATAR_INK = Color(0xFF18181B)

fun avatarRound2(v: Double): Float = ((v * 100.0).roundToInt() / 100.0).toFloat()
fun avatarRound2(v: Float): Float = ((v * 100f).roundToInt() / 100f)

fun avatarRound2Str(v: Double): String {
    val r = Math.round(v * 100.0) / 100.0
    return if (r == r.toLong().toDouble()) {
        r.toLong().toString()
    } else {
        val s = String.format(Locale.US, "%.2f", r)
        if (s.endsWith("0")) s.substring(0, s.length - 1) else s
    }
}

fun avatarRound2Str(v: Float): String = avatarRound2Str(v.toDouble())

fun avatarBlobPath(
    cx: Double,
    cy: Double,
    w: Double,
    top: Double,
    bottom: Double,
    square: Double,
    chin: Double = 1.0
): String {
    val k = 0.552 + square * 0.28
    val kb = k * chin
    val n = { v: Double -> avatarRound2Str(v) }
    return listOf(
        "M${n(cx)} ${n(cy - top)}",
        "C${n(cx + k * w)} ${n(cy - top)} ${n(cx + w)} ${n(cy - k * top)} ${n(cx + w)} ${n(cy)}",
        "C${n(cx + w)} ${n(cy + kb * bottom)} ${n(cx + kb * w)} ${n(cy + bottom)} ${n(cx)} ${n(cy + bottom)}",
        "C${n(cx - kb * w)} ${n(cy + bottom)} ${n(cx - w)} ${n(cy + kb * bottom)} ${n(cx - w)} ${n(cy)}",
        "C${n(cx - w)} ${n(cy - k * top)} ${n(cx - k * w)} ${n(cy - top)} ${n(cx)} ${n(cy - top)}Z"
    ).joinToString("")
}

fun avatarBlobPath(
    cx: Float,
    cy: Float,
    w: Float,
    top: Float,
    bottom: Float,
    square: Float,
    chin: Float = 1f
): String = avatarBlobPath(cx.toDouble(), cy.toDouble(), w.toDouble(), top.toDouble(), bottom.toDouble(), square.toDouble(), chin.toDouble())

fun Float.toExactDouble(): Double = Math.round(this * 100.0) / 100.0

fun avatarHairFrontPath(s: FaceShape, cx: Float, cy: Float): String {
    val w = s.headW.toExactDouble()
    val h = s.headH.toExactDouble()
    val v = s.hairVolume.toExactDouble()
    val cxD = cx.toDouble()
    val cyD = cy.toDouble()
    val temple = cyD - h * 0.12
    val crown = cyD - h - 1.5 - v * 4.0
    val side = w + 1.5 + v * 2.5
    val k = 0.552 + s.headSquare.toExactDouble() * 0.28
    val fringeY = cyD - h * (0.66 - s.hairFringe.toExactDouble() * 0.36)
    val partX = cxD + s.hairPart.toExactDouble() * w * 0.55
    val n = { x: Double -> avatarRound2Str(x) }
    return listOf(
        "M${n(cxD - side)} ${n(temple)}",
        "C${n(cxD - side)} ${n(cyD - h * k - v * 3.0)} ${n(cxD - k * side)} ${n(crown)} ${n(cxD)} ${n(crown)}",
        "C${n(cxD + k * side)} ${n(crown)} ${n(cxD + side)} ${n(cyD - h * k - v * 3.0)} ${n(cxD + side)} ${n(temple)}",
        "L${n(cxD + w - 1.0)} ${n(temple)}",
        "Q${n(cxD + w * 0.75)} ${n(fringeY + 1.0 - s.hairPart.toExactDouble() * 5.0)} ${n(partX)} ${n(fringeY - 2.5)}",
        "Q${n(cxD - w * 0.75)} ${n(fringeY + 1.0 + s.hairPart.toExactDouble() * 5.0)} ${n(cxD - w + 1.0)} ${n(temple)}Z"
    ).joinToString("")
}

fun avatarMouthPath(s: FaceShape, cx: Double, y: Double): String {
    val curve = s.mouthCurve.toExactDouble() * 2.0
    val open = s.mouthOpen.toExactDouble() * 5.0
    val mouthWD = s.mouthW.toExactDouble()
    val n = { x: Double -> avatarRound2Str(x) }
    return listOf(
        "M${n(cx - mouthWD)} ${n(y)}",
        "Q${n(cx)} ${n(y + curve)} ${n(cx + mouthWD)} ${n(y)}",
        "Q${n(cx)} ${n(y + curve + open)} ${n(cx - mouthWD)} ${n(y)}Z"
    ).joinToString("")
}

fun avatarMouthPath(s: FaceShape, cx: Float, y: Float): String =
    avatarMouthPath(s, cx.toDouble(), y.toDouble())

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

    val headW = 18.0 + r() * 5.0
    val headH = 22.0 + r() * 4.0
    val headSquare = r()
    val chin = 0.75 + r() * 0.25
    val hairVolume = r()
    val hairFringe = r()
    val hairPart = r() * 2.0 - 1.0
    val hairLength = r().pow(1.6)
    val eyeGap = 7.5 + r() * 3.0
    val eyeY = -2.0 + r() * 4.0
    val eyeRx = 1.7 + r() * 0.9
    val eyeRy = 1.9 + r() * 1.4
    val browTilt = -2.0 + r() * 4.0
    val browLift = 4.2 + r() * 2.0
    val browWeight = if (r() < 0.65) 1.3 + r() * 0.8 else 0.0
    val mouthW = 3.5 + r() * 3.0
    val mouthCurve = r() * 4.5
    val mouthOpen = if (r() < 0.3) 0.6 + r() * 0.4 else 0.0
    val nose = r()
    val glasses = if (r() < 0.25) 1.0 else 0.0
    val cheeks = r()
    val shoulders = r()

    val shape = FaceShape(
        headW = avatarRound2(headW),
        headH = avatarRound2(headH),
        headSquare = avatarRound2(headSquare),
        chin = avatarRound2(chin),
        hairVolume = avatarRound2(hairVolume),
        hairFringe = avatarRound2(hairFringe),
        hairPart = avatarRound2(hairPart),
        hairLength = avatarRound2(hairLength),
        eyeGap = avatarRound2(eyeGap),
        eyeY = avatarRound2(eyeY),
        eyeRx = avatarRound2(eyeRx),
        eyeRy = avatarRound2(eyeRy),
        browTilt = avatarRound2(browTilt),
        browLift = avatarRound2(browLift),
        browWeight = avatarRound2(browWeight),
        mouthW = avatarRound2(mouthW),
        mouthCurve = avatarRound2(mouthCurve),
        mouthOpen = avatarRound2(mouthOpen),
        nose = avatarRound2(nose),
        glasses = avatarRound2(glasses),
        cheeks = avatarRound2(cheeks),
        shoulders = avatarRound2(shoulders)
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

fun avatarPad3(v: Int): String {
    val norm = ((v % 1000) + 1000) % 1000
    return String.format(Locale.US, "%03d", norm)
}

fun avatarSeed(displayName: String, variant: Int): String {
    return "${displayName.trim().lowercase()}:$variant"
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

    // Coordinate mapping: Kokonut viewBox is (12, 14) with width 76, height 76
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

@Composable
fun KokonutAvatarPicker(
    initialName: String = "Robin Vale",
    initialVariant: Int = 0,
    title: String = "Pick your avatar",
    description: String = "Made from your name. Spin it or pick one.",
    submitLabel: String = "Continue",
    onComplete: (displayName: String, variant: Int, photoUri: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
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

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    userPrefs.savePhotoFromUri(uri)
                }
                if (ok) {
                    customPhotoPath = userPrefs.profilePhotoPath
                    haptics.success()
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
    val currentFace = remember(seed) { createAvatarFace(seed) }

    // Living eyes tracking
    var lookX by remember { mutableFloatStateOf(0f) }
    var lookY by remember { mutableFloatStateOf(0f) }
    var blinkScaleY by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(2600 + Random.nextLong(3400))
            blinkScaleY = 0.1f
            delay(120)
            blinkScaleY = 1f
        }
    }

    // Bezel tick lines
    val tickLines = remember {
        val c = AVATAR_BEZEL_SIZE / 2f
        (0 until AVATAR_TICKS).map { i ->
            val a = (i.toFloat() / AVATAR_TICKS) * 2f * PI.toFloat()
            val long = (i % 5 == 0)
            val r1 = AVATAR_BEZEL_SIZE / 2f - 3f
            val r0 = r1 - (if (long) 7f else 4f)
            Triple(
                Offset(c + sin(a) * r0, c - cos(a) * r0),
                Offset(c + sin(a) * r1, c - cos(a) * r1),
                long
            )
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

    KanriGlassCard(
        modifier = modifier.fillMaxWidth(),
        level = GlassLevel.Blur,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title & Description
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (dark) Zinc100 else Zinc900,
                    letterSpacing = (-0.02).sp
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = if (dark) Zinc400 else Zinc500
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bezel Stage (176dp) with rotating ticks and index mark
            val bezelRotation = angleAnim.value / 15f
            val tiltAngle = angleAnim.value - 180f * (angleAnim.value / 180f).roundToInt()

            Box(
                modifier = Modifier
                    .size(176.dp)
                    .pointerInput(Unit) {
                        var startX = 0f
                        var startAngle = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                customPhotoPath = null
                                isSpinning = true
                                startX = offset.x
                                startAngle = angleAnim.value
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val dx = change.position.x - startX
                                scope.launch { angleAnim.snapTo(startAngle + dx * AVATAR_DEG_PER_PX) }
                            },
                            onDragEnd = {
                                val targetStep = (angleAnim.value / 180f).roundToInt()
                                scope.launch {
                                    angleAnim.animateTo(targetStep * 180f, AVATAR_DRAG_SPRING)
                                    landSpin(targetStep)
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
                    tickLines.forEach { (p0, p1, long) ->
                        drawLine(
                            color = if (dark) Zinc700 else Zinc300,
                            start = p0,
                            end = p1,
                            strokeWidth = (if (long) 1.5f else 1.0f) * density.density,
                            cap = StrokeCap.Round,
                            alpha = if (long) 1f else 0.6f
                        )
                    }
                }

                // Fixed Top Center Index Mark (2dp wide, 12dp tall, rounded)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(width = 2.dp, height = 12.dp)
                        .clip(CircleShape)
                        .background(if (dark) Zinc100 else Zinc900)
                )

                // 3D Coin (128dp) with depth discs and perspective
                Box(
                    modifier = Modifier
                        .size(128.dp)
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
                    if (customPhotoPath != null) {
                        val bitmap = remember(customPhotoPath) {
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
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawFaceArt(
                                scope = this,
                                face = currentFace,
                                lookX = lookX,
                                lookY = lookY,
                                blinkScaleY = blinkScaleY
                            )
                        }
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
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = displayName.trim().ifEmpty { "Anonymous" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (dark) Zinc100 else Zinc900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
                Text(
                    text = "No. ${avatarPad3(shownVariant)}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (dark) Zinc500 else Zinc400
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5 Slots Toolbar + Shuffle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 5 Slots: 40dp circles
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    slots.forEachIndexed { i, slotVariant ->
                        val isSelected = (i == selectedSlot && customPhotoPath == null)
                        val slotSeed = remember(displayName, slotVariant) { avatarSeed(displayName, slotVariant) }
                        val slotFace = remember(slotSeed) { createAvatarFace(slotSeed) }

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
                                Canvas(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
                                    drawFaceArt(scope = this, face = slotFace)
                                }
                            }
                        }
                    }
                }

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
                        tint = if (dark) Zinc400 else Zinc600,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(diceRotation)
                    )
                }
            }

            // Custom Photo Secondary Button: full-width directly under toolbar (mt 12)
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, if (dark) Zinc800 else Zinc200, RoundedCornerShape(8.dp))
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
                        fontSize = 14.sp,
                        color = if (dark) Zinc400 else Zinc600
                    )
                }
            }

            // Display Name field: mt 20, height 40, rounded 8, 1dp border zinc-200/800
            Spacer(modifier = Modifier.height(20.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Display name",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (dark) Zinc100 else Zinc900
                )

                var isFocused by remember { mutableStateOf(false) }
                BasicTextField(
                    value = displayName,
                    onValueChange = { if (it.length <= 32) displayName = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = if (dark) Zinc100 else Zinc900
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = if (isFocused) (if (dark) Zinc600 else Zinc400) else (if (dark) Zinc800 else Zinc200),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(Color.Transparent)
                        .onFocusChanged { isFocused = it.isFocused }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            // Submit Button: mt 16, height 40, rounded 8, disabled if name < 2 chars
            Spacer(modifier = Modifier.height(16.dp))
            val isNameValid = displayName.trim().length >= 2

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isNameValid) (if (dark) Zinc100 else Zinc900)
                        else (if (dark) Zinc100.copy(alpha = 0.4f) else Zinc900.copy(alpha = 0.4f))
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
                        fontWeight = FontWeight.Medium,
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
}


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
            val bitmap = remember(photoPath) {
                BitmapFactory.decodeFile(photoPath)
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
