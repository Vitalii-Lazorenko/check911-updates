package com.example.check_911.data.networking.models

import android.util.Log
import com.example.check_911.data.db.repository.AppUrlRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UrlSelectInterceptor(


) : Interceptor, KoinComponent {
    private val appUrlRepository: AppUrlRepository by inject()
    override fun intercept(chain: Interceptor.Chain): Response {

        var request = chain.request()

        val host = runBlocking { appUrlRepository.host.first() }
        Log.d("qqq", "intercept: $host")
//        val newUrl = request.url().newBuilder()
        val newUrl = request.url.newBuilder()
            .host(host)
            .build()

        request = request.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(request)
    }

}