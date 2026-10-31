package com.danielmccullum.hrzones

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.unit.DpSize
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.UserProfile
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class HrZoneBarDataType(
    private val karooSystem: KarooSystemService,
    extension: String,
) : DataTypeImpl(extension, "hr-zone-bar") {
    private val glance = GlanceRemoteViews()

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        emitter.onNext(UpdateGraphicConfig(showHeader = false))
        val (width, height) = config.viewSize
        val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val job = CoroutineScope(Dispatchers.IO).launch {
            val hrFlow = if (config.preview) {
                // Editor preview: tick a counter, turned into a sweep through the zones below
                flow {
                    var tick = 0
                    while (true) {
                        emit(tick++)
                        delay(PREVIEW_TICK_MS)
                    }
                }
            } else {
                karooSystem.streamDataFlow(DataType.Type.HEART_RATE)
                    .map { (it as? StreamState.Streaming)?.dataPoint?.singleValue?.toInt() }
            }
            combine(hrFlow, karooSystem.consumerFlow<UserProfile>()) { value, profile ->
                val zones = profile.heartRateZones
                val hr = if (config.preview) previewHr(value ?: 0) else value
                Triple(hr, zones.size, hr?.let { zonePosition(it, zones) })
            }
                .distinctUntilChanged()
                .collect { (hr, zoneCount, position) ->
                    val bitmap = ZoneBarRenderer.render(width, height, zoneCount, position, hr, dark)
                    val result = glance.compose(context, DpSize.Unspecified) {
                        Image(
                            provider = ImageProvider(bitmap),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = GlanceModifier.fillMaxSize(),
                        )
                    }
                    emitter.updateView(result.remoteViews)
                }
        }
        emitter.setCancellable { job.cancel() }
    }

    private companion object {
        const val PREVIEW_HR_MIN = 85
        const val PREVIEW_HR_MAX = 190
        const val PREVIEW_TICK_MS = 250L
        const val PREVIEW_BPM_PER_TICK = 2

        /** Sweeps up and down between [PREVIEW_HR_MIN] and [PREVIEW_HR_MAX] for the editor preview. */
        fun previewHr(tick: Int): Int {
            val span = PREVIEW_HR_MAX - PREVIEW_HR_MIN
            val phase = (tick * PREVIEW_BPM_PER_TICK) % (2 * span)
            return PREVIEW_HR_MIN + if (phase < span) phase else 2 * span - phase
        }
    }
}
