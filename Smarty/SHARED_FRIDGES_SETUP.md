# Shared fridges update

## Install into your existing repository

1. Extract this ZIP into a separate temporary folder. Do not replace your repository wholesale.
2. In PowerShell, run the included installer, pointing to your existing Android project:

```powershell
powershell -ExecutionPolicy Bypass -File "C:\path\to\extracted\Apply-SharedFridges.ps1" -ProjectPath "C:\Documents\SESSIONAUTOMNE2026\HackDec\HackDecouverte2026\Smarty"
```

The installer checks every affected file against the uploaded snapshot BEFORE making changes. If you or a teammate edited an affected file since that snapshot, it stops and names the file for a manual merge. It backs up affected existing files beside your project before copying the update. Unrelated files, Firebase config, local.properties, generated output and Git history are untouched.

You can instead manually merge the files listed in UPDATE_MANIFEST.json. Keep your current google-services.json and emulator debug-token registration.

3. Open the updated `firestore.rules`, copy its complete contents into Firebase Console > Firestore Database > Rules, and Publish. The project ZIP does not publish rules for you.
4. Open your existing Smarty project in Android Studio, sync and run.
5. First login makes a personal fridge and copies the old inventory. Internet is required for setup.

## What's implemented

- A personal fridge is prepared once per account.
- Create additional named fridges, copy their invite codes, join and switch between fridges.
- Members see the same live item list and may add, edit and remove items.
- AI generation uses the selected inventory. Cooking validates and deducts from that fridge atomically.
- Cooking records are immutable; the same suggestion cannot be confirmed twice.
- Per-fridge, per-device reminders read current shared data. Each member enables notifications on their own device; this is not synchronized push delivery.
- Notification titles identify the fridge; tapping opens that fridge if the signed-in account is a member.
- Signing out cancels this device's reminders. Old user-only schedules are retired on login.

## Test with a teammate

1. Account A creates “Family fridge” and copies Invite code.
2. Account B signs in on another emulator/device, opens Switch / create / join, then Join with a code.
3. A adds an item; B sees it without restarting. B edits it; A sees the update.
4. Try a wrong code: access must be denied. Each account's personal fridge remains separate.
5. Both devices enable reminders for Family fridge. Add an item dated today through three days ahead and use Check expiry now on each device.
6. Generate a recipe in Family fridge. Confirm cooking on one device and check the remaining quantities on both.
7. Reopen the app: the selected fridge is remembered. Old items must not be imported a second time.

## Existing data and limits

The one-time migration copies up to 200 existing items with stable IDs into a new personal fridge and stores a marker in users/{uid}. The copy and marker commit together; original users/{uid}/items records are retained. Do not continue adding items through the old app after migration: later legacy edits are not synchronized to the new fridge. Have teammates update together. If migration fails, originals remain intact and Retry is available.

Codes are long random invitation credentials: paste them rather than typing. Any signed-in person with a code can join. This version has no code rotation, member removal, leaving, fridge deletion or renaming. Those can be added later. Owners and members can manage inventory; ownership and membership cannot be forged through the client rules.

Each member must allow notifications and enable reminders on their own device. Android may delay background work; it is not an exact-time alarm and requires internet. Manual device time/date affects the three-day window.

Quantity validation is enforced in the app and rules. The AI does not determine food safety or convert ambiguous units. The existing debug App Check provider remains development-only.

## Verification performed

Nine tests passed against the local Firestore emulator: atomic creation, non-member restrictions, wrong-code and forged-index rejection, repeat joining, member edits, ownership/invite protection, invalid quantities, concurrent recipe confirmation, and one-time migration. These test rules and transaction scenarios; they do not execute Android UI code.

Android compilation was attempted, but the environment could not download gradle-9.5.0-bin.zip (network unreachable). Android build, AI calls, and notifications still need validation in your Android Studio setup.

## Optional repeatable rules tests

Requires Node.js, Java and Firebase CLI on a development machine, not inside the Android app:

```powershell
npm --prefix tests install
firebase emulators:exec --only firestore --project demo-smarty-shared "npm --prefix tests test"
```

This runs only against the demo emulator, not your live Firebase project.
