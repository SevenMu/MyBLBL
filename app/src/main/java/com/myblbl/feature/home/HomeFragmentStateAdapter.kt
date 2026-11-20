package com.myblbl.feature.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.myblbl.MyBLBLApplication
import com.myblbl.R
import com.myblbl.feature.favorite.FavoriteFragment
import com.myblbl.feature.me.MeListFragment

/**
 * 首页二级 tab 配置项。
 * itemId 全局稳定（与位置无关）：「首页个人页签」开关切换或「首页板块管理」勾选变化
 * 时走 FragmentStateAdapter 的 itemId diff，未受影响的页及其数据/滚动位置原样保留。
 */
data class HomeTabSpec(
    val itemId: Long,
    val titleRes: Int,
    val factory: () -> Fragment
)

class HomeFragmentStateAdapter(
    private val fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    initialSpecs: List<HomeTabSpec> = DEFAULT_SPECS
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    private var currentSpecs: List<HomeTabSpec> = initialSpecs

    val specs: List<HomeTabSpec> get() = currentSpecs

    override fun getItemCount(): Int = currentSpecs.size

    override fun createFragment(position: Int): Fragment {
        return currentSpecs[position].factory()
    }

    override fun getItemId(position: Int): Long = currentSpecs[position].itemId

    override fun containsItem(itemId: Long): Boolean = currentSpecs.any { it.itemId == itemId }

    fun getCurrentFragment(position: Int): Fragment? {
        if (position < 0 || position >= currentSpecs.size) return null
        val fragmentTag = "f${getItemId(position)}"
        return fragmentManager.findFragmentByTag(fragmentTag)
    }

    fun getPageTitle(position: Int): CharSequence? {
        return currentSpecs.getOrNull(position)?.titleRes?.let {
            MyBLBLApplication.instance.getString(it)
        }
    }

    /** 换 tab 配置（板块管理/个人页签开关变化）：itemId 未变的页保留，移除的销毁，新增的按需创建。 */
    fun updateSpecs(newSpecs: List<HomeTabSpec>) {
        if (newSpecs == currentSpecs) return
        currentSpecs = newSpecs
        notifyDataSetChanged()
    }

    /** 分区页当前所在 tab 位置；不在展示集合时返回 -1。 */
    fun positionOfRegion(rid: Int): Int {
        val targetId = regionItemId(rid)
        return currentSpecs.indexOfFirst { it.itemId == targetId }
    }

    companion object {
        /** 「首页个人页签」开关存储键（值中文「开/关」，默认开，与 live_entry 同风格） */
        const val PERSONAL_TABS_KEY = "home_personal_tabs"

        /** 分区页 itemId 基数：与固定页(0..3)/个人页(4..6)错开，rid 直接偏移。 */
        private const val REGION_ID_BASE = 100_000_000L

        fun regionItemId(rid: Int): Long = REGION_ID_BASE + rid

        /** 单个分区页配置：标题取 HomeRegionCatalog 词条，页为 RegionFeedFragment。 */
        fun regionSpec(rid: Int): HomeTabSpec = HomeTabSpec(
            itemId = regionItemId(rid),
            titleRes = HomeRegionCatalog.titleOf(rid) ?: R.string.recommend,
            factory = { RegionFeedFragment.newInstance(rid) }
        )

        /** 默认配置（个人页签关、无分区勾选时的固定 4 页） */
        val DEFAULT_SPECS = listOf(
            HomeTabSpec(0, R.string.recommend) { RecommendListFragment.newInstance() },
            HomeTabSpec(1, R.string.hot) { HotListFragment.newInstance() },
            HomeTabSpec(2, R.string.animation) { HomeLaneFragment.newInstance(HomeLaneFragment.TYPE_ANIMATION) },
            HomeTabSpec(3, R.string.film_and_television) { HomeLaneFragment.newInstance(HomeLaneFragment.TYPE_CINEMA) }
        )

        /** 个人页 spec：历史/稍后观看插在热门后，收藏放最后。 */
        private val PERSONAL_HISTORY_SPEC =
            HomeTabSpec(4, R.string.history) { MeListFragment.newInstance(MeListFragment.TYPE_HISTORY) }
        private val PERSONAL_LATER_SPEC =
            HomeTabSpec(5, R.string.later_watch) { MeListFragment.newInstance(MeListFragment.TYPE_LATER) }
        private val PERSONAL_FAVORITE_SPEC =
            HomeTabSpec(6, R.string.collection) { FavoriteFragment.newEmbeddedInstance() }

        /**
         * 按当前设置组合完整 tab 配置：
         * 推荐、热门、[历史、稍后观看(个人页签开)]、番剧、影视、[勾选的分区板块们]、[收藏(个人页签开)]。
         * 分区板块由「首页板块管理」勾选结果动态生成。
         */
        fun buildSpecs(regionRids: List<Int>, personalEnabled: Boolean): List<HomeTabSpec> {
            val result = mutableListOf<HomeTabSpec>()
            result.add(DEFAULT_SPECS[0])
            result.add(DEFAULT_SPECS[1])
            if (personalEnabled) {
                result.add(PERSONAL_HISTORY_SPEC)
                result.add(PERSONAL_LATER_SPEC)
            }
            result.add(DEFAULT_SPECS[2])
            result.add(DEFAULT_SPECS[3])
            result.addAll(regionRids.map { regionSpec(it) })
            if (personalEnabled) {
                result.add(PERSONAL_FAVORITE_SPEC)
            }
            return result
        }

        /** 读「首页个人页签」开关（DataStore 无值视为默认开） */
        fun personalTabsEnabled(appSettings: com.myblbl.core.common.settings.AppSettingsDataStore): Boolean {
            return (appSettings.getCachedString(PERSONAL_TABS_KEY) ?: "开") == "开"
        }
    }
}
