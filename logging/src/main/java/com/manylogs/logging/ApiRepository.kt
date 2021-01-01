package com.manylogs.logging

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import okhttp3.*
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
import timber.log.Timber
import java.util.concurrent.TimeUnit

internal val MEDIA_TYPE_JSON = MediaType.parse("application/json")

internal class ApiRepository(
    val preferences: SharedPreferences,
    val host: String,
    val apiKey: String
) {

    private var isConfigSynced = false;

    private var hashIds = emptySet<Long>()
    private var settings: DataSettingsModel = DataSettingsModel()

    private val pathReplay = "$host/data-flow/replay"

    private val service: ApiService = createApiService(host = host)

    init {
        syncConfig() // note - this is also called with every Worker init (data sync)
    }

    fun postLogs(body: LogsBody) {
        service.sendLogs(body).execute()
    }

    fun createReplayRequest(requestHash: Long): Request {
        val json = ReplayBodyModel(requestHash = requestHash).json()
        val body = RequestBody.create(MEDIA_TYPE_JSON, json)
        return Request.Builder()
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .url(pathReplay)
            .build()
    }

    fun syncConfig() {
        Timber.d("Syncing config...")
        service.getConfig().enqueue(object : Callback<ConfigResponse> {

            override fun onFailure(call: Call<ConfigResponse>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onResponse(
                call: Call<ConfigResponse>,
                response: Response<ConfigResponse>
            ) {
                if (response.isSuccessful) {
                    Timber.d("Config synced successfully!")
                    isConfigSynced = true;
                    response.body()?.let {
                        hashIds = it.replayIds.toSet()
                        settings = it.dataSettings
                    }
                }
            }
        })
    }

    ///////////////////////////////////////////////////////////////////////////
    // Getters
    ///////////////////////////////////////////////////////////////////////////

    fun getReplayHashIds(): Set<Long> {
        if (!isConfigSynced) Timber.d("Trying to access config data (hash ids) before it has synced.")
        return hashIds
    }

    fun isDataReplayEnabled(): Boolean {
        if (!isConfigSynced) Timber.d("Trying to access config data (replay status) before it has synced.")
        return settings.isReplayEnabled;
    }

    fun isDataSyncEnabled(): Boolean {
        if (!isConfigSynced) Timber.d("Trying to access config data (sync status) before it has synced.")
        return settings.isDataStreamEnabled;
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
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .create()
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

internal class Authenticator(private val token: String) : Interceptor {

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
    @GET("user/config")
    fun getConfig(): Call<ConfigResponse>
}