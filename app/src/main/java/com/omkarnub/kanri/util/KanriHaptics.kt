package com.omkarnub.kanri.util

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Intelligent Haptic & Vibration Engine for Kanri.
 *
 * 1. Phones with True Haptic Support (e.g. Infinix Note 30, Pixel, Galaxy LRA):
 *    - Strictly uses HAPTICS ONLY via hardware-calibrated predefined effects
 *      (EFFECT_TICK, EFFECT_CLICK, EFFECT_HEAVY_CLICK, EFFECT_DOUBLE_CLICK)
 *      or micro-duration amplitude-attenuated waveforms routed as USAGE_TOUCH.
 *    - Never produces a legacy motor vibration or noisy rattle.
 *
 * 2. Phones without Advanced Haptics (Unsupported / Basic ERM spinning motors):
 *    - Falls back to ultra-soft micro-vibrations (3ms - 8ms) so the vibration
 *      feels like a gentle, quiet touch instead of a loud, jarring buzzer.
 */
class KanriHaptics private constructor(
    private val context: Context,
    private val composeHaptic: HapticFeedback? = null
) {

    /**
     * Subtle tick: for tab switches, segmented controls, quick steppers (+10, +50, +500), chips, month arrows.
     */
    fun tick() {
        perform(
            composeType = HapticFeedbackType.TextHandleMove,
            predefinedEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_TICK else null,
            richDurationMs = 8L,
            richAmplitude = 35,
            softVibrateMs = 3L // Ultra-soft 3ms micro-tap on ERM
        )
    }

    /**
     * Standard click: for card taps, list item clicks, opening modal sheets/dialogs, category picks.
     */
    fun click() {
        perform(
            composeType = HapticFeedbackType.TextHandleMove,
            predefinedEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_CLICK else null,
            richDurationMs = 12L,
            richAmplitude = 65,
            softVibrateMs = 5L // Ultra-soft 5ms tap on ERM
        )
    }

    /**
     * Primary action: for "+ Add", "New Goal", "Save", "Add Record", "Settle All".
     */
    fun primaryAction() {
        perform(
            composeType = HapticFeedbackType.LongPress,
            predefinedEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_HEAVY_CLICK else null,
            richDurationMs = 18L,
            richAmplitude = 110,
            softVibrateMs = 8L // Ultra-soft 8ms impulse on ERM
        )
    }

    /**
     * Success celebration: for successfully adding a transaction, settling debts, depositing to goals.
     */
    fun success() {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) {
                composeHaptic?.performHapticFeedback(HapticFeedbackType.LongPress)
                return
            }

            val isHaptic = isHapticSupported(vibrator)

            if (isHaptic) {
                // HAPTICS ONLY: Crisp high-definition dual impulse
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 8, 40, 12)
                    val amplitudes = intArrayOf(0, 70, 0, 140)
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                    vibrateWithTouchUsage(vibrator, effect)
                    return
                }
            } else {
                // UNSUPPORTED MOTOR: Ultra-soft, whisper-quiet micro pulses (never buzzes)
                try {
                    composeHaptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } catch (_: Exception) {}

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 4, 45, 5)
                    val effect = VibrationEffect.createWaveform(timings, -1)
                    vibrateWithTouchUsage(vibrator, effect)
                    return
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 4, 45, 5), -1)
                }
            }
        } catch (_: Exception) {
            composeHaptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    /**
     * Warning or destructive: for deleting an entry or goal, factory reset, or error warnings.
     */
    fun warning() {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) {
                composeHaptic?.performHapticFeedback(HapticFeedbackType.LongPress)
                return
            }

            val isHaptic = isHapticSupported(vibrator)

            if (isHaptic) {
                // HAPTICS ONLY: Crisp double-click
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                    vibrateWithTouchUsage(vibrator, effect)
                    return
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 12, 35, 12)
                    val amplitudes = intArrayOf(0, 100, 0, 100)
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                    vibrateWithTouchUsage(vibrator, effect)
                    return
                }
            } else {
                // UNSUPPORTED MOTOR: Ultra-soft, gentle micro-pulse
                try {
                    composeHaptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } catch (_: Exception) {}

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 6, 40, 6)
                    val effect = VibrationEffect.createWaveform(timings, -1)
                    vibrateWithTouchUsage(vibrator, effect)
                    return
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 6, 40, 6), -1)
                }
            }
        } catch (_: Exception) {
            composeHaptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    private fun perform(
        composeType: HapticFeedbackType,
        predefinedEffect: Int?,
        richDurationMs: Long,
        richAmplitude: Int,
        softVibrateMs: Long
    ) {
        try {
            val vibrator = getVibrator(context)
            if (vibrator != null && vibrator.hasVibrator()) {
                val isHaptic = isHapticSupported(vibrator)

                if (isHaptic) {
                    // PHONE HAS TRUE HAPTICS (e.g. Infinix Note 30, Pixel, Galaxy):
                    // USE HAPTICS ONLY!
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && predefinedEffect != null) {
                        val effect = VibrationEffect.createPredefined(predefinedEffect)
                        vibrateWithTouchUsage(vibrator, effect)
                        return
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasAmplitudeControl()) {
                        val effect = VibrationEffect.createOneShot(richDurationMs, richAmplitude)
                        vibrateWithTouchUsage(vibrator, effect)
                        return
                    }
                } else {
                    // PHONE DOES NOT HAVE ADVANCED HAPTICS:
                    // Make vibration even softer (3ms - 8ms) so it never sounds or feels like a harsh buzzer
                    try {
                        composeHaptic?.performHapticFeedback(composeType)
                    } catch (_: Exception) {}

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val effect = VibrationEffect.createOneShot(softVibrateMs, VibrationEffect.DEFAULT_AMPLITUDE)
                        vibrateWithTouchUsage(vibrator, effect)
                        return
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(softVibrateMs)
                        return
                    }
                }
            }
        } catch (_: Exception) {
            try {
                composeHaptic?.performHapticFeedback(composeType)
            } catch (_: Exception) {}
        }
    }

    companion object {
        fun create(context: Context, composeHaptic: HapticFeedback? = null): KanriHaptics {
            return KanriHaptics(context.applicationContext, composeHaptic)
        }

        fun tick(context: Context) = create(context).tick()
        fun click(context: Context) = create(context).click()
        fun primaryAction(context: Context) = create(context).primaryAction()
        fun success(context: Context) = create(context).success()
        fun warning(context: Context) = create(context).warning()

        /**
         * Detects whether the device hardware has true advanced haptic actuator support
         * (e.g. Infinix Note 30 Z-axis linear motor, Pixel, Galaxy LRA).
         */
        fun isHapticSupported(vibrator: Vibrator): Boolean {
            if (!vibrator.hasVibrator()) return false

            val hasAmplitude = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.hasAmplitudeControl()
            } else false

            val supportsEffects = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val supported = vibrator.areEffectsSupported(
                        VibrationEffect.EFFECT_CLICK,
                        VibrationEffect.EFFECT_TICK
                    )
                    supported.any { it == Vibrator.VIBRATION_EFFECT_SUPPORT_YES }
                } catch (_: Exception) {
                    false
                }
            } else {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            }

            return hasAmplitude || supportsEffects
        }

        @Suppress("DEPRECATION")
        private fun vibrateWithTouchUsage(vibrator: Vibrator, effect: VibrationEffect) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val attrs = VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_TOUCH)
                    .build()
                vibrator.vibrate(effect, attrs)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttrs = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build()
                vibrator.vibrate(effect, audioAttrs)
            } else {
                vibrator.vibrate(effect)
            }
        }

        private fun getVibrator(context: Context): Vibrator? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }
    }
}

/**
 * Rememberable Composable accessor for [KanriHaptics].
 */
@Composable
fun rememberKanriHaptics(): KanriHaptics {
    val context = LocalContext.current
    val composeHaptic = LocalHapticFeedback.current
    return remember(context, composeHaptic) {
        KanriHaptics.create(context, composeHaptic)
    }
}
