package com.example.check_911

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

private enum class InstructionFilterType {
    COMPLETED, INCOMPLETE, ALL
}

class InstructionsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var commentEditText: EditText
    private lateinit var btnPhoto: ImageButton
    private lateinit var btnReuse: Button
    private lateinit var btnClear: ImageButton
    private lateinit var btnArrowUp: ImageButton
    private lateinit var btnArrowDown: ImageButton
    private lateinit var searchBarContainer: View
    private lateinit var searchEditText: EditText

    private lateinit var adapter: InstructionAdapter
    private val commentByDetail = mutableMapOf<String, String>()
    private var currentDetailId: String? = null
    private var suppressCommentWatcher = false
    private var allItems: List<InstructionUiItem> = emptyList()
    private var currentFilter: InstructionFilterType = InstructionFilterType.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructions)

        toolbar = findViewById(R.id.instructionsToolbar)
        recyclerView = findViewById(R.id.instructionsRecyclerView)
        commentEditText = findViewById(R.id.instructionCommentEditText)
        btnPhoto = findViewById(R.id.btnInstructionPhoto)
        btnReuse = findViewById(R.id.btnInstructionReuse)
        btnClear = findViewById(R.id.btnInstructionClear)
        btnArrowUp = findViewById(R.id.btnInstructionArrowUp)
        btnArrowDown = findViewById(R.id.btnInstructionArrowDown)
        searchBarContainer = findViewById(R.id.instructionSearchBarContainer)
        searchEditText = findViewById(R.id.instructionSearchEditText)

        setupToolbar()
        setupList()
        setupBottomActions()
        setupCommentWatcher()
        setupSearch()
        loadInstruction()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.title = intent.getStringExtra(EXTRA_INSTRUCTION_TITLE).orEmpty().ifBlank { "Інструкція" }
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_instruction_done -> true
                R.id.action_instruction_more -> {
                    showInstructionOptionsMenu(toolbar)
                    true
                }
                else -> false
            }
        }
    }

    private fun showInstructionOptionsMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.instruction_options_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_clear_answers -> {
                    confirmClearAnswers()
                    true
                }
                R.id.menu_search -> {
                    toggleSearch()
                    true
                }
                R.id.filter_completed -> {
                    applyFilter(InstructionFilterType.COMPLETED)
                    true
                }
                R.id.filter_incomplete -> {
                    applyFilter(InstructionFilterType.INCOMPLETE)
                    true
                }
                R.id.filter_all -> {
                    applyFilter(InstructionFilterType.ALL)
                    true
                }
                else -> false
            }
        }
        popup.show()
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
            applyFilter(currentFilter)
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
            applyFilter(currentFilter)
        }

        btnArrowUp.setOnClickListener {
            val moved = adapter.moveSelection(-1)
            if (moved != null) {
                scrollToSelected()
            }
        }

        btnArrowDown.setOnClickListener {
            val moved = adapter.moveSelection(1)
            if (moved != null) {
                scrollToSelected()
            }
        }
    }

    private fun scrollToSelected() {
        val pos = adapter.getSelectedAdapterPosition()
        if (pos != RecyclerView.NO_POSITION) {
            recyclerView.smoothScrollToPosition(pos)
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                applyFilter(currentFilter)
            }
        })
    }

    private fun toggleSearch() {
        val isVisible = searchBarContainer.visibility == View.VISIBLE
        if (isVisible) {
            searchBarContainer.visibility = View.GONE
            searchEditText.text.clear()
            hideKeyboard()
        } else {
            searchBarContainer.visibility = View.VISIBLE
            searchEditText.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
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

    private fun confirmClearAnswers() {
        AlertDialog.Builder(this)
            .setTitle("Очистити відповіді?")
            .setMessage("Коментарі та фото-мітки будуть скинуті.")
            .setPositiveButton("Так") { _, _ ->
                commentByDetail.clear()
                suppressCommentWatcher = true
                commentEditText.setText("")
                suppressCommentWatcher = false
                adapter.setCompletedDetails(emptySet())
                applyFilter(currentFilter)
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun applyFilter(type: InstructionFilterType) {
        currentFilter = type
        val query = searchEditText.text?.toString()?.trim().orEmpty().lowercase()
        val completed = adapter.getCompletedDetailIds()

        val detailByCategory = allItems
            .filterIsInstance<InstructionUiItem.DetailItem>()
            .groupBy { it.detail.categoryId }

        val filteredDetailsByCategory = mutableMapOf<String, List<InstructionUiItem.DetailItem>>()

        detailByCategory.forEach { (categoryId, details) ->
            val afterFilter = details.filter { detailItem ->
                val isCompleted = completed.contains(detailItem.detail.localId)
                val passFilter = when (type) {
                    InstructionFilterType.ALL -> true
                    InstructionFilterType.COMPLETED -> isCompleted
                    InstructionFilterType.INCOMPLETE -> !isCompleted
                }
                val passSearch = query.isBlank() || detailItem.detail.title.lowercase().contains(query)
                passFilter && passSearch
            }
            if (afterFilter.isNotEmpty()) {
                filteredDetailsByCategory[categoryId] = afterFilter
            }
        }

        val rebuilt = mutableListOf<InstructionUiItem>()
        allItems.filterIsInstance<InstructionUiItem.CategoryItem>().forEach { cat ->
            val details = filteredDetailsByCategory[cat.categoryId].orEmpty()
            if (details.isNotEmpty()) {
                rebuilt.add(cat)
                rebuilt.addAll(details.sortedBy { it.detail.orderNumber })
            }
        }

        adapter.setItems(rebuilt)
        adapter.setCompletedDetails(completed)

        if (adapter.getSelectedDetail() == null) {
            adapter.selectFirstDetail()
            scrollToSelected()
        }
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

            allItems = buildList {
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
            applyFilter(InstructionFilterType.ALL)
            adapter.selectFirstDetail()
            scrollToSelected()
        }
    }

    companion object {
        const val EXTRA_INSTRUCTION_ID = "EXTRA_INSTRUCTION_ID"
        const val EXTRA_INSTRUCTION_TITLE = "EXTRA_INSTRUCTION_TITLE"
    }
}
