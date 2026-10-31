/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package org.catalyxx.lab

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.util.AttributeSet
import androidx.preference.ListPreference
import androidx.preference.PreferenceDataStore

/**
 * A [ListPreference] whose value is an int in Settings.System, under the
 * preference's key. LineagePreferenceLib has switches for Settings.System but
 * lists only for LineageSettings. The entry values are the ints as strings;
 * subclasses can store them in another notation.
 */
open class SystemSettingIntListPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : ListPreference(context, attrs) {

    init {
        preferenceDataStore =
            object : PreferenceDataStore() {
                override fun putString(key: String, value: String?) {
                    val int = value?.let { toInt(it) } ?: return
                    Settings.System.putInt(context.contentResolver, key, int)
                }

                override fun getString(key: String, defValue: String?): String? {
                    val stored = Settings.System.getString(context.contentResolver, key)
                    return stored?.toIntOrNull()?.let { fromInt(it) } ?: defValue
                }
            }
    }

    protected open fun toInt(value: String): Int? = value.toIntOrNull()

    protected open fun fromInt(value: Int): String = value.toString()
}

/** [SystemSettingIntListPreference] for an int in Settings.Global. */
class GlobalSettingIntListPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : ListPreference(context, attrs) {

    init {
        preferenceDataStore =
            object : PreferenceDataStore() {
                override fun putString(key: String, value: String?) {
                    val int = value?.toIntOrNull() ?: return
                    Settings.Global.putInt(context.contentResolver, key, int)
                }

                override fun getString(key: String, defValue: String?): String? {
                    val stored = Settings.Global.getString(context.contentResolver, key)
                    return stored?.toIntOrNull()?.toString() ?: defValue
                }
            }
    }
}

/**
 * A color stored as an ARGB int in Settings.System. Entry values are
 * "#AARRGGBB"; a stored color that isn't a preset reads "Custom (#RRGGBB)".
 * The icon shows the current color.
 */
class SystemSettingColorListPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
    SystemSettingIntListPreference(context, attrs) {

    init {
        summaryProvider =
            SummaryProvider<ListPreference> { list ->
                val index = list.findIndexOfValue(list.value)
                if (index >= 0) {
                    list.entries[index]
                } else {
                    val rgb = list.value?.takeLast(6) ?: ""
                    list.context.getString(R.string.custom_color_summary, "#$rgb")
                }
            }
    }

    override fun toInt(value: String): Int? =
        try {
            Color.parseColor(value)
        } catch (e: IllegalArgumentException) {
            null
        }

    override fun fromInt(value: Int): String = String.format("#%08X", value)

    override fun setValue(value: String?) {
        super.setValue(value)
        val color = value?.let { toInt(it) } ?: return
        val density = context.resources.displayMetrics.density
        val size = (24 * density).toInt()
        icon =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setSize(size, size)
                setColor(color)
                // A thin grey ring keeps light colors visible on a light theme.
                setStroke(density.toInt().coerceAtLeast(1), SWATCH_OUTLINE)
            }
    }

    private companion object {
        val SWATCH_OUTLINE = Color.argb(0x4D, 0x88, 0x88, 0x88)
    }
}
