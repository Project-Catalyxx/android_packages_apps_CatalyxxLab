/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package org.catalyxx.lab

import android.database.Cursor
import android.database.MatrixCursor
import android.provider.SearchIndexablesContract.INDEXABLES_RAW_COLUMNS
import android.provider.SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS
import android.provider.SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS
import android.provider.SearchIndexablesProvider
import android.view.CrossWindowBlurListeners.CROSS_WINDOW_BLUR_SUPPORTED
import androidx.annotation.XmlRes
import com.android.internal.util.catalyxx.GlassSettings

/** Names the row a Settings search result was found in (Settings' own extra). */
const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"

/**
 * The Lab's pages. A Settings search result opens one with its [action]; [LabActivity]
 * only opens the pages listed here.
 */
enum class LabPage(@XmlRes val xml: Int, val fragment: Class<out LabPageFragment>) {
    MAIN(R.xml.lab_main, LabFragment::class.java),
    STATUS_BAR(R.xml.lab_status_bar, StatusBarFragment::class.java),
    QUICK_SETTINGS(R.xml.lab_quick_settings, QuickSettingsFragment::class.java),
    USER_INTERFACE(R.xml.lab_user_interface, UserInterfaceFragment::class.java),
    GLASS(R.xml.lab_glass, GlassFragment::class.java),
    SETTINGS_HOME(R.xml.lab_settings_home, SettingsHomeFragment::class.java),
    LOCKSCREEN(R.xml.lab_lockscreen, LockscreenFragment::class.java),
    SOUND(R.xml.lab_sound, SoundFragment::class.java),
    NOTIFICATIONS(R.xml.lab_notifications, NotificationsFragment::class.java),
    BUTTONS(R.xml.lab_buttons, ButtonsFragment::class.java),
    MISC(R.xml.lab_misc, MiscFragment::class.java),
    ABOUT(R.xml.lab_about, AboutFragment::class.java);

    val action: String
        get() = "org.catalyxx.lab.action.SHOW_$name"

    companion object {
        fun forAction(action: String?): LabPage? = entries.firstOrNull { it.action == action }
    }
}

/**
 * Puts the Lab's settings in Settings' search: Settings Intelligence reads each page's
 * XML for its titles and opens a result with that page's action. It only reads providers
 * of system apps guarded by READ_SEARCH_INDEXABLES (AndroidManifest.xml).
 */
class LabSearchIndexablesProvider : SearchIndexablesProvider() {

    override fun onCreate() = true

    override fun queryXmlResources(projection: Array<String>?): Cursor =
        MatrixCursor(INDEXABLES_XML_RES_COLUMNS).apply {
            for (page in LabPage.entries) {
                addRow(
                    arrayOf<Any?>(
                        0, // rank
                        page.xml,
                        null, // class name: only for Settings' own fragments
                        R.drawable.ic_lab,
                        page.action,
                        requireContext().packageName,
                        LabActivity::class.java.name,
                    )
                )
            }
        }

    override fun queryRawData(projection: Array<String>?): Cursor =
        MatrixCursor(INDEXABLES_RAW_COLUMNS)

    override fun queryNonIndexableKeys(projection: Array<String>?): Cursor =
        MatrixCursor(NON_INDEXABLES_KEYS_COLUMNS).apply {
            for (key in NON_INDEXABLE_KEYS) {
                addRow(arrayOf<Any?>(key))
            }
            // Blur effects and glass only show on a device with system blur
            if (!CROSS_WINDOW_BLUR_SUPPORTED) {
                addRow(arrayOf<Any?>(UserInterfaceFragment.KEY_BLUR_EFFECTS))
                addRow(arrayOf<Any?>(UserInterfaceFragment.KEY_GLASS))
                for (key in GlassSettings.KEYS) {
                    addRow(arrayOf<Any?>(key))
                }
            }
            // and the torch rows only where the power button can turn the torch on
            if (!ButtonsFragment.isTorchGestureSupported(requireContext())) {
                addRow(arrayOf<Any?>(ButtonsFragment.KEY_TORCH_GESTURE))
                addRow(arrayOf<Any?>(ButtonsFragment.KEY_TORCH_TIMEOUT))
            }
        }

    private companion object {
        // Rows that aren't settings (intros, the front page's cards, empty pages' notes),
        // and the gradient colors, which Sound only shows in custom color mode
        val NON_INDEXABLE_KEYS =
            listOf(
                "lab_intro",
                "lab_dashboard",
                "about_intro",
                "glass_intro",
                GlassFragment.KEY_NEEDS_BLUR,
                GlassFragment.KEY_RIM_NOTE,
                "settings_home_intro",
                "coming_soon",
                SoundFragment.KEY_START_COLOR,
                SoundFragment.KEY_END_COLOR,
            )
    }
}
