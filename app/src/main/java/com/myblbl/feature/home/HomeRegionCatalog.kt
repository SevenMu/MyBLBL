package com.myblbl.feature.home

import com.myblbl.R
import com.myblbl.core.common.settings.AppSettingsDataStore

/**
 * 首页"推荐/热门/番剧/影视"固定 4 板块之后可勾选展示的分区板块目录。
 *
 * [rid] 即分区 cid（与 x/web-interface/region/feed/rcmd 的 from_region 参数同值，
 * 2025-10 经分区页网络请求 + 内容抽样双重验证）；entries 顺序即首页 tab 顺序。
 */
enum class HomeRegionCatalog(val rid: Int, val titleRes: Int) {
    ANIMATION(1005, R.string.cartoon),
    KICHIKU(1007, R.string.kichiku),
    DANCE(1004, R.string.dance),
    ENTERTAINMENT(1002, R.string.entainment),
    TECHNOLOGY(1012, R.string.technology),
    FOOD(1020, R.string.food),
    GAME(1008, R.string.game),
    MUSIC(1003, R.string.music),
    CINEPHILE(1001, R.string.film_and_television),
    KNOWLEDGE(1010, R.string.knowledge),
    INFORMATION(1009, R.string.information),
    SHORT_PLAY(1021, R.string.short_play),
    AUTOMOBILE(1013, R.string.auto),
    FASHION_BEAUTY(1014, R.string.fashion_beauty),
    SPORTS(1018, R.string.sports),
    ANIMAL(1024, R.string.animal),
    VLOG(1029, R.string.vlog),
    PAINTING(1006, R.string.painting),
    ARTIFICIAL_INTELLIGENCE(1011, R.string.artificial_intelligence),
    HOME_DECOR(1015, R.string.home_decor),
    OUTDOORS(1016, R.string.outdoors),
    FITNESS(1017, R.string.fitness),
    HANDCRAFT(1019, R.string.handcraft),
    TRAVEL(1022, R.string.travel),
    RURAL(1023, R.string.rural),
    PARENTING(1025, R.string.parenting),
    HEALTH(1026, R.string.health),
    EMOTION(1027, R.string.emotion),
    LIFE_INTEREST(1030, R.string.life_interest),
    LIFE_EXPERIENCE(1031, R.string.life_experience);

    companion object {

        /** 设置存储 key：勾选展示的分区 rid 集合（字符串形式）。 */
        const val PREF_KEY_REGION_TABS = "home_region_tabs"

        /** 默认（从未写过该设置时）：目录内全部板块都展示。 */
        fun defaultSelectedRids(): List<Int> = entries.map { it.rid }

        /**
         * 读取勾选展示的板块 rid（保持目录顺序）。
         * 未设置过 → 全量；显式存过空集 → 只显示固定 4 板块。
         */
        fun loadSelectedRids(settings: AppSettingsDataStore): List<Int> {
            val selected = settings.getCachedStringSet(
                key = PREF_KEY_REGION_TABS,
                defaultValue = defaultSelectedRids().map(Int::toString).toSet()
            )
            if (selected.isEmpty()) return emptyList()
            val selectedRids = selected.mapNotNull { it.toIntOrNull() }.toSet()
            return entries.filter { it.rid in selectedRids }.map { it.rid }
        }

        fun titleOf(rid: Int): Int? = entries.firstOrNull { it.rid == rid }?.titleRes

        /** 设置条目右侧信息文案："已选数/总数"。 */
        fun selectedCountText(settings: AppSettingsDataStore): String {
            return "${loadSelectedRids(settings).size}/${entries.size}"
        }
    }
}
