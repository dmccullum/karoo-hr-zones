package com.danielmccullum.hrzones

import android.content.Context
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
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.UserProfile
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class HrZoneBarDataType(
    private val karooSystem: KarooSystemService,
    extension: String,
) : DataTypeImpl(extension, "hr-zone-bar") {
    private val glance = GlanceRemoteViews()

    // Re-emit heart rate under this type's id so Karoo draws its own header and number
    override fun startStream(emitter: Emitter<StreamState>) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            karooSystem.streamDataFlow(DataType.Type.HEART_RATE).collect { state ->
                val hr = (state as? StreamState.Streaming)?.dataPoint?.singleValue
                emitter.onNext(
                    if (state is StreamState.Streaming && hr != null) {
                        state.copy(
                            dataPoint = state.dataPoint.copy(
                                dataTypeId = dataTypeId,
                                values = mapOf(DataType.Field.SINGLE to hr),
                            ),
                        )
                    } else {
                        state
                    },
                )
            }
        }
        emitter.setCancellable { job.cancel() }
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        emitter.onNext(UpdateGraphicConfig(formatDataTypeId = DataType.Type.HEART_RATE))
        val (width, height) = config.viewSize
        val job = CoroutineScope(Dispatchers.IO).launch {
            val hrFlow = if (config.preview) {
                flowOf(PREVIEW_HR)
            } else {
                karooSystem.streamDataFlow(DataType.Type.HEART_RATE)
                    .map { (it as? StreamState.Streaming)?.dataPoint?.singleValue?.toInt() }
            }
            combine(hrFlow, karooSystem.consumerFlow<UserProfile>()) { hr, profile ->
                val zones = profile.heartRateZones
                Pair(zones.size, hr?.let { zonePosition(it, zones) })
            }
                .distinctUntilChanged()
                .collect { (zoneCount, position) ->
                    val bitmap = ZoneBarRenderer.render(width, height, zoneCount, position)
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
        const val PREVIEW_HR = 148
    }
}
