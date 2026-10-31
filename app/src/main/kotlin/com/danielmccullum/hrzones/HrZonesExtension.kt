package com.danielmccullum.hrzones

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.models.DataType

class HrZonesExtension : KarooExtension("hr-zones", "1.2") {
    private val karooSystem by lazy { KarooSystemService(this) }

    override val types by lazy {
        listOf(
            ZoneBarDataType(
                karooSystem, extension, "hr-zone-bar", DataType.Type.HEART_RATE,
                { it.heartRateZones }, ZoneIcon.HEART, 85..190, 2,
            ),
            ZoneBarDataType(
                karooSystem, extension, "power-zone-bar", DataType.Type.SMOOTHED_3S_AVERAGE_POWER,
                { it.powerZones }, ZoneIcon.BOLT, 100..400, 6,
            ),
        )
    }

    override fun onCreate() {
        super.onCreate()
        karooSystem.connect()
    }

    override fun onDestroy() {
        karooSystem.disconnect()
        super.onDestroy()
    }
}
