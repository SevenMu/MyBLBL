package com.myblbl.repository.remote

import com.myblbl.R
import com.myblbl.MyBLBLApplication
import com.myblbl.core.common.log.AppLog
import com.myblbl.model.series.CheckUserSeriesResult
import com.myblbl.model.series.EpisodesDetailModel
import com.myblbl.model.series.FollowSeriesResult
import com.myblbl.model.series.MyFollowingResponseWrapper
import com.myblbl.model.series.RelatedRecommendResult
import com.myblbl.model.series.SectionModel
import com.myblbl.model.series.timeline.TimeLineADayModel
import com.myblbl.model.video.VideoModel
import com.myblbl.network.api.ApiService
import com.myblbl.network.security.NetworkSecurityGateway
import com.myblbl.network.session.NetworkSessionGateway
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class SeriesRepository(
    private val apiService: ApiService,
    private val sessionGateway: NetworkSessionGateway,
    private val securityGateway: NetworkSecurityGateway
) {

    suspend fun getSeriesDetail(seasonId: Long, epId: Long = 0): Result<EpisodesDetailModel> {
        return runCatching {
            securityGateway.ensureHealthyForPlay()
            val response = sessionGateway.retryOnRiskControl(
                key = "series_detail_$seasonId",
                source = "series.getSeriesDetail",
                getCode = { it.code },
                getMessage = { it.message },
                getIsSuccess = { it.isSuccess }
            ) {
                apiService.getVideoEpisodes(
                    if (seasonId > 0) seasonId else null,
                    if (epId > 0) epId else null
                ).let {
                    sessionGateway.syncAuthState(it, source = "series.getSeriesDetail")
                }
            }
            val detail = response.result
            if (response.isSuccess && detail != null) {
                val resolvedSeasonId = detail.seasonId.takeIf { it > 0 } ?: seasonId
                val sectionResult = if (resolvedSeasonId > 0) {
                    apiService.getVideoEpisodeSections(resolvedSeasonId)
                        .let { sessionGateway.syncAuthState(it, source = "series.getVideoEpisodeSections") }
                        .result
                } else {
                    null
                }
                // season/section 条目缺 bvid/ep_id（cid 可能为 0），逐分区走 ep/page
                // 分页详情回填播放身份（并行拉取，单分区失败保持原数据不阻塞详情加载）
                val rawSections = sectionResult?.section.orEmpty()
                val enrichedSections = if (resolvedSeasonId > 0 && rawSections.isNotEmpty()) {
                    enrichSectionsFromEpPage(resolvedSeasonId, rawSections)
                } else {
                    rawSections
                }
                val mergedDetail = detail.copy(
                    episodes = sectionResult?.mainSection?.episodes.orEmpty(),
                    section = enrichedSections,
                    mainSectionTitle = sectionResult?.mainSection?.title.orEmpty()
                )
                mergedDetail
            } else {
                throw IllegalStateException(response.errorMessage)
            }
        }
    }

    /**
     * 并行回填各分区的播放身份：每分区先取第 0 页（响应含全部分页元信息 pages_meta），
     * 按元信息逐页翻取后按 aid 匹配，把 season/section 缺失的 bvid/cid/epid/duration
     * 补到对应条目上（标题/封面等展示字段保持 section 原值，最小侵入）。
     */
    private suspend fun enrichSectionsFromEpPage(
        seasonId: Long,
        sections: List<SectionModel>
    ): List<SectionModel> = coroutineScope {
        sections.map { section ->
            async {
                runCatching { enrichSectionFromEpPage(seasonId, section) }
                    .onFailure { e ->
                        AppLog.w(
                            "SeriesDetail",
                            "enrichSectionFromEpPage failed: sectionId=${section.id} reason=${e.message}"
                        )
                    }
                    .getOrDefault(section)
            }
        }.awaitAll()
    }

    private suspend fun enrichSectionFromEpPage(seasonId: Long, section: SectionModel): SectionModel {
        if (section.id <= 0 || section.episodes.isEmpty()) {
            return section
        }
        val firstPage = apiService.getEpPage(seasonId = seasonId, sectionId = section.id, pageIndex = 0)
        if (!firstPage.isSuccess) {
            return section
        }
        val pageSection = firstPage.result?.sections?.firstOrNull { it.sectionId == section.id }
            ?: firstPage.result?.sections?.firstOrNull()
            ?: return section
        val collected = mutableListOf<VideoModel>()
        collected.addAll(pageSection.pages.flatMap { it.episodes })
        val totalPages = pageSection.pagesMeta.size
        if (totalPages > 1) {
            for (pageIndex in 1 until totalPages) {
                runCatching {
                    apiService.getEpPage(seasonId = seasonId, sectionId = section.id, pageIndex = pageIndex)
                }.onSuccess { page ->
                    page.result?.sections?.firstOrNull { it.sectionId == section.id }
                        ?.let { matched -> collected.addAll(matched.pages.flatMap { it.episodes }) }
                }
            }
        }
        if (collected.isEmpty()) {
            return section
        }
        val byAid = collected.filter { it.aid > 0L }.associateBy { it.aid }
        val enrichedEpisodes = section.episodes.map { episode ->
            val source = byAid[episode.aid] ?: return@map episode
            episode.copy(
                bvid = episode.bvid.ifBlank { source.bvid },
                cid = if (episode.cid > 0L) episode.cid else source.cid,
                epid = if (episode.epid > 0L) episode.epid else source.epid,
                duration = if (episode.duration > 0L) episode.duration else source.duration
            )
        }
        return section.copy(episodes = enrichedEpisodes)
    }

    suspend fun checkUserFollowStatus(seasonId: Long, epId: Long = 0): Result<CheckUserSeriesResult> {
        return runCatching {
            val response = sessionGateway.retryOnRiskControl(
                key = "series_status_$seasonId",
                source = "series.checkUserFollowStatus",
                getCode = { it.code },
                getMessage = { it.message },
                getIsSuccess = { it.isSuccess }
            ) {
                apiService.getSeriesUserStatus(
                    if (seasonId > 0) seasonId else null,
                    if (epId > 0) epId else null
                ).let {
                    sessionGateway.syncAuthState(it, source = "series.checkUserFollowStatus")
                }
            }
            val result = response.result
            if (response.isSuccess && result != null) {
                result
            } else {
                throw IllegalStateException(response.errorMessage.ifBlank { MyBLBLApplication.instance.getString(R.string.series_error_follow_status_failed) })
            }
        }
    }

    suspend fun followSeries(seasonId: Long): Result<FollowSeriesResult> {
        return runCatching {
            securityGateway.ensureHealthyForPlay()
            val csrf = sessionGateway.requireCsrfToken()
                ?: throw IllegalStateException("csrf token is blank")
            var response = apiService.followSeries(seasonId, csrf)
            if (response.code == -101 && sessionGateway.tryRecoverExpiredSession()) {
                val newCsrf = sessionGateway.requireCsrfToken()
                if (!newCsrf.isNullOrBlank()) {
                    response = apiService.followSeries(seasonId, newCsrf)
                }
            }
            val finalResponse = sessionGateway.syncAuthState(
                response,
                source = "series.followSeries"
            )
            if (finalResponse.isSuccess) {
                FollowSeriesResult(
                    relation = true,
                    status = 1,
                    toast = ""
                )
            } else {
                throw IllegalStateException(finalResponse.errorMessage.ifBlank { MyBLBLApplication.instance.getString(R.string.series_error_follow_failed) })
            }
        }
    }

    suspend fun cancelFollowSeries(seasonId: Long): Result<FollowSeriesResult> {
        return runCatching {
            securityGateway.ensureHealthyForPlay()
            val csrf = sessionGateway.requireCsrfToken()
                ?: throw IllegalStateException("csrf token is blank")
            var response = apiService.cancelFollowSeries(seasonId, csrf)
            if (response.code == -101 && sessionGateway.tryRecoverExpiredSession()) {
                val newCsrf = sessionGateway.requireCsrfToken()
                if (!newCsrf.isNullOrBlank()) {
                    response = apiService.cancelFollowSeries(seasonId, newCsrf)
                }
            }
            val finalResponse = sessionGateway.syncAuthState(
                response,
                source = "series.cancelFollowSeries"
            )
            if (finalResponse.isSuccess) {
                FollowSeriesResult(
                    relation = false,
                    status = 0,
                    toast = ""
                )
            } else {
                throw IllegalStateException(finalResponse.errorMessage.ifBlank { MyBLBLApplication.instance.getString(R.string.series_error_unfollow_failed) })
            }
        }
    }

    suspend fun getMyFollowingSeries(
        type: Int,
        page: Int,
        pageSize: Int,
        vmid: Long
    ): Result<MyFollowingResponseWrapper> {
        return runCatching {
            val response = sessionGateway.executeWithRiskControlRetry(
                key = "series_following_${type}_$vmid",
                source = "series.getMyFollowingSeries"
            ) {
                apiService.getMyFollowingSeries(
                    type = type,
                    page = page,
                    pageSize = pageSize,
                    vmid = vmid,
                    ts = System.currentTimeMillis()
                ).let {
                    sessionGateway.syncAuthState(it, source = "series.getMyFollowingSeries")
                }
            }
            if (response.code == 0 && response.data != null) {
                response.data
            } else {
                throw IllegalStateException(response.message.ifEmpty { response.msg })
            }
        }
    }

    suspend fun getSeriesTimeline(
        type: Int,
        before: Int = 6,
        after: Int = 6
    ): Result<List<TimeLineADayModel>> {
        return runCatching {
            val response = apiService.getSeriesTimeLine(
                type = type,
                before = before,
                after = after
            )
            if (response.code == 0) {
                response.result.map { day ->
                    day.copy(
                        episodes = day.episodes.map { episode ->
                            episode.copy(dayOfWeek = day.dayOfWeek)
                        }
                    )
                }
            } else {
                throw IllegalStateException(response.message.ifEmpty { MyBLBLApplication.instance.getString(R.string.series_error_timeline_failed) })
            }
        }
    }

    suspend fun getRelatedRecommend(seasonId: Long): Result<RelatedRecommendResult> {
        return runCatching {
            val response = apiService.getRelatedRecommend(seasonId)
            if (response.isSuccess && response.data != null) {
                response.data
            } else {
                throw IllegalStateException(response.errorMessage.ifEmpty { MyBLBLApplication.instance.getString(R.string.series_error_recommend_failed) })
            }
        }
    }
}
