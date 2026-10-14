package com.myblbl.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AppEventHub {

    sealed interface Event {
        data object UserSessionChanged : Event

        data object NetworkRecovered : Event

        data class PlaybackProgressUpdated(
            val aid: Long,
            val cid: Long,
            val progressMs: Long
        ) : Event

        data class EpisodePlaybackProgressUpdated(
            val episodeId: Long,
            val progressMs: Long,
            val episodeIndex: String
        ) : Event

        data class WatchLaterVideoRemoved(
            val aid: Long,
            val bvid: String
        ) : Event

        data class VideoBlockedByMinorProtection(
            val aid: Long,
            val bvid: String
        ) : Event

        /** 设置-通用-首页板块管理：勾选展示的分区板块集合发生变化。 */
        data object HomeRegionsChanged : Event
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 64)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    fun dispatch(event: Event) {
        _events.tryEmit(event)
    }
}
