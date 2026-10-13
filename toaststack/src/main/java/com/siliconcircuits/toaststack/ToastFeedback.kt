package com.siliconcircuits.toaststack

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Handles haptic feedback and sound effects when toasts appear.
 *
 * Both features are opt in per toast via [ToastData.hapticEnabled] and
 * [ToastData.soundEnabled]. They respect the device's system settings:
 * - Haptic feedback is skipped if the system haptic setting is turned off
 * - Sound is skipped if the device is in silent or Do Not Disturb mode
 *
 * This is an internal utility used by [ToastStackHost] when a toast
 * becomes visible. For future Compose Multiplatform support, this class
 * would become an `expect/actual` declaration.
 */
internal object ToastFeedback {

    /**
     * Plays a short haptic on [view] that fits the toast type. It uses the
     * view haptic API, so the app needs no VIBRATE permission, and Android
     * skips it when the user turned system haptics off.
     *
     * @param view The view the toast is drawn in.
     * @param type The toast type, used to pick the haptic.
     */
    fun haptic(view: View, type: ToastType) {
        val newApi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val constant = when (type) {
            ToastType.Error, ToastType.Warning ->
                if (newApi) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
            ToastType.Success ->
                if (newApi) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
            ToastType.Info, ToastType.Default, ToastType.Loading -> HapticFeedbackConstants.KEYBOARD_TAP
        }
        view.performHapticFeedback(constant)
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
        if (!soundAllowed(context)) return
        try {
            // Use the custom sound URI if provided, otherwise fall back
            // to the system's default notification sound.
            val uri = customSoundUri
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, uri) ?: return
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone.play()
        } catch (e: Exception) {
            // Some devices have no default notification sound, and a bad
            // soundUri fails here. Do not crash; say so once.
            if (!warnedSound) {
                warnedSound = true
                Log.w("ToastStack", "Could not play the toast sound. Logged once.", e)
            }
        }
    }

    @Volatile
    private var warnedSound = false

    /**
     * False when the ringer is silent or on vibrate, or Do Not Disturb holds
     * back some interruptions. A filter the app cannot read counts as allowed.
     */
    internal fun soundAllowed(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
        if (ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE) return false
        val filter = (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
            ?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        return filter == NotificationManager.INTERRUPTION_FILTER_ALL ||
            filter == NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }
}
