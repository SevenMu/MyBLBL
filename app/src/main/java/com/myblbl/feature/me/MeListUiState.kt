package com.myblbl.feature.me

import com.myblbl.model.video.HistoryVideoModel
import com.myblbl.model.video.VideoModel

data class MeListUiState(
    val historyVideos: List<HistoryVideoModel> = emptyList(),
    val laterVideos: List<VideoModel> = emptyList()
)
