package com.manylogs.logging

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit


internal class ApiRepository(
    val preferences: SharedPreferences,
    val host: String,
    val apiKey: String
) {

    private val service: ApiService = createApiService(host = host)

    fun postLogs(body: LogsBody) {
        service.sendLogs(body).execute()
    }

    private fun syncConfig() {
        service.getConfig().enqueue(object : Callback<ConfigResponse> {

            override fun onFailure(call: Call<ConfigResponse>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onResponse(
                call: Call<ConfigResponse>,
                response: Response<ConfigResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        // todo - save
                    }
                }
            }
        })
    }

    ///////////////////////////////////////////////////////////////////////////
    // Create
    ///////////////////////////////////////////////////////////////////////////

    private fun createClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        val authenticator = Authenticator(apiKey)
        logging.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient.Builder().apply {
            addInterceptor(authenticator)
            addInterceptor(logging)
            connectTimeout(30, TimeUnit.SECONDS)
            readTimeout(30, TimeUnit.SECONDS)
            writeTimeout(30, TimeUnit.SECONDS)
        }.build()
    }

    private fun createApiService(client: OkHttpClient = createClient(), host: String): ApiService {
        val gson = GsonBuilder().create()
        val retrofit = Retrofit.Builder()
            .baseUrl(host)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()
        return retrofit.create(ApiService::class.java)
    }

    companion object {
        fun createFromPreferences(context: Context): ApiRepository {
            val preferences = getPreferences(context)

            val host: String = preferences.get("host", "")
            val key: String = preferences.get("api_key", "")

            if (host.isEmpty() || key.isEmpty()) throw IllegalStateException("Host or api key is missing")

            return ApiRepository(host = host, apiKey = key, preferences = preferences)
        }
    }
}

internal class Authenticator(private val token: String): Interceptor {

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        return if (token.isEmpty()) {
            chain.proceed(chain.request())
        } else {
            val authenticatedRequest = chain.request()
                .newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(authenticatedRequest)
        }
    }
}

internal interface ApiService {

    @Headers("Accept: application/json; charset=utf-8")
    @POST("data-stream")
    fun sendLogs(@Body body: LogsBody): Call<Unit>

    @Headers("Accept: application/json; charset=utf-8")
    @GET("config")
    fun getConfig(): Call<ConfigResponse>
}