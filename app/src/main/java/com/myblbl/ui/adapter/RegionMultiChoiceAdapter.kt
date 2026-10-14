package com.myblbl.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.myblbl.R
import com.myblbl.databinding.CellSettingChoiceBinding

/**
 * 设置-通用-首页板块管理：多选列表。
 *
 * 复用单选 cell 布局（cell_setting_choice）：名称行 + 右侧勾选图标；
 * 点击即切换勾选并回调（不关闭弹窗，即时生效）。与
 * [SettingSelectionDialogAdapter] 的单选（点击即选中并关闭）不同。
 */
class RegionMultiChoiceAdapter(
    private val rids: List<Int>,
    private val names: List<String>,
    selectedRids: Set<Int>,
    private val onToggle: (rid: Int, checked: Boolean) -> Unit
) : RecyclerView.Adapter<RegionMultiChoiceAdapter.RegionViewHolder>() {

    private val selected = LinkedHashSet(rids.filter { it in selectedRids })

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegionViewHolder {
        return RegionViewHolder(
            CellSettingChoiceBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RegionViewHolder, position: Int) {
        holder.bind(rid = rids[position], name = names[position])
    }

    override fun getItemCount(): Int = rids.size

    fun requestInitialFocus(recyclerView: RecyclerView?) {
        recyclerView?.scrollToPosition(0)
        recyclerView?.post {
            recyclerView.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
        }
    }

    inner class RegionViewHolder(
        private val binding: CellSettingChoiceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                val rid = rids[position]
                val checked = if (rid in selected) {
                    selected.remove(rid)
                    false
                } else {
                    selected.add(rid)
                    true
                }
                applyState(isSelected = checked, isFocused = binding.root.hasFocus())
                onToggle(rid, checked)
            }
            binding.root.setOnFocusChangeListener { _, hasFocus ->
                applyState(
                    isSelected = rids.getOrNull(bindingAdapterPosition) in selected,
                    isFocused = hasFocus
                )
            }
        }

        fun bind(rid: Int, name: String) {
            binding.tvTitle.text = name
            applyState(
                isSelected = rid in selected,
                isFocused = binding.root.hasFocus()
            )
        }

        private fun applyState(isSelected: Boolean, isFocused: Boolean) {
            binding.tvCurrent.visibility = View.GONE
            binding.iconCheck.visibility = if (isSelected) View.VISIBLE else View.GONE

            binding.root.isSelected = isSelected
            binding.root.animate()
                .scaleX(if (isFocused) 1.02f else 1f)
                .scaleY(if (isFocused) 1.02f else 1f)
                .setDuration(120L)
                .start()

            val titleColor = if (isSelected) {
                ContextCompat.getColor(binding.root.context, R.color.colorAccent)
            } else {
                ContextCompat.getColor(binding.root.context, R.color.textColor)
            }
            binding.tvTitle.setTextColor(titleColor)
        }
    }
}
