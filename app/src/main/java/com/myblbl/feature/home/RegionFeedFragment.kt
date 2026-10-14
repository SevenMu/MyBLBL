package com.myblbl.feature.home

import androidx.core.os.bundleOf
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * 首页分区板块页：以推荐流形式展示指定分区（rid）的视频，
 * 完整复用 [VideoFeedFragment] 的列表渲染/懒加载/焦点控制/边缘切 tab 能力。
 */
class RegionFeedFragment : VideoFeedFragment() {

    companion object {
        private const val ARG_RID = "rid"

        fun newInstance(rid: Int): RegionFeedFragment {
            return RegionFeedFragment().apply {
                arguments = bundleOf(ARG_RID to rid)
            }
        }
    }

    private val regionRid: Int by lazy { arguments?.getInt(ARG_RID, 0) ?: 0 }

    private val viewModel: RegionFeedViewModel by viewModel { parametersOf(regionRid) }

    override val feedViewModel: VideoFeedViewModel
        get() = viewModel

    /**
     * 动态 tab 的位置随"A板板块管理"勾选配置变化（重排后仍被勾选的分区页会被
     * ViewPager 复用、不重建实例），因此位置必须运行时向宿主查询，而非构造时固定。
     */
    override val secondaryTabPosition: Int
        get() = (parentFragment as? HomeFragment)?.positionOfRegionPage(regionRid) ?: -1

    override val toastNonEmptyError: Boolean = true
    override val enableTvListFocusController: Boolean = true
}
