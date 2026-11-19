package com.qurantajweed.app

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppTheme(
    val id: String,
    val titleArabic: String,
    val descriptionArabic: String
) {
    OLED("oled", "أوليد (أسود عميق)", "شاشة سوداء نقية 100% موفرة للطاقة"),
    DARK("dark", "داكن (رمادي كحلي)", "خلفية داكنة هادئة غير معتمة بالكامل"),
    SEPIA("sepia", "أصفر (راحة العين)", "ورق مصحف دافئ يقلل إجهاد العين"),
    LIGHT("light", "أبيض (نهاري)", "خلفية بيضاء نقية مع خطوط واضحة")
}

data class ThemeColors(
    val isDark: Boolean,
    val background: Color,
    val cardSurface: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSubtle: Color,
    val accentEmerald: Color,
    val accentGold: Color,
    val pillBackground: Color,
    val pillBorder: Color
)

fun getThemeColors(theme: AppTheme): ThemeColors {
    return when (theme) {
        AppTheme.OLED -> ThemeColors(
            isDark = true,
            background = Color(0xFF000000),
            cardSurface = Color(0xFF0B0B0E),
            cardBorder = Color(0xFF1F1F26),
            textPrimary = Color(0xFFFFFFFF),
            textSubtle = Color(0xFFA1A1AA),
            accentEmerald = Color(0xFF10B981),
            accentGold = Color(0xFFD9A441),
            pillBackground = Color(0xFF141419),
            pillBorder = Color(0xFF272732)
        )
        AppTheme.DARK -> ThemeColors(
            isDark = true,
            background = Color(0xFF15161E),
            cardSurface = Color(0xFF1E212D),
            cardBorder = Color(0xFF2E3245),
            textPrimary = Color(0xFFF3F4F6),
            textSubtle = Color(0xFF9CA3AF),
            accentEmerald = Color(0xFF10B981),
            accentGold = Color(0xFFE5B558),
            pillBackground = Color(0xFF272B3B),
            pillBorder = Color(0xFF383D53)
        )
        AppTheme.SEPIA -> ThemeColors(
            isDark = false,
            background = Color(0xFFFBF1D8),
            cardSurface = Color(0xFFF3E7C8),
            cardBorder = Color(0xFFDFCFAF),
            textPrimary = Color(0xFF241B12),
            textSubtle = Color(0xFF786956),
            accentEmerald = Color(0xFF0F766E),
            accentGold = Color(0xFF9E680E),
            pillBackground = Color(0xFFE9DAC0),
            pillBorder = Color(0xFFD4C19C)
        )
        AppTheme.LIGHT -> ThemeColors(
            isDark = false,
            background = Color(0xFFF8FAFC),
            cardSurface = Color(0xFFFFFFFF),
            cardBorder = Color(0xFFE2E8F0),
            textPrimary = Color(0xFF0F172A),
            textSubtle = Color(0xFF64748B),
            accentEmerald = Color(0xFF059669),
            accentGold = Color(0xFFB45309),
            pillBackground = Color(0xFFF1F5F9),
            pillBorder = Color(0xFFCBD5E1)
        )
    }
}

val LocalAppTheme = compositionLocalOf { AppTheme.OLED }
val LocalThemeColors = compositionLocalOf { getThemeColors(AppTheme.OLED) }
val LocalTajweedEnabled = compositionLocalOf { true }

private val OledBlack: Color
    @Composable
    get() = LocalThemeColors.current.background

private val CardSurface: Color
    @Composable
    get() = LocalThemeColors.current.cardSurface

private val CardBorder: Color
    @Composable
    get() = LocalThemeColors.current.cardBorder

private val EmeraldAccent: Color
    @Composable
    get() = LocalThemeColors.current.accentEmerald

private val GoldAccent: Color
    @Composable
    get() = LocalThemeColors.current.accentGold

private val SubtleText: Color
    @Composable
    get() = LocalThemeColors.current.textSubtle

private val PureWhite: Color
    @Composable
    get() = LocalThemeColors.current.textPrimary

private val PillBackground: Color
    @Composable
    get() = LocalThemeColors.current.pillBackground

private val PillBorder: Color
    @Composable
    get() = LocalThemeColors.current.pillBorder

data class CameraCutoutPadding(
    val topDp: Dp = 0.dp,
    val leftDp: Dp = 0.dp,
    val rightDp: Dp = 0.dp,
    val bottomDp: Dp = 0.dp
)

@Composable
fun HomeScreen(
    repo: QuranRepository,
    cutout: CameraCutoutPadding,
    lastRead: Pair<Int, Int>?,
    bookmarks: List<BookmarkEntry>,
    onOpenSurah: (surahId: Int, initialVerse: Int) -> Unit,
    onRemoveBookmark: (surahId: Int, verseNumber: Int) -> Unit,
    onOpenSettings: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = السور, 1 = العلامات المرجعية

    val surahIconsFont = remember { FontFamily(repo.surahIconsTypeface) }
    val uthmaniFont = remember { FontFamily(repo.uthmaniTypeface) }

    val matchingSurahs = remember(searchQuery) { repo.searchSurahs(searchQuery) }
    val matchingVerses = remember(searchQuery) {
        if (searchQuery.trim().isNotEmpty()) repo.searchVerses(searchQuery, limit = 150) else emptyList()
    }

    val strings = LocalAppStrings.current
    val appDir = if (strings.isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    val startCutout = if (strings.isArabic) cutout.rightDp else cutout.leftDp
    val endCutout = if (strings.isArabic) cutout.leftDp else cutout.rightDp

    CompositionLocalProvider(LocalLayoutDirection provides appDir) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(OledBlack)
                .padding(
                    top = cutout.topDp.coerceAtLeast(16.dp),
                    start = startCutout.coerceAtLeast(12.dp),
                    end = endCutout.coerceAtLeast(12.dp),
                    bottom = cutout.bottomDp
                )
        ) {
            // Top Title + Continue Reading Button
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(EmeraldAccent.copy(alpha = 0.14f))
                        .border(1.dp, EmeraldAccent.copy(alpha = 0.45f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "۞",
                        color = EmeraldAccent,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.appTitle,
                        color = PureWhite,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = if (LocalTajweedEnabled.current) strings.subtitleTajweed else strings.subtitleNormal,
                        color = SubtleText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Settings Button (⚙)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PillBackground)
                        .border(1.dp, PillBorder, CircleShape)
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚙",
                        color = PureWhite,
                        fontSize = 20.sp
                    )
                }
            }

            // Friday Al-Kahf Reminder Card (Displayed prominently every Friday)
            val isFriday = remember {
                val cal = java.util.Calendar.getInstance()
                cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.FRIDAY
            }
            if (isFriday) {
                val fridayShape = RoundedCornerShape(20.dp)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onOpenSurah(18, 1) },
                    shape = fridayShape,
                    color = GoldAccent.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(GoldAccent.copy(alpha = 0.18f))
                                    .border(1.dp, GoldAccent.copy(alpha = 0.50f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🕌", fontSize = 18.sp)
                            }
                            Column {
                                Text(
                                    text = strings.fridayKahfTitle,
                                    color = GoldAccent,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = strings.fridayKahfSubtitle,
                                    color = SubtleText,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                        Text(
                            text = strings.readNowArrow,
                            color = GoldAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Dedicated Continue Reading Card (Always shown prominently at the top)
            if (lastRead != null) {
                val lastSurah = repo.getSurah(lastRead.first)
                val lastPage = remember(lastRead.first, lastRead.second) {
                    repo.getVersesForSurah(lastRead.first)
                        .find { it.verseNumber == lastRead.second }?.page ?: lastSurah.startPage
                }
                val continueShape = RoundedCornerShape(20.dp)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onOpenSurah(lastRead.first, lastRead.second) },
                    shape = continueShape,
                    color = EmeraldAccent.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, EmeraldAccent.copy(alpha = 0.50f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldAccent.copy(alpha = 0.18f))
                                    .border(1.dp, EmeraldAccent.copy(alpha = 0.50f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🔖", fontSize = 18.sp)
                            }
                            Column {
                                Text(
                                    text = strings.continueReadingPrefix(strings.surahName(lastSurah)),
                                    color = EmeraldAccent,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = strings.continueSubtext(lastPage, lastRead.second),
                                    color = SubtleText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Text(
                            text = strings.continueArrow,
                            color = EmeraldAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Search Bar (Surahs & Ayahs)
            val searchShape = RoundedCornerShape(999.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(searchShape)
                    .background(CardSurface)
                    .border(
                        width = 1.dp,
                        color = if (searchQuery.isNotEmpty()) EmeraldAccent.copy(alpha = 0.65f) else CardBorder,
                        shape = searchShape
                    )
                    .padding(horizontal = 16.dp, vertical = 13.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🔍",
                            fontSize = 16.sp
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = strings.searchPlaceholder,
                                    color = SubtleText.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = PureWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = if (strings.isArabic) TextAlign.Right else TextAlign.Left
                                ),
                                cursorBrush = SolidColor(EmeraldAccent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF27272A))
                                .clickable { searchQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                color = PureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Tabs (السور / العلامات المرجعية) when not searching
            if (searchQuery.trim().isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TabPillButton(
                        title = strings.surahsTabTitle(114),
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    TabPillButton(
                        title = strings.bookmarksTabTitle(bookmarks.size),
                        isSelected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Main Content List
            if (searchQuery.trim().isNotEmpty()) {
                SearchResultsList(
                    matchingSurahs = matchingSurahs,
                    matchingVerses = matchingVerses,
                    repo = repo,
                    surahIconsFont = surahIconsFont,
                    uthmaniFont = uthmaniFont,
                    onOpenSurah = onOpenSurah
                )
            } else if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(repo.surahs, key = { it.id }) { surah ->
                        SurahCardRow(
                            surah = surah,
                            surahIconsFont = surahIconsFont,
                            onClick = { onOpenSurah(surah.id, 1) }
                        )
                    }
                }
            } else {
                BookmarksListView(
                    bookmarks = bookmarks,
                    repo = repo,
                    uthmaniFont = uthmaniFont,
                    onOpenVerse = onOpenSurah,
                    onRemoveBookmark = onRemoveBookmark
                )
            }
        }
    }
}

@Composable
private fun TabPillButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabShape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .clip(tabShape)
            .background(if (isSelected) EmeraldAccent.copy(alpha = 0.16f) else CardSurface)
            .border(
                width = 1.dp,
                color = if (isSelected) EmeraldAccent.copy(alpha = 0.65f) else CardBorder,
                shape = tabShape
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) EmeraldAccent else SubtleText,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SurahCardRow(
    surah: SurahInfo,
    surahIconsFont: FontFamily,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(22.dp)
    val badgeShape = RoundedCornerShape(16.dp)
    val badgeBg = EmeraldAccent.copy(alpha = 0.14f)
    val badgeBorder = EmeraldAccent.copy(alpha = 0.45f)
    val pageShape = RoundedCornerShape(999.dp)

    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(CardSurface)
            .border(1.dp, CardBorder, cardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Surah Number Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(badgeShape)
                    .background(badgeBg)
                    .border(1.dp, badgeBorder, badgeShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.formatNumber(surah.id),
                    color = EmeraldAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title (Full column width, never squished)
                Text(
                    text = strings.surahTitlePrefix(strings.surahName(surah)),
                    color = PureWhite,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Subtitle Row: Meccan/Medinan Badge + Ayahs count + Page range
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (surah.isMeccan) GoldAccent.copy(alpha = 0.16f) else EmeraldAccent.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            if (surah.isMeccan) GoldAccent.copy(alpha = 0.40f) else EmeraldAccent.copy(alpha = 0.40f)
                        )
                    ) {
                        Text(
                            text = if (surah.isMeccan) strings.meccanLabel else strings.medinanLabel,
                            color = if (surah.isMeccan) GoldAccent else EmeraldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = strings.versesAndPageMeta(surah.versesCount, surah.startPage) +
                            if (surah.startPage != surah.endPage) " - ${strings.formatNumber(surah.endPage)}" else "",
                        color = SubtleText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }

        // Calligraphic Surah Name Icon + Page Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = surah.iconChar,
                fontFamily = surahIconsFont,
                color = GoldAccent,
                fontSize = 30.sp,
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .clip(pageShape)
                    .background(PillBackground)
                    .border(1.dp, PillBorder, pageShape)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.pageShort(surah.startPage),
                    color = PureWhite.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    matchingSurahs: List<SurahInfo>,
    matchingVerses: List<VerseData>,
    repo: QuranRepository,
    surahIconsFont: FontFamily,
    uthmaniFont: FontFamily,
    onOpenSurah: (surahId: Int, initialVerse: Int) -> Unit
) {
    val strings = LocalAppStrings.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (matchingSurahs.isNotEmpty()) {
            item(key = "header_surahs") {
                Text(
                    text = strings.matchingSurahsTitle(matchingSurahs.size),
                    color = EmeraldAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
            items(matchingSurahs, key = { "surah_${it.id}" }) { surah ->
                SurahCardRow(
                    surah = surah,
                    surahIconsFont = surahIconsFont,
                    onClick = { onOpenSurah(surah.id, 1) }
                )
            }
        }

        item(key = "header_verses") {
            Text(
                text = strings.matchingVersesTitle(matchingVerses.size, matchingVerses.size >= 150),
                color = GoldAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            )
        }

        if (matchingVerses.isEmpty() && matchingSurahs.isEmpty()) {
            item(key = "empty_search") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noSearchResults,
                        color = SubtleText,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            items(matchingVerses, key = { "verse_${it.verseKey}" }) { verse ->
                val surah = repo.getSurah(verse.surahId)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .clickable { onOpenSurah(verse.surahId, verse.verseNumber) }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldAccent.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = strings.searchVerseHeader(surah.id, strings.surahName(surah), verse.verseNumber),
                                    color = EmeraldAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = strings.searchVerseMeta(verse.page, verse.juz),
                            color = SubtleText,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = "${verse.uthmaniText} ﴿${QuranRepository.toArabicDigits(verse.verseNumber)}﴾",
                        fontFamily = uthmaniFont,
                        color = PureWhite,
                        fontSize = 22.sp,
                        lineHeight = 38.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarksListView(
    bookmarks: List<BookmarkEntry>,
    repo: QuranRepository,
    uthmaniFont: FontFamily,
    onOpenVerse: (surahId: Int, verseNumber: Int) -> Unit,
    onRemoveBookmark: (surahId: Int, verseNumber: Int) -> Unit
) {
    val strings = LocalAppStrings.current
    if (bookmarks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🔖",
                    fontSize = 36.sp
                )
                Text(
                    text = strings.noBookmarksTitle,
                    color = PureWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = strings.noBookmarksHint,
                    color = SubtleText,
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(bookmarks, key = { it.verseKey }) { entry ->
                val surah = repo.getSurah(entry.surahId)
                val verse = repo.getVersesForSurah(entry.surahId).getOrNull(entry.verseNumber - 1)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .clickable { onOpenVerse(entry.surahId, entry.verseNumber) }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GoldAccent.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = strings.bookmarkItemTitle(strings.surahName(surah), entry.verseNumber),
                                    color = GoldAccent,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (verse != null) {
                                Text(
                                    text = strings.pageShort(verse.page),
                                    color = SubtleText,
                                    fontSize = 12.5.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2A1215))
                                .clickable { onRemoveBookmark(entry.surahId, entry.verseNumber) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = strings.removeLabel,
                                color = Color(0xFFF87171),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (verse != null) {
                        Text(
                            text = "${verse.uthmaniText} ﴿${QuranRepository.toArabicDigits(verse.verseNumber)}﴾",
                            fontFamily = uthmaniFont,
                            color = PureWhite,
                            fontSize = 21.sp,
                            lineHeight = 36.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContinuousReaderScreen(
    surahId: Int,
    initialVerse: Int,
    fontSizeSp: Float,
    onFontSizeChange: (Float) -> Unit,
    cutout: CameraCutoutPadding,
    repo: QuranRepository,
    bookmarks: List<BookmarkEntry>,
    onToggleBookmark: (surahId: Int, verseNumber: Int) -> Boolean,
    onNavigateToSurah: (surahId: Int, initialVerse: Int) -> Unit,
    onSaveReadingPosition: (surahId: Int, verseNumber: Int) -> Unit,
    onBackToHome: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val surah = remember(surahId) { repo.getSurah(surahId) }
    val verses = remember(surahId) { repo.getVersesForSurah(surahId) }
    val bookmarkedSet = remember(bookmarks, surahId) {
        bookmarks.filter { it.surahId == surahId }.map { it.verseNumber }.toHashSet()
    }

    var controlsVisible by remember { mutableStateOf(false) }
    var selectedVerse by remember { mutableStateOf<VerseData?>(null) }
    var showQuickJumpModal by remember { mutableStateOf(false) }

    // Auto-scroll state & speed (persisted in repository)
    var autoScrollActive by remember { mutableStateOf(false) }
    var autoScrollSpeed by remember { mutableFloatStateOf(repo.getAutoScrollSpeed()) }

    // Header item is index 0, Bismillah/Spacer is index 1 -> Quran lines always start at index 2
    val hasBismillah = surahId != 1 && surahId != 9
    val headerItemsCount = 2

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Smooth 60fps auto-scroll loop that NEVER stops on user touch or manual dragging
    LaunchedEffect(autoScrollActive, autoScrollSpeed, surahId) {
        if (!autoScrollActive) return@LaunchedEffect
        while (autoScrollActive && isActive) {
            try {
                var lastNanos = 0L
                while (autoScrollActive && isActive) {
                    val frameNanos = withFrameNanos { it }
                    if (lastNanos != 0L) {
                        val deltaSec = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
                        // Speed 1.0x = ~38 px/sec
                        val dy = autoScrollSpeed * 38f * deltaSec
                        val consumed = listState.scrollBy(dy)
                        if (consumed == 0f && !listState.canScrollForward) {
                            autoScrollActive = false
                            break
                        }
                    }
                    lastNanos = frameNanos
                }
            } catch (e: CancellationException) {
                // If a user touch/drag interrupted scrollBy, do NOT stop autoScrollActive!
                // Only propagate cancellation if the coroutine itself or autoScrollActive was cancelled.
                if (!isActive || !autoScrollActive) {
                    throw e
                }
                // Wait briefly for the user touch gesture to settle, then seamlessly resume scrolling
                delay(60L)
            } catch (_: Exception) {
                delay(60L)
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(OledBlack)
        ) {
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val density = LocalDensity.current

            // Full screen width utilization:
            // - In portrait: no camera cutout on sides, use 3.dp edge buffer
            // - In landscape: respect cutout ONLY on the specific side where camera is located; other side uses 3.dp
            val edgeBufferDp = 3.dp
            val safeRightDp = if (isLandscape) {
                if (cutout.rightDp > 0.dp) cutout.rightDp else edgeBufferDp
            } else {
                edgeBufferDp
            }
            val safeLeftDp = if (isLandscape) {
                if (cutout.leftDp > 0.dp) cutout.leftDp else edgeBufferDp
            } else {
                edgeBufferDp
            }
            val safeTopDp = cutout.topDp.coerceAtLeast(4.dp)
            val safeBottomDp = cutout.bottomDp.coerceAtLeast(4.dp)

            val availableWidthPx = with(density) {
                (maxWidth - safeRightDp - safeLeftDp).toPx().coerceAtLeast(200f)
            }

            // In landscape mode, use available width with comfortable side margins for maximum immersive view
            val lineBreakWidthPx = availableWidthPx
            val containerHeightPx = with(density) { maxHeight.toPx() }
            val lineHeightPx = fontSizeSp * density.density * 1.94f
            val halfScreenLines = ((containerHeightPx * 0.38f) / lineHeightPx).toInt().coerceAtLeast(0)

            // Build continuous centered lines where every Ayah starts immediately after the previous Ayah on the same line
            val centeredLines = remember(
                surahId,
                verses,
                fontSizeSp,
                lineBreakWidthPx,
                configuration.orientation,
                density.density
            ) {
                buildCenteredSurahLines(
                    verses = verses,
                    fontSizeSp = fontSizeSp,
                    densityPxPerDp = density.density,
                    availableWidthPx = lineBreakWidthPx,
                    repo = repo
                )
            }

            // Real-time tracking of the currently visible Ayah number
            var currentReadingVerseNumber by remember(surahId) { mutableIntStateOf(initialVerse) }
            var lastTrackedOrientation by remember(surahId) { mutableIntStateOf(configuration.orientation) }
            var lastTrackedFontSize by remember(surahId) { mutableFloatStateOf(fontSizeSp) }

            var isLayoutTransitioning by remember { mutableStateOf(false) }

            // Track current visible verse for top bar metadata and saving reading progress
            val currentVisibleVerse by remember(surahId, verses, currentReadingVerseNumber) {
                derivedStateOf {
                    verses.find { it.verseNumber == currentReadingVerseNumber } ?: verses.firstOrNull()
                }
            }

            val currentOrientation = configuration.orientation
            val orientationChanged = currentOrientation != lastTrackedOrientation
            val fontSizeChanged = fontSizeSp != lastTrackedFontSize

            // CRITICAL: Synchronously lock layout transitioning in the composition phase!
            // This prevents the scroll tracker from reading stale indices before the list is repositioned on rotation.
            if (orientationChanged || fontSizeChanged) {
                isLayoutTransitioning = true
            }

            // Scroll to initialVerse ONLY once on initial entry or explicit navigation
            var initialScrollDoneForSurah by remember(surahId) { mutableStateOf(false) }
            LaunchedEffect(surahId, centeredLines) {
                if (!initialScrollDoneForSurah && centeredLines.isNotEmpty()) {
                    initialScrollDoneForSurah = true
                    currentReadingVerseNumber = initialVerse
                    if (initialVerse > 1) {
                        val targetLineIdx = centeredLines.indexOfFirst { line ->
                            line.tokens.any { it.verse.verseNumber == initialVerse }
                        }.coerceAtLeast(0)
                        val scrollLineIdx = (targetLineIdx - halfScreenLines).coerceAtLeast(0)
                        val targetItemIdx = if (targetLineIdx < halfScreenLines) 0 else headerItemsCount + scrollLineIdx
                        listState.scrollToItem(targetItemIdx)
                    } else {
                        listState.scrollToItem(0)
                    }
                    selectedVerse = null
                }
            }

            // When device rotates between Portrait and Landscape, or font size changes, maintain the EXACT Ayah in the screen center!
            LaunchedEffect(currentOrientation, fontSizeSp, centeredLines) {
                if (orientationChanged || fontSizeChanged) {
                    lastTrackedOrientation = currentOrientation
                    lastTrackedFontSize = fontSizeSp
                    if (centeredLines.isNotEmpty()) {
                        val verseToRestore = currentReadingVerseNumber
                        val targetLineIdx = if (verseToRestore <= 1) {
                            0
                        } else {
                            val found = centeredLines.indexOfFirst { line ->
                                line.tokens.any { it.verse.verseNumber == verseToRestore }
                            }
                            if (found >= 0) {
                                found
                            } else {
                                centeredLines.indexOfLast { line ->
                                    line.tokens.any { it.verse.verseNumber <= verseToRestore }
                                }.coerceAtLeast(0)
                            }
                        }

                        // Center the active line vertically on screen
                        val scrollLineIdx = (targetLineIdx - halfScreenLines).coerceAtLeast(0)
                        val targetItemIdx = if (verseToRestore <= 1 && targetLineIdx < halfScreenLines) 0 else headerItemsCount + scrollLineIdx
                        listState.scrollToItem(targetItemIdx)
                    }
                    delay(350)
                    isLayoutTransitioning = false
                }
            }

            // 1. Ultra-smooth, center-focal in-memory Ayah tracking (Zero scroll friction)
            // Tracks the exact Ayah currently right in front of the reader in the vertical center of the screen
            LaunchedEffect(listState, surahId, centeredLines) {
                snapshotFlow {
                    val layoutInfo = listState.layoutInfo
                    val visibleItems = layoutInfo.visibleItemsInfo
                    if (visibleItems.isEmpty()) {
                        null
                    } else {
                        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                        val focalItem = visibleItems.firstOrNull { item ->
                            item.offset <= viewportCenter && (item.offset + item.size) >= viewportCenter
                        } ?: visibleItems.minByOrNull { item ->
                            val itemCenter = item.offset + item.size / 2
                            kotlin.math.abs(itemCenter - viewportCenter)
                        }
                        focalItem?.index
                    }
                }.collect { focalIdx ->
                    if (isLayoutTransitioning || focalIdx == null) return@collect

                    val detectedVerse = if (focalIdx < headerItemsCount) {
                        1
                    } else if (centeredLines.isNotEmpty()) {
                        val lineIdx = (focalIdx - headerItemsCount).coerceIn(0, centeredLines.lastIndex)
                        centeredLines[lineIdx].tokens.firstOrNull()?.verse?.verseNumber ?: 1
                    } else {
                        1
                    }

                    if (detectedVerse > 0 && detectedVerse != currentReadingVerseNumber) {
                        currentReadingVerseNumber = detectedVerse
                    }
                }
            }

            // 2. Debounced Persistence: Only saves when scrolling settles, guaranteeing 100% 120 FPS buttery smooth scrolling
            LaunchedEffect(currentReadingVerseNumber, surahId) {
                delay(180)
                if (!isLayoutTransitioning) {
                    onSaveReadingPosition(surahId, currentReadingVerseNumber)
                }
            }

            // Auto-hide bars when user manually drags/scrolls
            LaunchedEffect(listState.isScrollInProgress) {
                if (listState.isScrollInProgress && !autoScrollActive) {
                    if (controlsVisible) controlsVisible = false
                }
            }

            // Continuous Centered Lines LazyColumn
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = safeRightDp, // In RTL start is Right
                        end = safeLeftDp     // In RTL end is Left
                    ),
                contentPadding = PaddingValues(
                    top = safeTopDp + 4.dp,
                    bottom = safeBottomDp + 150.dp
                )
            ) {
                // 0: Surah Ornamental Header
                item(key = "surah_header_$surahId") {
                    SurahOrnamentHeaderBanner(
                        surah = surah,
                        availableWidthPx = availableWidthPx,
                        repo = repo,
                        onTap = {
                            if (selectedVerse != null) {
                                selectedVerse = null
                            } else {
                                controlsVisible = !controlsVisible
                            }
                        },
                        isDarkTheme = LocalThemeColors.current.isDark
                    )
                }

                // 1: Bismillah Banner (for all Surahs except 1 and 9) or Header Bottom Spacer
                if (hasBismillah) {
                    item(key = "bismillah_$surahId") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(18.dp))
                            BismillahOrnamentBanner(
                                surahId = surahId,
                                fontSizeSp = fontSizeSp,
                                availableWidthPx = availableWidthPx,
                                repo = repo,
                                onTap = {
                                    if (selectedVerse != null) {
                                        selectedVerse = null
                                    } else {
                                        controlsVisible = !controlsVisible
                                    }
                                },
                                isDarkTheme = LocalThemeColors.current.isDark
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                } else {
                    item(key = "spacer_lines_nobis_$surahId") {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // Centered Continuous Quran Lines
                items(
                    items = centeredLines,
                    key = { "line_${surahId}_${it.lineIndex}" }
                ) { line ->
                    TajweedCenteredLineRow(
                        line = line,
                        fontSizeSp = fontSizeSp,
                        selectedVerseKey = selectedVerse?.verseKey,
                        bookmarkedVerseNums = bookmarkedSet,
                        repo = repo,
                        onSingleTapEmptyOrWord = {
                            if (selectedVerse != null) {
                                selectedVerse = null
                            } else {
                                controlsVisible = !controlsVisible
                            }
                        },
                        onAyahNumberTap = { tappedVerse ->
                            selectedVerse = if (selectedVerse?.verseKey == tappedVerse.verseKey) null else tappedVerse
                        },
                        onLongPressVerse = { pressedVerse ->
                            selectedVerse = pressedVerse
                        },
                        isDarkTheme = LocalThemeColors.current.isDark,
                        isTajweedEnabled = LocalTajweedEnabled.current
                    )
                }

                // End-of-Surah Next / Previous Surah Navigation Buttons
                item(key = "footer_nav_$surahId") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Right side in RTL: Previous Surah (surahId - 1)
                        val hasPrev = surahId > 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (hasPrev) EmeraldAccent.copy(alpha = 0.12f) else CardSurface
                                )
                                .border(
                                    1.5.dp,
                                    if (hasPrev) EmeraldAccent.copy(alpha = 0.45f) else CardBorder,
                                    RoundedCornerShape(999.dp)
                                )
                                .clickable(enabled = hasPrev) {
                                    autoScrollActive = false
                                    onNavigateToSurah(surahId - 1, 1)
                                }
                                .padding(vertical = 15.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val strings = LocalAppStrings.current
                            val prevName = if (hasPrev) strings.surahTitlePrefix(strings.surahName(repo.getSurah(surahId - 1))) else strings.surahStartLabel
                            Text(
                                text = strings.prevSurahButton(prevName),
                                color = if (hasPrev) EmeraldAccent else SubtleText.copy(alpha = 0.4f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Left side in RTL: Next Surah (surahId + 1)
                        val hasNext = surahId < 114
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (hasNext) EmeraldAccent.copy(alpha = 0.12f) else CardSurface
                                )
                                .border(
                                    1.5.dp,
                                    if (hasNext) EmeraldAccent.copy(alpha = 0.45f) else CardBorder,
                                    RoundedCornerShape(999.dp)
                                )
                                .clickable(enabled = hasNext) {
                                    autoScrollActive = false
                                    onNavigateToSurah(surahId + 1, 1)
                                }
                                .padding(vertical = 15.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val strings = LocalAppStrings.current
                            val nextName = if (hasNext) strings.surahTitlePrefix(strings.surahName(repo.getSurah(surahId + 1))) else strings.surahEndLabel
                            Text(
                                text = strings.nextSurahButton(nextName),
                                color = if (hasNext) EmeraldAccent else SubtleText.copy(alpha = 0.4f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Floating Top Bar (Shown on Single Tap)
            AnimatedVisibility(
                visible = controlsVisible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                val strings = LocalAppStrings.current
                val appDir = if (strings.isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
                val barStartCutout = if (strings.isArabic) safeRightDp else safeLeftDp
                val barEndCutout = if (strings.isArabic) safeLeftDp else safeRightDp
                val activeVerse = currentVisibleVerse
                CompositionLocalProvider(LocalLayoutDirection provides appDir) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OledBlack.copy(alpha = 0.94f))
                            .border(width = 0.5.dp, color = CardBorder)
                            .padding(
                                top = safeTopDp.coerceAtLeast(10.dp),
                                bottom = 10.dp,
                                start = barStartCutout.coerceAtLeast(12.dp),
                                end = barEndCutout.coerceAtLeast(12.dp)
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                    // Right button: Back to Surahs List
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(PillBackground)
                            .border(1.dp, PillBorder, RoundedCornerShape(999.dp))
                            .clickable {
                                autoScrollActive = false
                                onBackToHome()
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (strings.isArabic) "▶" else "◀",
                            color = EmeraldAccent,
                            fontSize = 12.sp
                        )
                        Text(
                            text = strings.surahsBackButton,
                            color = PureWhite,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Center: Surah Name & Page / Juz / Ayah Info (Tappable for Quick Jump)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showQuickJumpModal = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = strings.readerTopSurahTitle(surah.id, strings.surahName(surah)),
                                color = PureWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "▼",
                                color = EmeraldAccent,
                                fontSize = 10.sp
                            )
                        }
                        if (activeVerse != null) {
                            Text(
                                text = strings.readerTopSubinfo(activeVerse.page, activeVerse.juz, activeVerse.verseNumber),
                                color = SubtleText,
                                fontSize = 11.5.sp,
                                maxLines = 1
                            )
                        }
                    }

                    // Left button: Settings Toggle (⚙)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PillBackground)
                            .border(1.dp, PillBorder, CircleShape)
                            .clickable { onOpenSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚙",
                            color = PureWhite,
                            fontSize = 18.sp
                        )
                    }
                    }
                }
            }

            // Floating Bottom Controls: Auto-Scroll Speed Bar + Main Pill
            // Bottom Unified Floating Controls (Auto-Scroll & Font/Navigation)
            // Completely hidden while reading; appears with smooth slide/fade on tap
            AnimatedVisibility(
                visible = controlsVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = safeBottomDp.coerceAtLeast(14.dp),
                        start = safeRightDp.coerceAtLeast(12.dp),
                        end = safeLeftDp.coerceAtLeast(12.dp)
                    )
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = CardSurface.copy(alpha = 0.96f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    shadowElevation = 8.dp,
                    modifier = Modifier.widthIn(max = 480.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sub-row 1: Auto-Scroll controls (النزول التلقائي والتحكم بالسرعة)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val strings = LocalAppStrings.current
                            // Play / Pause Auto-scroll pill button
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (autoScrollActive) EmeraldAccent.copy(alpha = 0.22f)
                                        else PillBackground
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (autoScrollActive) EmeraldAccent else PillBorder,
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .clickable { autoScrollActive = !autoScrollActive }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (autoScrollActive) strings.autoScrollPause else strings.autoScrollStart,
                                    color = if (autoScrollActive) EmeraldAccent else PureWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Speed adjustment controls: −  السرعة 2x  +
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(PillBackground)
                                        .border(1.dp, PillBorder, CircleShape)
                                        .clickable {
                                            val next = ((kotlin.math.round((autoScrollSpeed - 0.25f) * 4f)) / 4f).coerceAtLeast(0.25f)
                                            autoScrollSpeed = next
                                            repo.saveAutoScrollSpeed(next)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "−",
                                        color = PureWhite,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                val speedFormatted = if (autoScrollSpeed % 1f == 0f) {
                                    "${autoScrollSpeed.toInt()}x"
                                } else {
                                    String.format(java.util.Locale.US, "%.2fx", autoScrollSpeed).replace(".00", "").replace(".50", ".5")
                                }
                                Text(
                                    text = strings.speedLabel(speedFormatted),
                                    color = PureWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(PillBackground)
                                        .border(1.dp, PillBorder, CircleShape)
                                        .clickable {
                                            val next = ((kotlin.math.round((autoScrollSpeed + 0.25f) * 4f)) / 4f).coerceAtMost(8.0f)
                                            autoScrollSpeed = next
                                            repo.saveAutoScrollSpeed(next)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+",
                                        color = PureWhite,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Subtle Divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(CardBorder)
                        )

                        // Sub-row 2: Navigation & Font Size
                        // [▶ السورة السابقة] | [A+] [حجم الخط] [A−] | [◀ السورة التالية]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val canPrevSurah = surahId > 1
                            val canNextSurah = surahId < 114

                            // 1. Right Arrow (▶): السورة السابقة (cur - 1)
                            IconButton(
                                onClick = {
                                    if (canPrevSurah) {
                                        autoScrollActive = false
                                        onNavigateToSurah(surahId - 1, 1)
                                    }
                                },
                                enabled = canPrevSurah
                            ) {
                                Text(
                                    text = "▶",
                                    color = if (canPrevSurah) PureWhite else SubtleText.copy(alpha = 0.25f),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Font size controls
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable { onFontSizeChange((fontSizeSp + 2f).coerceAtMost(64f)) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "A+",
                                        color = EmeraldAccent,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(PillBackground)
                                        .clickable { onOpenSettings() }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = QuranRepository.toArabicDigits(fontSizeSp.toInt()),
                                        color = PureWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable { onFontSizeChange((fontSizeSp - 2f).coerceAtLeast(18f)) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "A-",
                                        color = PureWhite,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // 2. Left Arrow (◀): السورة التالية (cur + 1)
                            IconButton(
                                onClick = {
                                    if (canNextSurah) {
                                        autoScrollActive = false
                                        onNavigateToSurah(surahId + 1, 1)
                                    }
                                },
                                enabled = canNextSurah
                            ) {
                                Text(
                                    text = "◀",
                                    color = if (canNextSurah) PureWhite else SubtleText.copy(alpha = 0.25f),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }



            // Floating Bookmark Bar when an Ayah is tapped (via Ayah number) or long-pressed
            AnimatedVisibility(
                visible = selectedVerse != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = if (controlsVisible) safeBottomDp + 156.dp else safeBottomDp.coerceAtLeast(20.dp),
                        start = 16.dp,
                        end = 16.dp
                    )
            ) {
                val targetV = selectedVerse
                if (targetV != null) {
                    val isSaved = bookmarkedSet.contains(targetV.verseNumber)
                    val bookmarkBg = if (isSaved) {
                        if (LocalThemeColors.current.isDark) Color(0xFF3F1D24) else Color(0xFFFEE2E2)
                    } else EmeraldAccent
                    val bookmarkText = if (isSaved) {
                        if (LocalThemeColors.current.isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                    } else if (LocalThemeColors.current.isDark) Color.Black else Color.White

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(CardSurface)
                            .border(1.5.dp, EmeraldAccent.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val strings = LocalAppStrings.current
                        Text(
                            text = strings.ayahModalTitle(strings.surahName(surah), targetV.verseNumber),
                            color = PureWhite,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(bookmarkBg)
                                .clickable {
                                    onToggleBookmark(targetV.surahId, targetV.verseNumber)
                                    selectedVerse = null
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isSaved) strings.removeBookmarkAction else strings.saveBookmarkAction,
                                color = bookmarkText,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(PillBackground)
                                .border(1.dp, PillBorder, CircleShape)
                                .clickable { selectedVerse = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                color = PureWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Quick Jump Modal (Jump to any Surah or any Ayah in current Surah)
            if (showQuickJumpModal) {
                QuickJumpDialog(
                    currentSurah = surah,
                    repo = repo,
                    onDismiss = { showQuickJumpModal = false },
                    onSelectSurahAndVerse = { targetSurahId, targetVerseNum ->
                        showQuickJumpModal = false
                        controlsVisible = false
                        if (targetSurahId == surahId) {
                            val idx = if (targetVerseNum <= 1) {
                                0
                            } else {
                                val lineIdx = centeredLines.indexOfFirst { line ->
                                    line.tokens.any { it.verse.verseNumber == targetVerseNum }
                                }.coerceAtLeast(0)
                                val scrollLineIdx = (lineIdx - halfScreenLines).coerceAtLeast(0)
                                if (lineIdx < halfScreenLines) 0 else headerItemsCount + scrollLineIdx
                            }
                            coroutineScope.launch {
                                listState.scrollToItem(idx)
                            }
                        } else {
                            autoScrollActive = false
                            onNavigateToSurah(targetSurahId, targetVerseNum)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickJumpDialog(
    currentSurah: SurahInfo,
    repo: QuranRepository,
    onDismiss: () -> Unit,
    onSelectSurahAndVerse: (surahId: Int, verseNumber: Int) -> Unit
) {
    var selectedSurahId by remember { mutableIntStateOf(currentSurah.id) }
    var surahFilter by remember { mutableStateOf("") }
    val selectedSurah = remember(selectedSurahId) { repo.getSurah(selectedSurahId) }
    val filteredSurahs = remember(surahFilter) { repo.searchSurahs(surahFilter) }
    val dialogShape = RoundedCornerShape(28.dp)

    val strings = LocalAppStrings.current
    val appDir = if (strings.isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides appDir) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .clip(dialogShape)
                        .background(CardSurface)
                        .border(1.dp, CardBorder, dialogShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // Intercept clicks inside
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.quickJumpTitle,
                            color = PureWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(PillBackground)
                                .border(1.dp, PillBorder, CircleShape)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = PureWhite, fontSize = 14.sp)
                        }
                    }

                    // Filter input
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp))
                            .background(PillBackground)
                            .border(1.dp, PillBorder, RoundedCornerShape(999.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (surahFilter.isEmpty()) {
                            Text(
                                text = strings.quickJumpFilterPlaceholder,
                                color = SubtleText,
                                fontSize = 13.5.sp
                            )
                        }
                        BasicTextField(
                            value = surahFilter,
                            onValueChange = { surahFilter = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = PureWhite,
                                fontSize = 14.sp,
                                textAlign = if (strings.isArabic) TextAlign.Right else TextAlign.Left
                            ),
                            cursorBrush = SolidColor(EmeraldAccent),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Two-column Split: Right = Surahs, Left = Verses of Selected Surah
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Surahs Column
                        LazyColumn(
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(14.dp))
                                .background(PillBackground)
                                .border(1.dp, PillBorder, RoundedCornerShape(14.dp)),
                            contentPadding = PaddingValues(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredSurahs, key = { it.id }) { s ->
                                val isSel = s.id == selectedSurahId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) EmeraldAccent.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable { selectedSurahId = s.id }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${strings.formatNumber(s.id)}. ${strings.surahName(s)}",
                                        color = if (isSel) EmeraldAccent else PureWhite,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = strings.pageShort(s.startPage),
                                        color = SubtleText,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }

                        // Verses Column
                        val verseCount = selectedSurah.versesCount
                        LazyColumn(
                            modifier = Modifier
                                .weight(0.9f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(14.dp))
                                .background(PillBackground)
                                .border(1.dp, PillBorder, RoundedCornerShape(14.dp)),
                            contentPadding = PaddingValues(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(verseCount, key = { it + 1 }) { idx ->
                                val vNum = idx + 1
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CardSurface)
                                        .clickable { onSelectSurahAndVerse(selectedSurahId, vNum) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.verseNumberLabel(vNum),
                                        color = PureWhite,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Direct button to open start of selected Surah
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp))
                            .background(EmeraldAccent)
                            .clickable { onSelectSurahAndVerse(selectedSurahId, 1) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.openSurahStart(strings.surahName(selectedSurah)),
                            color = if (LocalThemeColors.current.isDark) Color.Black else Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    currentTheme: AppTheme,
    onThemeSelect: (AppTheme) -> Unit,
    currentLanguage: AppLanguage,
    onLanguageSelect: (AppLanguage) -> Unit,
    isTajweedEnabled: Boolean,
    onTajweedEnabledChange: (Boolean) -> Unit,
    fontSizeSp: Float,
    onFontSizeChange: (Float) -> Unit,
    autoScrollSpeed: Float,
    onAutoScrollSpeedChange: (Float) -> Unit,
    repo: QuranRepository,
    onDismiss: () -> Unit
) {
    var showTajweedGuide by remember { mutableStateOf(false) }
    val strings = LocalAppStrings.current
    val appDir = if (strings.isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val uthmaniFont = remember { FontFamily(repo.uthmaniTypeface) }

    val dialogShape = RoundedCornerShape(28.dp)
    val sectionCardShape = RoundedCornerShape(18.dp)
    val pillShape = RoundedCornerShape(12.dp)

    CompositionLocalProvider(LocalLayoutDirection provides appDir) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .fillMaxHeight(0.86f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    shape = dialogShape,
                    color = CardSurface,
                    tonalElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 1. FIXED TOP HEADER
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardSurface)
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(EmeraldAccent.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⚙", fontSize = 20.sp)
                                }
                                Column {
                                    Text(
                                        text = strings.settingsTitle,
                                        color = PureWhite,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = strings.settingsSubtitle,
                                        color = SubtleText,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(PillBackground)
                                    .border(1.dp, PillBorder, CircleShape)
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✕", color = PureWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(CardBorder)
                        )

                        // 2. CLEANLY GROUPED SCROLLABLE CONTENT
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // --- SECTION 1: LANGUAGE ---
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(sectionCardShape)
                                    .background(PillBackground.copy(alpha = 0.55f))
                                    .border(1.dp, CardBorder, sectionCardShape)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = strings.languageSectionTitle,
                                    color = EmeraldAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val langOptions = listOf(
                                        AppLanguage.SYSTEM to strings.langSystem,
                                        AppLanguage.ARABIC to strings.langArabic,
                                        AppLanguage.ENGLISH to strings.langEnglish
                                    )
                                    langOptions.forEach { (lang, label) ->
                                        val selected = (lang == currentLanguage)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(pillShape)
                                                .background(if (selected) EmeraldAccent.copy(alpha = 0.18f) else CardSurface)
                                                .border(
                                                    width = if (selected) 1.5.dp else 1.dp,
                                                    color = if (selected) EmeraldAccent else PillBorder,
                                                    shape = pillShape
                                                )
                                                .clickable { onLanguageSelect(lang) }
                                                .padding(vertical = 9.dp, horizontal = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (selected) EmeraldAccent else PureWhite,
                                                fontSize = 12.5.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // --- SECTION 2: APPEARANCE (2x2 COMPACT GRID) ---
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(sectionCardShape)
                                    .background(PillBackground.copy(alpha = 0.55f))
                                    .border(1.dp, CardBorder, sectionCardShape)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = strings.appearanceSectionTitle,
                                    color = EmeraldAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val themes = AppTheme.values().toList()
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    themes.chunked(2).forEach { rowThemes ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowThemes.forEach { theme ->
                                                val isSelected = (theme == currentTheme)
                                                val themeColors = getThemeColors(theme)
                                                Row(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(pillShape)
                                                        .background(if (isSelected) EmeraldAccent.copy(alpha = 0.14f) else CardSurface)
                                                        .border(
                                                            width = if (isSelected) 1.5.dp else 1.dp,
                                                            color = if (isSelected) EmeraldAccent else PillBorder,
                                                            shape = pillShape
                                                        )
                                                        .clickable { onThemeSelect(theme) }
                                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                            .background(themeColors.background)
                                                            .border(1.5.dp, if (isSelected) EmeraldAccent else CardBorder, CircleShape)
                                                    )
                                                    Text(
                                                        text = strings.themeTitle(theme),
                                                        color = if (isSelected) EmeraldAccent else PureWhite,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // --- SECTION 3: MUSHAF & READING ---
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(sectionCardShape)
                                    .background(PillBackground.copy(alpha = 0.55f))
                                    .border(1.dp, CardBorder, sectionCardShape)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = strings.mushafSectionTitle,
                                    color = EmeraldAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // Tajweed vs Standard segmented control
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    @Composable
                                    fun ModeOption(
                                        selected: Boolean,
                                        title: String,
                                        desc: String,
                                        onClick: () -> Unit,
                                        modifier: Modifier
                                    ) {
                                        Column(
                                            modifier = modifier
                                                .clip(pillShape)
                                                .background(if (selected) EmeraldAccent.copy(alpha = 0.14f) else CardSurface)
                                                .border(
                                                    width = if (selected) 1.5.dp else 1.dp,
                                                    color = if (selected) EmeraldAccent else PillBorder,
                                                    shape = pillShape
                                                )
                                                .clickable(onClick = onClick)
                                                .padding(horizontal = 10.dp, vertical = 9.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = title,
                                                color = if (selected) EmeraldAccent else PureWhite,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = desc,
                                                color = SubtleText,
                                                fontSize = 10.5.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    ModeOption(
                                        selected = isTajweedEnabled,
                                        title = strings.tajweedModeTitle,
                                        desc = strings.tajweedModeDesc,
                                        onClick = { onTajweedEnabledChange(true) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ModeOption(
                                        selected = !isTajweedEnabled,
                                        title = strings.normalModeTitle,
                                        desc = strings.normalModeDesc,
                                        onClick = { onTajweedEnabledChange(false) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Tajweed Guide row
                                if (isTajweedEnabled) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(pillShape)
                                            .background(EmeraldAccent.copy(alpha = 0.09f))
                                            .border(1.dp, EmeraldAccent.copy(alpha = 0.30f), pillShape)
                                            .clickable { showTajweedGuide = true }
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(text = "🎨", fontSize = 14.sp)
                                            Text(
                                                text = strings.tajweedGuideButton,
                                                color = EmeraldAccent,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            val previewDots = listOf(
                                                Color(0xFFE30000), Color(0xFFFF8E3B), Color(0xFFFF5E8E),
                                                Color(0xFF26B55D), Color(0xFF3C84D5), Color(0xFF00DEFF)
                                            )
                                            for (dotColor in previewDots) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(dotColor)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = strings.viewArrow,
                                                color = EmeraldAccent,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CardBorder))

                                // Font Size Control + Preview
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = strings.fontSizeTitle,
                                            color = PureWhite,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = strings.fontSizeUnit(fontSizeSp.toInt()),
                                            color = EmeraldAccent,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(pillShape)
                                            .background(CardSurface)
                                            .border(1.dp, PillBorder, pillShape)
                                            .padding(vertical = 10.dp, horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                            fontFamily = uthmaniFont,
                                            color = PureWhite,
                                            fontSize = (fontSizeSp * 0.7f).coerceIn(16f, 32f).sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(CardSurface)
                                                .border(1.dp, PillBorder, CircleShape)
                                                .clickable { onFontSizeChange((fontSizeSp - 2f).coerceAtLeast(18f)) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "−", color = PureWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                        }

                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Slider(
                                                value = fontSizeSp,
                                                onValueChange = { onFontSizeChange(it) },
                                                valueRange = 18f..64f,
                                                modifier = Modifier.weight(1f),
                                                colors = SliderDefaults.colors(
                                                    thumbColor = EmeraldAccent,
                                                    activeTrackColor = EmeraldAccent,
                                                    inactiveTrackColor = PillBorder
                                                )
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(CardSurface)
                                                .border(1.dp, PillBorder, CircleShape)
                                                .clickable { onFontSizeChange((fontSizeSp + 2f).coerceAtMost(64f)) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "+", color = PureWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CardBorder))

                                // Auto-Scroll Speed
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = strings.autoScrollSpeedTitle,
                                            color = PureWhite,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${autoScrollSpeed}x",
                                            color = EmeraldAccent,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(CardSurface)
                                                .border(1.dp, PillBorder, CircleShape)
                                                .clickable {
                                                    val next = ((kotlin.math.round((autoScrollSpeed - 0.25f) * 4f)) / 4f).coerceAtLeast(0.5f)
                                                    onAutoScrollSpeedChange(next)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "−", color = PureWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                        }

                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Slider(
                                                value = autoScrollSpeed,
                                                onValueChange = { onAutoScrollSpeedChange((kotlin.math.round(it * 4f)) / 4f) },
                                                valueRange = 0.5f..6.0f,
                                                modifier = Modifier.weight(1f),
                                                colors = SliderDefaults.colors(
                                                    thumbColor = EmeraldAccent,
                                                    activeTrackColor = EmeraldAccent,
                                                    inactiveTrackColor = PillBorder
                                                )
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(CardSurface)
                                                .border(1.dp, PillBorder, CircleShape)
                                                .clickable {
                                                    val next = ((kotlin.math.round((autoScrollSpeed + 0.25f) * 4f)) / 4f).coerceAtMost(6.0f)
                                                    onAutoScrollSpeedChange(next)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "+", color = PureWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // --- SECTION 4: KEEP SCREEN ON ---
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(sectionCardShape)
                                    .background(EmeraldAccent.copy(alpha = 0.08f))
                                    .border(1.dp, EmeraldAccent.copy(alpha = 0.25f), sectionCardShape)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(text = "💡", fontSize = 17.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.keepScreenOnTitle,
                                        color = PureWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = strings.keepScreenOnDesc,
                                        color = SubtleText,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = strings.enabledBadge,
                                    color = EmeraldAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // --- SECTION 5: SUPPORT (BuyMeACoffee at the very bottom, neat & simple) ---
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(sectionCardShape)
                                    .background(GoldAccent.copy(alpha = 0.09f))
                                    .border(1.dp, GoldAccent.copy(alpha = 0.35f), sectionCardShape)
                                    .clickable {
                                        try {
                                            uriHandler.openUri("https://buymeacoffee.com/hxh3mk")
                                        } catch (_: Exception) {
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(GoldAccent.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "☕", fontSize = 16.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.buyMeACoffeeTitle,
                                        color = GoldAccent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = strings.buyMeACoffeeSubtitle,
                                        color = SubtleText,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = if (strings.isArabic) "◀" else "▶",
                                    color = GoldAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(CardBorder)
                        )

                        // 3. FIXED BOTTOM FOOTER
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardSurface)
                                .padding(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(EmeraldAccent)
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.saveAndBackButton,
                                    color = if (LocalThemeColors.current.isDark) Color.Black else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showTajweedGuide) {
            TajweedGuideDialog(onDismiss = { showTajweedGuide = false })
        }
    }
}

@Composable
fun TajweedGuideDialog(
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val appDir = if (strings.isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides appDir) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .fillMaxHeight(0.82f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    shape = RoundedCornerShape(26.dp),
                    color = CardSurface,
                    tonalElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardSurface)
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(EmeraldAccent.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎨", fontSize = 18.sp)
                                }
                                Column {
                                    Text(
                                        text = strings.tajweedGuideHeader,
                                        color = PureWhite,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = strings.tajweedGuideSubheader,
                                        color = SubtleText,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PillBackground)
                                    .border(1.dp, PillBorder, CircleShape)
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✕", color = PureWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(CardBorder)
                        )

                        val ruleColors = listOf(
                            Color(0xFFE30000),
                            Color(0xFFFF8E3B),
                            Color(0xFFFF5E8E),
                            Color(0xFF26B55D),
                            Color(0xFF3C84D5),
                            Color(0xFF00DEFF),
                            Color(0xFF9D9999)
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            strings.tajweedRules.forEachIndexed { index, rule ->
                                TajweedRuleCard(
                                    color = ruleColors.getOrElse(index) { Color.White },
                                    title = rule.title,
                                    subtitle = rule.subtitle,
                                    description = rule.description,
                                    example = rule.example
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(CardBorder)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardSurface)
                                .padding(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(EmeraldAccent)
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.closeGuideButton,
                                    color = if (LocalThemeColors.current.isDark) Color.Black else Color.White,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TajweedRuleCard(
    color: Color,
    title: String,
    subtitle: String,
    description: String,
    example: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PillBackground)
            .border(1.dp, PillBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Colored circular badge
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, PureWhite.copy(alpha = 0.25f), CircleShape)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = PureWhite,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = SubtleText,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = example,
                    color = color,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
