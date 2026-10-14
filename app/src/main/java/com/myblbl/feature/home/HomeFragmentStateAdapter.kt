package com.myblbl.feature.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.myblbl.MyBLBLApplication
import com.myblbl.R

/**
 * 首页 tab 适配器：固定 4 板块（推荐/热门/番剧/影视）+ 按"首页板块管理"勾选
 * 结果动态展示的分区板块（[RegionFeedFragment]）。
 *
 * 固定页 itemId 沿用 position（0..3，兼容既有 tag 寻址）；分区页 itemId
 * 用 rid 偏移（[REGION_ID_BASE] + rid），勾选集合变化后仍在场的分区页因 itemId
 * 稳定而被 ViewPager 复用——ViewModel、已加载内容与滚动进度原样保留。
 */
class HomeFragmentStateAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    regionRids: List<Int> = emptyList()
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    private val fragmentManager: FragmentManager = fragmentManager

    private sealed interface Page {
        data object Recommend : Page
        data object Hot : Page
        data object Animation : Page
        data object Cinema : Page
        data class Region(val rid: Int) : Page
    }

    private var regionRids: List<Int>
    private var pages: List<Page>

    init {
        this.regionRids = regionRids
        pages = buildPages(regionRids)
    }

    private fun buildPages(rids: List<Int>): List<Page> {
        val fixedPages = listOf<Page>(Page.Recommend, Page.Hot, Page.Animation, Page.Cinema)
        return fixedPages + rids.map { Page.Region(it) }
    }

    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment {
        return when (val page = pages.getOrNull(position)) {
            Page.Recommend -> RecommendListFragment.newInstance()
            Page.Hot -> HotListFragment.newInstance()
            Page.Animation -> HomeLaneFragment.newInstance(HomeLaneFragment.TYPE_ANIMATION)
            Page.Cinema -> HomeLaneFragment.newInstance(HomeLaneFragment.TYPE_CINEMA)
            is Page.Region -> RegionFeedFragment.newInstance(page.rid)
            null -> HomeLaneFragment.newInstance(HomeLaneFragment.TYPE_CINEMA)
        }
    }

    override fun getItemId(position: Int): Long {
        return when (val page = pages.getOrNull(position)) {
            is Page.Region -> REGION_ID_BASE + page.rid
            else -> position.toLong()
        }
    }

    override fun containsItem(itemId: Long): Boolean {
        if (itemId in 0 until FIXED_PAGE_COUNT) return true
        val rid = (itemId - REGION_ID_BASE).toInt()
        return rid > 0 && Page.Region(rid) in pages
    }

    fun getCurrentFragment(position: Int): Fragment? {
        if (position < 0 || position >= pages.size) return null
        val fragmentTag = "f${getItemId(position)}"
        return fragmentManager.findFragmentByTag(fragmentTag)
    }

    fun getPageTitle(position: Int): CharSequence? {
        val context = MyBLBLApplication.instance
        return when (val page = pages.getOrNull(position)) {
            Page.Recommend -> context.getString(R.string.recommend)
            Page.Hot -> context.getString(R.string.hot)
            Page.Animation -> context.getString(R.string.animation)
            Page.Cinema -> context.getString(R.string.film_and_television)
            is Page.Region -> HomeRegionCatalog.titleOf(page.rid)
                ?.let { context.getString(it) }
            null -> null
        }
    }

    /** 分区页当前所在 tab 位置；不在展示集合时返回 -1。 */
    fun positionOfRegion(rid: Int): Int {
        return pages.indexOfFirst { it is Page.Region && it.rid == rid }
    }

    /**
     * 更新勾选展示的分区集合；itemId 仍存在的分区页被复用（不销毁重建），
     * 被取消勾选的分区页由 ViewPager2 移除。返回集合是否发生变化。
     */
    fun updateRegions(newRegionRids: List<Int>): Boolean {
        if (regionRids == newRegionRids) return false
        regionRids = newRegionRids
        pages = buildPages(newRegionRids)
        notifyDataSetChanged()
        return true
    }

    /** 当前 position 的页身份（固定页 "fixed:n" / 分区页 "region:rid"），供重排后回跳同一页。 */
    fun pageIdentityAt(position: Int): String {
        return when (val page = pages.getOrNull(position)) {
            is Page.Region -> "region:${page.rid}"
            null, Page.Recommend -> "fixed:0"
            else -> "fixed:$position"
        }
    }

    fun positionOfIdentity(identity: String): Int {
        val separatorIndex = identity.indexOf(':')
        if (separatorIndex <= 0) return -1
        return when (identity.substring(0, separatorIndex)) {
            "region" -> {
                val rid = identity.substring(separatorIndex + 1).toIntOrNull() ?: return -1
                positionOfRegion(rid)
            }
            "fixed" -> {
                val index = identity.substring(separatorIndex + 1).toIntOrNull() ?: return -1
                if (index in 0 until FIXED_PAGE_COUNT) index else -1
            }
            else -> -1
        }
    }

    companion object {
        private const val FIXED_PAGE_COUNT = 4
        private const val REGION_ID_BASE = 100_000_000L
    }
}
