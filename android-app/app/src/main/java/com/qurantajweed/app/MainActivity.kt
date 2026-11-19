package com.qurantajweed.app

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var repo: QuranRepository
    private var displayListener: DisplayManager.DisplayListener? = null
    private var lastBackPressTime = 0L

    // Observable state for Compose
    private var cutoutPaddingState by mutableStateOf(CameraCutoutPadding())
    private var activeSurahIdState by mutableIntStateOf(0) // 0 = Home Screen (Surahs List)
    private var activeInitialVerseState by mutableIntStateOf(1)
    private var currentReadingVerseNumber: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on continuously while app is open - Never turn off or dim screen
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.keepScreenOn = true

        // True Edge-to-Edge Fullscreen with Camera Cutout support in Portrait & Landscape
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.decorView.setBackgroundColor(Color.BLACK)

        applyFullScreen()

        repo = QuranRepository(applicationContext)

        // Listen for WindowInsets / DisplayCutout changes
        window.decorView.setOnApplyWindowInsetsListener { v, insets ->
            updateCameraCutoutPadding()
            v.onApplyWindowInsets(insets)
        }

        // Listen for 90° / 270° landscape flips
        val dm = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        if (dm != null) {
            val listener = object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) {}
                override fun onDisplayRemoved(displayId: Int) {}
                override fun onDisplayChanged(displayId: Int) {
                    updateCameraCutoutPadding()
                    window.decorView.postDelayed({ updateCameraCutoutPadding() }, 120)
                }
            }
            displayListener = listener
            dm.registerDisplayListener(listener, null)
        }

        // Handle Back Navigation cleanly
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (activeSurahIdState != 0) {
                    activeSurahIdState = 0
                } else {
                    val now = System.currentTimeMillis()
                    if (now - lastBackPressTime < 2000L) {
                        finish()
                    } else {
                        lastBackPressTime = now
                        val lang = AppLanguage.fromCode(repo.getAppLanguage())
                        val msg = if (resolveIsArabic(lang)) ArabicStrings.pressBackAgainToExit else EnglishStrings.pressBackAgainToExit
                        Toast.makeText(
                            this@MainActivity,
                            msg,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })

        setContent {
            val rootAndroidView = androidx.compose.ui.platform.LocalView.current
            DisposableEffect(rootAndroidView) {
                rootAndroidView.keepScreenOn = true
                onDispose {
                    rootAndroidView.keepScreenOn = true
                }
            }

            var isReady by remember { mutableStateOf(repo.isLoaded) }
            var fontSizeSp by remember { mutableFloatStateOf(30f) }
            var lastRead by remember { mutableStateOf<Pair<Int, Int>?>(null) }
            var bookmarks by remember { mutableStateOf<List<BookmarkEntry>>(emptyList()) }

            var currentTheme by remember {
                val savedThemeId = repo.getAppTheme()
                mutableStateOf(
                    AppTheme.values().firstOrNull { it.id == savedThemeId } ?: AppTheme.OLED
                )
            }
            var isTajweedEnabled by remember {
                mutableStateOf(repo.isTajweedEnabled())
            }
            var currentLanguage by remember {
                mutableStateOf(AppLanguage.fromCode(repo.getAppLanguage()))
            }
            val isArabic = resolveIsArabic(currentLanguage)
            val appStrings = if (isArabic) ArabicStrings else EnglishStrings
            val appLayoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

            var showSettingsDialog by remember { mutableStateOf(false) }
            var autoScrollSpeed by remember { mutableFloatStateOf(repo.getAutoScrollSpeed()) }

            val themeColors = remember(currentTheme) { getThemeColors(currentTheme) }

            // Sync window background and status bar icons
            LaunchedEffect(currentTheme, themeColors) {
                val windowBg = if (themeColors.isDark) {
                    when (currentTheme) {
                        AppTheme.OLED -> Color.BLACK
                        AppTheme.DARK -> 0xFF15161E.toInt()
                        else -> Color.BLACK
                    }
                } else {
                    when (currentTheme) {
                        AppTheme.SEPIA -> 0xFFFBF1D8.toInt()
                        AppTheme.LIGHT -> 0xFFF8FAFC.toInt()
                        else -> Color.WHITE
                    }
                }
                window.decorView.setBackgroundColor(windowBg)
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !themeColors.isDark
                insetsController.isAppearanceLightNavigationBars = !themeColors.isDark
            }

            LaunchedEffect(Unit) {
                if (!repo.isLoaded) {
                    withContext(Dispatchers.IO) {
                        repo.ensureLoaded()
                    }
                }
                fontSizeSp = repo.getFontSizeSp()
                lastRead = repo.getLastReadPosition()
                bookmarks = repo.getBookmarks()
                isReady = true
                updateCameraCutoutPadding()
            }

            CompositionLocalProvider(
                LocalAppTheme provides currentTheme,
                LocalThemeColors provides themeColors,
                LocalTajweedEnabled provides isTajweedEnabled,
                LocalAppStrings provides appStrings,
                LocalLayoutDirection provides appLayoutDirection
            ) {
                if (!isReady) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(themeColors.background),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = themeColors.accentEmerald
                        )
                    }
                } else {
                    val currentSurahId = activeSurahIdState
                    if (currentSurahId == 0) {
                        HomeScreen(
                            repo = repo,
                            cutout = cutoutPaddingState,
                            lastRead = lastRead,
                            bookmarks = bookmarks,
                            onOpenSurah = { surahId, initialVerse ->
                                activeInitialVerseState = initialVerse
                                currentReadingVerseNumber = initialVerse
                                activeSurahIdState = surahId
                                repo.saveLastReadPosition(surahId, initialVerse)
                                lastRead = Pair(surahId, initialVerse)
                            },
                            onRemoveBookmark = { surahId, verseNum ->
                                repo.removeBookmark(surahId, verseNum)
                                bookmarks = repo.getBookmarks()
                                Toast.makeText(
                                    this@MainActivity,
                                    appStrings.bookmarkRemovedToast,
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onOpenSettings = {
                                showSettingsDialog = true
                            }
                        )
                    } else {
                        ContinuousReaderScreen(
                            surahId = currentSurahId,
                            initialVerse = activeInitialVerseState,
                            fontSizeSp = fontSizeSp,
                            onFontSizeChange = { newSize ->
                                fontSizeSp = newSize
                                repo.saveFontSizeSp(newSize)
                            },
                            cutout = cutoutPaddingState,
                            repo = repo,
                            bookmarks = bookmarks,
                            onToggleBookmark = { surahId, verseNum ->
                                vibrateLight()
                                val added = repo.toggleBookmark(surahId, verseNum)
                                bookmarks = repo.getBookmarks()
                                val msg = if (added) {
                                    appStrings.bookmarkAddedToast
                                } else {
                                    appStrings.bookmarkRemovedToast
                                }
                                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                added
                            },
                            onNavigateToSurah = { targetSurahId, targetVerse ->
                                activeInitialVerseState = targetVerse
                                currentReadingVerseNumber = targetVerse
                                activeSurahIdState = targetSurahId
                                repo.saveLastReadPosition(targetSurahId, targetVerse)
                                lastRead = Pair(targetSurahId, targetVerse)
                            },
                            onSaveReadingPosition = { surahId, verseNum ->
                                currentReadingVerseNumber = verseNum
                                repo.saveLastReadPosition(surahId, verseNum)
                                lastRead = Pair(surahId, verseNum)
                            },
                            onBackToHome = {
                                activeSurahIdState = 0
                            },
                            onOpenSettings = {
                                showSettingsDialog = true
                            }
                        )
                    }

                    if (showSettingsDialog) {
                        SettingsDialog(
                            currentTheme = currentTheme,
                            onThemeSelect = { newTheme ->
                                currentTheme = newTheme
                                repo.saveAppTheme(newTheme.id)
                            },
                            currentLanguage = currentLanguage,
                            onLanguageSelect = { newLang ->
                                currentLanguage = newLang
                                repo.saveAppLanguage(newLang.code)
                            },
                            isTajweedEnabled = isTajweedEnabled,
                            onTajweedEnabledChange = { enabled ->
                                isTajweedEnabled = enabled
                                repo.saveTajweedEnabled(enabled)
                            },
                            fontSizeSp = fontSizeSp,
                            onFontSizeChange = { newSize ->
                                fontSizeSp = newSize
                                repo.saveFontSizeSp(newSize)
                            },
                            autoScrollSpeed = autoScrollSpeed,
                            onAutoScrollSpeedChange = { newSpeed ->
                                autoScrollSpeed = newSpeed
                                repo.saveAutoScrollSpeed(newSpeed)
                            },
                            repo = repo,
                            onDismiss = {
                                showSettingsDialog = false
                            }
                        )
                    }
                }
            }
        }
    }

    private fun updateCameraCutoutPadding() {
        try {
            val density = resources.displayMetrics.density.coerceAtLeast(1f)
            var topPx = 0
            var leftPx = 0
            var rightPx = 0
            var bottomPx = 0

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val rootInsets = window.decorView.rootWindowInsets
                val cutout = rootInsets?.displayCutout
                if (cutout != null) {
                    topPx = cutout.safeInsetTop
                    leftPx = cutout.safeInsetLeft
                    rightPx = cutout.safeInsetRight
                    bottomPx = cutout.safeInsetBottom
                }
            }

            cutoutPaddingState = CameraCutoutPadding(
                topDp = (topPx / density).dp,
                leftDp = (leftPx / density).dp,
                rightDp = (rightPx / density).dp,
                bottomDp = (bottomPx / density).dp
            )
        } catch (_: Exception) {
        }
    }

    private fun vibrateLight() {
        try {
            @Suppress("DEPRECATION")
            val v = getSystemService(VIBRATOR_SERVICE) as? Vibrator
            if (v != null && v.hasVibrator()) {
                v.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {
        }
    }

    private fun applyFullScreen() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyFullScreen()
        updateCameraCutoutPadding()
        window.decorView.postDelayed({ updateCameraCutoutPadding() }, 150)
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.keepScreenOn = true
        applyFullScreen()
        updateCameraCutoutPadding()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.decorView.keepScreenOn = true
            applyFullScreen()
            updateCameraCutoutPadding()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::repo.isInitialized && activeSurahIdState in 1..114) {
            repo.commitLastReadPosition(activeSurahIdState, currentReadingVerseNumber)
        }
    }

    override fun onStop() {
        super.onStop()
        if (::repo.isInitialized && activeSurahIdState in 1..114) {
            repo.commitLastReadPosition(activeSurahIdState, currentReadingVerseNumber)
        }
    }

    override fun onDestroy() {
        displayListener?.let { listener ->
            val dm = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            dm?.unregisterDisplayListener(listener)
        }
        super.onDestroy()
    }
}
