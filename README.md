# Catalyxx Lab

*Mix your own formula.*

Project Catalyxx's own settings. Settings shows the Lab as a card on its home
screen, right after Connected devices, so Catalyxx needs no fork of the Settings
app for it: Settings picks up `IA_SETTINGS` activities of system apps, the same
way it hosts LineageOS's LineageParts.

## Pages

The front page (`res/layout/lab_dashboard.xml`, after Matrixx Lab's dashboard)
has a card per feature area; each opens its page. The cards' animations are
Matrixx Lab's, recolored to Catalyxx's purples with
`catalyxx-drafts/tools/recolor_lottie.py`.

| Card | Page | Settings it controls |
|---|---|---|
| Status bar | `lab_status_bar.xml` | nothing yet |
| Quick settings | `lab_quick_settings.xml` | `Settings.System.QS_COLOR_POP` |
| User interface | `lab_user_interface.xml` | nothing yet |
| Lock screen | `lab_lockscreen.xml` | nothing yet |
| Misc | `lab_misc.xml` | opens Settings' Spoofing page |
| Sound | `lab_sound.xml` | `Settings.System.VOLUME_SLIDER_GRADIENT`, `CUSTOM_GRADIENT_COLOR_MODE`, `CUSTOM_GRADIENT_START_COLOR`, `CUSTOM_GRADIENT_END_COLOR`, `SHOW_VOLUME_PERCENTAGE`; `Settings.Secure.VOLUME_DIALOG_HAPTIC_FEEDBACK` |
| Notifications | `lab_notifications.xml` | nothing yet |
| Buttons | `lab_buttons.xml` | opens LineageOS's Power menu page (LineageParts) |
| About | `lab_about.xml` | shows the `ro.catalyxx.*` version properties |

The features themselves live in Project Catalyxx's `frameworks/base`; the Lab only
reads and writes their settings. SystemUI picks a volume change up the next time
the volume dialog opens, and a Color pop change straight away.

## Adding a feature

1. Its settings go on the page of its card, in `res/xml/lab_<page>.xml`; drop
   that page's "coming soon" line when its first feature lands.
   Switches for `Settings.System` and `Settings.Secure` keys are
   `lineageos.preference.SystemSettingSwitchPreference` and
   `SecureSettingSwitchPreference`; lists of ints in `Settings.System` are
   `org.catalyxx.lab.SystemSettingIntListPreference`, and colors
   `SystemSettingColorListPreference`. Give each the same default SystemUI
   assumes when the key is unset.
2. A feature area of its own gets a card in `lab_dashboard.xml`, a page XML, a
   fragment in `LabFragments.kt` (`class <Page>Fragment : LabPageFragment(R.xml.lab_<page>)`)
   and an entry in `LabFragment.PAGES`.

Keep pages device-neutral: every Catalyxx device gets the same Lab.

## Build

`CatalyxxLab`, listed in `vendor/catalyxx/config/common.mk`. Platform-signed and
privileged in `system_ext`, because Settings only shows tiles from system apps and
the haptics switch writes a `Settings.Secure` key.
