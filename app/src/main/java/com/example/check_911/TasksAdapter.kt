package com.example.check_911

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.check_911.data.db.entity.TaskEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.recyclerview.widget.ListAdapter


class TasksAdapter : ListAdapter<TaskEntity, TasksAdapter.TaskViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textDate: TextView = itemView.findViewById(R.id.textDate)
        private val textTask: TextView = itemView.findViewById(R.id.textTask)

        fun bind(item: TaskEntity) {

            // 👉 дата (дд.мм.гг)
            textDate.text = formatDate(item.createdAt)

            // 👉 текст (обрезка сама через ellipsize)
            textTask.text = item.taskText ?: ""

            itemView.setOnClickListener {
                val context = itemView.context

                val intent = Intent(context, TaskDetailsActivity::class.java)
                intent.putExtra("task_id", item.id)
                context.startActivity(intent)
            }
        }

        private fun formatDate(date: String?): String {
            return try {
                val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val output = SimpleDateFormat("dd.MM.yy", Locale.getDefault())

                val parsed = input.parse(date ?: "")
                output.format(parsed ?: Date())
            } catch (e: Exception) {
                ""
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TaskEntity>() {
        override fun areItemsTheSame(oldItem: TaskEntity, newItem: TaskEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TaskEntity, newItem: TaskEntity) =
            oldItem == newItem
    }
}