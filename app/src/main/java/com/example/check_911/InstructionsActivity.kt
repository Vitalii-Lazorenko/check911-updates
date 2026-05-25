package com.example.check_911

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class InstructionsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var commentEditText: EditText
    private lateinit var btnPhoto: ImageButton
    private lateinit var btnReuse: Button
    private lateinit var btnClear: ImageButton

    private lateinit var adapter: InstructionAdapter
    private val commentByDetail = mutableMapOf<String, String>()
    private var currentDetailId: String? = null
    private var suppressCommentWatcher = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructions)

        toolbar = findViewById(R.id.instructionsToolbar)
        recyclerView = findViewById(R.id.instructionsRecyclerView)
        commentEditText = findViewById(R.id.instructionCommentEditText)
        btnPhoto = findViewById(R.id.btnInstructionPhoto)
        btnReuse = findViewById(R.id.btnInstructionReuse)
        btnClear = findViewById(R.id.btnInstructionClear)

        setupToolbar()
        setupList()
        setupBottomActions()
        setupCommentWatcher()
        loadInstruction()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.title = intent.getStringExtra(EXTRA_INSTRUCTION_TITLE).orEmpty().ifBlank { "Інструкція" }
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_instruction_done -> {
                    Toast.makeText(this, "Підтвердження буде додано на наступному кроці", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_instruction_more -> {
                    Toast.makeText(this, "Додаткове меню буде додано на наступному кроці", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupList() {
        adapter = InstructionAdapter { detail ->
            currentDetailId = detail.localId
            val existing = commentByDetail[detail.localId].orEmpty()
            suppressCommentWatcher = true
            commentEditText.setText(existing)
            commentEditText.setSelection(commentEditText.text.length)
            suppressCommentWatcher = false
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupBottomActions() {
        btnPhoto.setOnClickListener {
            if (adapter.getSelectedDetail() == null) {
                Toast.makeText(this, "Оберіть пункт інструкції", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            adapter.markSelectedDetailAsCompleted()
            Toast.makeText(this, "Фото-дію позначено (камеру додамо наступним кроком)", Toast.LENGTH_SHORT).show()
        }

        btnReuse.setOnClickListener {
            Toast.makeText(this, "Масове застосування фото додамо наступним кроком", Toast.LENGTH_SHORT).show()
        }

        btnClear.setOnClickListener {
            val selected = adapter.getSelectedDetail()
            if (selected == null) {
                Toast.makeText(this, "Оберіть пункт інструкції", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            adapter.clearSelectedDetailCompletion()
            commentByDetail.remove(selected.localId)
            suppressCommentWatcher = true
            commentEditText.setText("")
            suppressCommentWatcher = false
        }
    }

    private fun setupCommentWatcher() {
        commentEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (suppressCommentWatcher) return
                val key = currentDetailId ?: return
                commentByDetail[key] = s?.toString().orEmpty()
            }
        })
    }

    private fun loadInstruction() {
        val instructionId = intent.getStringExtra(EXTRA_INSTRUCTION_ID)
        if (instructionId.isNullOrBlank()) {
            Toast.makeText(this, "Інструкцію не знайдено", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            val all = (application as App).database.instructionDao().getInstructionsWithCategories()
            val instruction = all.firstOrNull { it.instruction.id == instructionId }
            if (instruction == null) {
                Toast.makeText(this@InstructionsActivity, "Інструкцію не знайдено в локальній БД", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            val uiItems = buildList {
                instruction.categories.forEach { cat ->
                    add(InstructionUiItem.CategoryItem(cat.category.id, cat.category.title))
                    cat.details
                        .sortedBy { it.orderNumber }
                        .forEach { detail ->
                            add(
                                InstructionUiItem.DetailItem(
                                    InstructionDetailUi(
                                        localId = detail.localId,
                                        id = detail.id,
                                        categoryId = detail.categoryId,
                                        title = detail.title,
                                        templateId = detail.templateId,
                                        orderNumber = detail.orderNumber
                                    )
                                )
                            )
                        }
                }
            }
            adapter.setItems(uiItems)
        }
    }

    companion object {
        const val EXTRA_INSTRUCTION_ID = "EXTRA_INSTRUCTION_ID"
        const val EXTRA_INSTRUCTION_TITLE = "EXTRA_INSTRUCTION_TITLE"
    }
}
