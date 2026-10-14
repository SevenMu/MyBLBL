package com.myblbl.model

import java.io.Serializable

data class CategoryModel(
    val id: Int = 0,
    val name: String = "",
    /** PGC 板块（纪录片/电影/电视剧/国创/番剧/综艺）：id 即剧集排行榜 season_type。 */
    val isPgc: Boolean = false
) : Serializable
