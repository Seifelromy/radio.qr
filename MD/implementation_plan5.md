# Implementation Plan - App Icon, Tab Adjustments, and Card Spacing Refinements

This plan details the changes required to update the application icon to the new gold crescent and Quran design, simplify the navigation tabs by removing "روائع التلاوات" from the "الأذكار" tab, place the Rosary ("المسبحة") as the first tab in the Adhkar tab row, and optimize visual spacing across the Home screen cards.

---

## User Review Required

> [!IMPORTANT]
> - **Icon Replacement Method:** We will process the high-resolution source icon `quran_radio_cairo_icon_1781434381465.png` in the project root using a temporary compiled Java utility. This utility will crop the center `640x640` icon, apply circular/rounded rectangle masks, filter out the green gradient background to generate a transparent adaptive foreground, scale them to standard densities, and delete old legacy `.webp` files to avoid cache conflicts.
> - **Navigation Tab Hierarchy Simplification:** Removing the "روائع التلاوات" sub-tab simplifies the "الأذكار" section to display the adhkar and rosary screens directly, keeping the design clean and focused.

---

## Proposed Changes

### 1. Resource Assets & App Icon Configuration

#### [MODIFY] [ic_launcher.xml](file:///e:/app/radio.qr/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml)
#### [MODIFY] [ic_launcher_round.xml](file:///e:/app/radio.qr/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml)

- Point the adaptive foreground drawable path to the new PNG launcher resource:
  ```xml
  <foreground android:drawable="@mipmap/ic_launcher_foreground" />
  <monochrome android:drawable="@mipmap/ic_launcher_foreground" />
  ```

#### [NEW] Temporary Java Icon Generator Utility
- Create a temporary class in the root directory `e:\app\radio.qr\ProcessIconResources.java` to perform:
  - **Cropping:** Extract the `640x640` square centered at `(512, 512)` from `quran_radio_cairo_icon_1781434381465.png`.
  - **Legacy Icon:** Apply a rounded-rectangle mask with `136px` corner radius.
  - **Legacy Round Icon:** Apply a perfect circular mask.
  - **Adaptive Foreground:** Filter the green background gradient using a chroma-key mask:
    - Target pixels with dominant green (where `g > 20 && g > r - 12 && g > b - 12 && (r+g+b)/3 < 185 && r-g < 18`) and replace them with fully transparent pixels (`0x00000000`).
  - **Downscaling:** Resize these assets to standard Android resource densities:
    - `mdpi` (48x48 legacy/round, 108x108 foreground)
    - `hdpi` (72x72 legacy/round, 162x162 foreground)
    - `xhdpi` (96x96 legacy/round, 216x216 foreground)
    - `xxhdpi` (144x144 legacy/round, 324x324 foreground)
    - `xxxhdpi` (192x192 legacy/round, 432x432 foreground)
  - **Cache Cleaning:** Programmatically locate and delete `ic_launcher.webp` and `ic_launcher_round.webp` in all `res/mipmap-*` folders to ensure the new PNGs are loaded.

---

### 2. Tab Navigation & Screen Refactoring

#### [MODIFY] [MainActivity.kt](file:///e:/app/radio.qr/app/src/main/java/com/example/MainActivity.kt)

- **Sub-Tab Removal:**
  - Simplify `LibraryScreen` by removing the sub-tabs `TabRow` (which toggled between "روائع التلاوات" and "الأذكار والتسبيح").
  - Render `AdhkarScreen(viewModel)` directly inside `LibraryScreen`.
- **Rosary Re-ordering:**
  - Shift `المسبحة` (index 5) to index 0 in the `ScrollableTabRow` title list inside `AdhkarScreen`:
    `listOf("المسبحة", "أذكار الصباح", "أذكار المساء", "أذكار النوم", "أذكار الاستيقاظ", "أذكار بعد الصلاة")`
  - Update `if (selectedTabGroup == 5)` display check to `if (selectedTabGroup == 0)`.
  - Re-align indices inside the `when(selectedTabGroup)` lists for morning, evening, sleep, and after-prayer adhkar (e.g. index 1 maps to `morningAdhkar`, index 2 to `eveningAdhkar`, etc.).
  - Shift the `text = when (selectedTabGroup)` title labels inside the Adhkar list headers to correspond with the new indices.

---

### 3. Spacing & Card Padding Reductions

#### [MODIFY] [MainActivity.kt](file:///e:/app/radio.qr/app/src/main/java/com/example/MainActivity.kt)

- **Home Screen Spacing (`PlayerScreen`):**
  - Reduce `spacedBy(16.dp)` in `LazyColumn` to `spacedBy(12.dp)`.
  - Reduce `contentPadding` in `LazyColumn` from `16.dp` to `12.dp`.
- **Card Padding & Heights:**
  - **Location & Dates Card:** Reduce internal padding from `18.dp` to `14.dp` and shrink height spacers between headers and date text from `14.dp` to `10.dp`.
  - **Next Prayer Card:** Reduce internal padding from `18.dp` to `14.dp` and height spacers from `14.dp` to `10.dp`.
  - **Sticky Bottom Player Card:** Reduce vertical padding from `14.dp` to `10.dp` and horizontal padding from `20.dp` to `16.dp`.

---

## Verification Plan

### Automated Verification
1. **Compile & Run Java Utility:** Execute the compiled Java program to generate and install the launcher PNG files.
2. **Build Diagnostics:** Run `./gradlew.bat compileDebugKotlin` to ensure Composable signatures and Kotlin code build successfully.
3. **Apk Assembly:** Run `./gradlew.bat assembleRelease` to verify that the Gradle build generates the final release package.

### Manual Verification
- Deploy to the emulator / device.
- Confirm the new Crescent/Quran app icon appears on the device home screen and launcher.
- Check the "الأذكار" screen to verify that "روائع التلاوات" is gone and the screen loads the Adhkar sections directly.
- Verify that "المسبحة" is the first tab on the Adhkar page and works as expected.
- Visually verify that spacing and paddings on the main screen feel compact and premium.
