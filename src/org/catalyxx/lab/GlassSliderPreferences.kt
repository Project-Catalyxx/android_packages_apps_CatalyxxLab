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
 * [minValue] to [maxValue] in steps of [step], shown as a percentage. SystemUI follows a change
 * at once.
 */
abstract class GlassSliderPreference(
    context: Context,
    attrs: AttributeSet?,
    minValue: Int,
    maxValue: Int,
    private val defaultSetting: Int,
    @StringRes textStart: Int,
    @StringRes textEnd: Int,
    private val step: Int = 5,
) : SliderPreference(context, attrs) {

    init {
        isPersistent = false
        min = minValue
        max = maxValue
        setSliderIncrement(step)
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
        value = (min + (setting - min + step / 2) / step * step).coerceIn(min, max)
        showSetting(setting)
    }

    protected open fun showSetting(setting: Int) {
        show(NumberFormat.getPercentInstance().format(setting / 100.0))
    }

    /** Shows [text] as the setting, to the eye and to TalkBack. */
    protected fun show(text: String) {
        setSliderStateDescription(text)
        summary = text
    }
}

/**
 * A slider for glass's curved rim. Waits while the style is Flat, which draws no rim.
 */
abstract class GlassRimSliderPreference(
    context: Context,
    attrs: AttributeSet?,
    minValue: Int,
    maxValue: Int,
    defaultSetting: Int,
    @StringRes textStart: Int,
    @StringRes textEnd: Int,
    step: Int = 5,
) :
    GlassSliderPreference(
        context,
        attrs,
        minValue,
        maxValue,
        defaultSetting,
        textStart,
        textEnd,
        step,
    ) {

    override fun refresh() {
        isEnabled =
            Settings.Global.getInt(
                context.contentResolver,
                GlassSettings.STYLE,
                GlassSettings.DEFAULT_STYLE,
            ) != GlassSettings.STYLE_FLAT
        super.refresh()
    }
}

/** Edge bend: how far the rim reaches past the edge to bend in what's there. */
class GlassBendPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
    GlassRimSliderPreference(
        context,
        attrs,
        GlassSettings.MIN_PERCENT,
        GlassSettings.MAX_PERCENT,
        GlassSettings.DEFAULT_BEND,
        R.string.glass_bend_flat,
        R.string.glass_bend_strong,
    )

/** Rim width, in dp. */
class GlassRimWidthPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
    GlassRimSliderPreference(
        context,
        attrs,
        GlassSettings.MIN_RIM_WIDTH,
        GlassSettings.MAX_RIM_WIDTH,
        GlassSettings.DEFAULT_RIM_WIDTH,
        R.string.glass_rim_width_thin,
        R.string.glass_rim_width_wide,
        step = 2,
    ) {

    override fun showSetting(setting: Int) {
        show(context.getString(R.string.glass_rim_width_summary, setting))
    }
}

/** Rim light: the bright line along the rim and its glow. */
class GlassRimLightPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
    GlassRimSliderPreference(
        context,
        attrs,
        GlassSettings.MIN_PERCENT,
        GlassSettings.MAX_PERCENT,
        GlassSettings.DEFAULT_RIM_LIGHT,
        R.string.glass_rim_light_none,
        R.string.glass_rim_light_bright,
    )

/** Rainbow edges: how much the rim splits light into colors. */
class GlassRainbowPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
    GlassRimSliderPreference(
        context,
        attrs,
        GlassSettings.MIN_PERCENT,
        GlassSettings.MAX_PERCENT,
        GlassSettings.DEFAULT_RAINBOW,
        R.string.glass_rainbow_none,
        R.string.glass_rainbow_vivid,
    )

/** Shadow: the soft shadow around the glass, which the rim bends in too. */
class GlassShadowPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
    GlassRimSliderPreference(
        context,
        attrs,
        GlassSettings.MIN_PERCENT,
        GlassSettings.MAX_PERCENT,
        GlassSettings.DEFAULT_SHADOW,
        R.string.glass_shadow_none,
        R.string.glass_shadow_dark,
    )

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
