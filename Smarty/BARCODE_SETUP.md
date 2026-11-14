# Barcode scanning update

## Install into the project you already use

Extract the ZIP into a separate temporary folder. The extracted Smarty folder supplies update files; do not open it as a second working project.

Open a terminal in the folder containing Apply-BarcodeScanning.ps1 and run:

```powershell
powershell -ExecutionPolicy Bypass -File .\Apply-BarcodeScanning.ps1 -ProjectPath "C:\Documents\SESSIONAUTOMNE2026\HackDec\HackDecouverte2026\Smarty"
```

The script compares affected files against the working shared-fridge snapshot. It stops before making changes if a teammate has edited them, and backs up affected existing files beside the project. No Git history, local configuration or unrelated files are changed.

Keep working in HackDecouverte2026/Smarty. In Android Studio sync Gradle, then Run. No Firestore rules or Firebase Console changes are needed for this update.

## Use it

1. Select the fridge you want to add to.
2. Tap Scan / barcode.
3. Scan with camera, or type a barcode and tap Look up.
4. Check the product name. Enter the actual amount you own, its unit and the expiry date from your package.
5. Save. Nothing is written to Firestore until you confirm the item form.

For a quick lookup test without an emulator camera, try 3017620422003. Product details are community data and may change.

If the product is unknown or the request fails, Use barcode manually opens a blank-name form retaining the barcode. Ordinary Add item also remains available. Repeated scans create separate stock entries, which may have different dates; they do not silently combine quantities.

## Scanner requirements

Google Code Scanner uses Google Play services and does not request camera permission in Smarty. The scanner module may need a download on first use. Some emulators lack a usable camera or Google Play services: use manual barcode lookup there and test the camera on a compatible device. This is a runtime device requirement, not a Firebase setting.

## Data handling

Only the grocery barcode is sent to Open Food Facts. Names, brand and package label are shown for review; available image metadata is stored with the item but this update does not add image display. Package size is not used as stock quantity automatically, and expiry dates are never inferred from the product database.

Lookups use HTTPS, timeouts, a bounded response size, a custom app User-Agent and a ten-minute in-memory cache. Empty fields, unknown products, invalid input, network errors and cancellation have manual fallbacks. Edits preserve existing barcode/image metadata. The Open Food Facts attribution appears in the lookup dialog.

## Verification

- Compiled the actual parser and network service with Kotlin 2.2.10.
- Passed the included JUnit parser test (19 checks) covering leading zeroes, supported lengths, invalid values, language/name fallbacks, missing products, non-HTTPS image URLs and malformed JSON.
- A live Open Food Facts v3 request returned HTTP 200 and a product.
- Camera scanning, the full Android build and saving through the UI require testing in your Android Studio environment. The existing shared-fridge rules and paths are unchanged.

To rerun the Android unit test after Gradle sync:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.example.smarty.ProductBarcodeParserTest
```
