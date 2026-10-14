package com.myblbl.feature.home

import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2
import com.myblbl.databinding.FragmentHomeBinding
import com.myblbl.ui.activity.MainActivity
import com.myblbl.ui.fragment.main.MainNavigationViewModel
import com.myblbl.ui.fragment.main.MainTabFocusTarget
import com.myblbl.core.ui.base.OnBackPressedHandler
import com.myblbl.core.ui.tab.enableTouchNavigation
import com.myblbl.core.ui.tab.focusNearestTabTo
import com.myblbl.core.ui.tab.focusSelectedTab
import com.myblbl.core.ui.tab.disableAdjacentPagePrefetch
import com.myblbl.core.ui.tab.retainAllPagesAfterFirstLayout
import com.myblbl.core.ui.focus.SpatialFocusNavigator
import com.myblbl.core.common.ext.getHomeDefaultStartPageIndex
import com.myblbl.core.common.settings.AppSettingsDataStore
import com.myblbl.event.AppEventHub
import com.myblbl.core.common.log.AppLog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class HomeFragment : Fragment(), MainTabFocusTarget, OnBackPressedHandler {

    companion object {
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val mainNavigationViewModel: MainNavigationViewModel by activityViewModels()
    private val appSettings: AppSettingsDataStore by inject()
    private val appEventHub: AppEventHub by inject()
    private lateinit var adapter: HomeFragmentStateAdapter
    private var tabMediator: TabLayoutMediator? = null
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private var tabSelectedListener: TabLayout.OnTabSelectedListener? = null
    private var lastTabSelectedPosition = -1
    private var lastTabSelectedTime = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val t0 = SystemClock.elapsedRealtime()
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        AppLog.i("STARTUP", "HomeFragment.onCreateView elapsed=${SystemClock.elapsedRealtime() - t0}ms")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val t0 = SystemClock.elapsedRealtime()
        super.onViewCreated(view, savedInstanceState)
        adapter = HomeFragmentStateAdapter(
            fragmentManager = childFragmentManager,
            lifecycle = viewLifecycleOwner.lifecycle,
            regionRids = HomeRegionCatalog.loadSelectedRids(appSettings)
        )
        binding.viewPager.adapter = adapter
        // 冷启动先按需创建（只建当前页，不拖慢首帧），首帧后再保留全部 tab 页：
        // 此后切 tab 不销毁 fragment，滚动进度/焦点/已加载数据切回来原样保留
        // （配合各 tab 页"非当前页不加载"的懒加载，空壳成本极低）
        binding.viewPager.disableAdjacentPagePrefetch()
        binding.viewPager.retainAllPagesAfterFirstLayout()
        tabMediator = TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = adapter.getPageTitle(position)
        }.also { it.attach() }
        binding.tabLayout.enableTouchNavigation(
            viewPager = binding.viewPager,
            onNavigateDown = ::focusCurrentPageNearestContent,
            onNavigateLeft = ::focusLeftFunctionArea,
            onTabReselected = { index ->
                // 重选当前 tab = 刷新（v2.0.9 行为，对齐 blbl refreshCurrentPageFromTabReselect；
                // feed 页 refresh 自带刷新后回顶，"没看完的视频"顾虑在 blbl 同语义下未成立）
                (adapter.getCurrentFragment(index) as? HomeTabPage)?.refresh()
            }
        )

        tabSelectedListener = object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                lastTabSelectedPosition = tab.position
                lastTabSelectedTime = System.currentTimeMillis()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        }.also { binding.tabLayout.addOnTabSelectedListener(it) }
        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                notifyTabSelected(position)
            }
        }.also { callback ->
            binding.viewPager.registerOnPageChangeCallback(callback)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainNavigationViewModel.events.collectLatest { event ->
                    val currentBinding = _binding ?: return@collectLatest
                    if (isHidden) {
                        return@collectLatest
                    }
                    if (event is MainNavigationViewModel.Event.MainTabSelected && event.index == 0) {
                        (adapter.getCurrentFragment(currentBinding.viewPager.currentItem) as? HomeTabPage)
                            ?.onTabSelected()
                    }
                }
            }
        }

        // "首页板块管理"勾选变化：即时重排 tab（设置页是 overlay，本页仍 STARTED 可实时收到；
        // 另在 onResume 幂等兜底，防止事件被错过）
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appEventHub.events.collectLatest { event ->
                    if (event is AppEventHub.Event.HomeRegionsChanged) {
                        applyRegionTabsIfNeeded()
                    }
                }
            }
        }

        binding.viewPager.currentItem = getDefaultTabIndex()
        AppLog.i("STARTUP", "HomeFragment.onViewCreated elapsed=${SystemClock.elapsedRealtime() - t0}ms")
    }

    private fun getDefaultTabIndex(): Int {
        return getHomeDefaultStartPageIndex(
            maxIndex = adapter.itemCount - 1,
            defaultIndex = 0
        )
    }

    override fun onResume() {
        super.onResume()
        // 兜底：SharedFlow 无 replay，若"首页板块管理"事件在本页非 STARTED 时发出会错过，
        // 这里幂等比对一次勾选集合（无变化时 zero-cost）。
        applyRegionTabsIfNeeded()
    }

    /**
     * 按设置重排分区板块 tab（幂等）：
     * 仍被勾选的分区页因 itemId 稳定被 ViewPager 复用（滚动进度/已加载内容保留），
     * 重排后尽量停留回当前正在看的页（被取消勾选的分区页回落到"推荐"）。
     */
    private fun applyRegionTabsIfNeeded() {
        val currentBinding = _binding ?: return
        if (!this::adapter.isInitialized) return
        val newRids = HomeRegionCatalog.loadSelectedRids(appSettings)
        val currentPageIdentity = adapter.pageIdentityAt(currentBinding.viewPager.currentItem)
        if (!adapter.updateRegions(newRids)) {
            return
        }
        // 页数可能增加：首帧后保留全部页的 offscreenPageLimit 需跟随新页数，
        // 否则超出旧 limit 的分区页会被销毁重建（ViewModel 与滚动进度丢失）。
        val pageCount = adapter.itemCount
        currentBinding.viewPager.offscreenPageLimit =
            if (pageCount > 1) pageCount - 1 else ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        val targetPosition = adapter.positionOfIdentity(currentPageIdentity)
        currentBinding.viewPager.setCurrentItem(
            if (targetPosition >= 0) targetPosition else 0,
            false
        )
        AppLog.i("HomeRegions", "region tabs updated size=${newRids.size} resume=$currentPageIdentity->$targetPosition")
    }

    /** 分区页位置查询：动态 tab 下 RegionFeedFragment 向宿主反查自身 tab 序号。 */
    fun positionOfRegionPage(rid: Int): Int {
        if (!this::adapter.isInitialized) return -1
        return adapter.positionOfRegion(rid)
    }


    /**
     * BACK 三层分发（对齐 blbl）：
     * 内容区 → 聚焦当前选中的二级 tab；tab 栏 → 回左侧主边栏当前主 tab 按钮；
     * 都不是（如焦点已在主边栏）→ 返回 false 交给 MainActivity 走双击退出。
     */
    override fun onBackPressed(): Boolean {
        val currentBinding = _binding ?: return false
        val tabFocus = currentBinding.tabLayout.hasFocus()
        val contentFocus = currentBinding.viewPager.hasFocus()
        AppLog.d("HomeBack", "onBackPressed tabFocus=$tabFocus contentFocus=$contentFocus isHidden=$isHidden")
        if (tabFocus) {
            val handled = (activity as? MainActivity)?.focusSidebarCurrentTab() == true
            AppLog.d("HomeBack", "tab->sidebar handled=$handled")
            return handled
        }
        if (contentFocus) {
            val handled = currentBinding.tabLayout.focusSelectedTab()
            AppLog.d("HomeBack", "content->tab handled=$handled")
            return handled
        }
        return false
    }

    fun focusCurrentTab(anchorView: View? = view?.findFocus() ?: activity?.currentFocus): Boolean {
        val currentBinding = _binding ?: return false
        return currentBinding.tabLayout.focusNearestTabTo(anchorView)
    }

    /**
     * 列表顶行 UP 的落点：聚焦当前选中的二级 tab（不切页），对齐 blbl focusSelectedTabIfAvailable。
     */
    fun focusSelectedTabFromContent(): Boolean {
        return _binding?.tabLayout?.focusSelectedTab() == true
    }

    /**
     * 内容网格左右边缘切二级 tab（对齐 blbl 的边缘切页）。
     * 首 tab 左边缘回退到主边栏、末 tab 右边缘吞键，均保持原有行为。
     */
    fun switchAdjacentTabFromContentEdge(delta: Int): Boolean {
        val currentBinding = _binding ?: return false
        val target = currentBinding.viewPager.currentItem + delta
        if (target < 0) {
            return focusLeftFunctionArea()
        }
        if (target >= adapter.itemCount) {
            return true
        }
        currentBinding.viewPager.setCurrentItem(target, true)
        // 切页后旧页焦点随页面滑出，需把焦点交接到新页（页面 retain，恢复该页原浏览位置）；
        // 平滑滚动期间布局未稳时首次请求可能落空，补一次延迟重试
        currentBinding.viewPager.post {
            if (_binding == null) return@post
            if (!focusCurrentPageNearestContent()) {
                currentBinding.viewPager.postDelayed({
                    if (_binding != null) {
                        focusCurrentPageNearestContent()
                    }
                }, 120L)
            }
        }
        return true
    }

    override fun focusEntryFromMainTab(): Boolean {
        return focusEntryFromMainTab(anchorView = null, preferSpatialEntry = false)
    }

    override fun focusEntryFromMainTab(anchorView: View?, preferSpatialEntry: Boolean): Boolean {
        if (preferSpatialEntry && anchorView != null) {
            val currentBinding = _binding ?: return false
            if (isVerticallyAlignedWith(anchorView, currentBinding.tabLayout)) {
                if (focusCurrentTab(anchorView)) return true
            }
            val handled = SpatialFocusNavigator.requestBestDescendant(
                anchorView = anchorView,
                root = currentBinding.root,
                direction = View.FOCUS_RIGHT,
                fallback = null
            )
            if (handled) return true
        }
        val handled = focusCurrentPagePrimaryContent(anchorView, preferSpatialEntry) ||
            focusCurrentTab(anchorView)
        return handled
    }

    private fun isVerticallyAlignedWith(anchor: View, target: View): Boolean {
        val anchorRect = Rect()
        val targetRect = Rect()
        if (!anchor.getGlobalVisibleRect(anchorRect)) return false
        if (!target.getGlobalVisibleRect(targetRect)) return false
        return maxOf(anchorRect.top, targetRect.top) < minOf(anchorRect.bottom, targetRect.bottom)
    }

    private fun focusCurrentPagePrimaryContent(anchorView: View? = null, preferSpatialEntry: Boolean = false): Boolean {
        return getCurrentTabPage()?.focusPrimaryContent(anchorView, preferSpatialEntry) == true
    }

    /** 顶部 tab 栏按 DOWN：就近聚焦可见第一项（列表位置不动，半截轻滚补齐）；回顶只由重选 tab 承担。 */
    private fun focusCurrentPageNearestContent(): Boolean {
        val page = getCurrentTabPage() ?: return false
        if (page.focusNearestVisibleContent()) {
            return true
        }
        return focusCurrentPagePrimaryContent()
    }

    private fun focusLeftFunctionArea(): Boolean {
        return (activity as? MainActivity)?.focusLeftFunctionArea() == true
    }

    private fun getCurrentTabPage(): HomeTabPage? {
        val currentItem = binding.viewPager.currentItem
        val fragmentTag = "f${adapter.getItemId(currentItem)}"
        return childFragmentManager.findFragmentByTag(fragmentTag) as? HomeTabPage
    }

    fun isCurrentPage(position: Int): Boolean {
        val currentBinding = _binding ?: return false
        return currentBinding.viewPager.currentItem == position
    }

    private fun notifyTabSelected(position: Int, retries: Int = 5) {
        val page = adapter.getCurrentFragment(position) as? HomeTabPage
        if (page != null) {
            page.onTabSelected()
        } else if (retries > 0) {
            binding.viewPager.post { notifyTabSelected(position, retries - 1) }
        }
    }
}
