package com.myblbl.ui.adapter

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.myblbl.core.ui.base.BaseVideoAdapter
import com.myblbl.core.ui.focus.VideoCardFocusHelper
import com.myblbl.core.ui.image.ImageLoader
import com.myblbl.databinding.CellSeriesBinding
import com.myblbl.model.video.VideoModel

/**
 * 分类页 PGC 板块（纪录片/电影/电视剧/国创/番剧/综艺）专用：
 * 按首页番剧栏的剧集海报卡样式（cell_series：3:4 封面 + 标题 + 更新进度副标题）
 * 渲染 pgc/season/rank/list 排行榜数据，无子分区栏、无 UP 行、无角标。
 * 骨架沿用 [BaseVideoAdapter]（diff/焦点/stableId/边缘导航与 [VideoAdapter] 同构）。
 */
class PgcSeriesGridAdapter(
    private val onItemClick: (VideoModel) -> Unit = {},
    onTopEdgeUp: (() -> Boolean)? = null,
    onBottomEdgeDown: (() -> Boolean)? = null,
    onLeftEdge: (() -> Boolean)? = null,
    onRightEdge: (() -> Boolean)? = null,
    onItemFocused: ((Int) -> Unit)? = null,
    onItemFocusedWithView: ((View, Int) -> Unit)? = null,
    onItemDpad: ((View, Int, KeyEvent) -> Boolean)? = null,
    onItemsChanged: (() -> Unit)? = null
) : BaseVideoAdapter<VideoModel, PgcSeriesGridAdapter.SeriesCardViewHolder>() {

    init {
        this.onTopEdgeUp = onTopEdgeUp
        this.onBottomEdgeDown = onBottomEdgeDown
        this.onLeftEdge = onLeftEdge
        this.onRightEdge = onRightEdge
        this.onItemFocused = onItemFocused
        this.onItemFocusedWithView = onItemFocusedWithView
        this.onItemDpad = onItemDpad
        this.onItemsChanged = onItemsChanged
        setHasStableIds(true)
    }

    override fun itemKey(item: VideoModel): String {
        return when {
            item.sid > 0L -> "sid:${item.sid}"
            item.aid > 0L -> "aid:${item.aid}"
            else -> "title:${item.title}|cover:${item.coverUrl}"
        }
    }

    override fun areContentsSame(old: VideoModel, new: VideoModel): Boolean {
        return old.title == new.title &&
            old.cover == new.cover &&
            old.desc == new.desc &&
            old.viewCount == new.viewCount
    }

    override fun coverUrlOf(item: VideoModel): String? {
        return item.cover.takeIf { it.isNotBlank() }
    }

    override fun onCreateContentViewHolder(parent: ViewGroup, viewType: Int): SeriesCardViewHolder {
        val binding = CellSeriesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SeriesCardViewHolder(
            binding = binding,
            onItemClick = onItemClick,
            onTopEdgeUp = onTopEdgeUp,
            onLeftEdge = onLeftEdge,
            onRightEdge = onRightEdge,
            onBottomEdgeDown = onBottomEdgeDown,
            onItemFocusedWithView = onItemFocusedWithView,
            onItemDpad = onItemDpad
        )
    }

    override fun onBindContentViewHolder(holder: SeriesCardViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    class SeriesCardViewHolder(
        private val binding: CellSeriesBinding,
        private val onItemClick: (VideoModel) -> Unit,
        onTopEdgeUp: (() -> Boolean)?,
        onLeftEdge: (() -> Boolean)?,
        onRightEdge: (() -> Boolean)?,
        onBottomEdgeDown: (() -> Boolean)?,
        private val onItemFocusedWithView: ((View, Int) -> Unit)?,
        private val onItemDpad: ((View, Int, KeyEvent) -> Boolean)?
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        private var currentItem: VideoModel? = null

        init {
            binding.clickView.setOnClickListener {
                currentItem?.let(onItemClick)
            }
            binding.clickView.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    val position = bindingAdapterPosition
                    if (position != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                        onItemFocusedWithView?.invoke(view, position)
                    }
                }
            }
            binding.clickView.setOnKeyListener { view, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                    false
                } else {
                    onItemDpad?.invoke(view, bindingAdapterPosition, event) == true
                }
            }
            VideoCardFocusHelper.bindSidebarExit(
                view = binding.clickView,
                onTopEdgeUp = onTopEdgeUp,
                onLeftEdge = onLeftEdge,
                onRightEdge = onRightEdge,
                onBottomEdgeDown = onBottomEdgeDown
            )
        }

        fun bind(item: VideoModel) {
            currentItem = item

            ImageLoader.loadSeriesCover(binding.imageView, item.cover)

            val title = item.title
            if (title.isBlank()) {
                binding.textView.visibility = View.GONE
            } else {
                binding.textView.visibility = View.VISIBLE
                binding.textView.text = title
            }

            // 副标题：new_ep.index_show（"更新至第X话"等更新进度），见 fromPgcSeasonJson
            if (item.desc.isBlank()) {
                binding.textSub.visibility = View.GONE
            } else {
                binding.textSub.visibility = View.VISIBLE
                binding.textSub.text = item.desc
            }

            binding.textBadge.visibility = View.GONE
        }
    }
}
