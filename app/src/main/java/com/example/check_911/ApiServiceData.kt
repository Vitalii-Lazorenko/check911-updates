package com.example.check_911

import com.example.check_911.data.networking.models.SurveyDto
import com.example.check_911.data.networking.networking.models.UsersDto
import com.example.check_911.data.networking.networking.models.SurveyUploadRequest
import com.example.check_911.data.networking.networking.models.TaskAnswerRequest
import com.example.check_911.data.networking.networking.models.TaskDto
import com.example.check_911.data.networking.networking.models.InstructionDto
import com.example.check_911.data.networking.networking.models.InstructionLogPostRequest
import com.example.check_911.data.networking.networking.models.InstructionLogPostResponse
import com.example.check_911.data.networking.networking.models.reserve.AuthorizationResponse
import com.example.check_911.data.networking.networking.models.reserve.LoginRequest
import com.example.check_911.data.networking.networking.models.reserve.SurveyUploadResponse
import com.example.check_911.data.networking.networking.models.reserve.TokenResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
//import com.google.android.gms.common.api.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
//import com.squareup.moshi.JsonClass
//import org.apache.poi.ss.usermodel.Cell
//import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Part

interface ApiServiceData {

    @GET("token/get")
    suspend fun getToken(): TokenResponse

//    авторизация, получение токена, названия тт и адреса тт
    @POST("authorization/login")
    suspend fun login(@Body request: LoginRequest): AuthorizationResponse

//    получение пользователей
    @GET("/user/get/all")
    suspend fun getUsers(@Header("Authorization") token: String): List<UsersDto>

//    получение опросников
    @GET("survey/get/all")
    suspend fun getSurveys(
        @Header("Authorization") token: String,
        @Query("onlyPharmacy") onlyPharmacy: Int = 1
    ): Response<List<SurveyDto>>

    // получение опросников по QR-code
    @GET("survey/get/all")
    suspend fun getSurveysByHeaderId(
        @Header("Authorization") token: String,
//        @Query("headerId") headerId: String,
        @Query("headerParentId") headerId: String,
        @Query("onlyPharmacy") onlyPharmacy: Int = 0
    ): Response<List<SurveyDto>>

//      выгрузка ответов
    @POST("survey_log/post")
    suspend fun uploadSurvey(
        @Header("Authorization") token: String,
        @Body request: SurveyUploadRequest
    ): Response<SurveyUploadResponse>

//    метод для выгрузки фото
    @Multipart
    @PUT("survey_log/accept/file")
    suspend fun uploadPhoto(
        @Header("Authorization") token: String,
        @Query("pharmacyId") pharmacyId: Long,
        @Part("logHeaderId") logHeaderId: RequestBody,
        @Part("questionId") questionId: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<Unit>

    // метод для подтверджения завершения выгрузки результатов
    @POST("/survey_log/finalize")
    suspend fun finalizeSurveyUpload(
        @Header("Authorization") token: String,
        @Query("logHeaderId") logHeaderId: String,
//        @Query("pharmacyId") pharmacyId: Long
    ): Response<Unit>

//    метод для получения задач для тт
    @GET("/survey_pharmacy_task/get")
    suspend fun getTasks(
        @Header("Authorization") token: String
    ): Response<List<TaskDto>>

    @GET("/instruction/get/all")
    suspend fun getInstructions(
        @Header("Authorization") token: String
    ): Response<List<InstructionDto>>

    @POST("/instruction_log/post")
    suspend fun postInstructionLog(
        @Header("Authorization") token: String,
        @Body body: InstructionLogPostRequest
    ): Response<InstructionLogPostResponse>

    @Multipart
    @PUT("/instruction_log/accept/file")
    suspend fun uploadInstructionPhoto(
        @Header("Authorization") token: String,
        @Query("pharmacyId") pharmacyId: Long,
        @Part file: MultipartBody.Part,
        @Part("logHeaderId") logHeaderId: RequestBody,
        @Part("comment") comment: RequestBody,
        @Part("details[]") details: List<RequestBody>
    ): Response<Unit>

//    метод для отправки выполнения задачи
    @PATCH("/survey_pharmacy_task/id/{id}/set/answer")
    suspend fun sendTaskAnswer(
        @Header("Authorization") token: String,
        @Path("id") taskId: String,
        @Body body: TaskAnswerRequest
    ): Response<Unit>
}
