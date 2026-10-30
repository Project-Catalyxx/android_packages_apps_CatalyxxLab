/*
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package org.catalyxx.lab

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity

/**
 * Catalyxx Lab, opened from its card on Settings' home screen. Shows [LabFragment]
 * and opens each feature's page in its place, with the collapsing toolbar that
 * Settings' own pages use. A Settings search result opens its page directly.
 */
class LabActivity :
    CollapsingToolbarBaseActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val page = LabPage.forAction(intent?.action) ?: LabPage.MAIN
            val fragment =
                supportFragmentManager.fragmentFactory.instantiate(classLoader, page.fragment.name)
            fragment.arguments = Bundle().apply {
                putString(EXTRA_FRAGMENT_ARG_KEY, intent?.getStringExtra(EXTRA_FRAGMENT_ARG_KEY))
            }
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit()
        }
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference,
    ): Boolean {
        val name = pref.fragment ?: return false
        openPage(name, pref.extras)
        return true
    }

    /**
     * The toolbar's arrow goes back one page, like Back, and closes the Lab only
     * from the page it opened on. SettingsLib's version also closes it whenever the
     * back stack ends up empty, which is every return to the front page here: the
     * front page isn't on the back stack, as Settings has one page per activity.
     */
    override fun onNavigateUp(): Boolean {
        if (!supportFragmentManager.popBackStackImmediate()) finishAfterTransition()
        return true
    }

    /** Opens the page [name], a fragment class, in place of this one; Back returns here. */
    fun openPage(name: String, args: Bundle? = null) {
        val fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, name)
        fragment.arguments = args
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content_frame, fragment)
            .addToBackStack(null)
            .commit()
    }
}
