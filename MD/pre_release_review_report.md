# PRE-RELEASE PRODUCT REVIEW & REFINEMENT PLAN
**Egyptian Holy Quran Live Radio — Public Preview Candidate**

Under the supervision of: **Senior Mobile Architect & UI/UX Expert**
Date: June 14, 2026

---

## 1. Branding Review & Recommendations

The application currently contains development-era branding ("Mizan" / "Mizan Radio"). Per user instruction, the application will be renamed to **"إذاعة القرآن الكريم"** (Holy Quran Radio). 

### Identified Brand References to Remove:
* **[strings.xml](file:///e:/app/radio.qr/app/src/main/res/values/strings.xml)**:
  * `<string name="app_name">Mizan Radio</string>` ➡️ Change to `إذاعة القرآن الكريم`
  * `<string name="about_app_title">نبذة عن راديو ميزان</string>` ➡️ Change to `نبذة عن التطبيق`
  * `<string name="about_app_desc">... راديو ميزان ...</string>` ➡️ Change references of "راديو ميزان" to "تطبيق إذاعة القرآن الكريم".
  * `<string name="privacy_policy_content">...</string>` & `<string name="terms_of_use_content">...</string>` ➡️ Replace all occurrences of "راديو ميزان" with "تطبيق إذاعة القرآن الكريم".
* **[MainActivity.kt](file:///e:/app/radio.qr/app/src/main/java/com/example/MainActivity.kt)**:
  * Line 474: `text = "مِيزَانُ الرَّادْيُو"` ➡️ Change to `text = "إِذَاعَةُ القُرْآنِ الكَرِيمِ"` (styled with premium tashkeel).
  * Line 2350: `text = "ميزان راديو القرآن الكريم"` ➡️ Change to `text = "إذاعة القرآن الكريم من القاهرة"`.
  * Line 1439: `"... - شارك من تطبيق راديو ميزان"` ➡️ Change to `"... - شارك من تطبيق إذاعة القرآن الكريم"`.
* **[PlaybackService.kt](file:///e:/app/radio.qr/app/src/main/java/com/example/playback/PlaybackService.kt)**:
  * Line 192: `.setAlbumTitle("برنامج راديو ميزان للبث الرقمي")` ➡️ Change to `.setAlbumTitle("إذاعة القرآن الكريم من القاهرة")`.

---

## 2. Auto-Play Experience

To create a seamless "plug-and-play" experience for first-time and elderly users, the application will automatically start streaming upon launch.

### Implementation Plan:
1. **Initialize Playback on Connection**:
   Inside `MainViewModel.kt`'s `initMediaController()` listener, once the `MediaController` connects successfully to the background `PlaybackService`:
   * Check if the player is currently idle and not already playing (to prevent interrupting existing background playback when reopening the app).
   * If it is idle, fetch the user's preferred stream (defaulting to the primary official stream) and call `playStream(active)`.
2. **Respect User Controls**:
   * Auto-play is only triggered *once* during ViewModel initialization on application startup.
   * If the user manually pauses the player, the state remains paused. Reopening the app from the background does not trigger auto-play again, preserving standard Android UX principles.

---

## 3. Stream Source Audit

We audited the four default streams configured in `AppDatabase.kt` to ensure they genuinely broadcast the Egyptian Holy Quran Radio station.

### Stream Assessment Table:

| ID | Name & URL | Verification | Decision & Justification |
| :--- | :--- | :--- | :--- |
| **1** | **Official Primary Stream**<br>`https://stream.radiojar.com/8s5u5tpdtwzuv` | **Genuine** | **KEEP**: Official Radiojar stream broadcasting Cairo Quran Radio. Most stable source. |
| **2** | **Official Backup Stream (Zeno)**<br>`https://stream.zeno.fm/juwfhuodjgmuv` | **Unofficial / Commercial** | **REMOVE**: An unofficial community relay hosted on Zeno.fm. It injects commercial audio ads on connection and suffers from high latency/downtime. |
| **3** | **Mirror Backup Stream**<br>`http://live.mp3quran.net:9722` | **Genuine** | **KEEP**: Reliable direct broadcast relay operated by the reputable MP3Quran network. |
| **4** | **Community Backup Stream**<br>`http://66.45.232.131:9994/;stream.mp3` | **Genuine** | **KEEP**: Direct Icecast server relaying Cairo Quran Radio. Serves as a perfect emergency fallback. |

### Database Seeding Changes:
We will remove Zeno from the default list in `AppDatabase.getDefaultStreams()`, leaving exactly three verified channels:
1. Primary Stream (Radiojar) - Rank 1
2. Mirror Stream (MP3Quran) - Rank 2
3. Emergency Stream (Community Icecast) - Rank 3

---

## 4. Navigation Review

### Tab Rename Recommendation:
* **Current Title**: "القرآن والأذكار" (Quran & Adhkar)
* **Status**: The Quran section only contains 6 static historical audio files (Surah Yaseen, Al-Rahman, Al-Kahf, Al-Hujurat, Al-Mulk, and Qisar al-Suwar). It does not contain a complete interactive Quran browser (reading pages, all 114 Surahs, translations).
* **Recommendation**: Rename the second bottom navigation tab to **"الأذكار"** (Adhkar) to prevent misleading users. Inside the tab, we will rename the sub-tabs to:
  * Sub-tab 1: "روائع التلاوات" (Choice Recitations) or "تسجيلات عطرة" (Fragrant Recitations).
  * Sub-tab 2: "الأذكار والتسبيح" (Adhkar & Tasbih).

---

## 5. UI Simplification (Home Screen Clean-Up)

The home screen currently displays technical debug statistics such as stream latencies in milliseconds and status indicators like "تالف" (damaged/broken). This clutter disrupts the spiritual, peaceful nature of the application.

### Refining the Home Screen:
1. **Remove Stream Switcher Grid from Home**:
   * Hide the backup stream selector card and its real-time latency millisecond text from the player tab.
   * Move this layout into the **Developer Diagnostics Panel** accessible via developer mode in Settings.
2. **Keep Home Screen Beautiful & Peaceful**:
   * The home tab will now only contain:
     1. App Header with the new elegant title: `إِذَاعَةُ القُرْآنِ الكَرِيمِ`.
     2. Hijri & Gregorian date display card.
     3. Elegant Prayer Times Countdown & Schedule Drawer.
     4. Premium Audio Player card with the stream title, status indicator, play/pause controls, and the animated audio waveform visualizer.

---

## 6. Prayer Card Wording Improvements

The current wording on the prayer times card can be improved to feel warmer and more natural to Arabic-speaking listeners.

### Wording Refinements:

* **Current Wording (Active Prayer)**:
  `الآن صلاة: [اسم الصلاة]` (Now Prayer: [Name])
  * *Critique*: Incorrect when the prayer time entered hours ago.
  * *Revised Suggestion*: `دخل وقت صلاة [اسم الصلاة]` (Prayer time has entered) or `أُقيمت صلاة [اسم الصلاة]` (Prayer was established).

* **Current Countdown Format**:
  `متبقي %d س و %d د على %s`
  * *Critique*: The letters "س" (hours) and "د" (minutes) look like development shorthand.
  * *Revised Suggestion*: Use full classical words:
    * `باقي على أذان %s: %d ساعة و %d دقيقة` (Remaining to Adhan [Name]: %d hours and %d minutes)
    * `باقي على أذان %s: %d دقيقة` (if remaining time is under an hour)

---

## 7. Recommended Action Plan (Next Steps)

Once this Pre-Release Plan is approved, we will execute the following code changes:
1. Modify `AppDatabase.kt` to seed exactly 3 streams, completely deleting Zeno.
2. Clean `MainActivity.kt`'s `PlayerScreen` to remove the technical streams grid and millisecond display.
3. Update `MainViewModel.kt`'s `initMediaController()` to initiate auto-play on launch.
4. Replace all string references to "Mizan" and "Mizan Radio" with "إذاعة القرآن الكريم".
5. Rename navigation items and adjust prayer card wording for premium localization.
6. Verify release APK/AAB compilation and package generation.
