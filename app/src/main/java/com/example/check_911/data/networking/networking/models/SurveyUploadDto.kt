package com.example.check_911.data.networking.networking.models

import com.google.gson.annotations.SerializedName
import java.util.Date

data class SurveyUploadRequest(
    val surveyId: String,
    val pharmacyId: Long,
    val gammaId: Long,
//    val dateVersion: String,
    val dateVersion: Date?,
    val answers: List<SurveyAnswerUpload>
)

data class SurveyAnswerUpload(
    val questionId: String,
    val imageData: String?,    // base64
    val text: String?,
    val number: Int?,
    val percent: Double?,
    val comment: String?,
    val single: String?,        // ID одного ответа
    val multi: List<String>  = emptyList()   // ID нескольких ответов
)

