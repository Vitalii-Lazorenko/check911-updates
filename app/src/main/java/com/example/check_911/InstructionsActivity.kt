package com.example.check_911

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.check_911.data.utils.AppLogger
import com.example.check_911.data.db.entity.InstructionAnswerEntity
import com.example.check_911.data.db.entity.InstructionResultEntity
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.File
import android.graphics.BitmapFactory
import java.util.UUID

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
    private var suppressCommentWatcher = false
    private var allItems: List<InstructionUiItem> = emptyList()
    private var currentFilter: InstructionFilterType = InstructionFilterType.ALL
    private val commentByDetail = mutableMapOf<String, String>()
    private val photoByDetail = mutableMapOf<String, String>()
    private val groupByDetail = mutableMapOf<String, String>()
    private var currentDetailId: String? = null
    private var currentPhotoPath: String? = null

    private lateinit var instructionId: String
    private lateinit var instructionTitle: String

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val selected = adapter.getSelectedDetail() ?: return@registerForActivityResult
            val rawPath = result.data?.getStringExtra("photoPath") ?: return@registerForActivityResult
            val path = compressImage(rawPath)
            val groupKey = UUID.randomUUID().toString()

            photoByDetail[selected.localId] = path
            groupByDetail[selected.localId] = groupKey
            val comment = commentByDetail[selected.localId].orEmpty()
            applyGroupComment(groupKey, comment)

            lifecycleScope.launch {
                persistSelectedAnswer(selected)
                persistAllAnswers()
            }
            refreshCompleted()
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val selected = adapter.getSelectedDetail() ?: return@registerForActivityResult
            val rawPath = currentPhotoPath ?: return@registerForActivityResult
            val path = compressImage(rawPath)
            val file = File(path)
            if (!file.exists() || file.length() <= 0L) return@registerForActivityResult
            val groupKey = UUID.randomUUID().toString()
            photoByDetail[selected.localId] = path
            groupByDetail[selected.localId] = groupKey
            val comment = commentByDetail[selected.localId].orEmpty()
            applyGroupComment(groupKey, comment)
            lifecycleScope.launch {
                persistSelectedAnswer(selected)
                persistAllAnswers("draft")
            }
            refreshCompleted()
        }

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

        instructionId = intent.getStringExtra(EXTRA_INSTRUCTION_ID).orEmpty()
        instructionTitle = intent.getStringExtra(EXTRA_INSTRUCTION_TITLE).orEmpty()
        if (instructionId.isBlank()) {
            Toast.makeText(this, "Інструкцію не знайдено", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupList()
        setupBottomActions()
        setupCommentWatcher()
        setupSearch()
        loadInstruction()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        toolbar.title = instructionTitle.ifBlank { "Інструкція" }
        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.survey_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_finish -> {
                validateAndMarkInstructionReady()
                true
            }
            R.id.action_options -> {
                showInstructionOptionsMenu(resolveOptionsAnchor())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun validateAndMarkInstructionReady() {
        val allDetailIds = allItems.filterIsInstance<InstructionUiItem.DetailItem>().map { it.detail.localId }
        val missing = allDetailIds.filter { photoByDetail[it].isNullOrBlank() }.toSet()
        if (missing.isNotEmpty()) {
            applyFilter(InstructionFilterType.ALL)
            adapter.highlightIncomplete(missing)
            adapter.selectNearestIncomplete()
            scrollToSelected()
            Toast.makeText(this, "Додайте фото для всіх пунктів інструкції", Toast.LENGTH_LONG).show()
            lifecycleScope.launch { persistAllAnswers("draft") }
            return
        }

        adapter.highlightIncomplete(emptySet())
        lifecycleScope.launch {
            persistAllAnswers("ready")
            (application as App).database.instructionResultDao().upsertResultSafe(
                InstructionResultEntity(
                    instructionId = instructionId,
                    instructionTitle = instructionTitle,
                    status = "ready"
                )
            )
            Toast.makeText(this@InstructionsActivity, "Інструкцію позначено як готову до відправки", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun showInstructionOptionsMenu(anchor: View) {
        val popup = PopupMenu(this, anchor, Gravity.END)
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

    private fun resolveOptionsAnchor(): View {
        val actionView = toolbar.findViewById<View>(R.id.action_options)
        if (actionView != null) return actionView

        val actionMenuView = (0 until toolbar.childCount)
            .map { toolbar.getChildAt(it) }
            .firstOrNull { it is ActionMenuView } as? ActionMenuView

        if (actionMenuView != null && actionMenuView.childCount > 0) {
            return actionMenuView.getChildAt(actionMenuView.childCount - 1)
        }
        return toolbar
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
            checkPermissionsAndOpenCamera()
        }

        btnReuse.setOnClickListener {
            showReuseDialog()
        }

        btnClear.setOnClickListener {
            val selected = adapter.getSelectedDetail()
            if (selected == null) {
                Toast.makeText(this, "Оберіть пункт інструкції", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            groupByDetail.remove(selected.localId)
            photoByDetail.remove(selected.localId)
            commentByDetail.remove(selected.localId)
            suppressCommentWatcher = true
            commentEditText.setText("")
            suppressCommentWatcher = false
            lifecycleScope.launch { persistAllAnswers("draft") }
            refreshCompleted()
        }

        btnArrowUp.setOnClickListener {
            val moved = adapter.moveSelection(-1)
            if (moved != null) scrollToSelected()
        }
        btnArrowDown.setOnClickListener {
            val moved = adapter.moveSelection(1)
            if (moved != null) scrollToSelected()
        }
    }

    private fun showReuseDialog() {
        val source = adapter.getSelectedDetail()
        if (source == null) {
            Toast.makeText(this, "Оберіть пункт-джерело", Toast.LENGTH_SHORT).show()
            return
        }
        val photoPath = photoByDetail[source.localId]
        if (photoPath.isNullOrBlank()) {
            Toast.makeText(this, "Спочатку зробіть фото для цього пункту", Toast.LENGTH_SHORT).show()
            return
        }

        val sourceGroup = groupByDetail[source.localId] ?: UUID.randomUUID().toString().also {
            groupByDetail[source.localId] = it
        }
        val sourceComment = commentByDetail[source.localId].orEmpty()

        val details = allItems.filterIsInstance<InstructionUiItem.DetailItem>().map { it.detail }
            .filter { it.localId != source.localId }
        if (details.isEmpty()) return

        val titles = details.map { it.title }.toTypedArray()
        val sourceGroupExisting = groupByDetail[source.localId]
        val checked = BooleanArray(details.size) { idx ->
            val d = details[idx]
            !sourceGroupExisting.isNullOrBlank() && groupByDetail[d.localId] == sourceGroupExisting
        }

        AlertDialog.Builder(this)
            .setTitle("Застосувати фото до інших пунктів")
            .setMultiChoiceItems(titles, checked) { _, which, isChecked -> checked[which] = isChecked }
            .setPositiveButton("Застосувати") { _, _ ->
                details.forEachIndexed { index, detail ->
                    if (checked[index]) {
                        photoByDetail[detail.localId] = photoPath
                        groupByDetail[detail.localId] = sourceGroup
                        commentByDetail[detail.localId] = sourceComment
                    } else {
                        if (groupByDetail[detail.localId] == sourceGroup) {
                            groupByDetail.remove(detail.localId)
                            photoByDetail.remove(detail.localId)
                            commentByDetail.remove(detail.localId)
                        }
                    }
                }
                lifecycleScope.launch { persistAllAnswers("draft") }
                refreshCompleted()
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun scrollToSelected() {
        val pos = adapter.getSelectedAdapterPosition()
        if (pos != RecyclerView.NO_POSITION) recyclerView.smoothScrollToPosition(pos)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        }
    }

    private fun checkPermissionsAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
        }
    }

    private fun openCamera() {
        if (shouldUseCustomCamera()) {
            cameraLauncher.launch(Intent(this, CameraActivity::class.java))
            return
        }

        val file = createImageFile() ?: return
        currentPhotoPath = file.absolutePath
        val uri: Uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        takePictureLauncher.launch(intent)
    }

    private fun shouldUseCustomCamera(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        return m.contains("xiaomi") || model.contains("redmi")
    }

    private fun createImageFile(): File? {
        return try {
            File.createTempFile(
                "instruction_${System.currentTimeMillis()}",
                ".jpg",
                getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun compressImage(path: String, quality: Int = 70): String {
        return runCatching {
            val source = File(path)
            if (!source.exists()) return path
            val bitmap = BitmapFactory.decodeFile(source.absolutePath) ?: return path
            val compressed = File(source.parentFile, "compressed_${source.nameWithoutExtension}.jpg")
            FileOutputStream(compressed).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
            }
            bitmap.recycle()
            if (compressed.exists() && compressed.length() > 0L) {
                if (compressed.absolutePath != source.absolutePath) {
                    runCatching { source.delete() }
                }
                compressed.absolutePath
            } else {
                path
            }
        }.getOrDefault(path)
    }

    private fun setupCommentWatcher() {
        commentEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (suppressCommentWatcher) return
                val key = currentDetailId ?: return
                val text = s?.toString().orEmpty()
                commentByDetail[key] = text
                val groupKey = groupByDetail[key]
                if (!groupKey.isNullOrBlank()) {
                    applyGroupComment(groupKey, text)
                }
                lifecycleScope.launch { persistAllAnswers("draft") }
            }
        })
    }

    private fun applyGroupComment(groupKey: String, comment: String) {
        groupByDetail.filterValues { it == groupKey }.keys.forEach { localId ->
            commentByDetail[localId] = comment
        }
    }

    private fun confirmClearAnswers() {
        AlertDialog.Builder(this)
            .setTitle("Очистити відповіді?")
            .setMessage("Коментарі та фото-мітки будуть скинуті.")
            .setPositiveButton("Так") { _, _ ->
                commentByDetail.clear()
                photoByDetail.clear()
                groupByDetail.clear()
                suppressCommentWatcher = true
                commentEditText.setText("")
                suppressCommentWatcher = false
                lifecycleScope.launch {
                    (application as App).database.instructionResultDao().clearAnswersForInstruction(instructionId)
                    (application as App).database.instructionResultDao().upsertResultSafe(
                        InstructionResultEntity(instructionId, instructionTitle, "draft")
                    )
                }
                refreshCompleted()
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun refreshCompleted() {
        val completed = photoByDetail.filterValues { !it.isNullOrBlank() }.keys.toSet()
        adapter.setCompletedDetails(completed)
        applyFilter(currentFilter)
    }

    private fun applyFilter(type: InstructionFilterType) {
        currentFilter = type
        val query = searchEditText.text?.toString()?.trim().orEmpty().lowercase()
        val completed = photoByDetail.filterValues { !it.isNullOrBlank() }.keys.toSet()

        val detailByCategory = allItems
            .filterIsInstance<InstructionUiItem.DetailItem>()
            .groupBy { it.detail.categoryId }

        val filteredDetailsByCategory = mutableMapOf<String, List<InstructionUiItem.DetailItem>>()
        detailByCategory.forEach { (categoryId, details) ->
            val filtered = details.filter { detailItem ->
                val isCompleted = completed.contains(detailItem.detail.localId)
                val passFilter = when (type) {
                    InstructionFilterType.ALL -> true
                    InstructionFilterType.COMPLETED -> isCompleted
                    InstructionFilterType.INCOMPLETE -> !isCompleted
                }
                val passSearch = query.isBlank() || detailItem.detail.title.lowercase().contains(query)
                passFilter && passSearch
            }
            if (filtered.isNotEmpty()) filteredDetailsByCategory[categoryId] = filtered
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
        lifecycleScope.launch {
            val db = (application as App).database
            val all = db.instructionDao().getInstructionsWithCategories()
            val instruction = all.firstOrNull { it.instruction.id == instructionId }
            if (instruction == null) {
                Toast.makeText(this@InstructionsActivity, "Інструкцію не знайдено в локальній БД", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            val existingResult = db.instructionResultDao().getResult(instructionId)
            if (existingResult == null) {
                db.instructionResultDao().upsertResultSafe(
                    InstructionResultEntity(instructionId, instructionTitle, "draft")
                )
            }

            val savedAnswers = db.instructionResultDao().getAnswersByInstruction(instructionId)
            AppLogger.log(
                "InstructionsActivity",
                "loadInstruction: instructionId=$instructionId, savedAnswers=${savedAnswers.size}",
                this@InstructionsActivity
            )
            val detailsById = instruction.categories
                .flatMap { category -> category.details }
                .groupBy { it.id }
            val detailsByLegacyLocalId = instruction.categories
                .flatMap { category -> category.details.map { detail -> "${category.category.id}_${detail.id}_${detail.orderNumber}" to detail.localId } }
                .toMap()
            savedAnswers.forEach {
                val resolvedLocalId = when {
                    detailsByLegacyLocalId.containsKey(it.detailLocalId) -> detailsByLegacyLocalId[it.detailLocalId]
                    detailsById[it.detailId].isNullOrEmpty() -> null
                    else -> detailsById[it.detailId]?.firstOrNull()?.localId
                } ?: it.detailLocalId

                if (!it.photoPath.isNullOrBlank()) photoByDetail[resolvedLocalId] = it.photoPath
                if (!it.comment.isNullOrBlank()) commentByDetail[resolvedLocalId] = it.comment
                if (!it.groupKey.isNullOrBlank()) groupByDetail[resolvedLocalId] = it.groupKey
            }
            AppLogger.log(
                "InstructionsActivity",
                "loadInstruction mapped: photos=${photoByDetail.size}, comments=${commentByDetail.size}, groups=${groupByDetail.size}",
                this@InstructionsActivity
            )

            allItems = buildList {
                instruction.categories.forEach { cat ->
                    add(InstructionUiItem.CategoryItem(cat.category.id, cat.category.title))
                    cat.details.sortedBy { it.orderNumber }.forEach { detail ->
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

    private suspend fun persistAllAnswers(statusOverride: String = "draft") {
        val db = (application as App).database
        val previousAnswers = db.instructionResultDao().getAnswersByInstruction(instructionId)
        val previousPaths = previousAnswers.mapNotNull { it.photoPath }.toSet()
        val detailItems = allItems.filterIsInstance<InstructionUiItem.DetailItem>().map { it.detail }
        val answers = detailItems.mapNotNull { detail ->
            val photo = photoByDetail[detail.localId]
            val comment = commentByDetail[detail.localId]
            val group = groupByDetail[detail.localId]
            if (photo.isNullOrBlank() && comment.isNullOrBlank()) return@mapNotNull null

            InstructionAnswerEntity(
                detailLocalId = detail.localId,
                instructionId = instructionId,
                detailId = detail.id,
                detailTitle = detail.title,
                groupKey = group,
                comment = comment,
                photoPath = photo
            )
        }

        db.instructionResultDao().replaceInstructionAnswers(instructionId, answers)
        val afterSave = db.instructionResultDao().getAnswersByInstruction(instructionId)
        AppLogger.log(
            "InstructionsActivity",
            "persistAllAnswers: instructionId=$instructionId, status=$statusOverride, detailItems=${detailItems.size}, saved=${answers.size}, dbAfterSave=${afterSave.size}",
            this@InstructionsActivity
        )
        val currentPaths = answers.mapNotNull { it.photoPath }.toSet()
        (previousPaths - currentPaths).forEach { path ->
            runCatching {
                val f = File(path)
                if (f.exists()) f.delete()
            }
        }

        val current = db.instructionResultDao().getResult(instructionId)
        db.instructionResultDao().upsertResultSafe(
            InstructionResultEntity(
                instructionId = instructionId,
                instructionTitle = instructionTitle,
                status = statusOverride,
                sentDate = if (statusOverride == "sent") current?.sentDate else null
            )
        )
    }

    private suspend fun persistSelectedAnswer(detail: InstructionDetailUi) {
        val db = (application as App).database
        val photo = photoByDetail[detail.localId]
        val comment = commentByDetail[detail.localId]
        val group = groupByDetail[detail.localId]
        val existingResult = db.instructionResultDao().getResult(instructionId)
        if (existingResult == null) {
            db.instructionResultDao().upsertResultSafe(
                InstructionResultEntity(instructionId, instructionTitle, "draft")
            )
        }
        if (photo.isNullOrBlank() && comment.isNullOrBlank()) {
            db.instructionResultDao().deleteAnswer(instructionId, detail.localId)
            return
        }
        db.instructionResultDao().upsertAnswer(
            InstructionAnswerEntity(
                detailLocalId = detail.localId,
                instructionId = instructionId,
                detailId = detail.id,
                detailTitle = detail.title,
                groupKey = group,
                comment = comment,
                photoPath = photo
            )
        )
        AppLogger.log(
            "InstructionsActivity",
            "persistSelectedAnswer: instructionId=$instructionId, detailLocalId=${detail.localId}, hasPhoto=${!photo.isNullOrBlank()}",
            this@InstructionsActivity
        )
    }

    companion object {
        const val EXTRA_INSTRUCTION_ID = "EXTRA_INSTRUCTION_ID"
        const val EXTRA_INSTRUCTION_TITLE = "EXTRA_INSTRUCTION_TITLE"
    }
}
