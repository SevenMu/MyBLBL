package com.myblbl.feature.me

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.myblbl.core.common.format.NumberUtils
import com.myblbl.core.ui.image.ImageLoader
import com.myblbl.databinding.ItemContinuationCardBinding
import com.myblbl.model.video.HistoryVideoModel

/**
 * 历史页「继续观看」横条卡片。点击复用历史条目的跳播放链路；
 * 进度行显示「看到 N」。
 */
class ContinuationCardAdapter(
    private val onItemClick: (HistoryVideoModel) -> Unit,
) : RecyclerView.Adapter<ContinuationCardAdapter.ViewHolder>() {

    private val items = ArrayList<HistoryVideoModel>()

    fun submitList(list: List<HistoryVideoModel>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemContinuationCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(
        private val binding: ItemContinuationCardBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HistoryVideoModel) {
            ImageLoader.load(binding.ivCover, item.cover)
            binding.tvTitle.text = item.title
            binding.tvProgress.text =
                binding.root.context.getString(
                    com.myblbl.R.string.history_watched_progress,
                    NumberUtils.formatDuration(item.progress),
                )
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }
}
