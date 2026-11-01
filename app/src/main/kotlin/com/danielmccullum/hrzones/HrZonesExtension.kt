package com.danielmccullum.hrzones

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.models.DataType

class HrZonesExtension : KarooExtension("hr-zones", "1.2") {
    private val karooSystem by lazy { KarooSystemService(this) }

    override val types by lazy {
        listOf(
            hrZones("hr-zone-bar", ZoneBarStyle.ZONE_LABEL),
            powerZones("power-zone-bar", ZoneBarStyle.ZONE_LABEL),
            hrZones("hr-zone-bar-big-zone", ZoneBarStyle.BIG_ZONE),
            powerZones("power-zone-bar-big-zone", ZoneBarStyle.BIG_ZONE),
            hrZones("hr-zone-bar-no-header", ZoneBarStyle.VALUE_IN_PILL),
            powerZones("power-zone-bar-no-header", ZoneBarStyle.VALUE_IN_PILL),
        )
    }

    private fun hrZones(typeId: String, style: ZoneBarStyle) = ZoneBarDataType(
        karooSystem, extension, typeId, DataType.Type.HEART_RATE,
        { it.heartRateZones }, ZoneIcon.HEART, style, 85..190, 2,
    )

    private fun powerZones(typeId: String, style: ZoneBarStyle) = ZoneBarDataType(
        karooSystem, extension, typeId, DataType.Type.SMOOTHED_3S_AVERAGE_POWER,
        { it.powerZones }, ZoneIcon.BOLT, style, 100..400, 6,
    )

    override fun onCreate() {
        super.onCreate()
        karooSystem.connect()
    }

    override fun onDestroy() {
        karooSystem.disconnect()
        super.onDestroy()
    }
}
