package com.myblbl.model.series

import com.google.gson.annotations.SerializedName
import com.myblbl.model.video.VideoModel
import java.io.Serializable

/**
 * 剧集分页详情（pgc/view/web/ep/page）：按分区分页返回剧集条目，
 * 条目带完整 bvid/cid/ep_id（season/section 接口缺失的播放身份由它补全）。
 */
data class EpPageResult(
    @SerializedName("sections")
    val sections: List<EpPageSection> = emptyList()
) : Serializable

data class EpPageSection(
    @SerializedName("section_id")
    val sectionId: Long = 0,
    @SerializedName("pages")
    val pages: List<EpPage> = emptyList(),
    /** 全部分页的元信息（title 形如 "1-50"），大小即总页数，用于逐页翻取。 */
    @SerializedName("pages_meta")
    val pagesMeta: List<EpPageMeta> = emptyList()
) : Serializable

data class EpPage(
    @SerializedName("page_index")
    val pageIndex: Int = 0,
    @SerializedName("episodes")
    val episodes: List<VideoModel> = emptyList()
) : Serializable

data class EpPageMeta(
    @SerializedName("page_index")
    val pageIndex: Int = 0,
    @SerializedName("title")
    val title: String = ""
) : Serializable
