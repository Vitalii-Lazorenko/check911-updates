package com.example.check_911

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class InstructionAdapter(
    private val onDetailSelected: (InstructionDetailUi) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val originalItems = mutableListOf<InstructionUiItem>()
    private val visibleItems = mutableListOf<InstructionUiItem>()
    private val expandedCategories = mutableSetOf<String>()
    private val completedDetailIds = mutableSetOf<String>()
    private var selectedDetailLocalId: String? = null

    companion object {
        private const val CATEGORY = 0
        private const val DETAIL = 1
    }

    fun setItems(items: List<InstructionUiItem>) {
        originalItems.clear()
        originalItems.addAll(items)
        expandedCategories.clear()
        items.filterIsInstance<InstructionUiItem.CategoryItem>().forEach {
            expandedCategories.add(it.categoryId)
        }
        rebuildVisibleItems()
    }

    fun setCompletedDetails(localIds: Set<String>) {
        completedDetailIds.clear()
        completedDetailIds.addAll(localIds)
        notifyDataSetChanged()
    }

    fun markSelectedDetailAsCompleted() {
        val id = selectedDetailLocalId ?: return
        completedDetailIds.add(id)
        notifyDataSetChanged()
    }

    fun clearSelectedDetailCompletion() {
        val id = selectedDetailLocalId ?: return
        completedDetailIds.remove(id)
        notifyDataSetChanged()
    }

    fun getSelectedDetail(): InstructionDetailUi? {
        val selectedId = selectedDetailLocalId ?: return null
        val item = visibleItems.firstOrNull {
            it is InstructionUiItem.DetailItem && it.detail.localId == selectedId
        } as? InstructionUiItem.DetailItem
        return item?.detail
    }

    fun getCompletedDetailIds(): Set<String> = completedDetailIds.toSet()

    override fun getItemViewType(position: Int): Int = when (visibleItems[position]) {
        is InstructionUiItem.CategoryItem -> CATEGORY
        is InstructionUiItem.DetailItem -> DETAIL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == CATEGORY) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
            CategoryVH(v)
        } else {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_instruction_detail, parent, false)
            DetailVH(v)
        }
    }

    override fun getItemCount(): Int = visibleItems.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = visibleItems[position]) {
            is InstructionUiItem.CategoryItem -> (holder as CategoryVH).bind(item)
            is InstructionUiItem.DetailItem -> (holder as DetailVH).bind(item.detail)
        }
    }

    private fun rebuildVisibleItems() {
        visibleItems.clear()
        originalItems.forEach { item ->
            when (item) {
                is InstructionUiItem.CategoryItem -> visibleItems.add(item)
                is InstructionUiItem.DetailItem -> {
                    if (expandedCategories.contains(item.detail.categoryId)) {
                        visibleItems.add(item)
                    }
                }
            }
        }
        notifyDataSetChanged()
    }

    inner class CategoryVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title = itemView.findViewById<TextView>(R.id.textViewCategoryTitle)
        private val arrow = itemView.findViewById<ImageView>(R.id.imageViewArrow)

        fun bind(item: InstructionUiItem.CategoryItem) {
            title.text = item.title
            val expanded = expandedCategories.contains(item.categoryId)
            arrow.rotation = if (expanded) 90f else 0f

            itemView.setOnClickListener {
                if (expandedCategories.contains(item.categoryId)) {
                    expandedCategories.remove(item.categoryId)
                } else {
                    expandedCategories.add(item.categoryId)
                }
                rebuildVisibleItems()
            }
        }
    }

    inner class DetailVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text = itemView.findViewById<TextView>(R.id.textViewInstructionDetail)
        private val camera = itemView.findViewById<ImageView>(R.id.imageViewInstructionCamera)

        fun bind(detail: InstructionDetailUi) {
            text.text = detail.title

            val completed = completedDetailIds.contains(detail.localId)
            camera.setColorFilter(if (completed) Color.BLUE else Color.RED)

            val selected = selectedDetailLocalId == detail.localId
            itemView.setBackgroundColor(if (selected) Color.parseColor("#FCFEBB") else Color.TRANSPARENT)

            itemView.setOnClickListener {
                val previous = selectedDetailLocalId
                selectedDetailLocalId = detail.localId
                onDetailSelected(detail)
                if (previous != null) notifyDataSetChanged() else notifyItemChanged(bindingAdapterPosition)
            }

            camera.setOnClickListener {
                val previous = selectedDetailLocalId
                selectedDetailLocalId = detail.localId
                onDetailSelected(detail)
                if (previous != null) notifyDataSetChanged() else notifyItemChanged(bindingAdapterPosition)
            }
        }
    }
}
