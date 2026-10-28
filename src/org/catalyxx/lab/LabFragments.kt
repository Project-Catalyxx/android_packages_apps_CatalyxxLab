/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package org.catalyxx.lab

import android.os.Bundle
import android.os.SystemProperties
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.annotation.XmlRes
import androidx.fragment.app.Fragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.android.settingslib.widget.LayoutPreference

/** A Lab page: one preference XML, whose title becomes the toolbar's. */
abstract class LabPageFragment(@XmlRes private val xml: Int) : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(xml, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // A Settings search result opens the page at the row it found
        if (savedInstanceState == null) {
            arguments?.getString(EXTRA_FRAGMENT_ARG_KEY)?.let { scrollToPreference(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        // Coming back from a page restores this page's title too.
        activity?.title = preferenceScreen.title
    }
}

/**
 * The Lab's front page: a card per feature area (lab_dashboard.xml, after Matrixx Lab's
 * dashboard), each opening its page.
 */
class LabFragment : LabPageFragment(R.xml.lab_main) {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        val dashboard = findPreference<LayoutPreference>(KEY_DASHBOARD) ?: return
        for (card in SMALL_CARDS) {
            dashboard.findViewById<View>(card.id)?.let { fillSmallCard(it, card) }
        }
        for ((id, page) in PAGES) {
            dashboard.findViewById<View>(id)?.setOnClickListener {
                (activity as? LabActivity)?.openPage(page.name)
            }
        }
    }

    private fun fillSmallCard(view: View, card: SmallCard) {
        view.findViewById<TextView>(R.id.lab_small_card_title).setText(card.title)
        view.findViewById<TextView>(R.id.lab_small_card_summary).setText(card.summary)
        view.findViewById<LottieAnimationView>(R.id.lab_small_card_animation).apply {
            setAnimation(card.animation)
            repeatCount = LottieDrawable.INFINITE
            playAnimation()
        }
    }

    /** One of the small cards, which share lab_small_card.xml. */
    private class SmallCard(
        @IdRes val id: Int,
        @StringRes val title: Int,
        @StringRes val summary: Int,
        @RawRes val animation: Int,
    )

    private companion object {
        const val KEY_DASHBOARD = "lab_dashboard"

        val SMALL_CARDS =
            listOf(
                SmallCard(
                    R.id.card_sound,
                    R.string.sound_title,
                    R.string.sound_summary,
                    R.raw.lab_card_sound,
                ),
                SmallCard(
                    R.id.card_notifications,
                    R.string.notifications_title,
                    R.string.notifications_summary,
                    R.raw.lab_card_notifications,
                ),
                SmallCard(
                    R.id.card_buttons,
                    R.string.buttons_title,
                    R.string.buttons_summary,
                    R.raw.lab_card_buttons,
                ),
                SmallCard(
                    R.id.card_about,
                    R.string.about_title,
                    R.string.about_summary,
                    R.raw.lab_card_about,
                ),
            )

        val PAGES: Map<Int, Class<out Fragment>> =
            mapOf(
                R.id.card_status_bar to StatusBarFragment::class.java,
                R.id.card_quick_settings to QuickSettingsFragment::class.java,
                R.id.card_user_interface to UserInterfaceFragment::class.java,
                R.id.card_lockscreen to LockscreenFragment::class.java,
                R.id.card_misc to MiscFragment::class.java,
                R.id.card_sound to SoundFragment::class.java,
                R.id.card_notifications to NotificationsFragment::class.java,
                R.id.card_buttons to ButtonsFragment::class.java,
                R.id.card_about to AboutFragment::class.java,
            )
    }
}

/** Status bar: nothing yet. */
class StatusBarFragment : LabPageFragment(R.xml.lab_status_bar)

/** Quick settings: Color pop. */
class QuickSettingsFragment : LabPageFragment(R.xml.lab_quick_settings)

/** User interface: nothing yet. */
class UserInterfaceFragment : LabPageFragment(R.xml.lab_user_interface)

/** Lock screen: nothing yet. */
class LockscreenFragment : LabPageFragment(R.xml.lab_lockscreen)

/** Misc: Spoofing, which opens in Settings. */
class MiscFragment : LabPageFragment(R.xml.lab_misc)

/** Sound: the volume dialog's slider gradient, percentage and haptics. */
class SoundFragment : LabPageFragment(R.xml.lab_sound) {

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

    // The colors' keys are also in LabSearchIndexablesProvider's hidden rows
    internal companion object {
        const val KEY_COLOR_MODE = "custom_gradient_color_mode"
        const val KEY_START_COLOR = "custom_gradient_start_color"
        const val KEY_END_COLOR = "custom_gradient_end_color"
        const val MODE_CUSTOM = "1"
    }
}

/** Notifications: nothing yet. */
class NotificationsFragment : LabPageFragment(R.xml.lab_notifications)

/** Buttons: nothing yet. */
class ButtonsFragment : LabPageFragment(R.xml.lab_buttons)

/** About: the ro.catalyxx.* properties, whose names are the rows' keys, and the source code. */
class AboutFragment : LabPageFragment(R.xml.lab_about) {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        for (property in PROPERTIES) {
            findPreference<Preference>(property)?.summary =
                SystemProperties.get(property).ifEmpty { getString(R.string.about_unknown) }
        }
    }

    private companion object {
        val PROPERTIES =
            listOf(
                "ro.catalyxx.display.version",
                "ro.catalyxx.codename",
                "ro.catalyxx.build.version",
                "ro.catalyxx.maintainer",
            )
    }
}
