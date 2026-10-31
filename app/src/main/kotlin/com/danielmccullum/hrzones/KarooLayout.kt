package com.danielmccullum.hrzones

/**
 * Text sizes Karoo's ride app picks for a data field, ported from its `DataElementConstraints` table
 * (Karoo 3 branch). Everything is in pixels. Grid spans are out of 60 columns.
 *
 * [labelSize] is the header title size; Karoo also sizes the header icon box to it.
 * [singleLineLabel] is true for full-width fields; narrower ones reserve two title lines, which is
 * why their title sits lower. [lineSpacing] is null when the header style's default (0.7) applies.
 */
internal data class KarooConstraints(
    val labelSize: Float,
    val singleLineLabel: Boolean,
    val lineSpacing: Float?,
    val labelTranslationY: Float,
) {
    companion object {
        private const val FULL_WIDTH = 60
        private const val HALF_WIDTH = 30

        /**
         * [dataSize] is the number size Karoo passes as ViewConfig.textSize (converted to pixels). It only
         * tells apart the large-label user setting, which changes the table for some half-width heights.
         */
        fun forGrid(columnSpan: Int, rowSpan: Int, dataSize: Float): KarooConstraints {
            val fullWidth = columnSpan == FULL_WIDTH
            return when {
                fullWidth && rowSpan >= 15 -> KarooConstraints(36f, true, null, 0f)
                fullWidth && rowSpan >= 12 -> KarooConstraints(33f, true, null, 0f)
                columnSpan == HALF_WIDTH && rowSpan >= 15 -> KarooConstraints(33f, false, null, 0f)
                columnSpan == HALF_WIDTH && rowSpan >= 12 && dataSize.toInt() == LARGE_LABEL_DATA_SIZE ->
                    KarooConstraints(33f, false, 0.6f, -3f)
                else -> KarooConstraints(29f, fullWidth, 0.6f, 0f)
            }
        }

        private const val LARGE_LABEL_DATA_SIZE = 78
    }
}
