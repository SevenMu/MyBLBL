package com.myblbl.core.navigation

import android.content.Context
import android.content.ContextWrapper
import com.myblbl.model.video.HistoryVideoModel
import com.myblbl.model.video.VideoModel
import com.myblbl.ui.activity.LivePlayerActivity
import com.myblbl.ui.activity.MainActivity
import com.myblbl.ui.activity.PlayerActivity
import com.myblbl.feature.detail.VideoDetailFragment
import com.myblbl.feature.player.PlaybackStartupTrace
import com.myblbl.feature.player.settings.PlayerSettingsStore
import com.myblbl.core.common.ext.isOpenDetailFirstEnabled
import com.myblbl.core.common.ext.toast
import com.myblbl.core.common.log.AppLog

object VideoRouteNavigator {

    private const val TAG = "VideoRouteNav"

    fun openVideo(
        context: Context,
        video: VideoModel,
        playQueue: List<VideoModel> = emptyList(),
        seekPositionMs: Long = 0L,
        startEpisodeIndex: Int = -1,
        forcePlayer: Boolean = false
    ) {
        val traceId = PlaybackStartupTrace.newTraceId()
        val traceStartElapsedMs = PlaybackStartupTrace.nowMs()
        PlaybackStartupTrace.log(
            traceId = traceId,
            startElapsedMs = traceStartElapsedMs,
            step = "card_click",
            message = "aid=${video.aid} bvid=${video.bvid} cid=${video.cid} title=${video.title.take(32)}"
        )
        AppLog.d(TAG, "openVideo: bvid=${video.bvid}, aid=${video.aid}, title=${video.title}, forcePlayer=$forcePlayer")
        // 青少年模式：休息期间拦截视频入口
        com.myblbl.core.common.content.TeenModeTimer.consumeBlockReason()?.let {
            context.toast(it)
            return
        }
        if (!video.hasPlaybackIdentity) {
            AppLog.w(TAG, "openVideo blocked: missing playback identity, title=${video.title}, cid=${video.cid}")
            context.toast("当前卡片数据不完整，暂时无法播放")
            return
        }
        // PGC 剧集条目（历史的番剧/影视记录等）→ 剧集详情页选集：
        // 剧集级 aid（无 bvid）走 UGC 详情页会拿到单集花絮数据，页面内容错乱。
        // 仅浏览语境（forcePlayer=false）分流；剧集详情页内的选集/继续观看
        // 走 forcePlayer=true 的明确播放意图，直接进播放器，否则会重进详情页
        // 循环（点正片"无法播放"，日志 play-1792158116816-18 实证）。
        if (!forcePlayer && video.isPgc && video.playbackSeasonId > 0L) {
            val hostActivity = findMainActivityHost(context)
            AppLog.d(TAG, "openVideo: pgc season, host=$hostActivity seasonId=${video.playbackSeasonId}")
            if (hostActivity != null) {
                hostActivity.openInHostContainer(
                    com.myblbl.feature.series.SeriesDetailFragment.newInstance(
                        seasonId = video.playbackSeasonId,
                        epId = video.playbackEpId
                    )
                )
                return
            }
        }
        if (!forcePlayer && shouldOpenVideoDetailFirst(video)) {
            val hostActivity = findMainActivityHost(context)
            AppLog.d(TAG, "openVideo: detailFirst=true, host=$hostActivity")
            if (hostActivity != null) {
                hostActivity.openInHostContainer(VideoDetailFragment.newInstance(video, playQueue))
                return
            }
        }
        AppLog.d(TAG, "openVideo: starting PlayerActivity")
        val shouldUseHistoryProgress = seekPositionMs <= 0L &&
            PlayerSettingsStore.load(context).resumePlayback
        PlayerActivity.start(
            context = context,
            video = video,
            seekPositionMs = seekPositionMs.takeIf { it > 0L }
                ?: if (shouldUseHistoryProgress) video.historyProgress * 1000L else 0L,
            playQueue = playQueue,
            startEpisodeIndex = startEpisodeIndex,
            startupTraceId = traceId,
            startupTraceStartElapsedMs = traceStartElapsedMs
        )
    }

    /**
     * 显式打开视频详情页（标签/简介/互动数据），不受“显示视频详情页”设置开关限制，
     * 供长按菜单等用户明确表达意图的入口使用。
     */
    fun openVideoDetail(
        context: Context,
        video: VideoModel,
        playQueue: List<VideoModel> = emptyList()
    ): Boolean {
        val hostActivity = findMainActivityHost(context)
        if (hostActivity != null) {
            hostActivity.openInHostContainer(VideoDetailFragment.newInstance(video, playQueue))
            return true
        }
        return false
    }

    fun openHistory(
        context: Context,
        historyVideo: HistoryVideoModel,
        playQueue: List<VideoModel> = emptyList(),
        forcePlayer: Boolean = false
    ) {
        resolveLiveRoomId(historyVideo)?.let { roomId ->
            LivePlayerActivity.start(context, roomId)
            return
        }
        openVideo(
            context = context,
            video = historyVideo.toVideoModel(),
            playQueue = playQueue,
            forcePlayer = forcePlayer
        )
    }

    private fun shouldOpenVideoDetailFirst(video: VideoModel): Boolean {
        if (!isOpenDetailFirstEnabled()) {
            return false
        }
        if (video.isLive || video.roomId > 0L || video.historyBusiness == "live") {
            return false
        }
        return video.aid > 0L || video.bvid.isNotBlank()
    }

    private fun resolveLiveRoomId(historyVideo: HistoryVideoModel): Long? {
        val history = historyVideo.history ?: return null
        if (history.business != "live") {
            return null
        }
        return history.oid.takeIf { it > 0L }
    }

    private fun findMainActivityHost(context: Context): MainActivity? {
        var current: Context? = context
        while (current != null) {
            when (current) {
                is MainActivity -> {
                    if (!current.isFinishing && !current.isDestroyed) {
                        return current
                    }
                    return null
                }
                is ContextWrapper -> current = current.baseContext
                else -> return null
            }
        }
        return null
    }
}
