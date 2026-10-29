/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package org.catalyxx.lab

import android.content.Context
import android.media.AudioManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.AttributeSet
import com.android.settingslib.widget.SliderPreference

/**
 * A vibration intensity in Settings.System, under the preference's key, as a slider from
 * off to high. It is the setting the matching slider in Settings > Sound & vibration >
 * Vibration & haptics changes, with the same side effects, so the two always agree. Each
 * step plays a short preview at the new intensity.
 */
// HAPTIC_FEEDBACK_ENABLED and VIBRATE_WHEN_RINGING are deprecated, but Settings keeps them in step
@Suppress("DEPRECATION")
class VibrationIntensityPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : SliderPreference(context, attrs) {

    private val vibrator = context.getSystemService(Vibrator::class.java)
    private val audioManager = context.getSystemService(AudioManager::class.java)

    private val usage: Int
        get() = USAGES[key] ?: VibrationAttributes.USAGE_UNKNOWN

    init {
        isPersistent = false
        min = Vibrator.VIBRATION_INTENSITY_OFF
        max = Vibrator.VIBRATION_INTENSITY_HIGH
        setSliderIncrement(1)
        setTickVisible(true)
        // Write every step while the slider moves, so its preview plays at the new intensity
        setUpdatesContinuously(true)
        setOnPreferenceChangeListener { _, newValue ->
            val intensity = newValue as Int
            write(intensity)
            showIntensity(intensity)
            playPreview()
            true
        }
    }

    /** Reads the setting again, and whether it can be changed now: Settings may have moved it. */
    fun refresh() {
        val resolver = context.contentResolver
        var intensity =
            Settings.System.getInt(resolver, key, vibrator.getDefaultVibrationIntensity(usage))
        // Settings still honors the deprecated switch if the user turned touch feedback off with it
        if (
            key == Settings.System.HAPTIC_FEEDBACK_INTENSITY &&
                Settings.System.getInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 0
        ) {
            intensity = Vibrator.VIBRATION_INTENSITY_OFF
        }
        value = intensity
        isEnabled =
            Settings.System.getInt(resolver, Settings.System.VIBRATE_ON, 1) == 1 && !silenced()
        showIntensity(intensity)
    }

    private fun write(intensity: Int) {
        val resolver = context.contentResolver
        val off = intensity == Vibrator.VIBRATION_INTENSITY_OFF
        Settings.System.putInt(resolver, key, intensity)
        // The deprecated switches still have to reflect the intensity, as Settings keeps them
        when (key) {
            Settings.System.HAPTIC_FEEDBACK_INTENSITY -> {
                Settings.System.putInt(
                    resolver,
                    Settings.System.HAPTIC_FEEDBACK_ENABLED,
                    if (off) 0 else 1,
                )
                // Hardware feedback follows touch feedback, but turning that off leaves it on
                Settings.System.putInt(
                    resolver,
                    Settings.System.HARDWARE_HAPTIC_FEEDBACK_INTENSITY,
                    if (off) vibrator.getDefaultVibrationIntensity(usage) else intensity,
                )
            }
            Settings.System.RING_VIBRATION_INTENSITY ->
                Settings.System.putInt(
                    resolver,
                    Settings.System.VIBRATE_WHEN_RINGING,
                    if (off) 0 else 1,
                )
        }
    }

    private fun showIntensity(intensity: Int) {
        val level = context.getString(LEVELS.getOrElse(intensity) { LEVELS.last() })
        setSliderStateDescription(level)
        summary = if (silenced()) context.getString(R.string.vibration_silent_summary) else level
    }

    // Ring and notification vibrations are off while the ringer is silent; Settings
    // disables their sliders then, too.
    private fun silenced() =
        key in SILENCED_BY_RINGER &&
            audioManager.ringerModeInternal == AudioManager.RINGER_MODE_SILENT

    private fun playPreview() {
        val attributes =
            VibrationAttributes.Builder()
                .setUsage(usage)
                .setFlags(
                    // The new intensity was written just now: don't play the cached one.
                    // Bypass Do Not Disturb, as Settings' previews do.
                    VibrationAttributes.FLAG_INVALIDATE_SETTINGS_CACHE or
                        VibrationAttributes.FLAG_BYPASS_INTERRUPTION_POLICY
                )
                .build()
        vibrator.vibrate(PREVIEW, attributes)
    }

    private companion object {
        val PREVIEW: VibrationEffect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)

        val USAGES =
            mapOf(
                Settings.System.RING_VIBRATION_INTENSITY to VibrationAttributes.USAGE_RINGTONE,
                Settings.System.NOTIFICATION_VIBRATION_INTENSITY to
                    VibrationAttributes.USAGE_NOTIFICATION,
                Settings.System.ALARM_VIBRATION_INTENSITY to VibrationAttributes.USAGE_ALARM,
                Settings.System.HAPTIC_FEEDBACK_INTENSITY to VibrationAttributes.USAGE_TOUCH,
                Settings.System.MEDIA_VIBRATION_INTENSITY to VibrationAttributes.USAGE_MEDIA,
            )

        val SILENCED_BY_RINGER =
            setOf(
                Settings.System.RING_VIBRATION_INTENSITY,
                Settings.System.NOTIFICATION_VIBRATION_INTENSITY,
            )

        // Indexed by Vibrator.VIBRATION_INTENSITY_*
        val LEVELS =
            listOf(
                R.string.vibration_intensity_off,
                R.string.vibration_intensity_low,
                R.string.vibration_intensity_medium,
                R.string.vibration_intensity_high,
            )
    }
}
