package com.qurantajweed.app

import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

enum class AppLanguage(val code: String) {
    SYSTEM("system"),
    ARABIC("ar"),
    ENGLISH("en");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.find { it.code == code } ?: SYSTEM
    }
}

fun resolveIsArabic(language: AppLanguage): Boolean = when (language) {
    AppLanguage.ARABIC -> true
    AppLanguage.ENGLISH -> false
    AppLanguage.SYSTEM -> Locale.getDefault().language.equals("ar", ignoreCase = true)
}

data class TajweedRuleText(
    val title: String,
    val subtitle: String,
    val description: String,
    val example: String
)

data class AppStrings(
    val isArabic: Boolean,
    val appTitle: String,
    val subtitleTajweed: String,
    val subtitleNormal: String,
    val fridayKahfTitle: String,
    val fridayKahfSubtitle: String,
    val readNowArrow: String,
    val continueReadingPrefix: (String) -> String,
    val continueSubtext: (Int, Int) -> String,
    val continueArrow: String,
    val searchPlaceholder: String,
    val surahsTabTitle: (Int) -> String,
    val bookmarksTabTitle: (Int) -> String,
    val surahTitlePrefix: (String) -> String,
    val meccanLabel: String,
    val medinanLabel: String,
    val versesAndPageMeta: (Int, Int) -> String,
    val pageShort: (Int) -> String,
    val matchingSurahsTitle: (Int) -> String,
    val matchingVersesTitle: (Int, Boolean) -> String,
    val noSearchResults: String,
    val searchVerseHeader: (Int, String, Int) -> String,
    val searchVerseMeta: (Int, Int) -> String,
    val noBookmarksTitle: String,
    val noBookmarksHint: String,
    val bookmarkItemTitle: (String, Int) -> String,
    val removeLabel: String,
    val surahStartLabel: String,
    val surahEndLabel: String,
    val prevSurahButton: (String) -> String,
    val nextSurahButton: (String) -> String,
    val surahsBackButton: String,
    val readerTopSurahTitle: (Int, String) -> String,
    val readerTopSubinfo: (Int, Int, Int) -> String,
    val autoScrollPause: String,
    val autoScrollStart: String,
    val speedLabel: (String) -> String,
    val ayahModalTitle: (String, Int) -> String,
    val removeBookmarkAction: String,
    val saveBookmarkAction: String,
    val quickJumpTitle: String,
    val quickJumpFilterPlaceholder: String,
    val verseNumberLabel: (Int) -> String,
    val openSurahStart: (String) -> String,
    val settingsTitle: String,
    val settingsSubtitle: String,
    val languageSectionTitle: String,
    val langSystem: String,
    val langArabic: String,
    val langEnglish: String,
    val appearanceSectionTitle: String,
    val mushafSectionTitle: String,
    val tajweedModeTitle: String,
    val tajweedModeDesc: String,
    val normalModeTitle: String,
    val normalModeDesc: String,
    val tajweedGuideButton: String,
    val viewArrow: String,
    val fontSizeTitle: String,
    val fontSizeUnit: (Int) -> String,
    val autoScrollSpeedTitle: String,
    val keepScreenOnTitle: String,
    val keepScreenOnDesc: String,
    val enabledBadge: String,
    val supportSectionTitle: String,
    val buyMeACoffeeTitle: String,
    val buyMeACoffeeSubtitle: String,
    val saveAndBackButton: String,
    val tajweedGuideHeader: String,
    val tajweedGuideSubheader: String,
    val closeGuideButton: String,
    val tajweedRules: List<TajweedRuleText>,
    val pressBackAgainToExit: String,
    val bookmarkAddedToast: String,
    val bookmarkRemovedToast: String,
    val themeTitle: (AppTheme) -> String,
    val themeDesc: (AppTheme) -> String
) {
    fun formatNumber(num: Int): String =
        if (isArabic) QuranRepository.toArabicDigits(num) else num.toString()

    fun surahName(surah: SurahInfo): String =
        if (isArabic) surah.nameArabic else ENGLISH_SURAH_NAMES.getOrElse(surah.id - 1) { surah.nameArabic }
}

val ENGLISH_SURAH_NAMES = listOf(
    "Al-Fatihah", "Al-Baqarah", "Ali 'Imran", "An-Nisa", "Al-Ma'idah", "Al-An'am",
    "Al-A'raf", "Al-Anfal", "At-Tawbah", "Yunus", "Hud", "Yusuf",
    "Ar-Ra'd", "Ibrahim", "Al-Hijr", "An-Nahl", "Al-Isra", "Al-Kahf",
    "Maryam", "Taha", "Al-Anbya", "Al-Hajj", "Al-Mu'minun", "An-Nur",
    "Al-Furqan", "Ash-Shu'ara", "An-Naml", "Al-Qasas", "Al-'Ankabut", "Ar-Rum",
    "Luqman", "As-Sajdah", "Al-Ahzab", "Saba", "Fatir", "Ya-Sin",
    "As-Saffat", "Sad", "Az-Zumar", "Ghafir", "Fussilat", "Ash-Shura",
    "Az-Zukhruf", "Ad-Dukhan", "Al-Jathiyah", "Al-Ahqaf", "Muhammad", "Al-Fath",
    "Al-Hujurat", "Qaf", "Adh-Dhariyat", "At-Tur", "An-Najm", "Al-Qamar",
    "Ar-Rahman", "Al-Waqi'ah", "Al-Hadid", "Al-Mujadila", "Al-Hashr", "Al-Mumtahanah",
    "As-Saf", "Al-Jumu'ah", "Al-Munafiqun", "At-Taghabun", "At-Talaq", "At-Tahrim",
    "Al-Mulk", "Al-Qalam", "Al-Haqqah", "Al-Ma'arij", "Nuh", "Al-Jinn",
    "Al-Muzzammil", "Al-Muddaththir", "Al-Qiyamah", "Al-Insan", "Al-Mursalat", "An-Naba",
    "An-Nazi'at", "'Abasa", "At-Takwir", "Al-Infitar", "Al-Mutaffifin", "Al-Inshiqaq",
    "Al-Buruj", "At-Tariq", "Al-A'la", "Al-Ghashiyah", "Al-Fajr", "Al-Balad",
    "Ash-Shams", "Al-Layl", "Ad-Duhaa", "Ash-Sharh", "At-Tin", "Al-'Alaq",
    "Al-Qadr", "Al-Bayyinah", "Az-Zalzalah", "Al-'Adiyat", "Al-Qari'ah", "At-Takathur",
    "Al-'Asr", "Al-Humazah", "Al-Fil", "Quraysh", "Al-Ma'un", "Al-Kawthar",
    "Al-Kafirun", "An-Nasr", "Al-Masad", "Al-Ikhlas", "Al-Falaq", "An-Nas"
)

val ArabicStrings = AppStrings(
    isArabic = true,
    appTitle = "القرآن الكريم",
    subtitleTajweed = "مصحف التجويد الملون • القراءة المستمرة",
    subtitleNormal = "المصحف الشريف (لون موحد) • القراءة المستمرة",
    fridayKahfTitle = "يوم الجمعة • اقرأ سورة الكهف",
    fridayKahfSubtitle = "«نورٌ ما بين الجمعتين» • اضغط للقراءة الآن",
    readNowArrow = "قراءة ◀",
    continueReadingPrefix = { name -> "إكمال القراءة: سورة $name" },
    continueSubtext = { page, verse ->
        "صفحة ${QuranRepository.toArabicDigits(page)} • آية ${QuranRepository.toArabicDigits(verse)}"
    },
    continueArrow = "متابعة ◀",
    searchPlaceholder = "ابحث باسم السورة، رقم الصفحة، أو نص الآية (مثال: الكرسي، ٢:٢٥٥)...",
    surahsTabTitle = { count -> "سور القرآن (${QuranRepository.toArabicDigits(count)})" },
    bookmarksTabTitle = { count -> "العلامات المرجعية (${QuranRepository.toArabicDigits(count)})" },
    surahTitlePrefix = { name -> "سورة $name" },
    meccanLabel = "مكية",
    medinanLabel = "مدنية",
    versesAndPageMeta = { count, page ->
        "آياتها: ${QuranRepository.toArabicDigits(count)} • ص ${QuranRepository.toArabicDigits(page)}"
    },
    pageShort = { page -> "ص ${QuranRepository.toArabicDigits(page)}" },
    matchingSurahsTitle = { count -> "السور المطابقة (${QuranRepository.toArabicDigits(count)})" },
    matchingVersesTitle = { count, capped ->
        "الآيات المطابقة (${QuranRepository.toArabicDigits(count)}${if (capped) "+" else ""})"
    },
    noSearchResults = "لا توجد نتائج مطابقة للبحث",
    searchVerseHeader = { sid, name, vnum ->
        "${QuranRepository.toArabicDigits(sid)}. سورة $name • آية ${QuranRepository.toArabicDigits(vnum)}"
    },
    searchVerseMeta = { page, juz ->
        "الصفحة ${QuranRepository.toArabicDigits(page)} • الجزء ${QuranRepository.toArabicDigits(juz)}"
    },
    noBookmarksTitle = "لا توجد علامات مرجعية محفوظة حالياً",
    noBookmarksHint = "اضغط على رقم أي آية أو اضغط مطولاً على الآية أثناء القراءة لحفظها هنا.",
    bookmarkItemTitle = { name, verse ->
        "سورة $name • الآية ${QuranRepository.toArabicDigits(verse)}"
    },
    removeLabel = "حذف",
    surahStartLabel = "بداية المصحف",
    surahEndLabel = "نهاية المصحف",
    prevSurahButton = { prevName -> "▶  السابقة: $prevName" },
    nextSurahButton = { nextName -> "التالية: $nextName  ◀" },
    surahsBackButton = "السور",
    readerTopSurahTitle = { id, name -> "${QuranRepository.toArabicDigits(id)}. سورة $name" },
    readerTopSubinfo = { page, juz, verse ->
        "صفحة ${QuranRepository.toArabicDigits(page)} • جزء ${QuranRepository.toArabicDigits(juz)} • آية ${QuranRepository.toArabicDigits(verse)}"
    },
    autoScrollPause = "⏸ إيقاف",
    autoScrollStart = "▼ نزول تلقائي",
    speedLabel = { speed -> "السرعة $speed" },
    ayahModalTitle = { name, verse -> "سورة $name • آية ${QuranRepository.toArabicDigits(verse)}" },
    removeBookmarkAction = "إزالة العلامة المرجعية ✕",
    saveBookmarkAction = "حفظ علامة مرجعية 🔖",
    quickJumpTitle = "الانتقال السريع للسورة والآية",
    quickJumpFilterPlaceholder = "تصفية السور بالاسم أو الرقم...",
    verseNumberLabel = { v -> "آية ${QuranRepository.toArabicDigits(v)}" },
    openSurahStart = { name -> "فتح بداية سورة $name" },
    settingsTitle = "الإعدادات والمظهر",
    settingsSubtitle = "اللغة، المظهر، نمط المصحف والخط",
    languageSectionTitle = "لغة التطبيق",
    langSystem = "النظام (تلقائي)",
    langArabic = "العربية",
    langEnglish = "English",
    appearanceSectionTitle = "سمة التطبيق وألوان القراءة",
    mushafSectionTitle = "المصحف والقراءة",
    tajweedModeTitle = "مصحف التجويد الملون",
    tajweedModeDesc = "تلوين الحروف وفق أحكام التجويد السبعة",
    normalModeTitle = "المصحف العادي",
    normalModeDesc = "لون واحد موحد لكافة الحروف",
    tajweedGuideButton = "دليل ألوان أحكام التجويد",
    viewArrow = "عرض ◀",
    fontSizeTitle = "حجم خط الآيات (Aa)",
    fontSizeUnit = { pt -> "${QuranRepository.toArabicDigits(pt)} نقطة" },
    autoScrollSpeedTitle = "سرعة النزول التلقائي",
    keepScreenOnTitle = "إبقاء الشاشة مضاءة دائماً",
    keepScreenOnDesc = "مفعل تلقائياً أثناء التلاوة",
    enabledBadge = "مفعل ✓",
    supportSectionTitle = "الدعم والمساهمة",
    buyMeACoffeeTitle = "ادعم المطور (Buy Me a Coffee)",
    buyMeACoffeeSubtitle = "buymeacoffee.com/hxh3mk",
    saveAndBackButton = "تم الحفظ والرجوع",
    tajweedGuideHeader = "دليل ألوان أحكام التجويد",
    tajweedGuideSubheader = "الأحكام السبعة المعتمدة في مصاحف التجويد",
    closeGuideButton = "إغلاق الدليل",
    tajweedRules = listOf(
        TajweedRuleText(
            title = "المد اللازم (٦ حركات)",
            subtitle = "أحمر داكن • أطول المدود",
            description = "المد اللازم الكلمي والحرفي، المثقل والمخفف.",
            example = "مثل: الحَاقَّةُ، دَابَّةٍ، الٓمٓ، قٓ"
        ),
        TajweedRuleText(
            title = "المد الواجب والمنفصل (٤ أو ٥ حركات)",
            subtitle = "برتقالي • مد متصل ومنفصل",
            description = "المد المتصل والمد المنفصل ومد الصلة الكبرى.",
            example = "مثل: جَآءَ، سُوٓءَ، بِمَآ أُنزِلَ، أَنفُسَكُمْ"
        ),
        TajweedRuleText(
            title = "المد الجائز والبدل والصلة (٢ أو ٤ أو ٦ حركات)",
            subtitle = "وردي • عارض وبدل ولين",
            description = "المد العارض للسكون، ومد البدل، ومد اللين.",
            example = "مثل: الرَّحِيمِ، ءَامَنُوا، إِيمَانًا، خَوْفٍ"
        ),
        TajweedRuleText(
            title = "الغنة والإخفاء والإقلاب (حركتان)",
            subtitle = "أخضر • غنن ونون ساكنة وتنوين",
            description = "الإخفاء الحقيقي، والإقلاب، والإدغام بغنة، والميم المشددة.",
            example = "مثل: مَن يَقُولُ، مِن بَعْدِ، كُنتُمْ، إِنسَانٌ"
        ),
        TajweedRuleText(
            title = "القلقلة (ق - ط - ب - ج - د)",
            subtitle = "أزرق • حروف قطب جد عند السكون",
            description = "قلقلة صغرى في وسط الكلمة، وقلقلة كبرى عند الوقف.",
            example = "مثل: الفَلَقِ، يَقْطَعُونَ، أَحَدْ، مُحِيطٌ"
        ),
        TajweedRuleText(
            title = "التفخيم",
            subtitle = "سماوي • الحروف المفخمة",
            description = "تفخيم حرف الراء، ولام لفظ الجلالة، وأحرف الاستعلاء (خُصَّ ضَغْطٍ قِظْ).",
            example = "مثل: ٱللَّهَ، قَالُوا، غَفُورٌ، خَالِدِينَ"
        ),
        TajweedRuleText(
            title = "الحروف التي لا تُلفظ (ساقطة)",
            subtitle = "رمادي • لا صوت لها عند التلاوة",
            description = "همزة الوصل عند الوصل، واللام الشمسية، وحروف الإدغام الكامل.",
            example = "مثل: ٱلشَّمْسِ، مِّن رَّبِّهِمْ، قَالُوا۟ ٱدْعُ"
        )
    ),
    pressBackAgainToExit = "اضغط مرة أخرى للخروج من المصحف",
    bookmarkAddedToast = "تم حفظ الآية الكريمة في العلامات المرجعية",
    bookmarkRemovedToast = "تمت إزالة العلامة المرجعية",
    themeTitle = { theme -> theme.titleArabic },
    themeDesc = { theme -> theme.descriptionArabic }
)

val EnglishStrings = AppStrings(
    isArabic = false,
    appTitle = "The Noble Quran",
    subtitleTajweed = "Color-Coded Tajweed • Continuous Reading",
    subtitleNormal = "Standard Mushaf (Monochrome) • Continuous Reading",
    fridayKahfTitle = "Friday • Read Surah Al-Kahf",
    fridayKahfSubtitle = "«A light between the two Fridays» • Tap to read now",
    readNowArrow = "Read ▶",
    continueReadingPrefix = { name -> "Continue Reading: $name" },
    continueSubtext = { page, verse -> "Page $page • Ayah $verse" },
    continueArrow = "Resume ▶",
    searchPlaceholder = "Search by Surah name, page number, or Ayah text (e.g. 2:255)...",
    surahsTabTitle = { count -> "Surahs ($count)" },
    bookmarksTabTitle = { count -> "Bookmarks ($count)" },
    surahTitlePrefix = { name -> name },
    meccanLabel = "Meccan",
    medinanLabel = "Medinan",
    versesAndPageMeta = { count, page -> "$count Ayahs • p. $page" },
    pageShort = { page -> "p. $page" },
    matchingSurahsTitle = { count -> "Matching Surahs ($count)" },
    matchingVersesTitle = { count, capped -> "Matching Ayahs ($count${if (capped) "+" else ""})" },
    noSearchResults = "No matching results found",
    searchVerseHeader = { sid, name, vnum -> "$sid. $name • Ayah $vnum" },
    searchVerseMeta = { page, juz -> "Page $page • Juz $juz" },
    noBookmarksTitle = "No saved bookmarks yet",
    noBookmarksHint = "Tap any Ayah number or long-press an Ayah while reading to save it here.",
    bookmarkItemTitle = { name, verse -> "$name • Ayah $verse" },
    removeLabel = "Remove",
    surahStartLabel = "Start of Mushaf",
    surahEndLabel = "End of Mushaf",
    prevSurahButton = { prevName -> "◀  Prev: $prevName" },
    nextSurahButton = { nextName -> "Next: $nextName  ▶" },
    surahsBackButton = "Surahs",
    readerTopSurahTitle = { id, name -> "$id. $name" },
    readerTopSubinfo = { page, juz, verse -> "Page $page • Juz $juz • Ayah $verse" },
    autoScrollPause = "⏸ Pause",
    autoScrollStart = "▼ Auto-Scroll",
    speedLabel = { speed -> "Speed $speed" },
    ayahModalTitle = { name, verse -> "$name • Ayah $verse" },
    removeBookmarkAction = "Remove Bookmark ✕",
    saveBookmarkAction = "Save Bookmark 🔖",
    quickJumpTitle = "Quick Jump to Surah & Ayah",
    quickJumpFilterPlaceholder = "Filter Surahs by name or number...",
    verseNumberLabel = { v -> "Ayah $v" },
    openSurahStart = { name -> "Open start of $name" },
    settingsTitle = "Settings & Appearance",
    settingsSubtitle = "Language, Theme, Mushaf style & Font",
    languageSectionTitle = "App Language",
    langSystem = "System",
    langArabic = "العربية",
    langEnglish = "English",
    appearanceSectionTitle = "Theme & Reading Colors",
    mushafSectionTitle = "Mushaf & Reading",
    tajweedModeTitle = "Color-Coded Tajweed",
    tajweedModeDesc = "Color letters by the 7 Tajweed rules",
    normalModeTitle = "Standard Mushaf",
    normalModeDesc = "Uniform single color for all text",
    tajweedGuideButton = "Tajweed Color Guide",
    viewArrow = "View ▶",
    fontSizeTitle = "Ayah Font Size (Aa)",
    fontSizeUnit = { pt -> "$pt pt" },
    autoScrollSpeedTitle = "Auto-Scroll Speed",
    keepScreenOnTitle = "Keep Screen Always On",
    keepScreenOnDesc = "Enabled automatically while reading",
    enabledBadge = "Active ✓",
    supportSectionTitle = "Support",
    buyMeACoffeeTitle = "Support Developer (Buy Me a Coffee)",
    buyMeACoffeeSubtitle = "buymeacoffee.com/hxh3mk",
    saveAndBackButton = "Save & Close",
    tajweedGuideHeader = "Tajweed Color Guide",
    tajweedGuideSubheader = "The 7 standard color-coded Tajweed rules",
    closeGuideButton = "Close Guide",
    tajweedRules = listOf(
        TajweedRuleText(
            title = "Madd Lazim (6 counts)",
            subtitle = "Deep Red • Longest elongation",
            description = "Necessary word & letter elongation (heavy and light).",
            example = "e.g.: الحَاقَّةُ، دَابَّةٍ، الٓمٓ، قٓ"
        ),
        TajweedRuleText(
            title = "Madd Wajib & Munfasil (4–5 counts)",
            subtitle = "Orange • Connected & separated elongation",
            description = "Obligatory connected, permissible separated, and major Silah.",
            example = "e.g.: جَآءَ، سُوٓءَ، بِمَآ أُنزِلَ، أَنفُسَكُمْ"
        ),
        TajweedRuleText(
            title = "Madd Jaiz, Badal & Silah (2, 4, or 6 counts)",
            subtitle = "Pink • Temporary, substitute & soft",
            description = "Elongation due to pause, substitute Madd, and Leen.",
            example = "e.g.: الرَّحِيمِ، ءَامَنُوا، إِيمَانًا، خَوْفٍ"
        ),
        TajweedRuleText(
            title = "Ghunnah, Ikhfaa & Iqlab (2 counts)",
            subtitle = "Green • Nasalization & Noon Sakinah",
            description = "Concealment, conversion, Idgham with Ghunnah, and doubled Meem.",
            example = "e.g.: مَن يَقُولُ، مِن بَعْدِ، كُنتُمْ، إِنسَانٌ"
        ),
        TajweedRuleText(
            title = "Qalqalah (ق - ط - ب - ج - د)",
            subtitle = "Blue • Echoing consonant on Sukoon",
            description = "Minor Qalqalah mid-word and major Qalqalah when stopping.",
            example = "e.g.: الفَلَقِ، يَقْطَعُونَ، أَحَدْ، مُحِيطٌ"
        ),
        TajweedRuleText(
            title = "Tafkheem (Heavy Letters)",
            subtitle = "Cyan • Emphatic pronunciation",
            description = "Heavy Raa, Lam in Allah's name, and elevated letters (خص ضغط قظ).",
            example = "e.g.: ٱللَّهَ، قَالُوا، غَفُورٌ، خَالِدِينَ"
        ),
        TajweedRuleText(
            title = "Silent / Unpronounced Letters",
            subtitle = "Grey • Not pronounced when connecting",
            description = "Hamzat Al-Wasl in flow, Lam Shamsiyyah, and complete Idgham.",
            example = "e.g.: ٱلشَّمْسِ، مِّن رَّبِّهِمْ، قَالُوا۟ ٱدْعُ"
        )
    ),
    pressBackAgainToExit = "Press back again to exit",
    bookmarkAddedToast = "Ayah saved to bookmarks",
    bookmarkRemovedToast = "Bookmark removed",
    themeTitle = { theme ->
        when (theme) {
            AppTheme.OLED -> "OLED Black"
            AppTheme.DARK -> "Dark Slate"
            AppTheme.SEPIA -> "Warm Sepia"
            AppTheme.LIGHT -> "Daylight White"
        }
    },
    themeDesc = { theme ->
        when (theme) {
            AppTheme.OLED -> "Pure 100% black, battery saving"
            AppTheme.DARK -> "Calm dark navy background"
            AppTheme.SEPIA -> "Warm paper, reduces eye strain"
            AppTheme.LIGHT -> "Crisp white with dark ink"
        }
    }
)

val LocalAppStrings = staticCompositionLocalOf { ArabicStrings }
