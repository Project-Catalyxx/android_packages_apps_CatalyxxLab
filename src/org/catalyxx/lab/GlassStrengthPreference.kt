/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package org.catalyxx.lab

import android.content.Context
import android.provider.Settings
import android.util.AttributeSet
import com.android.internal.util.catalyxx.GlassSettings
import com.android.settingslib.widget.SliderPreference
import java.text.NumberFormat

/**
 * Blur strength: how much glass blurs what's behind it, from a light haze to frosted. The middle
 * keeps each place's own blur; SystemUI follows a change at once.
 */
class GlassStrengthPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : SliderPreference(context, attrs) {

    init {
        isPersistent = false
        min = GlassSettings.MIN_BLUR_STRENGTH
        max = GlassSettings.MAX_BLUR_STRENGTH
        setSliderIncrement(STEP)
        setTextStart(R.string.glass_blur_strength_light)
        setTextEnd(R.string.glass_blur_strength_frosted)
        setOnPreferenceChangeListener { _, newValue ->
            val strength = newValue as Int
            Settings.Global.putInt(context.contentResolver, GlassSettings.BLUR_STRENGTH, strength)
            showStrength(strength)
            true
        }
    }

    override fun onAttached() {
        super.onAttached()
        refresh()
    }

    /** Reads the setting again. */
    fun refresh() {
        val strength =
            Settings.Global.getInt(
                context.contentResolver,
                GlassSettings.BLUR_STRENGTH,
                GlassSettings.DEFAULT_BLUR_STRENGTH,
            )
        // The slider only takes its steps; a value set some other way shows at the nearest
        val step =
            ((strength + STEP / 2) / STEP * STEP).coerceIn(
                GlassSettings.MIN_BLUR_STRENGTH,
                GlassSettings.MAX_BLUR_STRENGTH,
            )
        value = step
        showStrength(strength)
    }

    private fun showStrength(strength: Int) {
        val percent = NumberFormat.getPercentInstance().format(strength / 100.0)
        setSliderStateDescription(percent)
        summary = percent
    }

    private companion object {
        const val STEP = 5
    }
}
