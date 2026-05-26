package com.example.check_911

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class InstructionTasksActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var titleText: TextView
    private val adapter = InstructionTasksAdapter()
    private val database by lazy { (application as App).database }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instruction_tasks)

        recyclerView = findViewById(R.id.recyclerViewTasks)
        titleText = findViewById(R.id.textTitle)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            database.instructionTaskDao().getAllTasks().collect { tasks ->
                adapter.submitList(tasks)
                titleText.text = "Задачі по інструкціях (${tasks.size})"
            }
        }
    }
}
