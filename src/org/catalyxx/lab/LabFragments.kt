/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package org.catalyxx.lab

import android.os.Bundle
import androidx.annotation.XmlRes
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

/** A Lab page: one preference XML, whose title becomes the toolbar's. */
abstract class LabPageFragment(@XmlRes private val xml: Int) : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(xml, rootKey)
    }

    override fun onResume() {
        super.onResume()
        // Coming back from a page restores this page's title too.
        activity?.title = preferenceScreen.title
    }
}

/** The Lab's front page: one entry per feature area. */
class LabFragment : LabPageFragment(R.xml.lab_main)

/** The volume dialog: slider gradient, percentage and haptics. */
class VolumeDialogFragment : LabPageFragment(R.xml.lab_volume_dialog) {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        val mode = findPreference<ListPreference>(KEY_COLOR_MODE) ?: return
        showCustomColors(mode.value)
        mode.setOnPreferenceChangeListener { _, newValue ->
            showCustomColors(newValue as String)
            true
        }
    }

    // The start and end colors only apply in custom mode; theme mode takes the
    // system palette's primary and secondary.
    private fun showCustomColors(mode: String?) {
        val custom = mode == MODE_CUSTOM
        for (key in arrayOf(KEY_START_COLOR, KEY_END_COLOR)) {
            findPreference<Preference>(key)?.isVisible = custom
        }
    }

    private companion object {
        const val KEY_COLOR_MODE = "custom_gradient_color_mode"
        const val KEY_START_COLOR = "custom_gradient_start_color"
        const val KEY_END_COLOR = "custom_gradient_end_color"
        const val MODE_CUSTOM = "1"
    }
}

/** Quick settings: Color pop. */
class QuickSettingsFragment : LabPageFragment(R.xml.lab_quick_settings)
