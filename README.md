# Catalyxx Lab

*Mix your own formula.*

Project Catalyxx's own settings. Settings shows the Lab as a card on its home
screen, right after Connected devices, so Catalyxx needs no fork of the Settings
app for it: Settings picks up `IA_SETTINGS` activities of system apps, the same
way it hosts LineageOS's LineageParts.

## Pages

| Page | Settings it controls |
|---|---|
| Volume dialog | `Settings.System.VOLUME_SLIDER_GRADIENT`, `CUSTOM_GRADIENT_COLOR_MODE`, `CUSTOM_GRADIENT_START_COLOR`, `CUSTOM_GRADIENT_END_COLOR`, `SHOW_VOLUME_PERCENTAGE`; `Settings.Secure.VOLUME_DIALOG_HAPTIC_FEEDBACK` |
| Quick settings | `Settings.System.QS_COLOR_POP` |

The features themselves live in Project Catalyxx's `frameworks/base`; the Lab only
reads and writes their settings. SystemUI picks a volume change up the next time
the volume dialog opens, and a Color pop change straight away.

## Adding a page

1. A preference XML in `res/xml/lab_<page>.xml`, titled with the page's name.
   Switches for `Settings.System` and `Settings.Secure` keys are
   `lineageos.preference.SystemSettingSwitchPreference` and
   `SecureSettingSwitchPreference`; lists of ints in `Settings.System` are
   `org.catalyxx.lab.SystemSettingIntListPreference`, and colors
   `SystemSettingColorListPreference`. Give each the same default SystemUI
   assumes when the key is unset.
2. A fragment in `LabFragments.kt`: `class <Page>Fragment : LabPageFragment(R.xml.lab_<page>)`.
3. An entry for it in `res/xml/lab_main.xml`.

Keep pages device-neutral: every Catalyxx device gets the same Lab.

## Build

`CatalyxxLab`, listed in `vendor/catalyxx/config/common.mk`. Platform-signed and
privileged in `system_ext`, because Settings only shows tiles from system apps and
the haptics switch writes a `Settings.Secure` key.
