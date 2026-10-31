package com.danielmccullum.hrzones

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.core.graphics.PathParser
import io.hammerhead.karooext.models.ViewConfig
import kotlin.math.max
import kotlin.math.min

/**
 * Draws the whole field: a header like Karoo's own (icon plus the current value in the title slot) and,
 * below it, a row of zone segments in the style of the Apple Watch workout view. The current zone
 * expands into a pill labelled "ZONE n" with a pointer showing the position inside the zone.
 *
 * Header geometry follows the ride app's own layout: sizes come from [KarooConstraints], dp values
 * use the device density, and the title is a real TextView set up like Karoo's header title so font
 * metrics, line spacing and vertical centring match. The bitmap is transparent so Karoo's card
 * shows through.
 */
/** Header icon drawn next to the value. */
enum class ZoneIcon { HEART, BOLT }

object ZoneBarRenderer {
    // Z1..Z5 sampled from Karoo's own HR zone field, Z6 and Z7 added for power; zones beyond seven reuse the last color
    private val zoneColors = intArrayOf(
        Color.rgb(0x60, 0xEE, 0xB2), // mint
        Color.rgb(0x00, 0xB9, 0x88), // teal green
        Color.rgb(0xFF, 0xF5, 0x00), // yellow
        Color.rgb(0xFB, 0x8C, 0x65), // salmon
        Color.rgb(0xD6, 0x04, 0x04), // red
        Color.rgb(0x9B, 0x3D, 0xE0), // purple
        Color.rgb(0xA8, 0x00, 0x6E), // deep magenta
    )
    private const val DIMMED_ALPHA = 110
    private const val PILL_WEIGHT = 3.2f
    private const val POINTER_BELOW = 0.45f
    private val KAROO_GREEN = Color.rgb(18, 154, 94)

    // Karoo's header: 1 dp padding plus 3 dp margin each side, 3 dp above and below the icon, 22 dp minimum
    private const val HEADER_SIDE_DP = 4f
    private const val HEADER_ICON_MARGIN_DP = 3f
    private const val HEADER_MIN_HEIGHT_DP = 22f
    private const val HEADER_DEFAULT_LINE_SPACING = 0.7f

    // Zone row, in dp so it looks the same in every field size
    private const val ROW_TOP_GAP_DP = 2f
    private const val ROW_BOTTOM_PAD_DP = 4f
    private const val SEGMENT_GAP_DP = 2.2f
    private const val SEGMENT_RADIUS_DP = 13f
    private const val POINTER_HEIGHT_DP = 10f

    // Karoo's own font ships in the system image
    private val labelTypeface: Typeface by lazy {
        try {
            Typeface.createFromFile("/system/fonts/IBMPlexSansCondensed-Medium.otf")
        } catch (_: Exception) {
            Typeface.DEFAULT_BOLD
        }
    }

    // Header icons share a 38x38 viewport and a 4 unit inset around the glyph
    private const val ICON_VIEWPORT = 38f
    private const val ICON_GLYPH_INSET = 4f

    // Path data copied from the ride app's ic_heart (outline drawn with even-odd fill)
    private const val HEART_PATH_DATA =
        "M29.55,16.936l-10.568,12.43 -10.568,-12.43 -0.007,0.006c-2.141,-2.518 -1.827,-6.301 0.7,-8.433 " +
            "2.355,-1.99 5.814,-1.852 8.005,0.216 0.16,0.15 0.312,0.31 0.457,0.48 0.882,1.04 1.348,2.293 " +
            "1.413,3.558 0.064,-1.265 0.53,-2.518 1.412,-3.557 0.146,-0.171 0.297,-0.332 0.457,-0.48 " +
            "2.191,-2.07 5.65,-2.206 8.005,-0.217 2.527,2.132 2.841,5.915 0.7,8.433l-0.006,-0.006z" +
            "M30.788,6.236c-3.469,-2.927 -8.515,-2.785 -11.806,0.156 -3.292,-2.94 -8.338,-3.083 -11.807,-0.156 " +
            "-3.79,3.2 -4.258,8.854 -1.048,12.63l12.855,15.12 12.854,-15.12c3.21,-3.776 2.741,-9.43 -1.048,-12.63z"
    private val heartPath: Path by lazy { PathParser.createPathFromPathData(HEART_PATH_DATA) }

    // Lightning bolt drawn as a stroked outline to match the heart
    private const val BOLT_PATH_DATA = "M12,5L12,20.4L16.2,20.4L16.2,33L26,16.2L20.4,16.2L26,5Z"
    private const val BOLT_STROKE = 3f
    private val boltPath: Path by lazy { PathParser.createPathFromPathData(BOLT_PATH_DATA) }

    fun colorFor(index: Int): Int = zoneColors[index.coerceAtMost(zoneColors.lastIndex)]

    fun render(
        context: Context,
        config: ViewConfig,
        zoneCount: Int,
        position: ZonePosition?,
        value: Int?,
        icon: ZoneIcon,
        dark: Boolean,
    ): Bitmap {
        val (width, height) = config.viewSize
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val d = context.resources.displayMetrics.density
        val ink = if (dark) Color.WHITE else Color.BLACK

        val (columns, rows) = config.gridSize
        val constraints = KarooConstraints.forGrid(columns, rows, config.textSize * d)
        val headerBottom = drawHeader(context, canvas, paint, w, d, constraints, config.alignment, value, icon, ink)

        // The row lines up with the icon glyph on the left and the title's edge on the right
        val side = HEADER_SIDE_DP * d
        val rowLeft = side + ICON_GLYPH_INSET * constraints.labelSize / ICON_VIEWPORT
        val pointerHeight = POINTER_HEIGHT_DP * d
        val row = RectF(
            rowLeft,
            headerBottom + ROW_TOP_GAP_DP * d,
            w - side,
            h - ROW_BOTTOM_PAD_DP * d - pointerHeight * POINTER_BELOW,
        )
        if (zoneCount > 0 && row.height() > 0 && row.width() > 0) {
            drawZones(canvas, paint, zoneCount, position, constraints.labelSize, d, row, pointerHeight, ink)
        }
        return bitmap
    }

    /** Draws the header and returns its bottom edge. */
    private fun drawHeader(
        context: Context,
        canvas: Canvas,
        paint: Paint,
        width: Int,
        d: Float,
        constraints: KarooConstraints,
        alignment: ViewConfig.Alignment,
        value: Int?,
        icon: ZoneIcon,
        ink: Int,
    ): Float {
        val label = constraints.labelSize
        val side = HEADER_SIDE_DP * d
        val iconGap = HEADER_ICON_MARGIN_DP * d
        val title = TextView(context).apply {
            typeface = labelTypeface
            setTextSize(TypedValue.COMPLEX_UNIT_PX, label)
            setTextColor(ink)
            includeFontPadding = false
            setLineSpacing(0f, constraints.lineSpacing ?: HEADER_DEFAULT_LINE_SPACING)
            setLines(if (constraints.singleLineLabel) 1 else 2)
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            text = value?.toString() ?: "--"
        }
        title.measure(
            View.MeasureSpec.makeMeasureSpec((width - 2 * side - label - iconGap).toInt().coerceAtLeast(1), View.MeasureSpec.AT_MOST),
            View.MeasureSpec.UNSPECIFIED,
        )
        val titleWidth = title.measuredWidth.toFloat()
        val titleHeight = title.measuredHeight.toFloat()

        // Icon box and title are both centred vertically in a header as tall as the larger of the two
        val headerHeight = max(HEADER_MIN_HEIGHT_DP * d, max(label + 2 * iconGap, titleHeight))
        val iconTop = (headerHeight - label) / 2
        val titleTop = (headerHeight - titleHeight) / 2 + constraints.labelTranslationY

        // The three arrangements of the ride app's DataHeaderView
        val (iconLeft, titleLeft) = when (alignment) {
            ViewConfig.Alignment.LEFT -> (side + titleWidth + iconGap) to side
            ViewConfig.Alignment.CENTER -> {
                val start = (width - (label + iconGap + titleWidth)) / 2
                start to start + label + iconGap
            }
            ViewConfig.Alignment.RIGHT -> side to (width - side - titleWidth)
        }

        drawIcon(canvas, paint, icon, iconLeft, iconTop, label)
        title.layout(0, 0, titleWidth.toInt(), titleHeight.toInt())
        canvas.save()
        canvas.translate(titleLeft, titleTop)
        title.draw(canvas)
        canvas.restore()
        return headerHeight
    }

    /** Draws the header icon in Karoo's green, scaled into a square box of [size] px like its centerInside icons. */
    private fun drawIcon(canvas: Canvas, paint: Paint, icon: ZoneIcon, left: Float, top: Float, size: Float) {
        val scale = size / ICON_VIEWPORT
        val source = if (icon == ZoneIcon.HEART) heartPath else boltPath
        val path = Path(source).apply {
            if (icon == ZoneIcon.HEART) fillType = Path.FillType.EVEN_ODD
            transform(
                Matrix().apply {
                    postScale(scale, scale)
                    postTranslate(left, top)
                },
            )
        }
        paint.alpha = 255
        paint.color = KAROO_GREEN
        if (icon == ZoneIcon.HEART) {
            paint.style = Paint.Style.FILL
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = BOLT_STROKE * scale
            paint.strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, paint)
        paint.strokeJoin = Paint.Join.MITER
    }

    private fun drawZones(
        canvas: Canvas,
        paint: Paint,
        zoneCount: Int,
        position: ZonePosition?,
        labelSize: Float,
        d: Float,
        row: RectF,
        pointerHeight: Float,
        ink: Int,
    ) {
        val gap = SEGMENT_GAP_DP * d
        val current = position?.index
        val totalWeight = (zoneCount - 1) + if (current != null) PILL_WEIGHT else 1f
        val unit = (row.width() - gap * (zoneCount - 1)) / totalWeight

        var left = row.left
        for (i in 0 until zoneCount) {
            val segmentWidth = unit * if (i == current) PILL_WEIGHT else 1f
            val rect = RectF(left, row.top, left + segmentWidth, row.bottom)
            // Fixed corner radius, capped so narrow segments stay fully rounded instead of distorting
            val radius = min(SEGMENT_RADIUS_DP * d, min(rect.width(), rect.height()) / 2)
            paint.style = Paint.Style.FILL
            paint.color = colorFor(i)
            paint.alpha = if (i == current) 255 else DIMMED_ALPHA
            canvas.drawRoundRect(rect, radius, radius, paint)
            if (i == current && position != null) {
                drawPillLabel(canvas, paint, rect, i, labelSize, colorFor(i), pointerHeight * (1 - POINTER_BELOW))
                drawPointer(canvas, paint, rect, position.fraction, row.bottom - pointerHeight * (1 - POINTER_BELOW), pointerHeight, ink)
            }
            left += segmentWidth + gap
        }
    }

    private fun drawPillLabel(canvas: Canvas, paint: Paint, rect: RectF, index: Int, labelSize: Float, fill: Int, pointerOverlap: Float) {
        val textColor = if (luminance(fill) > 0.45f) Color.BLACK else Color.WHITE
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        paint.color = textColor
        paint.typeface = labelTypeface
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = labelSize

        // "ZONE n" only when it fits with room to spare; smaller fields get "Zn"
        var text = "ZONE ${index + 1}"
        var textWidth = paint.measureText(text)
        if (textWidth > rect.width() * 0.75f) {
            text = "Z${index + 1}"
            textWidth = paint.measureText(text)
        }
        val available = rect.width() * 0.88f
        if (textWidth > available) {
            paint.textSize = labelSize * available / textWidth
            textWidth = paint.measureText(text)
        }
        // Centre in the part of the pill above the pointer so the two never collide
        val centerY = (rect.top + rect.bottom - pointerOverlap) / 2
        val baseline = centerY - (paint.ascent() + paint.descent()) / 2
        canvas.drawText(text, rect.centerX() - textWidth / 2, baseline, paint)
    }

    private fun drawPointer(canvas: Canvas, paint: Paint, pill: RectF, fraction: Float, top: Float, height: Float, ink: Int) {
        val halfWidth = height * 0.9f
        val inset = halfWidth + min(pill.height(), pill.width()) * 0.15f
        val minX = pill.left + inset
        val maxX = pill.right - inset
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
