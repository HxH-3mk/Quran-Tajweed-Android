package com.quranwbw.portable

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.LruCache
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class SurahInfo(
    val id: Int,
    val nameArabic: String,
    val isMeccan: Boolean,
    val versesCount: Int,
    val startPage: Int,
    val endPage: Int,
    val iconChar: String,
    val headerChar: String,
    val normalizedName: String
)

data class VerseData(
    val surahId: Int,
    val verseNumber: Int,
    val page: Int,
    val juz: Int,
    val hizb: Int,
    val words: List<String>,
    val endGlyph: String,
    val uthmaniText: String,
    val normalizedText: String
) {
    val verseKey: String get() = "$surahId:$verseNumber"
}

data class BookmarkEntry(
    val surahId: Int,
    val verseNumber: Int,
    val timestamp: Long
) {
    val verseKey: String get() = "$surahId:$verseNumber"
}

object TajweedPalette {
    // Official CPAL Palette 1 (OLED / Dark Mode Tajweed Palette)
    // Guaranteed #FFFFFF pure white for normal letters (0, 13, 14) and vibrant Tajweed colors on #000000 OLED black
    val DARK_COLORS = intArrayOf(
        Color.parseColor("#FFFFFF"), // 0: White (Normal Quran letters)
        Color.parseColor("#9D9999"), // 1: Grey (Silent letters / Hamzat Wasl)
        Color.parseColor("#999999"), // 2: Grey (Idgham / Silent)
        Color.parseColor("#E30000"), // 3: Red (Madd Necessary / Lazim)
        Color.parseColor("#FF8E3B"), // 4: Orange (Madd Obligatory / Wajib)
        Color.parseColor("#FFC1E0"), // 5: Pink (Madd Permissible / Jaiz)
        Color.parseColor("#26B55D"), // 6: Green (Ghunnah / Ikhfaa)
        Color.parseColor("#3C84D5"), // 7: Blue (Qalqalah)
        Color.parseColor("#00DEFF"), // 8: Cyan (Iqlab / Tafkheem)
        Color.parseColor("#FF5E8E"), // 9: Rose (Madd Normal)
        Color.parseColor("#009B68"), // 10: Emerald (Ayah ornament outer ring / Surah header frame)
        Color.parseColor("#AA8A1C"), // 11: Gold (Ayah ornament accents / Surah header frame)
        Color.parseColor("#2E544F"), // 12: Dark Teal (Ayah ornament inner fill)
        Color.parseColor("#FFFFFF"), // 13: White (Ayah ornament digits / Surah header calligraphy)
        Color.parseColor("#FBFFFF"), // 14: Pure White
        Color.parseColor("#919999"), // 15: Grey
        Color.parseColor("#CC9E00"), // 16: Gold Accent (Surah header)
        Color.parseColor("#00B000"), // 17: Green Accent
        Color.parseColor("#080808"), // 18: Deep Dark fill
        Color.parseColor("#18322E")  // 19: Deep Emerald fill (Surah header background)
    )

    // Official CPAL Palette 0 (Light Mode & Warm Sepia Paper Palette)
    // Dark Charcoal / Espresso for normal letters and rich, high-contrast Tajweed colors
    val LIGHT_COLORS = intArrayOf(
        Color.parseColor("#18181B"), // 0: Dark Charcoal (Normal Quran letters)
        Color.parseColor("#8E8E93"), // 1: Grey (Silent letters / Hamzat Wasl)
        Color.parseColor("#8E8E93"), // 2: Grey (Idgham / Silent)
        Color.parseColor("#D60000"), // 3: Deep Red (Madd Necessary / Lazim)
        Color.parseColor("#E66E00"), // 4: Deep Orange (Madd Obligatory / Wajib)
        Color.parseColor("#C2185B"), // 5: Rose Pink (Madd Permissible / Jaiz)
        Color.parseColor("#15803D"), // 6: Forest Green (Ghunnah / Ikhfaa)
        Color.parseColor("#1D4ED8"), // 7: Deep Blue (Qalqalah)
        Color.parseColor("#0284C7"), // 8: Deep Cyan (Iqlab / Tafkheem)
        Color.parseColor("#BE185D"), // 9: Deep Rose (Madd Normal)
        Color.parseColor("#047857"), // 10: Emerald (Ayah ornament outer ring)
        Color.parseColor("#B45309"), // 11: Gold (Ayah ornament accents)
        Color.parseColor("#1E3A34"), // 12: Dark Teal (Ayah ornament inner fill)
        Color.parseColor("#FFFFFF"), // 13: White (Ayah ornament digits inside dark circle)
        Color.parseColor("#FFFFFF"), // 14: White
        Color.parseColor("#8E8E93"), // 15: Grey
        Color.parseColor("#B45309"), // 16: Gold Accent
        Color.parseColor("#15803D"), // 17: Green Accent
        Color.parseColor("#18181B"), // 18: Dark fill
        Color.parseColor("#132A24")  // 19: Deep Emerald fill
    )

    fun getColor(colorId: Int, isDark: Boolean = true): Int {
        val palette = if (isDark) DARK_COLORS else LIGHT_COLORS
        return if (colorId in palette.indices) palette[colorId] else if (isDark) Color.WHITE else Color.parseColor("#18181B")
    }

    fun getNormalTextColor(isDark: Boolean = true): Int {
        return if (isDark) Color.WHITE else Color.parseColor("#18181B")
    }
}

class QuranRepository(private val context: Context) {

    val surahs = ArrayList<SurahInfo>(114)
    private val surahMap = HashMap<Int, SurahInfo>(128)
    private val versesBySurah = HashMap<Int, ArrayList<VerseData>>(128)
    val allVerses = ArrayList<VerseData>(6236)

    // fontId (1..604 = page fonts, 605 = bismillah, 606 = chapter_headers) -> Map<baseCodepoint, IntArray of packed (layerCodepoint shl 8) or colorId>
    private val colrLayersByFont = Array<HashMap<Int, IntArray>?>(607) { null }

    private val pageTypefaceCache = object : LruCache<Int, Typeface>(45) {
        override fun create(page: Int): Typeface {
            val fileName = when (page) {
                605 -> "fonts/bismillah.ttf"
                606 -> "fonts/chapter_headers.ttf"
                else -> String.format("fonts/pages/p%03d.ttf", page.coerceIn(1, 604))
            }
            return Typeface.createFromAsset(context.assets, fileName)
        }
    }

    val uthmaniTypeface: Typeface by lazy {
        Typeface.createFromAsset(context.assets, "fonts/uthmani.ttf")
    }

    val surahIconsTypeface: Typeface by lazy {
        Typeface.createFromAsset(context.assets, "fonts/surah_icons.ttf")
    }

    val bismillahTypeface: Typeface get() = getTypefaceForFontId(605)
    val chapterHeadersTypeface: Typeface get() = getTypefaceForFontId(606)

    private val prefs = context.getSharedPreferences("quran_native_oled_prefs", Context.MODE_PRIVATE)

    @Volatile
    var isLoaded = false
        private set

    fun ensureLoaded() {
        if (isLoaded) return
        synchronized(this) {
            if (isLoaded) return
            loadSurahs()
            loadVerses()
            loadColrLayers()
            isLoaded = true
        }
    }

    private fun loadSurahs() {
        surahs.clear()
        surahMap.clear()
        BufferedReader(InputStreamReader(context.assets.open("data/surahs.tsv"), Charsets.UTF_8)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                if (line.isNotBlank()) {
                    val parts = line.split('\t')
                    if (parts.size >= 8) {
                        val id = parts[0].toInt()
                        val name = parts[1]
                        val isMeccan = parts[2].toInt() == 1
                        val versesCount = parts[3].toInt()
                        val startPage = parts[4].toInt()
                        val endPage = parts[5].toInt()
                        val iconChar = parts[6]
                        val headerChar = parts[7]
                        val info = SurahInfo(
                            id = id,
                            nameArabic = name,
                            isMeccan = isMeccan,
                            versesCount = versesCount,
                            startPage = startPage,
                            endPage = endPage,
                            iconChar = iconChar,
                            headerChar = headerChar,
                            normalizedName = normalizeArabic("سورة $name")
                        )
                        surahs.add(info)
                        surahMap[id] = info
                    }
                }
                line = reader.readLine()
            }
        }
    }

    private fun loadVerses() {
        versesBySurah.clear()
        allVerses.clear()
        for (i in 1..114) {
            versesBySurah[i] = ArrayList()
        }
        BufferedReader(InputStreamReader(context.assets.open("data/verses.tsv"), Charsets.UTF_8)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                if (line.isNotBlank()) {
                    val parts = line.split('\t')
                    if (parts.size >= 8) {
                        val sid = parts[0].toInt()
                        val vid = parts[1].toInt()
                        val page = parts[2].toInt()
                        val juz = parts[3].toInt()
                        val hizb = parts[4].toInt()
                        val words = if (parts[5].isEmpty()) emptyList() else parts[5].split('|')
                        val endGlyph = parts[6]
                        val uthmani = parts[7]
                        val verse = VerseData(
                            surahId = sid,
                            verseNumber = vid,
                            page = page,
                            juz = juz,
                            hizb = hizb,
                            words = words,
                            endGlyph = endGlyph,
                            uthmaniText = uthmani,
                            normalizedText = normalizeArabic(uthmani)
                        )
                        versesBySurah[sid]?.add(verse)
                        allVerses.add(verse)
                    }
                }
                line = reader.readLine()
            }
        }
    }

    private fun loadColrLayers() {
        val bytes = context.assets.open("data/colr_layers.bin").use { it.readBytes() }
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val numFonts = buf.short.toInt() and 0xFFFF
        for (f in 0 until numFonts) {
            val fontId = buf.short.toInt() and 0xFFFF
            val numGlyphs = buf.short.toInt() and 0xFFFF
            val map = HashMap<Int, IntArray>(numGlyphs * 4 / 3 + 1)
            for (g in 0 until numGlyphs) {
                val baseCp = buf.short.toInt() and 0xFFFF
                val numLayers = buf.get().toInt() and 0xFF
                val packedLayers = IntArray(numLayers)
                for (l in 0 until numLayers) {
                    val layerCp = buf.short.toInt() and 0xFFFF
                    val colorId = buf.get().toInt() and 0xFF
                    packedLayers[l] = (layerCp shl 8) or colorId
                }
                map[baseCp] = packedLayers
            }
            if (fontId in colrLayersByFont.indices) {
                colrLayersByFont[fontId] = map
            }
        }
    }

    fun getSurah(surahId: Int): SurahInfo {
        return surahMap[surahId] ?: surahs.first()
    }

    fun getVersesForSurah(surahId: Int): List<VerseData> {
        return versesBySurah[surahId] ?: emptyList()
    }

    fun getTypefaceForFontId(fontId: Int): Typeface {
        return pageTypefaceCache.get(fontId)
    }

    fun getColrLayers(fontId: Int, baseCodepoint: Int): IntArray? {
        if (fontId !in colrLayersByFont.indices) return null
        return colrLayersByFont[fontId]?.get(baseCodepoint)
    }

    fun getBismillahText(surahId: Int): String {
        return when (surahId) {
            2 -> "\uFC9A\uFC9B\uFC9E\uFCA4"
            95, 97 -> "\uFB57\uFCAB\uFCAE\uFCB4"
            else -> "\uFCAA\uFCAB\uFCAE\uFCB4"
        }
    }

    // Search Surahs and Verses
    fun searchSurahs(query: String): List<SurahInfo> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return surahs
        val norm = normalizeArabic(trimmed)
        val lower = trimmed.lowercase()
        val numQuery = trimmed.toIntOrNull()
        return surahs.filter { s ->
            val enName = ENGLISH_SURAH_NAMES.getOrElse(s.id - 1) { "" }.lowercase()
            (numQuery != null && (s.id == numQuery || numQuery in s.startPage..s.endPage)) ||
                s.normalizedName.contains(norm) ||
                s.nameArabic.contains(trimmed) ||
                enName.contains(lower)
        }
    }

    fun searchVerses(query: String, limit: Int = 150): List<VerseData> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Support "2:255" or "2 255" direct key search
        val keyMatch = Regex("""^(\d{1,3})\s*[:/\s]\s*(\d{1,3})$""").matchEntire(trimmed)
        if (keyMatch != null) {
            val sid = keyMatch.groupValues[1].toIntOrNull()
            val vid = keyMatch.groupValues[2].toIntOrNull()
            if (sid != null && vid != null) {
                val v = versesBySurah[sid]?.find { it.verseNumber == vid }
                if (v != null) return listOf(v)
            }
        }

        val norm = normalizeArabic(trimmed)
        if (norm.isEmpty()) return emptyList()

        val results = ArrayList<VerseData>()
        for (v in allVerses) {
            if (v.normalizedText.contains(norm) || v.uthmaniText.contains(trimmed)) {
                results.add(v)
                if (results.size >= limit) break
            }
        }
        return results
    }

    // Persistence: App Language ("system", "ar", "en")
    fun getAppLanguage(): String {
        return prefs.getString("app_language", "system") ?: "system"
    }

    fun saveAppLanguage(langCode: String) {
        prefs.edit().putString("app_language", langCode).apply()
    }

    // Persistence: Theme (OLED, Dark, Sepia, Light)
    fun getAppTheme(): String {
        return prefs.getString("app_theme", "oled") ?: "oled"
    }

    fun saveAppTheme(themeId: String) {
        prefs.edit().putString("app_theme", themeId).apply()
    }

    // Persistence: Design Style (Material 3 Expressive vs Classic Minimal)
    fun getDesignStyle(): String {
        return prefs.getString("design_style", "m3_expressive") ?: "m3_expressive"
    }

    fun saveDesignStyle(style: String) {
        prefs.edit().putString("design_style", style).apply()
    }

    // Persistence: Tajweed Coloring vs Normal Uniform Coloring
    fun isTajweedEnabled(): Boolean {
        return prefs.getBoolean("tajweed_enabled", true)
    }

    fun saveTajweedEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("tajweed_enabled", enabled).apply()
    }

    // Persistence: Font Size & Auto-Scroll Speed
    fun getFontSizeSp(): Float {
        return prefs.getFloat("font_size_sp", 30f).coerceIn(18f, 64f)
    }

    fun saveFontSizeSp(sizeSp: Float) {
        prefs.edit().putFloat("font_size_sp", sizeSp.coerceIn(18f, 64f)).apply()
    }

    fun getAutoScrollSpeed(): Float {
        return prefs.getFloat("auto_scroll_speed", 1.5f).coerceIn(0.25f, 8.0f)
    }

    fun saveAutoScrollSpeed(speed: Float) {
        prefs.edit().putFloat("auto_scroll_speed", speed.coerceIn(0.25f, 8.0f)).apply()
    }

    // Persistence: Last Read Position
    fun getLastReadPosition(): Pair<Int, Int>? {
        val sid = prefs.getInt("last_surah_id", 0)
        val vid = prefs.getInt("last_verse_num", 1)
        if (sid !in 1..114) return null
        return Pair(sid, vid)
    }

    fun saveLastReadPosition(surahId: Int, verseNumber: Int) {
        if (surahId !in 1..114) return
        prefs.edit()
            .putInt("last_surah_id", surahId)
            .putInt("last_verse_num", verseNumber.coerceAtLeast(1))
            .apply()
    }

    fun commitLastReadPosition(surahId: Int, verseNumber: Int) {
        if (surahId !in 1..114) return
        prefs.edit()
            .putInt("last_surah_id", surahId)
            .putInt("last_verse_num", verseNumber.coerceAtLeast(1))
            .commit()
    }

    // Persistence: Bookmarks
    fun getBookmarks(): List<BookmarkEntry> {
        val raw = prefs.getString("bookmarks_v2", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(';')
            .mapNotNull { token ->
                val p = token.split(',')
                if (p.size >= 3) {
                    val s = p[0].toIntOrNull()
                    val v = p[1].toIntOrNull()
                    val t = p[2].toLongOrNull()
                    if (s != null && v != null && t != null && s in 1..114) {
                        BookmarkEntry(s, v, t)
                    } else null
                } else null
            }
            .sortedByDescending { it.timestamp }
    }

    fun isBookmarked(surahId: Int, verseNumber: Int): Boolean {
        return getBookmarks().any { it.surahId == surahId && it.verseNumber == verseNumber }
    }

    fun toggleBookmark(surahId: Int, verseNumber: Int): Boolean {
        val current = getBookmarks().toMutableList()
        val existingIdx = current.indexOfFirst { it.surahId == surahId && it.verseNumber == verseNumber }
        val added = if (existingIdx >= 0) {
            current.removeAt(existingIdx)
            false
        } else {
            current.add(0, BookmarkEntry(surahId, verseNumber, System.currentTimeMillis()))
            true
        }
        saveBookmarksList(current)
        return added
    }

    fun removeBookmark(surahId: Int, verseNumber: Int) {
        val current = getBookmarks().filterNot { it.surahId == surahId && it.verseNumber == verseNumber }
        saveBookmarksList(current)
    }

    private fun saveBookmarksList(list: List<BookmarkEntry>) {
        val serialized = list.joinToString(";") { "${it.surahId},${it.verseNumber},${it.timestamp}" }
        prefs.edit().putString("bookmarks_v2", serialized).apply()
    }

    companion object {
        fun normalizeArabic(input: String): String {
            val sb = StringBuilder(input.length)
            for (ch in input) {
                when (ch) {
                    // Strip tashkeel / harakat / Quranic annotation marks / tatweel
                    in '\u0610'..'\u061A',
                    in '\u064B'..'\u065F',
                    '\u0670', // dagger alif
                    in '\u06D6'..'\u06ED',
                    '\u0640' -> {
                        // skip
                    }
                    // Normalize Alif variants to bare Alif 'ا'
                    'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
                    // Normalize Taa Marbuta to Haa (optional, helps flexible search) or keep distinct
                    'ؤ' -> sb.append('و')
                    'ئ', 'ى' -> sb.append('ي')
                    else -> sb.append(ch)
                }
            }
            return sb.toString().lowercase().trim()
        }

        private val ARABIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

        fun toArabicDigits(num: Int): String {
            val s = num.toString()
            val sb = StringBuilder(s.length)
            for (c in s) {
                if (c in '0'..'9') {
                    sb.append(ARABIC_DIGITS[c - '0'])
                } else {
                    sb.append(c)
                }
            }
            return sb.toString()
        }
    }
}
