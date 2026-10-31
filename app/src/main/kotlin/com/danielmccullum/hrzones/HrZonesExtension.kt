package com.danielmccullum.hrzones

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension

class HrZonesExtension : KarooExtension("hr-zones", "1.1") {
    private val karooSystem by lazy { KarooSystemService(this) }

    override val types by lazy {
        listOf(HrZoneBarDataType(karooSystem, extension))
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
