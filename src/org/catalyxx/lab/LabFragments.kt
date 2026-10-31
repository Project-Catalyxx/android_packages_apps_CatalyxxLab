/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package org.catalyxx.lab

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemProperties
import android.provider.Settings
import android.view.CrossWindowBlurListeners.CROSS_WINDOW_BLUR_SUPPORTED
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.annotation.XmlRes
import androidx.fragment.app.Fragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.android.internal.util.catalyxx.GlassSettings
import com.android.settingslib.widget.LayoutPreference
import lineageos.providers.LineageSettings

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

/**
 * User interface: fonts and the home app, which open in Settings, blur effects, the glass and
 * Settings home pages, and animation speed.
 */
class UserInterfaceFragment : LabPageFragment(R.xml.lab_user_interface) {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        // Glass is blur, so like Blur effects it needs a device with system blur
        findPreference<Preference>(KEY_GLASS)?.isVisible = CROSS_WINDOW_BLUR_SUPPORTED
    }

    override fun onResume() {
        super.onResume()
        // Settings' homepage device card and Accessibility switch the same setting.
        findPreference<BlurEffectsPreference>(KEY_BLUR_EFFECTS)?.refresh()
        // Developer options and Accessibility's Remove animations change the same scales.
        findPreference<AnimationSpeedPreference>(KEY_ANIMATION_SPEED)?.refresh()
        // Back from the glass page
        findPreference<Preference>(KEY_GLASS)?.setSummary(
            if (GlassSettings.isEnabled(requireContext().contentResolver)) R.string.glass_on
            else R.string.glass_off
        )
        // and from the Settings home page
        findPreference<Preference>(KEY_SETTINGS_HOME)?.setSummary(
            if (SettingsHomeFragment.isClassic(requireContext())) {
                R.string.settings_home_style_classic
            } else {
                R.string.settings_home_style_strips
            }
        )
    }

    // The blur and glass keys are also in LabSearchIndexablesProvider's hidden rows
    internal companion object {
        const val KEY_BLUR_EFFECTS = "blur_effects"
        const val KEY_GLASS = "glass"
        const val KEY_SETTINGS_HOME = "settings_home"
        const val KEY_ANIMATION_SPEED = "animation_speed"
    }
}

/**
 * Settings home: the style of Settings' homepage, Classic or the periodic home, and the periodic
 * home's live values. Settings reads the same Settings.System values (PeriodicHome there).
 */
class SettingsHomeFragment : LabPageFragment(R.xml.lab_settings_home) {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        findPreference<Preference>(KEY_STYLE)?.setOnPreferenceChangeListener { _, newValue ->
            showPeriodicOptions((newValue as String).toIntOrNull() != STYLE_CLASSIC)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        showPeriodicOptions(!isClassic(requireContext()))
    }

    // The periodic home's options wait while Classic is picked
    private fun showPeriodicOptions(periodic: Boolean) {
        findPreference<Preference>(KEY_LIVE_VALUES)?.isEnabled = periodic
    }

    internal companion object {
        const val KEY_STYLE = "catalyxx_settings_home_style"
        const val KEY_LIVE_VALUES = "catalyxx_settings_home_live_values"
        const val STYLE_CLASSIC = 0

        /** Whether Settings shows its Classic homepage, the default. */
        fun isClassic(context: Context) =
            Settings.System.getInt(context.contentResolver, KEY_STYLE, STYLE_CLASSIC) ==
                STYLE_CLASSIC
    }
}

/**
 * The glass page: Catalyxx's system glass ([GlassSettings]) on or off, its color and tint, how
 * much it blurs, where it shows, and Battery Saver. Glass needs Blur effects, so while that's off
 * the page offers to turn it on and the rest waits.
 */
class GlassFragment : LabPageFragment(R.xml.lab_glass) {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        findPreference<Preference>(GlassSettings.ENABLED)?.title =
            getString(R.string.glass_main_switch_title, getString(R.string.glass_title))
        findPreference<Preference>(GlassSettings.COLOR)?.setOnPreferenceChangeListener { _, _ ->
            // The list writes Settings.Global after this returns; Tint reads it once it has.
            listView.post { findPreference<GlassTintPreference>(GlassSettings.TINT)?.refresh() }
            true
        }
        findPreference<Preference>(GlassSettings.STYLE)?.setOnPreferenceChangeListener { _, _ ->
            // Flat draws no rim, so the rim sliders wait while it's picked
            listView.post { refreshRimSliders() }
            true
        }
        findPreference<Preference>(KEY_NEEDS_BLUR)?.setOnPreferenceClickListener {
            Settings.Global.putInt(
                requireContext().contentResolver,
                Settings.Global.DISABLE_WINDOW_BLURS,
                0,
            )
            refresh()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    /** Reads what can also change elsewhere: Blur effects and the sliders. */
    private fun refresh() {
        val isBlurOn =
            Settings.Global.getInt(
                requireContext().contentResolver,
                Settings.Global.DISABLE_WINDOW_BLURS,
                0,
            ) == 0
        findPreference<Preference>(KEY_NEEDS_BLUR)?.isVisible = !isBlurOn
        // The rest of the page depends on the main switch, so it waits too
        findPreference<Preference>(GlassSettings.ENABLED)?.isEnabled = isBlurOn
        findPreference<GlassTintPreference>(GlassSettings.TINT)?.refresh()
        findPreference<GlassStrengthPreference>(GlassSettings.BLUR_STRENGTH)?.refresh()
        refreshRimSliders()
    }

    private fun refreshRimSliders() {
        for (key in RIM_SLIDER_KEYS) {
            findPreference<GlassRimSliderPreference>(key)?.refresh()
        }
    }

    // Also in LabSearchIndexablesProvider's hidden rows
    internal companion object {
        const val KEY_NEEDS_BLUR = "glass_needs_blur"
        const val KEY_RIM_NOTE = "glass_rim_note"

        private val RIM_SLIDER_KEYS =
            arrayOf(
                GlassSettings.BEND,
                GlassSettings.RIM_WIDTH,
                GlassSettings.RIM_LIGHT,
                GlassSettings.RAINBOW,
                GlassSettings.SHADOW,
            )
    }
}

/** Lock screen: nothing yet. */
class LockscreenFragment : LabPageFragment(R.xml.lab_lockscreen)

/** Misc: Spoofing, which opens in Settings. */
class MiscFragment : LabPageFragment(R.xml.lab_misc)

/** Sound: the volume dialog's slider gradient, percentage and haptics, and vibration. */
class SoundFragment : LabPageFragment(R.xml.lab_sound) {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        findPreference<Preference>(KEY_VIBRATE_ON)?.setOnPreferenceChangeListener { _, _ ->
            // The switch writes Settings.System after this returns; read it once it has.
            listView.post { refreshVibration() }
            true
        }
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

    override fun onResume() {
        super.onResume()
        // Settings' Vibration & haptics page may have changed them meanwhile.
        refreshVibration()
    }

    private fun refreshVibration() {
        val resolver = requireContext().contentResolver
        findPreference<TwoStatePreference>(KEY_VIBRATE_ON)?.isChecked =
            Settings.System.getInt(resolver, Settings.System.VIBRATE_ON, 1) == 1
        val category = findPreference<PreferenceCategory>(KEY_VIBRATION_CATEGORY) ?: return
        for (i in 0 until category.preferenceCount) {
            (category.getPreference(i) as? VibrationIntensityPreference)?.refresh()
        }
    }

    // The colors' keys are also in LabSearchIndexablesProvider's hidden rows
    internal companion object {
        const val KEY_COLOR_MODE = "custom_gradient_color_mode"
        const val KEY_START_COLOR = "custom_gradient_start_color"
        const val KEY_END_COLOR = "custom_gradient_end_color"
        const val MODE_CUSTOM = "1"
        const val KEY_VIBRATION_CATEGORY = "vibration_category"
        const val KEY_VIBRATE_ON = "vibrate_on"
    }
}

/** Notifications: nothing yet. */
class NotificationsFragment : LabPageFragment(R.xml.lab_notifications)

/** Buttons: the power menu, which opens in LineageParts, and long press power for torch. */
class ButtonsFragment : LabPageFragment(R.xml.lab_buttons) {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        val torch = isTorchGestureSupported(requireContext())
        for (key in arrayOf(KEY_TORCH_GESTURE, KEY_TORCH_TIMEOUT)) {
            findPreference<Preference>(key)?.isVisible = torch
        }
    }

    override fun onResume() {
        super.onResume()
        // LineageParts' Buttons page may have changed them meanwhile.
        val resolver = requireContext().contentResolver
        findPreference<TwoStatePreference>(KEY_TORCH_GESTURE)?.isChecked =
            LineageSettings.System.getInt(
                resolver,
                LineageSettings.System.TORCH_LONG_PRESS_POWER_GESTURE,
                0,
            ) == 1
        findPreference<ListPreference>(KEY_TORCH_TIMEOUT)?.value =
            LineageSettings.System.getInt(
                    resolver,
                    LineageSettings.System.TORCH_LONG_PRESS_POWER_TIMEOUT,
                    0,
                )
                .toString()
    }

    // The torch keys are also in LabSearchIndexablesProvider's hidden rows
    internal companion object {
        const val KEY_TORCH_GESTURE = "torch_long_press_power_gesture"
        const val KEY_TORCH_TIMEOUT = "torch_long_press_power_timeout"

        /** The torch needs a flash, and a power button that works with the screen off. */
        fun isTorchGestureSupported(context: Context) =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) &&
                context.resources.getBoolean(
                    com.android.internal.R.bool.config_supportLongPressPowerWhenNonInteractive
                )
    }
}

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
