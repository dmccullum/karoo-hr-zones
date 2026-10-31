package com.danielmccullum.hrzones

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/**
 * Draws one rounded segment per zone along the bottom of the field. The current zone is drawn
 * at full color with a ring marker (zone-colored fill, white ring, thin black outline) at the
 * rider's position inside it; other zones are dimmed.
 * The rest of the bitmap is transparent so Karoo's own header and number show through.
 */
object ZoneBarRenderer {
    // Z1..Z5; zones beyond five reuse the last color
    private val zoneColors = intArrayOf(
        Color.rgb(0x8E, 0x8E, 0x93), // grey
        Color.rgb(0x0A, 0x84, 0xFF), // blue
        Color.rgb(0x30, 0xD1, 0x58), // green
        Color.rgb(0xFF, 0x9F, 0x0A), // orange
        Color.rgb(0xFF, 0x37, 0x5F), // red
    )
    private const val DIMMED_ALPHA = 80

    fun colorFor(index: Int): Int = zoneColors[index.coerceAtMost(zoneColors.lastIndex)]

    fun render(width: Int, height: Int, zoneCount: Int, position: ZonePosition?): Bitmap {
        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        if (zoneCount == 0) return bitmap
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val inset = width * 0.05f
        val barHeight = (height * 0.06f).coerceIn(5f, 12f)
        val markerRadius = barHeight * 0.85f
        val bottom = height - height * 0.05f - (markerRadius - barHeight / 2)
        val top = bottom - barHeight
        val gap = barHeight * 0.6f
        val segmentWidth = (width - 2 * inset - gap * (zoneCount - 1)) / zoneCount
        val radius = barHeight / 2

        for (i in 0 until zoneCount) {
            val left = inset + i * (segmentWidth + gap)
            paint.color = colorFor(i)
            paint.alpha = if (i == position?.index) 255 else DIMMED_ALPHA
            canvas.drawRoundRect(RectF(left, top, left + segmentWidth, bottom), radius, radius, paint)
        }

        if (position != null) {
            val ringWidth = barHeight * 0.3f
            val outlineWidth = barHeight * 0.12f
            // Outer edge of the black outline, measured from the marker center
            val extent = markerRadius + ringWidth / 2 + outlineWidth
            val left = inset + position.index * (segmentWidth + gap)
            val cx = if (segmentWidth >= 2 * extent) {
                left + extent + (segmentWidth - 2 * extent) * position.fraction
            } else {
                left + segmentWidth / 2
            }
            val cy = (top + bottom) / 2
            paint.alpha = 255
            paint.style = Paint.Style.FILL
            paint.color = colorFor(position.index)
            canvas.drawCircle(cx, cy, markerRadius, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = ringWidth
            paint.color = Color.WHITE
            canvas.drawCircle(cx, cy, markerRadius, paint)
            paint.strokeWidth = outlineWidth
            paint.color = Color.BLACK
            canvas.drawCircle(cx, cy, markerRadius + ringWidth / 2 + outlineWidth / 2, paint)
        }
        return bitmap
    }
}
