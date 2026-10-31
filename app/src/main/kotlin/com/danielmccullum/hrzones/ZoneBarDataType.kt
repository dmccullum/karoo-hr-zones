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
class ZoneBarDataType(
    private val karooSystem: KarooSystemService,
    extension: String,
    typeId: String,
    private val streamTypeId: String,
    private val zonesOf: (UserProfile) -> List<UserProfile.Zone>,
    private val icon: ZoneIcon,
    private val previewRange: IntRange,
    private val previewStep: Int,
) : DataTypeImpl(extension, typeId) {
    private val glance = GlanceRemoteViews()

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        emitter.onNext(UpdateGraphicConfig(showHeader = false))
        val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val job = CoroutineScope(Dispatchers.IO).launch {
            val valueFlow = if (config.preview) {
                // Editor preview: tick a counter, turned into a sweep through the zones below
                flow {
                    var tick = 0
                    while (true) {
                        emit(tick++)
                        delay(PREVIEW_TICK_MS)
                    }
                }
            } else {
                karooSystem.streamDataFlow(streamTypeId)
                    .map { (it as? StreamState.Streaming)?.dataPoint?.singleValue?.toInt() }
            }
            combine(valueFlow, karooSystem.consumerFlow<UserProfile>()) { raw, profile ->
                val zones = zonesOf(profile)
                val value = if (config.preview) previewValue(raw ?: 0) else raw
                Triple(value, zones.size, value?.let { zonePosition(it, zones) })
            }
                .distinctUntilChanged()
                .collect { (value, zoneCount, position) ->
                    val bitmap = ZoneBarRenderer.render(context, config, zoneCount, position, value, icon, dark)
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

    /** Sweeps up and down across [previewRange] for the editor preview. */
    private fun previewValue(tick: Int): Int {
        val span = previewRange.last - previewRange.first
        val phase = (tick * previewStep) % (2 * span)
        return previewRange.first + if (phase < span) phase else 2 * span - phase
    }

    private companion object {
        const val PREVIEW_TICK_MS = 250L
    }
}
