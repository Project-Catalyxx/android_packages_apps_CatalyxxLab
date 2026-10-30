/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package org.catalyxx.lab

import android.content.Context
import android.provider.Settings
import android.util.AttributeSet
import androidx.annotation.StringRes
import com.android.settingslib.widget.SliderPreference
import kotlin.math.abs

/**
 * Animation speed: one slider for the three scales Developer options sets one by one (window,
 * transition and animator duration), from slower than normal through faster to off. Android
 * applies a change to them at once. Speeds set there that the slider has no step for, or that
 * differ, show as custom, with the slider at the animator scale's nearest step until it moves.
 */
class AnimationSpeedPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : SliderPreference(context, attrs) {

    init {
        isPersistent = false
        min = 0
        max = STEPS.lastIndex
        setSliderIncrement(1)
        setTickVisible(true)
        setTextStart(R.string.animation_speed_slower)
        setTextEnd(R.string.animation_speed_faster)
        setOnPreferenceChangeListener { _, newValue ->
            val step = newValue as Int
            for (key in KEYS) {
                Settings.Global.putFloat(context.contentResolver, key, STEPS[step].scale)
            }
            showStep(step, custom = false)
            true
        }
    }

    override fun onAttached() {
        super.onAttached()
        refresh()
    }

    /** Reads the scales again: Developer options or Accessibility may have changed them. */
    fun refresh() {
        // Unset means Android's normal speed
        val scales = KEYS.map { Settings.Global.getFloat(context.contentResolver, it, 1f) }
        val step = STEPS.indices.minBy { abs(STEPS[it].scale - scales.last()) }
        value = step
        showStep(step, custom = scales.any { abs(it - STEPS[step].scale) > TOLERANCE })
    }

    private fun showStep(step: Int, custom: Boolean) {
        val speed = context.getString(STEPS[step].label)
        setSliderStateDescription(speed)
        summary = if (custom) context.getString(R.string.animation_speed_custom_summary) else speed
    }

    /** A slider step: the scale it sets, 1 being Android's normal speed, and its name. */
    private class Step(val scale: Float, @StringRes val label: Int)

    private companion object {
        // The animator scale last: the slider follows it when they differ
        val KEYS =
            listOf(
                Settings.Global.WINDOW_ANIMATION_SCALE,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                Settings.Global.ANIMATOR_DURATION_SCALE,
            )

        // Slowest first, so the slider runs from slower to faster, then off
        val STEPS =
            listOf(
                Step(2f, R.string.animation_speed_very_slow),
                Step(1.5f, R.string.animation_speed_slow),
                Step(1f, R.string.animation_speed_normal),
                Step(0.75f, R.string.animation_speed_fast),
                Step(0.5f, R.string.animation_speed_very_fast),
                Step(0.25f, R.string.animation_speed_fastest),
                Step(0f, R.string.animation_speed_off),
            )

        const val TOLERANCE = 0.01f
    }
}
