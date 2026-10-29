/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package org.catalyxx.lab

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import android.util.AttributeSet
import android.view.CrossWindowBlurListeners.CROSS_WINDOW_BLUR_SUPPORTED
import lineageos.preference.SelfRemovingSwitchPreference

/**
 * Blur effects: the blur behind the shade, quick settings, dialogs and the launcher. On means
 * Android's disable_window_blurs is 0, so this is Accessibility > Color and motion > Reduce blur
 * effects the other way round; a long press on Settings' homepage device card switches the same
 * setting. Only shown on a device with system blur.
 */
class BlurEffectsPreference(context: Context, attrs: AttributeSet?) :
    SelfRemovingSwitchPreference(context, attrs) {

    init {
        isVisible = CROSS_WINDOW_BLUR_SUPPORTED
    }

    override fun onAttached() {
        super.onAttached()
        refresh()
    }

    /** Reads the setting again, and greys the switch out while Battery Saver keeps blur off. */
    fun refresh() {
        isChecked = getBoolean(key, true)
        val batterySaver = context.getSystemService(PowerManager::class.java).isPowerSaveMode
        isEnabled = !batterySaver
        setSummary(
            if (batterySaver) R.string.blur_effects_battery_saver_summary
            else R.string.blur_effects_summary
        )
    }

    // Unset reads as on, so opening the page writes nothing
    override fun isPersisted() = true

    override fun putBoolean(key: String?, value: Boolean) {
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.DISABLE_WINDOW_BLURS,
            if (value) 0 else 1,
        )
    }

    override fun getBoolean(key: String?, defaultValue: Boolean) =
        Settings.Global.getInt(context.contentResolver, Settings.Global.DISABLE_WINDOW_BLURS, 0) == 0
}
