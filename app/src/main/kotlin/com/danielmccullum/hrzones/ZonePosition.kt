package com.danielmccullum.hrzones

import io.hammerhead.karooext.models.UserProfile

/** Which zone [hr] falls in (0-based) and how far through that zone it is, 0..1. */
data class ZonePosition(val index: Int, val fraction: Float)

fun zonePosition(hr: Int, zones: List<UserProfile.Zone>): ZonePosition? {
    if (zones.isEmpty()) return null
    if (hr <= zones.first().min) return ZonePosition(0, 0f)
    if (hr >= zones.last().max) return ZonePosition(zones.lastIndex, 1f)
    val index = zones.indexOfLast { hr >= it.min }.coerceAtLeast(0)
    val zone = zones[index]
    val span = (zone.max - zone.min).coerceAtLeast(1)
    return ZonePosition(index, ((hr - zone.min).toFloat() / span).coerceIn(0f, 1f))
}
