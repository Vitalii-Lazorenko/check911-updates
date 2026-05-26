package com.example.check_911.data.db.repository

import com.example.check_911.ApiServiceData
import com.example.check_911.data.db.dao.InstructionResultDao
import com.example.check_911.data.db.entity.InstructionAnswerEntity
import com.example.check_911.data.networking.networking.models.InstructionLogPostRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InstructionUploadRepository(
    private val api: ApiServiceData,
    private val dao: InstructionResultDao
) {
    suspend fun uploadReadyInstructions(
        token: String,
        pharmacyId: Long,
        gammaId: Long,
        dateVersion: String
    ) {
        val parsedDateVersion = parseDateVersion(dateVersion)
        val ready = dao.getAllResults().filter { it.status == "ready" }
        ready.forEach { result ->
            uploadSingleInstruction(token, pharmacyId, gammaId, parsedDateVersion, result.instructionId)
        }
    }

    private suspend fun uploadSingleInstruction(
        token: String,
        pharmacyId: Long,
        gammaId: Long,
        dateVersion: Date?,
        instructionId: String
    ) {
        val answers = dao.getAnswersByInstruction(instructionId).filter { !it.photoPath.isNullOrBlank() }
        if (answers.isEmpty()) return

        val logResponse = api.postInstructionLog(
            token = token,
            body = InstructionLogPostRequest(
                pharmacyId = pharmacyId,
                gammaId = gammaId,
                dateVersion = dateVersion,
                instructionHeaderId = instructionId
            )
        )
        if (!logResponse.isSuccessful) {
            throw Exception("instruction_log/post error ${logResponse.code()}: ${logResponse.errorBody()?.string()}")
        }
        val logHeaderId = logResponse.body()?.logHeaderId
            ?: throw Exception("instruction_log/post missing logHeaderId")

        val grouped = answers.groupBy { it.groupKey ?: it.detailLocalId }
        grouped.values.forEach { groupAnswers ->
            uploadGroup(token, pharmacyId, logHeaderId, groupAnswers)
        }

        dao.updateResultStatus(
            instructionId = instructionId,
            status = "sent",
            sentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
    }

    private suspend fun uploadGroup(
        token: String,
        pharmacyId: Long,
        logHeaderId: String,
        answers: List<InstructionAnswerEntity>
    ) {
        val first = answers.first()
        val photoPath = first.photoPath ?: throw Exception("photoPath is missing for instruction answer group")
        val file = File(photoPath)
        if (!file.exists()) throw Exception("Photo file not found: $photoPath")

        val filePart = MultipartBody.Part.createFormData(
            "file",
            file.name,
            file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        )
        val logHeaderPart = logHeaderId.toRequestBody("text/plain".toMediaTypeOrNull())
        val commentPart = (first.comment ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
        val detailsParts = answers.map {
            it.detailId.toRequestBody("text/plain".toMediaTypeOrNull())
        }

        val response = api.uploadInstructionPhoto(
            token = token,
            pharmacyId = pharmacyId,
            file = filePart,
            logHeaderId = logHeaderPart,
            comment = commentPart,
            details = detailsParts
        )
        if (!response.isSuccessful) {
            throw Exception("instruction_log/accept/file error ${response.code()}: ${response.errorBody()?.string()}")
        }
    }

    private fun parseDateVersion(raw: String): Date? {
        val input = SimpleDateFormat("dd.MM.yy", Locale.getDefault()).apply {
            isLenient = false
        }
        return runCatching { input.parse(raw.trim()) }.getOrNull()
    }
}
