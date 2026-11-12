# BagTag font assets

## Required application font

BagTag uses **American Captain** as the application typeface. This is a
project requirement. `BagTagTheme` applies the bundled TTF to Material
typography so that screen text uses the same family.

| Item | Location |
|---|---|
| Original owner-supplied font folder | `C:\Users\aman\Downloads\american_captain` |
| Original files supplied | `American Captain.ttf`, `American Captain.otf` |
| Bundled Android asset | `android/app/src/main/res/font/american_captain.ttf` |
| Theme integration | `android/app/src/main/java/com/bagtag/app/ui/theme/BagTagTheme.kt` |
| Bundled TTF SHA-256 | `0FFB63E3554242A821003A4A770B653A14136D2A4E522626D61EA8F3E841589C` |

The owner supplied these files for this personal project. Keep the original
folder intact as the source copy. Do not replace the bundled font with an
unverified third-party download. If the app is distributed beyond personal
use, verify that the applicable font licence permits app embedding.

## Maintenance rule

If the font is replaced, preserve the resource filename
`american_captain.ttf` or update `BagTagTheme.kt` in the same change. Rebuild
the Android app and perform the UI smoke test after changing any font asset.
