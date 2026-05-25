package com.example.check_911

//import com.example.ekka_mini_v1.data.networking.UrlSelectInterceptor
import com.example.check_911.data.networking.models.UrlSelectInterceptor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.localebro.okhttpprofiler.OkHttpProfilerInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


//import com.localebro.okhttpprofiler.OkHttpProfilerInterceptor


object NetWorkProvider {
    val BASE_URL = "http://10.128.233.15:3350"

    fun provideRetrofit(
        okHttpClient: OkHttpClient = provideOkHttpClient(),
        link: String
    ): Retrofit {
        val gson = GsonBuilder()
            .serializeNulls() // <-- ВАЖНО: сериализовать null-поля
            .create()

        return Retrofit.Builder()
            .baseUrl(link)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson)) // <-- передаём gson с serializeNulls
            .build()
    }

    private fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(UrlSelectInterceptor())

        // Добавляем OkHttpProfilerInterceptor только в режиме отладки
        if (BuildConfig.DEBUG_VERSION) {
            if (BuildConfig.DEBUG) {
                builder.addInterceptor(OkHttpProfilerInterceptor())
            }
        }

        return builder.build()
    }

    fun <S> provideApiService(retrofit: Retrofit, clazz: Class<S>): S = retrofit.create(clazz)


}

fun <S> provideApiService(retrofit: Retrofit, clazz: Class<S>): S = retrofit.create(clazz)