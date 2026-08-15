# Radio QR — تطبيق بث القرآن الكريم

[English version](#radio-qr--quran-radio-streaming-app)

## نبذة عن المشروع

**Radio QR** هو تطبيق أندرويد مفتوح المصدر للاستماع إلى بث القرآن الكريم عبر الإنترنت من خلال واجهة بسيطة وهادئة، مع التركيز على سهولة التشغيل واستمرارية الاستماع أثناء انتقال المستخدم بين التطبيقات أو إغلاق الشاشة. يهدف المشروع إلى توفير قاعدة قابلة للتطوير يمكن للمطورين والمهتمين بالمشاريع القرآنية الاستفادة منها وبناء مزايا إضافية فوقها.

يعتمد التطبيق على خدمة تشغيل صوتية مرتبطة بجلسة الوسائط في أندرويد، لذلك يمكن أن يستمر التشغيل في الخلفية، كما يعرّف التطبيق صلاحيات الشبكة وخدمة التشغيل الأمامية وقفل الاستيقاظ والإشعارات بما يتناسب مع سيناريو بث الصوت. لا يتضمن المستودع مفاتيح سرية أو ملفات إعدادات محلية أو ملفات البناء الثنائية.

## ما الذي يقدمه التطبيق؟

| المجال | الوصف |
|---|---|
| الاستماع المباشر | تشغيل بث صوتي مباشر عبر الإنترنت من داخل تطبيق أندرويد. |
| التشغيل في الخلفية | استخدام خدمة تشغيل مخصصة للصوت والوسائط للحفاظ على تجربة الاستماع عند الانتقال إلى تطبيق آخر. |
| التحكم بالوسائط | التكامل مع جلسة الوسائط في أندرويد لعرض حالات التشغيل والتحكم بها من الواجهة المناسبة للنظام. |
| الإعدادات والتفضيلات | طبقة مخصصة لإدارة تفضيلات المستخدم وإعدادات التطبيق. |
| البيانات المحلية | استخدام Room لحفظ البيانات المحلية اللازمة للتطبيق وسجلات التشخيص عند الحاجة. |
| تجربة الواجهة | بناء الواجهة باستخدام Jetpack Compose مع دعم بنية قابلة للتوسع والاختبار. |
| الاستعداد لمشكلات الشبكة | فصل منطق البث عن الواجهة، بما يسهل إضافة إعادة المحاولة ومصادر بث متعددة وتحسين التعامل مع حالات الانقطاع. |

## البنية التقنية

| المكوّن | التقنية المستخدمة |
|---|---|
| المنصة | Android |
| لغة البرمجة | Kotlin |
| بناء الواجهة | Jetpack Compose وMaterial 3 |
| تشغيل الصوت | AndroidX Media3 / ExoPlayer |
| التشغيل المستمر | Foreground Service مع MediaSession |
| التخزين المحلي | Room وطبقة تفضيلات للتطبيق |
| الاتصالات | Retrofit وOkHttp وMoshi عند الحاجة إلى خدمات الشبكة |
| البرمجة غير المتزامنة | Kotlin Coroutines |
| الاختبارات | اختبارات وحدة واختبارات واجهة واختبارات Android ضمن المشروع |

## تشغيل المشروع محليًا

1. نزّل المستودع:

   ```bash
   git clone https://github.com/Seifelromy/radio.qr.git
   cd radio.qr
   ```

2. افتح المجلد في **Android Studio** وانتظر اكتمال مزامنة Gradle. استخدم نسخة Android Studio ونسخة JDK المتوافقتين مع إعدادات Gradle الموجودة في المشروع.

3. شغّل التطبيق على محاكي أندرويد أو جهاز حقيقي، وتأكد من السماح بالإشعارات إذا طلب النظام ذلك. يحتاج التطبيق إلى اتصال بالإنترنت حتى يتمكن من الوصول إلى البث المباشر.

4. لبناء نسخة Debug من سطر الأوامر:

   على Linux أو macOS:

   ```bash
   ./gradlew assembleDebug
   ```

   على Windows:

   ```bat
   gradlew.bat assembleDebug
   ```

   ستجد ملف APK الناتج عادةً داخل `app/build/outputs/apk/debug/`.

## ملاحظات مهمة حول البث

يعمل التطبيق كواجهة ومشغل لبث صوتي مباشر؛ لذلك تعتمد جودة الاستماع على توفر مصدر البث وسرعة الاتصال واستقراره. لم يتم تضمين ملف APK أو AAB النهائي داخل المستودع، لأن ملفات البناء قابلة لإعادة الإنشاء من المصدر وتزيد حجم المستودع بلا حاجة. يمكن للمطور ضبط مصدر البث داخل طبقة إدارة البث في المشروع وفق الترخيص والمصدر الذي يختاره، مع الالتزام بحقوق الاستخدام وشروط مزود البث.

يجب عدم وضع مفاتيح API أو كلمات مرور أو ملفات `local.properties` أو ملفات توقيع الإصدار داخل المستودع العام. استخدم متغيرات البيئة أو آليات الأسرار الخاصة ببيئة البناء عند إضافة أي خدمة خارجية.

## المساهمة في المشروع

المساهمات مرحب بها، سواء كانت إصلاحًا لمشكلة، تحسينًا في تجربة الاستخدام، إضافة اختبار، تحسينًا في الوصول، أو تطويرًا في التعامل مع حالات انقطاع الشبكة. قبل إرسال Pull Request، يرجى وصف التغيير بوضوح، اختبار التطبيق على محاكي أو جهاز حقيقي متى أمكن، والتأكد من عدم إضافة بيانات سرية أو ملفات بناء مولدة.

للمقترحات أو الإبلاغ عن مشكلة، أنشئ Issue جديدة مع وصف خطوات إعادة المشكلة، إصدار أندرويد، نوع الجهاز، وسجل الخطأ إن توفر، مع تجنب مشاركة أي معلومات شخصية.

## الترخيص

هذا المشروع مرخّص بموجب **ترخيص MIT**. يتيح الترخيص استخدام الكود ونسخه وتعديله ودمجه ونشره وتوزيعه وترخيصه من الباطن وبيعه، مع الاحتفاظ بإشعار حقوق النشر ونص الترخيص في النسخ أو الأجزاء المهمة من المشروع. يُرجى مراجعة الملف [`LICENSE`](LICENSE) للاطلاع على النص الكامل للترخيص.

ترخيص الكود لا يمنح تلقائيًا حقوقًا على المحتوى أو الشعارات أو مصادر البث الخارجية التي قد تُضاف إلى التطبيق. يتحمل كل مستخدم أو موزّع مسؤولية التأكد من أن استخدامه للمحتوى والخدمات والموارد الخارجية متوافق مع حقوق أصحابها وشروطها.

## إخلاء مسؤولية

هذا المشروع برمجي مفتوح المصدر يهدف إلى تسهيل الوصول إلى بث القرآن الكريم. لا يضمن المشروع توفر أي مصدر بث خارجي أو استمراره، ولا يتحمل المطورون مسؤولية محتوى أو شروط أو انقطاع المصادر التي يختار المستخدم إضافتها. يرجى احترام حقوق النشر والخصوصية وشروط استخدام أي خدمة أو رابط بث.

---

# Radio QR — Quran Radio Streaming App

## Project overview

**Radio QR** is an open-source Android application for listening to a live Quran radio stream over the internet through a calm and focused interface. The project is designed around reliable audio playback and continued listening while the user switches applications or turns off the screen. It also provides a maintainable foundation that developers and community projects can extend with additional Quran-related features.

The app uses an Android media playback service connected to a media session, allowing audio to continue in the background. Its manifest declares the network, foreground-service, wake-lock, and notification capabilities needed for a live audio scenario. The public repository intentionally excludes secrets, local configuration files, and generated binary build outputs.

## Features and scope

| Area | Description |
|---|---|
| Live listening | Plays a live audio stream from inside an Android application. |
| Background playback | Uses a dedicated audio/media service so playback can continue while the user leaves the app. |
| Media controls | Integrates with Android media-session behavior for playback state and system-level controls. |
| Settings and preferences | Includes a dedicated layer for managing application preferences and user settings. |
| Local data | Uses Room for local data required by the app and diagnostic records when applicable. |
| UI foundation | Uses Jetpack Compose and Material 3 with a structure intended to remain testable and extensible. |
| Network resilience | Separates stream-management logic from the UI, making future retry logic, multiple sources, and better offline/error states easier to add. |

## Technology stack

| Component | Technology |
|---|---|
| Platform | Android |
| Language | Kotlin |
| UI | Jetpack Compose and Material 3 |
| Audio playback | AndroidX Media3 / ExoPlayer |
| Persistent playback | Foreground Service with MediaSession |
| Local storage | Room and an application preferences layer |
| Networking | Retrofit, OkHttp, and Moshi where network services are needed |
| Asynchronous work | Kotlin Coroutines |
| Testing | Unit, UI, and Android test sources included in the project |

## Getting started

1. Clone the repository:

   ```bash
   git clone https://github.com/Seifelromy/radio.qr.git
   cd radio.qr
   ```

2. Open the project in **Android Studio** and allow Gradle synchronization to finish. Use Android Studio and JDK versions compatible with the Gradle configuration included in the repository.

3. Run the application on an Android emulator or a physical device. Grant notification permission if Android requests it. An internet connection is required to reach the live stream.

4. Build a Debug APK from the command line:

   On Linux or macOS:

   ```bash
   ./gradlew assembleDebug
   ```

   On Windows:

   ```bat
   gradlew.bat assembleDebug
   ```

   The generated APK is normally placed under `app/build/outputs/apk/debug/`.

## Stream considerations

The application is a client and audio player for a live stream, so listening quality depends on the availability of the selected stream source and the stability of the user’s internet connection. The repository does not include a final APK or AAB because build artifacts can be reproduced from source and unnecessarily increase repository size. Developers may configure the stream source in the project’s stream-management layer, provided that the selected source is legally usable and its provider’s terms are respected.

Never commit API keys, passwords, `local.properties`, signing keys, or other credentials to this public repository. Use environment variables or the secret-management facilities of the chosen build environment when adding an external service.

## Contributing

Contributions are welcome, including bug fixes, UX improvements, accessibility work, tests, and better handling of network interruptions. Before opening a Pull Request, describe the change clearly, test it on an emulator or physical device when possible, and verify that no credentials or generated build files are included.

For an issue report, include reproducible steps, Android version, device model, and relevant logs when available. Remove personal or sensitive information before sharing logs publicly.

## License

This project is licensed under the **MIT License**. The license permits people to use, copy, modify, merge, publish, distribute, sublicense, and sell copies of the code, provided that the copyright notice and license notice remain in copies or substantial portions of the Software. See the [`LICENSE`](LICENSE) file for the complete terms.

The code license does not automatically grant rights to external content, logos, or streaming sources that may be added to the application. Each user or distributor is responsible for ensuring that their use of external content, services, and resources complies with the rights of their owners and applicable terms.

## Disclaimer

This open-source project is intended to make Quran radio streaming more accessible. It does not guarantee the availability or continuity of any external stream source and does not take responsibility for the content, terms, or interruptions of sources configured by users. Respect applicable copyright, privacy, and service terms when using or extending the project.

## References

[1]: https://developer.android.com/jetpack/compose "Android Developers — Jetpack Compose"
[2]: https://developer.android.com/media/media3 "Android Developers — Media3"
[3]: https://developer.android.com/training/data-storage/room "Android Developers — Save data in a local database using Room"
[4]: https://kotlinlang.org/docs/coroutines-overview.html "Kotlin Documentation — Coroutines overview"
[5]: https://docs.github.com/en/repositories/creating-and-managing-repositories/about-repositories "GitHub Docs — About repositories"
