package com.example.check_911

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import com.example.check_911.data.utils.AppLogger

class TasksActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var titleText: TextView

    private val adapter = TasksAdapter()

    private val database by lazy { (application as App).database }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks)

        recyclerView = findViewById(R.id.recyclerViewTasks)
        titleText = findViewById(R.id.textTitle)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        observeTasks()
    }

    private fun observeTasks() {
        lifecycleScope.launch {
//            database.taskDao().getAllTasksFlow().collect { tasks ->
            database.taskDao().getAllTasks().collect { tasks ->

                // 👉 сортировка: новые сверху
//                val sorted = tasks.sortedByDescending { it.createdAt }

//                adapter.submitList(sorted)
                adapter.submitList(tasks)
                AppLogger.log("TasksActivity", "Завдання: $tasks")
                titleText.text = "Мої завдання (${tasks.size})"
            }
        }
    }
}