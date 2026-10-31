package com.danielmccullum.hrzones

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface

/**
 * Draws the whole field: the heart rate number on top and, below it, a row of zone segments in
 * the style of the Apple Watch workout view. The current zone expands into a pill labelled
 * "ZONE n" with a bolt, and a small pointer under the pill shows the position inside the zone.
 * The bitmap is transparent so Karoo's own card and header show through.
 */
object ZoneBarRenderer {
    // Z1..Z5 sampled from Karoo's own HR zone field; zones beyond five reuse the last color
    private val zoneColors = intArrayOf(
        Color.rgb(0x60, 0xEE, 0xB2), // mint
        Color.rgb(0x00, 0xB9, 0x88), // teal green
        Color.rgb(0xFF, 0xF5, 0x00), // yellow
        Color.rgb(0xFB, 0x8C, 0x65), // salmon
        Color.rgb(0xD6, 0x04, 0x04), // red
    )
    private const val DIMMED_ALPHA = 110
    private const val PILL_WEIGHT = 3.2f
    private const val POINTER_BELOW = 0.45f
    private const val SMALL_WIDTH = 320
    private const val SIDE_LEFT = 11f
    private const val SIDE_RIGHT = 9f
    private val KAROO_GREEN = Color.rgb(18, 154, 94)

    // Karoo's own number font ships in the system image
    private val numberTypeface: Typeface by lazy { systemFont("IBMPlexSansCondensed-Medium.otf", Typeface.DEFAULT_BOLD) }
    private val labelTypeface: Typeface by lazy { systemFont("IBMPlexSansCondensed-Medium.otf", Typeface.DEFAULT_BOLD) }

    private fun systemFont(file: String, fallback: Typeface): Typeface =
        try {
            Typeface.createFromFile("/system/fonts/$file")
        } catch (_: Exception) {
            fallback
        }

    fun colorFor(index: Int): Int = zoneColors[index.coerceAtMost(zoneColors.lastIndex)]

    fun render(
        width: Int,
        height: Int,
        zoneCount: Int,
        position: ZonePosition?,
        hr: Int?,
        dark: Boolean,
    ): Bitmap {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val ink = if (dark) Color.WHITE else Color.BLACK

        // Header proportions measured from Karoo's own fields. Half-width cells put the title lower
        // and slightly smaller than full-width ones; side margins are fixed pixels in both.
        val small = w < SMALL_WIDTH
        val headerTop = h * if (small) 0.17f else 0.082f
        val titleCap = h * if (small) 0.15f else 0.17f
        val iconTop = h * if (small) 0.153f else 0.061f
        val iconWidth = h * if (small) 0.177f else 0.184f
        val iconHeight = h * if (small) 0.17f else 0.177f
        drawHeart(canvas, paint, RectF(SIDE_LEFT, iconTop, SIDE_LEFT + iconWidth, iconTop + iconHeight), KAROO_GREEN)
        drawNumber(canvas, paint, hr, w, headerTop, headerTop + titleCap, SIDE_RIGHT, ink)

        // The pointer straddles the bottom edge of the pill: most of it overlaps the pill, the rest hangs below
        val pointerHeight = h * 0.13f
        val rowTop = headerTop + titleCap + h * if (small) 0.07f else 0.085f
        val rowBottom = h - h * 0.04f - pointerHeight * POINTER_BELOW
        if (zoneCount > 0 && rowBottom > rowTop) {
            // Row edges line up with the icon on the left and the number on the right
            drawZones(canvas, paint, w, zoneCount, position, rowTop, rowBottom, pointerHeight, SIDE_LEFT, SIDE_RIGHT, ink)
        }
        return bitmap
    }

    /** Draws an outline heart whose outer edge fills [box], like Karoo's own field icons. */
    private fun drawHeart(canvas: Canvas, paint: Paint, box: RectF, color: Int) {
        // Unit-square heart: every control point stays inside 0..1 so the curve spans the box exactly
        val path = Path().apply {
            moveTo(0.5f, 1f)
            cubicTo(0.2f, 0.78f, 0f, 0.58f, 0f, 0.3f)
            cubicTo(0f, 0.12f, 0.13f, 0f, 0.27f, 0f)
            cubicTo(0.4f, 0f, 0.47f, 0.08f, 0.5f, 0.18f)
            cubicTo(0.53f, 0.08f, 0.6f, 0f, 0.73f, 0f)
            cubicTo(0.87f, 0f, 1f, 0.12f, 1f, 0.3f)
            cubicTo(1f, 0.58f, 0.8f, 0.78f, 0.5f, 1f)
            close()
        }
        val strokeWidth = box.height() * 0.115f
        val inner = RectF(box).apply { inset(strokeWidth / 2, strokeWidth / 2) }
        path.transform(Matrix().apply { setRectToRect(RectF(0f, 0f, 1f, 1f), inner, Matrix.ScaleToFit.FILL) })
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.strokeJoin = Paint.Join.ROUND
        paint.alpha = 255
        paint.color = color
        canvas.drawPath(path, paint)
        paint.strokeJoin = Paint.Join.MITER
    }

    private fun drawNumber(canvas: Canvas, paint: Paint, hr: Int?, width: Int, top: Float, bottom: Float, pad: Float, ink: Int) {
        val text = hr?.toString() ?: "--"
        paint.style = Paint.Style.FILL
        paint.color = ink
        paint.typeface = numberTypeface
        paint.textAlign = Paint.Align.RIGHT
        // Size by digit height, then shrink if the text would not fit across the field
        paint.textSize = 100f
        val bounds = android.graphics.Rect()
        paint.getTextBounds("8", 0, 1, bounds)
        val capHeight = bounds.height().coerceAtLeast(1)
        var size = 100f * (bottom - top) / capHeight
        paint.textSize = size
        val maxWidth = width - 2 * pad
        val measured = paint.measureText(text)
        if (measured > maxWidth) {
            size *= maxWidth / measured
            paint.textSize = size
        }
        canvas.drawText(text, width - pad, bottom, paint)
    }

    private fun drawZones(
        canvas: Canvas,
        paint: Paint,
        width: Int,
        zoneCount: Int,
        position: ZonePosition?,
        top: Float,
        bottom: Float,
        pointerHeight: Float,
        padLeft: Float,
        padRight: Float,
        ink: Int,
    ) {
        val rowHeight = bottom - top
        val gap = rowHeight * 0.05f
        val radius = rowHeight * 0.3f
        val current = position?.index
        val totalWeight = (zoneCount - 1) + if (current != null) PILL_WEIGHT else 1f
        val unit = (width - padLeft - padRight - gap * (zoneCount - 1)) / totalWeight

        var left = padLeft
        for (i in 0 until zoneCount) {
            val segmentWidth = unit * if (i == current) PILL_WEIGHT else 1f
            val rect = RectF(left, top, left + segmentWidth, bottom)
            paint.style = Paint.Style.FILL
            paint.color = colorFor(i)
            paint.alpha = if (i == current) 255 else DIMMED_ALPHA
            canvas.drawRoundRect(rect, radius, radius, paint)
            if (i == current && position != null) {
                drawPillContent(canvas, paint, rect, i, colorFor(i))
                drawPointer(canvas, paint, rect, position.fraction, bottom - pointerHeight * (1 - POINTER_BELOW), pointerHeight, ink)
            }
            left += segmentWidth + gap
        }
    }

    private fun drawPillContent(canvas: Canvas, paint: Paint, rect: RectF, index: Int, fill: Int) {
        val label = "ZONE ${index + 1}"
        val short = "Z${index + 1}"
        val textColor = if (luminance(fill) > 0.45f) Color.BLACK else Color.WHITE
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        paint.color = textColor
        paint.typeface = labelTypeface
        paint.textAlign = Paint.Align.LEFT

        var textSize = rect.height() * 0.4f
        paint.textSize = textSize
        var text = label
        var textWidth = paint.measureText(text)
        val available = rect.width() * 0.88f
        // "ZONE n" only when it fits with room to spare; smaller fields get "Zn"
        if (textWidth > rect.width() * 0.75f) {
            text = short
            textWidth = paint.measureText(text)
        }
        if (textWidth > available) {
            textSize *= available / textWidth
            paint.textSize = textSize
            textWidth = paint.measureText(text)
        }

        val baseline = rect.centerY() - (paint.ascent() + paint.descent()) / 2
        canvas.drawText(text, rect.centerX() - textWidth / 2, baseline, paint)
    }

    private fun drawPointer(canvas: Canvas, paint: Paint, pill: RectF, fraction: Float, top: Float, height: Float, ink: Int) {
        val halfWidth = height * 0.9f
        val minX = pill.left + halfWidth + pill.height() * 0.15f
        val maxX = pill.right - halfWidth - pill.height() * 0.15f
        val cx = if (maxX > minX) minX + (maxX - minX) * fraction else pill.centerX()
        val path = Path().apply {
            moveTo(cx, top)
            lineTo(cx - halfWidth, top + height)
            lineTo(cx + halfWidth, top + height)
            close()
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        paint.color = ink
        canvas.drawPath(path, paint)
        // Contrasting edge keeps the pointer visible against both the pill and the card
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = height * 0.12f
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = if (ink == Color.WHITE) Color.BLACK else Color.WHITE
        canvas.drawPath(path, paint)
        paint.strokeJoin = Paint.Join.MITER
    }

    private fun luminance(color: Int): Float =
        (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)) / 255f
}
