/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package org.catalyxx.lab

import android.content.Context
import android.provider.Settings
import android.util.AttributeSet
import androidx.annotation.StringRes
import com.android.internal.util.catalyxx.GlassSettings
import com.android.settingslib.widget.SliderPreference
import java.text.NumberFormat

/**
 * A slider on the glass page: a Settings.Global int under the preference's key, from
 * [minValue] to [maxValue] in steps of 5, shown as a percentage. SystemUI follows a change at
 * once.
 */
abstract class GlassSliderPreference(
    context: Context,
    attrs: AttributeSet?,
    minValue: Int,
    maxValue: Int,
    private val defaultSetting: Int,
    @StringRes textStart: Int,
    @StringRes textEnd: Int,
) : SliderPreference(context, attrs) {

    init {
        isPersistent = false
        min = minValue
        max = maxValue
        setSliderIncrement(STEP)
        setTextStart(textStart)
        setTextEnd(textEnd)
        setOnPreferenceChangeListener { _, newValue ->
            val setting = newValue as Int
            Settings.Global.putInt(context.contentResolver, key, setting)
            showSetting(setting)
            true
        }
    }

    override fun onAttached() {
        super.onAttached()
        refresh()
    }

    /** Reads the setting again. */
    open fun refresh() {
        val setting = Settings.Global.getInt(context.contentResolver, key, defaultSetting)
        // The slider only takes its steps; a value set some other way shows at the nearest
        value = ((setting + STEP / 2) / STEP * STEP).coerceIn(min, max)
        showSetting(setting)
    }

    protected open fun showSetting(setting: Int) {
        val percent = NumberFormat.getPercentInstance().format(setting / 100.0)
        setSliderStateDescription(percent)
        summary = percent
    }

    private companion object {
        const val STEP = 5
    }
}

/**
 * Blur strength: how much glass blurs what's behind it, from a light haze to frosted. The middle
 * keeps each place's own blur.
 */
class GlassStrengthPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
    GlassSliderPreference(
        context,
        attrs,
        GlassSettings.MIN_BLUR_STRENGTH,
        GlassSettings.MAX_BLUR_STRENGTH,
        GlassSettings.DEFAULT_BLUR_STRENGTH,
        R.string.glass_blur_strength_light,
        R.string.glass_blur_strength_frosted,
    )

/**
 * Tint: how much of the glass color covers what's behind, from clear to solid. Waits while the
 * glass color is Android default, which brings its own tint.
 */
class GlassTintPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
    GlassSliderPreference(
        context,
        attrs,
        GlassSettings.MIN_TINT,
        GlassSettings.MAX_TINT,
        GlassSettings.DEFAULT_TINT,
        R.string.glass_tint_clear,
        R.string.glass_tint_solid,
    ) {

    override fun refresh() {
        isEnabled = !isAndroidColor()
        super.refresh()
    }

    override fun showSetting(setting: Int) {
        super.showSetting(setting)
        if (isAndroidColor()) {
            summary = context.getString(R.string.glass_tint_android_summary)
        }
    }

    private fun isAndroidColor() =
        Settings.Global.getInt(
            context.contentResolver,
            GlassSettings.COLOR,
            GlassSettings.COLOR_FROSTED,
        ) == GlassSettings.COLOR_ANDROID
}
