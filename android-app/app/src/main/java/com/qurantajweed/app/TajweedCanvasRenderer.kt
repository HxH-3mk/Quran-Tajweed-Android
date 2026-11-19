package com.qurantajweed.app

import android.content.res.Configuration
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object TajweedCanvasRenderer {

    private val layerCharBuf = CharArray(1)

    fun drawColoredGlyphString(
        canvas: android.graphics.Canvas,
        text: String,
        rightX: Float,
        baselineY: Float,
        fontId: Int,
        typeface: Typeface,
        textSizePx: Float,
        repo: QuranRepository,
        paint: Paint,
        isDarkTheme: Boolean = true,
        isTajweedEnabled: Boolean = true
    ) {
        paint.typeface = typeface
        paint.textSize = textSizePx
        paint.textAlign = Paint.Align.LEFT

        val normalTextColor = TajweedPalette.getNormalTextColor(isDarkTheme)

        var penX = rightX
        for (i in 0 until text.length) {
            val ch = text[i]
            val cp = ch.code
            layerCharBuf[0] = ch
            val charAdvance = paint.measureText(layerCharBuf, 0, 1)
            penX -= charAdvance

            if (isTajweedEnabled) {
                val layers = repo.getColrLayers(fontId, cp)
                if (layers != null && layers.isNotEmpty()) {
                    for (packed in layers) {
                        val layerCp = (packed ushr 8) and 0xFFFF
                        val colorId = packed and 0xFF
                        layerCharBuf[0] = layerCp.toChar()
                        paint.color = TajweedPalette.getColor(colorId, isDarkTheme)
                        canvas.drawText(layerCharBuf, 0, 1, penX, baselineY, paint)
                    }
                } else {
                    layerCharBuf[0] = ch
                    paint.color = normalTextColor
                    canvas.drawText(layerCharBuf, 0, 1, penX, baselineY, paint)
                }
            } else {
                // Normal Quran mode (بدون تجويد): توحيد اللون بالكامل لكل الحروف والكلمات
                layerCharBuf[0] = ch
                paint.color = normalTextColor
                canvas.drawText(layerCharBuf, 0, 1, penX, baselineY, paint)
            }
        }
    }
}

class CenteredLineToken(
    val verse: VerseData,
    val text: String,
    val isAyahEnd: Boolean,
    val offsetFromLineRight: Float,
    val widthPx: Float
)

class CenteredSurahLine(
    val lineIndex: Int,
    val firstVerseNumber: Int,
    val lineHeightPx: Float,
    val contentWidth: Float,
    val tokens: List<CenteredLineToken>
)

fun buildCenteredSurahLines(
    verses: List<VerseData>,
    fontSizeSp: Float,
    densityPxPerDp: Float,
    availableWidthPx: Float,
    repo: QuranRepository
): List<CenteredSurahLine> {
    if (verses.isEmpty()) return emptyList()

    val textSizePx = fontSizeSp * densityPxPerDp
    val horizontalMargin = 2f
    val maxLineWidth = (availableWidthPx - horizontalMargin * 2f).coerceAtLeast(120f)
    val wordGap = (textSizePx * 0.16f).coerceAtLeast(5f)
    val lineHeightPx = textSizePx * 1.94f

    val measurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = textSizePx
    }

    // Flatten all tokens of the Surah into a single continuous stream so every Ayah continues
    // immediately after the previous Ayah on the exact same line!
    class RawToken(
        val verse: VerseData,
        val text: String,
        val isAyahEnd: Boolean,
        val widthPx: Float
    )

    val rawTokens = ArrayList<RawToken>(verses.size * 10)
    var cachedPage = -1
    val charBuf = CharArray(1)

    for (v in verses) {
        if (v.page != cachedPage) {
            cachedPage = v.page
            measurePaint.typeface = repo.getTypefaceForFontId(cachedPage)
        }
        for (w in v.words) {
            var tw = 0f
            for (i in 0 until w.length) {
                charBuf[0] = w[i]
                tw += measurePaint.measureText(charBuf, 0, 1)
            }
            rawTokens.add(RawToken(v, w, false, tw))
        }
        if (v.endGlyph.isNotEmpty()) {
            var ew = 0f
            for (i in 0 until v.endGlyph.length) {
                charBuf[0] = v.endGlyph[i]
                ew += measurePaint.measureText(charBuf, 0, 1)
            }
            rawTokens.add(RawToken(v, v.endGlyph, true, ew))
        }
    }

    if (rawTokens.isEmpty()) return emptyList()

    // Wrap into lines greedily with Quranic typographic rules:
    // Never orphan an Ayah end marker alone at the beginning of a line!
    val resultLines = ArrayList<CenteredSurahLine>()
    var lineStart = 0
    var currentSpan = 0f

    fun emitCenteredLine(firstIdx: Int, lastIdx: Int) {
        if (firstIdx > lastIdx || firstIdx !in rawTokens.indices || lastIdx !in rawTokens.indices) return
        var totalWordsWidth = 0f
        for (i in firstIdx..lastIdx) {
            totalWordsWidth += rawTokens[i].widthPx
        }
        val count = lastIdx - firstIdx + 1

        // Dynamically compute the exact wordGap for this line so that lineContentWidth NEVER exceeds maxLineWidth
        val effectiveWordGap = if (count > 1) {
            val idealTotal = totalWordsWidth + wordGap * (count - 1)
            if (idealTotal > maxLineWidth) {
                // Slightly compress gaps so all tokens (including end-of-ayah marker) stay strictly within maxLineWidth
                ((maxLineWidth - totalWordsWidth) / (count - 1)).coerceIn(wordGap * 0.45f, wordGap)
            } else {
                wordGap
            }
        } else {
            0f
        }

        val lineContentWidth = (totalWordsWidth + effectiveWordGap * (count - 1).coerceAtLeast(0)).coerceAtMost(maxLineWidth)

        val placedTokens = ArrayList<CenteredLineToken>(count)
        var currentOffset = 0f
        for (i in firstIdx..lastIdx) {
            val rt = rawTokens[i]
            placedTokens.add(
                CenteredLineToken(
                    verse = rt.verse,
                    text = rt.text,
                    isAyahEnd = rt.isAyahEnd,
                    offsetFromLineRight = currentOffset,
                    widthPx = rt.widthPx
                )
            )
            currentOffset += rt.widthPx + effectiveWordGap
        }

        val firstVerseNum = rawTokens[firstIdx].verse.verseNumber
        resultLines.add(
            CenteredSurahLine(
                lineIndex = resultLines.size,
                firstVerseNumber = firstVerseNum,
                lineHeightPx = lineHeightPx,
                contentWidth = lineContentWidth,
                tokens = placedTokens
            )
        )
    }

    var i = 0
    while (i < rawTokens.size) {
        val rt = rawTokens[i]
        val w = rt.widthPx
        val needed = if (i == lineStart) w else currentSpan + wordGap + w

        if (i > lineStart && needed > maxLineWidth) {
            // Check if we can keep this token (especially Ayah end marker) by gently compressing word gaps
            val countWithThis = (i - lineStart + 1)
            var sumW = 0f
            for (k in lineStart..i) sumW += rawTokens[k].widthPx
            val minNeeded = sumW + (wordGap * 0.50f) * (countWithThis - 1)

            if (rt.isAyahEnd && minNeeded <= maxLineWidth) {
                currentSpan = minNeeded
                i++
                continue
            }

            // Normal wrap: ensure an Ayah end marker NEVER starts a line alone!
            // Pull the preceding word with it to the next line.
            val breakBefore = if (rt.isAyahEnd && i > lineStart + 1) {
                i - 1
            } else {
                i
            }

            emitCenteredLine(lineStart, breakBefore - 1)
            lineStart = breakBefore
            currentSpan = rawTokens[breakBefore].widthPx
            i = breakBefore + 1
        } else {
            currentSpan = needed
            i++
        }
    }
    if (lineStart < rawTokens.size) {
        emitCenteredLine(lineStart, rawTokens.lastIndex)
    }

    return resultLines
}

@Composable
fun TajweedCenteredLineRow(
    line: CenteredSurahLine,
    fontSizeSp: Float,
    selectedVerseKey: String?,
    bookmarkedVerseNums: Set<Int>,
    repo: QuranRepository,
    onSingleTapEmptyOrWord: () -> Unit,
    onAyahNumberTap: (VerseData) -> Unit,
    onLongPressVerse: (VerseData) -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true,
    isTajweedEnabled: Boolean = true
) {
    val density = LocalDensity.current
    val textSizePx = with(density) { fontSizeSp.dp.toPx() }
    val heightDp: Dp = with(density) { line.lineHeightPx.toDp() }

    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    }
    val highlightPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG)
    }
    val highlightRect = remember { RectF() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp)
            .pointerInput(line.lineIndex, line.contentWidth) {
                detectTapGestures(
                    onTap = { offset ->
                        val rowW = size.width.toFloat()
                        val lineRightX = ((rowW + line.contentWidth) * 0.5f).coerceIn(line.contentWidth + 2f, (rowW - 2f).coerceAtLeast(line.contentWidth + 2f))
                        val hitAyahEnd = line.tokens.firstOrNull { t ->
                            if (!t.isAyahEnd) return@firstOrNull false
                            val tRight = lineRightX - t.offsetFromLineRight
                            val tLeft = tRight - t.widthPx
                            offset.x >= tLeft - 20f && offset.x <= tRight + 20f
                        }
                        if (hitAyahEnd != null) {
                            onAyahNumberTap(hitAyahEnd.verse)
                        } else {
                            onSingleTapEmptyOrWord()
                        }
                    },
                    onLongPress = { offset ->
                        val rowW = size.width.toFloat()
                        val lineRightX = ((rowW + line.contentWidth) * 0.5f).coerceIn(line.contentWidth + 2f, (rowW - 2f).coerceAtLeast(line.contentWidth + 2f))
                        val hitToken = line.tokens.firstOrNull { t ->
                            val tRight = lineRightX - t.offsetFromLineRight
                            val tLeft = tRight - t.widthPx
                            offset.x >= tLeft - 16f && offset.x <= tRight + 16f
                        } ?: line.tokens.minByOrNull { t ->
                            val tRight = lineRightX - t.offsetFromLineRight
                            val mid = tRight - t.widthPx * 0.5f
                            kotlin.math.abs(offset.x - mid)
                        }
                        if (hitToken != null) {
                            onLongPressVerse(hitToken.verse)
                        }
                    }
                )
            }
    ) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val rowW = size.width
        // Dead-center the line horizontally in the canvas width, safely clamped from edges
        val lineRightX = ((rowW + line.contentWidth) * 0.5f).coerceIn(line.contentWidth + 2f, (rowW - 2f).coerceAtLeast(line.contentWidth + 2f))
        val baselineY = line.lineHeightPx * 0.67f

        // Only draw highlight if the verse is currently selected (user tapped Ayah number or long-pressed)
        if (selectedVerseKey != null) {
            var i = 0
            while (i < line.tokens.size) {
                val tok = line.tokens[i]
                val vKey = tok.verse.verseKey
                val isSel = (selectedVerseKey == vKey)

                if (isSel) {
                    val spanRight = lineRightX - tok.offsetFromLineRight + 8f
                    var j = i
                    while (j + 1 < line.tokens.size && line.tokens[j + 1].verse.verseKey == vKey) {
                        j++
                    }
                    val lastTok = line.tokens[j]
                    val spanLeft = lineRightX - lastTok.offsetFromLineRight - lastTok.widthPx - 8f

                    highlightRect.set(spanLeft, 4f, spanRight, size.height - 4f)
                    highlightPaint.style = Paint.Style.FILL
                    highlightPaint.color = android.graphics.Color.argb(48, 16, 185, 129)
                    nativeCanvas.drawRoundRect(highlightRect, 20f, 20f, highlightPaint)

                    highlightPaint.style = Paint.Style.STROKE
                    highlightPaint.strokeWidth = 3f
                    highlightPaint.color = android.graphics.Color.argb(190, 16, 185, 129)
                    nativeCanvas.drawRoundRect(highlightRect, 20f, 20f, highlightPaint)

                    i = j + 1
                } else {
                    i++
                }
            }
        }

        // Draw each token using its exact page font and OLED Tajweed layers
        for (t in line.tokens) {
            val tRight = lineRightX - t.offsetFromLineRight
            val tLeft = tRight - t.widthPx
            val isBm = bookmarkedVerseNums.contains(t.verse.verseNumber)

            if (t.isAyahEnd && isBm) {
                highlightPaint.style = Paint.Style.FILL
                highlightPaint.color = android.graphics.Color.argb(65, 234, 179, 8)
                val cx = (tLeft + tRight) * 0.5f
                val cy = size.height * 0.5f
                val r = (t.widthPx.coerceAtLeast(textSizePx * 0.85f)) * 0.56f
                nativeCanvas.drawCircle(cx, cy, r, highlightPaint)
            }

            val pageTypeface = repo.getTypefaceForFontId(t.verse.page)
            TajweedCanvasRenderer.drawColoredGlyphString(
                canvas = nativeCanvas,
                text = t.text,
                rightX = tRight,
                baselineY = baselineY,
                fontId = t.verse.page,
                typeface = pageTypeface,
                textSizePx = textSizePx,
                repo = repo,
                paint = paint,
                isDarkTheme = isDarkTheme,
                isTajweedEnabled = isTajweedEnabled
            )
        }
    }
}

@Composable
fun SurahOrnamentHeaderBanner(
    surah: SurahInfo,
    availableWidthPx: Float,
    repo: QuranRepository,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Header frame aspect ratio is 8256 advance : 1026 vertical span (~8.047 : 1).
    // In portrait, use ~96% width.
    // In landscape, cap width to comfortable ~490dp so it never balloons vertically.
    val maxFrameWidthPx = if (isLandscape) {
        with(density) { 490.dp.toPx() }
    } else {
        availableWidthPx * 0.96f
    }
    val targetWidthPx = maxFrameWidthPx.coerceAtMost(availableWidthPx - with(density) { 8.dp.toPx() })

    // Compute text size directly so the frame fits targetWidthPx:
    val targetTextSize = targetWidthPx / (8256f / 2500f)
    val frameHeightPx = targetTextSize * (1026f / 2500f)

    val topPadPx = with(density) { 16.dp.toPx() }
    val bottomPadPx = with(density) { 16.dp.toPx() }
    val bannerHeightDp = with(density) { (frameHeightPx + topPadPx + bottomPadPx).toDp() }

    val typeface = remember { repo.chapterHeadersTypeface }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(bannerHeightDp)
            .pointerInput(surah.id) {
                detectTapGestures(onTap = { onTap() })
            }
    ) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val text = surah.headerChar
        if (text.isNotEmpty()) {
            paint.typeface = typeface
            paint.textSize = targetTextSize
            val actualW = paint.measureText(text)
            val rightX = (size.width + actualW) * 0.5f
            // Center baseline in the middle of frame:
            val baselineY = topPadPx + frameHeightPx * 0.50f

            TajweedCanvasRenderer.drawColoredGlyphString(
                canvas = nativeCanvas,
                text = text,
                rightX = rightX,
                baselineY = baselineY,
                fontId = 606,
                typeface = typeface,
                textSizePx = targetTextSize,
                repo = repo,
                paint = paint,
                isDarkTheme = isDarkTheme
            )
        }
    }
}

@Composable
fun BismillahOrnamentBanner(
    surahId: Int,
    fontSizeSp: Float,
    availableWidthPx: Float,
    repo: QuranRepository,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // In bismillah.ttf: unitsPerEm = 2500, advance = 28014, yMax = 3174, yMin = -483, span = 3657.
    // Advance ratio to em = 28014 / 2500 = 11.2056f
    // Keep Bismillah proportional to Surah Header
    val maxBismillahWidth = if (isLandscape) {
        with(density) { 440.dp.toPx() }
    } else {
        availableWidthPx * 0.88f
    }.coerceAtMost(availableWidthPx - with(density) { 12.dp.toPx() })

    val effectiveTextSize = maxBismillahWidth / 11.2056f
    val actualAscentPx = effectiveTextSize * (3174f / 2500f)
    val actualDescentPx = effectiveTextSize * (483f / 2500f)
    val actualGlyphHeightPx = actualAscentPx + actualDescentPx

    val topPadPx = with(density) { 18.dp.toPx() }
    val bottomPadPx = with(density) { 18.dp.toPx() }
    val bannerHeightDp = with(density) { (actualGlyphHeightPx + topPadPx + bottomPadPx).toDp() }

    val typeface = remember { repo.bismillahTypeface }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG) }
    val bismillahText = remember(surahId) { repo.getBismillahText(surahId) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(bannerHeightDp)
            .pointerInput(surahId) {
                detectTapGestures(onTap = { onTap() })
            }
    ) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        paint.typeface = typeface
        paint.textSize = effectiveTextSize

        var finalW = 0f
        val buf = CharArray(1)
        for (i in 0 until bismillahText.length) {
            buf[0] = bismillahText[i]
            finalW += paint.measureText(buf, 0, 1)
        }
        val rightX = (size.width + finalW) * 0.5f

        // Baseline positioned mathematically from exact typographic ascent:
        // Top of glyph will be at baselineY - actualAscentPx = topPadPx!
        val baselineY = topPadPx + actualAscentPx

        TajweedCanvasRenderer.drawColoredGlyphString(
            canvas = nativeCanvas,
            text = bismillahText,
            rightX = rightX,
            baselineY = baselineY,
            fontId = 605,
            typeface = typeface,
            textSizePx = effectiveTextSize,
            repo = repo,
            paint = paint,
            isDarkTheme = isDarkTheme
        )
    }
}
