package com.myblbl.feature.home

import android.content.Context
import com.myblbl.R
import com.myblbl.model.video.VideoModel
import com.myblbl.network.api.BiliApi

/**
 * 首页分区板块 feed（x/web-interface/region/feed/rcmd，网页版分区页同款推荐流）。
 *
 * display_id 递增翻页（每屏 15 条）；无本地缓存（分区内容实时性强，且 tab 数量多，
 * 缓存收益低），刷新/初始加载均走网络。被本地内容过滤掉的视频会异步反馈 dislike。
 */
class RegionFeedViewModel(
    private val regionRid: Int,
    dislikeFeedback: RecommendDislikeFeedback,
    context: Context
) : BaseVideoFeedViewModel(context, dislikeFeedback) {

    override suspend fun fetchPage(
        page: Int,
        replace: Boolean,
        fromInitial: Boolean,
        fromRefresh: Boolean
    ): Result<FetchedPage> {
        return runCatching {
            BiliApi.regionFeedRcmd(fromRegion = regionRid, displayId = page)
        }.map { rawItems ->
            FetchedPage(
                items = filterForDisplay(rawItems.filter { it.isSupportedHomeVideoCard }),
                hasMore = rawItems.size >= PAGE_SIZE
            )
        }
    }

    override fun errorMessage(throwable: Throwable): String {
        return throwable.message ?: appContext.getString(R.string.category_load_failed)
    }

    override suspend fun writeCache(items: List<VideoModel>) = Unit

    companion object {
        private const val PAGE_SIZE = 15
    }
}
