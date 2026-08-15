# UX Audit Report

This report presents a thorough UX and UI audit of **Mizan Radio**, identifying visual, typographic, layout, and usability weaknesses.

---

## 1. Visual Walkthrough & Screenshots

The screenshots below show the state of the application after Phase 1 and prior to the redesign.

````carousel
![1. Splash / Booting warning](file:///C:/Users/الكمبيوتر/.gemini/antigravity-ide/brain/29553027-48db-4d3d-a3fc-48a92c67c17a/home_screen_fresh.png)
<!-- slide -->
![2. System UI dialog shown](file:///C:/Users/الكمبيوتر/.gemini/antigravity-ide/brain/29553027-48db-4d3d-a3fc-48a92c67c17a/home_screen_active.png)
<!-- slide -->
![3. Home Screen (Scrolled showing Play Button)](file:///C:/Users/الكمبيوتر/.gemini/antigravity-ide/brain/29553027-48db-4d3d-a3fc-48a92c67c17a/home_screen_scrolled.png)
<!-- slide -->
![4. Home Screen (Active Playback and Waveform)](file:///C:/Users/الكمبيوتر/.gemini/antigravity-ide/brain/29553027-48db-4d3d-a3fc-48a92c67c17a/home_screen_playing_latest.png)
<!-- slide -->
![5. Quran Recitations Screen](file:///C:/Users/الكمبيوتر/.gemini/antigravity-ide/brain/29553027-48db-4d3d-a3fc-48a92c67c17a/quran_screen.png)
<!-- slide -->
![6. Morning/Evening Adhkar Screen](file:///C:/Users/الكمبيوتر/.gemini/antigravity-ide/brain/29553027-48db-4d3d-a3fc-48a92c67c17a/adhkar_screen.png)
<!-- slide -->
![7. Electronic Subhah (Tasbih) Screen](file:///C:/Users/الكمبيوتر/.gemini/antigravity-ide/brain/29553027-48db-4d3d-a3fc-48a92c67c17a/subhah_screen.png)
<!-- slide -->
![8. Settings Screen (Preferred Stream Bug)](file:///C:/Users/الكمبيوتر/.gemini/antigravity-ide/brain/29553027-48db-4d3d-a3fc-48a92c67c17a/settings_screen.png)
<!-- slide -->
![9. Settings Screen Scrolled (Technical Diagnostics logs)](file:///C:/Users/الكمبيوتر/.gemini/antigravity-ide/brain/29553027-48db-4d3d-a3fc-48a92c67c17a/settings_scrolled.png)
````

---

## 2. List of Confirmed Issues

The audit identified **6 confirmed bugs and weaknesses** in the current interface.

### Issue 1: Play/Pause Button Cut-off (Critical Layout Bug)
- **Problem:** On initial launch, the large circular Play button is positioned inside a scrollable `LazyColumn`. Because the cards above it (Brand banner, Date Card, and Prayer Times Card) are too tall, the Play button is pushed off-screen behind the bottom navigation bar. Only a tiny green curve is visible.
- **Usability Impact:** Users are unaware that there is a play button. They must manually swipe up to discover it. The play button should be fixed, sticky, or positioned in a way that is immediately accessible without scrolling.

### Issue 2: Arabic Typographic Distortions (Rendering Bug)
- **Problem:** Surah titles on the Quran screen are declared using standard diacritics and kashidas: `سُـورة` (with damma and kashida). The font engine/system font renders the combination of these characters distortedly. It renders the `س` as `ش`, making the text appear as `شُورة` (Shurah) instead of `سُورة` (Surah).
- **Usability Impact:** Typographical and spelling error in holy words.

### Issue 3: Bidirectional RTL Number Reversal (Layout Bug)
- **Problem:** On the Adhkar cards, the tap count status is formatted as `"$localTapCount / ${item.countTarget}"`. Due to default bidirectional formatting rules in RTL layouts, the slash character `/` reverses the layout order of numerical bounds, rendering as `Target / Count` (e.g. `1 / 0` when count is 0 and target is 1).
- **Usability Impact:** Users think they have already completed 1 tap out of 0. Tapping it updates it to `1 / 1`.

### Issue 4: Preferred Stream Infinite Loading Spinner (Logic Bug)
- **Problem:** `SettingsRepository.kt` defines the default preferred stream URL as `https://stream.radiojar.com/0u5beuqm0uquv`. However, the production streams database defines the primary stream URL as `https://stream.radiojar.com/8s5u5tpdtwzuv`. Because they do not match, `preferredStream` evaluates to null on startup, and the button renders `جاري التحميل...` (Loading...) forever.
- **Usability Impact:** Users cannot see their active selected stream and think the app is broken/still loading configs.

### Issue 5: Electronic Subhah Phrase Selection Inconsistency (Settings/UI Bug)
- **Problem:** The default selected phrase of the electronic Tasbih is `"سبحان الله وبحمده"`. However, the 4 quick selection cards below are: `"سبحان الله"`, `"الحمد لله"`, `"أستغفر الله"`, `"الله أكبر"`. Because of this mismatch, none of the cards are highlighted initially.
- **Usability Impact:** Visual inconsistency.

### Issue 6: Expanded Diagnostics Logs Layout Overflow (Accessibility Bug)
- **Problem:** When the Technical Diagnostics log panel is expanded on the Settings screen, the scroll behavior of the main screen and inner list conflicts. The logs list gets cut off behind the bottom navigation bar.
- **Usability Impact:** Technical audit logs are difficult to view on medium/small phone screens.

---

## 3. Recommendations for Phase 3 UI Redesign

To elevate Mizan Radio to a premium, world-class experience, we recommend the following visual and structural improvements:

### A. Layout Structure & Navigation
- **Floating Player Sheet:** Introduce a persistent, sleek floating mini-player at the bottom (just above the navigation bar) that remains visible on all tabs. This solves the Play button cut-off issue.
- **Sticky Home Elements:** Make the primary playback control a fixed card or floating action button, separate from the scrollable list.
- **Simplify Banners:** Remove the large Crescent vector header to save vertical space. Place the brand badge on the top toolbar.

### B. Typography & RTL Polish
- **Clean Arabic Text:** Remove diacritics (`ُ`) and kashidas (`ـ`) from UI words like `سورة` to prevent font engine glyph distortions. Write them as standard `سورة`.
- **RTL Number Formatting:** Wrap bidirectional texts with unicode direction isolation blocks or format counters using localized words instead of raw slashes (e.g., `العدد: 0 من 1` or explicitly specifying `LayoutDirection.Ltr` for numbers).

### C. Aesthetic Improvements
- **Harmonious Color Palette:** Use dark modes with deep emerald accents (`#0f5132`) and rich gold details (`#ffd700`) rather than standard gray cards.
- **Smooth Micro-Animations:** Animate tab transitions, wave animations, and tap interactions with spring physics to give the application a premium feel.
