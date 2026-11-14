# Smarty frontend update

## Apply it to your EXISTING project

Your working Android project remains:
C:\Documents\SESSIONAUTOMNE2026\HackDec\HackDecouverte2026\Smarty

1. Keep your current work committed on your own branch (`annesophie`).
2. Extract this ZIP into a separate temporary folder. The extracted `Smarty` folder is the update source, not a new repository to work in.
3. Run this command, adjusting only the path to the extracted installer:

```powershell
powershell -ExecutionPolicy Bypass -File "C:\path\to\extracted\Apply-Frontend.ps1" -ProjectPath "C:\Documents\SESSIONAUTOMNE2026\HackDec\HackDecouverte2026\Smarty"
```

The installer validates all affected files against the barcode version before copying. If your or your teammate's edits differ, it stops BEFORE changing files and lists the files that need merging. It backs up existing affected files beside your project and leaves unrelated files, Git history and Firebase configuration untouched.

4. Stay in the existing Android Studio project, sync Gradle and Run.
5. No Firebase Console, Firestore rule or data migration changes are needed.

## Design and navigation

- A consistent cream, sage and peach light theme. Wallpaper dynamic colors are disabled; the light theme is intentional for the prototype.
- New original bundled vector illustrations: dairy, eggs, vegetables, fruit, bread, meat, fish, grocery bag, plus a fridge mascot and a generic meal bowl. No runtime image generation or asset downloads.
- Smarty branding and selected-fridge switcher above the app. Tap the rounded fridge name to switch/create/join; the round + beside it opens the invite code.
- Fridge tab: use-soon preview, recipe action, category chips, item cards, expiry badges, Add and Scan.
- Item actions live in each card's three-dot menu. Removal now asks for confirmation. Date entry retains manual typing and also offers the native calendar button.
- Recipes tab: real recipe generation entry point. The preferences and results are not fake history pages. Loading state is retained across tabs.
- Recipe results use a full-screen view with ingredient sections, quantities, numbered steps and the existing two-stage cooking confirmation.
- Settings tab: existing enable/check/disable reminders, selected-fridge information and logout.
- Login and registration follow the same palette. Existing authentication, validation and password handling are retained.

Ingredient icons are chosen from normalized English/French names and the optional stored category, strictly for display. Unknown foods use a grocery bag. Food photos already stored by barcode lookup are preserved, but this version displays the bundled icons for consistency. The recipe bowl is a generic illustration, not a generated picture of the suggested recipe.

## Files to understand

- `ui/theme/`: palette, typography and rounded shapes.
- `ui/FoodPresentation.kt`: pure ingredient matching, date labels and quantity display.
- `ui/components/SmartyComponents.kt`: shared visual components and small line icons.
- `FridgeScreen.kt`: retains inventory listener and write callbacks; adds tabs and the dashboard presentation.
- `SharedFridgeHome.kt`: retains initialization/membership/switching; styles its header and management dialogs.
- `RecipeControls.kt`: keeps recipe generation and confirmation calls; adds an external launch request so dashboard and Recipes tab share one mounted controller.
- `ExpiryReminders.kt`: only UI imports and ReminderControls presentation changed. Worker/scheduler/permission-check logic is unchanged.
- `MainActivity.kt`: light system-bar styling only; App Check and notification intent routing retained.
- `BarcodeLookupControls.kt`: button styling only.

The Firebase rules and SharedFridgeService, RecipeService and OpenFoodFactsService files are byte-identical to the working barcode baseline.

## Verification completed

- Compiled all 19 main Kotlin source files using Kotlin 2.2.10, the Compose compiler plugin and resolved app library versions (including Compose BOM 2026.02.01 and Firebase BOM 34.6.0), using an Android API 36 jar and a generated resource-ID stub for compile verification.
- This is a Kotlin/Compose compilation check, not an APK build, Android resource linking, emulator run or screenshot test. Your normal Android Studio build still uses the project's original SDK/build settings.
- The included JUnit FoodPresentationTest passed 25 checks: English/French matching, unknown-food fallback, expiry boundary dates, past/invalid dates and quantity formatting.
- XML resources parsed successfully. The bundled vector illustrations were rendered and visually inspected.
- Byte comparisons confirmed preservation of services, rules and the reminder worker/scheduler body.

## Check on your emulator before pushing

1. Log out (Settings) and verify styled registration/login, validation and sign-in.
2. Switch fridges; create/join and invite code should work as before.
3. Check a small screen and long item names. Scroll the list and category chips; verify every item remains reachable above the floating Add/Scan buttons.
4. Edit an item with the calendar, then type a date manually. Cancel should not save. Confirm quantity/unit/image/barcode metadata are retained.
5. Scan/look up a product, confirm data and save. Test manual fallback too.
6. Generate a recipe from the dashboard and from Recipes. While generating, switching tabs must not launch a second request.
7. Closing a recipe must not deduct stock. Confirm cooking once and verify all shared inventory quantities update.
8. In Settings, enable reminders, Check expiry now and disable. Check that notification taps still open the right fridge.
9. Try large system text and a small emulator: dialogs and screen content should scroll rather than hide controls.

## Push after testing

From HackDecouverte2026, on your own branch:

```powershell
git status
git add Smarty
git commit -m "Polish Smarty interface with food illustrations and clearer navigation"
git push
```

Do not add installer backup folders. The updater uses `Smarty-before-frontend-*`, matching the backup exclusion already discussed.
