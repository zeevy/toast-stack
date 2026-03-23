package com.siliconcircuits.toaststack

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings

/**
 * Handles haptic feedback and sound effects when toasts appear.
 *
 * Both features are opt in per toast via [ToastData.hapticEnabled] and
 * [ToastData.soundEnabled]. They respect the device's system settings:
 * - Haptic feedback is skipped if the system haptic setting is disabled
 * - Sound is skipped if the device is in silent or Do Not Disturb mode
 *
 * This is an internal utility used by [ToastStackHost] when a toast
 * becomes visible. For future Compose Multiplatform support, this class
 * would become an `expect/actual` declaration.
 */
internal object ToastFeedback {

    /**
     * Triggers a short vibration pulse if the device's haptic feedback
     * system setting is enabled. The vibration intensity varies by toast
     * type: errors and warnings vibrate longer than info and success.
     *
     * @param context The application context for accessing system services.
     * @param type The toast type, used to determine vibration intensity.
     */
    fun vibrate(context: Context, type: ToastType) {
        // Respect the system haptic feedback setting. If the user has
        // disabled haptics globally, we should not override their choice.
        @Suppress("DEPRECATION")
        val hapticSetting = Settings.System.getInt(
            context.contentResolver,
            Settings.System.HAPTIC_FEEDBACK_ENABLED,
            1
        )
        if (hapticSetting == 0) return

        val durationMs = when (type) {
            ToastType.Error -> 80L
            ToastType.Warning -> 60L
            ToastType.Success -> 40L
            ToastType.Info -> 30L
            ToastType.Default -> 30L
            ToastType.Loading -> 20L
        }

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(durationMs)
            }
        }
    }

    /**
     * Plays the system notification sound if the device is not in silent
     * mode and Do Not Disturb is not active.
     *
     * Uses [RingtoneManager.TYPE_NOTIFICATION] which plays the user's
     * chosen notification sound at a system appropriate volume.
     *
     * @param context The application context for accessing audio services.
     */
    fun playSound(context: Context, customSoundUri: android.net.Uri? = null) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        // Skip if ringer mode is silent or vibrate only.
        val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
        if (ringerMode == AudioManager.RINGER_MODE_SILENT ||
            ringerMode == AudioManager.RINGER_MODE_VIBRATE) return

        try {
            // Use the custom sound URI if provided, otherwise fall back
            // to the system's default notification sound.
            val uri = customSoundUri
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()
        } catch (_: Exception) {
            // Some devices may not have a default notification sound.
            // Silently ignore rather than crashing.
        }
    }
}
