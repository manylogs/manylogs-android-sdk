package com.manylogs.logging

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.reactivex.rxjava3.subjects.PublishSubject
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

private val gson = Gson()

val MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8")

val HOST = "http://192.168.1.85:8080/api/v1"
val PATH_LOGS = "$HOST/logs"
val PATH_CONFIG = "$HOST/config"
val PATH_REPLAY = "$HOST/replay"

@Deprecated("")
class ManyLogs(context: Context) {

    private val publisher: PublishSubject<LogDataModel> = PublishSubject.create()
    private val cachedRequests: ArrayList<String> = restoreConfig()
    private val preferences = getLogzyPreferences(context)

    init {
        client = createClient()
        syncConfig()

        publisher
                .buffer(3, TimeUnit.SECONDS)
                .filter { it.size > 0 }
                .doOnNext { it.removeAll { it.request.url == PATH_REPLAY } } // removes REPLAY calls
                .doOnNext {  Timber.d("Requests count: ${it.size}") }
                .map {
                    val filename = "logzy_${System.currentTimeMillis()}"
                    val content = it.json()
                    context.openFileOutput(filename, Context.MODE_PRIVATE).use {
                        it.write(content.toByteArray())
                    }
                    return@map filename
                }
                .doOnNext {
                    val work = OneTimeWorkRequestBuilder<LogsWorker>()
                            .setInputData(workDataOf("filename" to it))
                            .build()
                    WorkManager.getInstance(context).enqueue(work)
                }
                .subscribe()
    }

    fun parseRequest(request: Request): Request {

//        val body = request.body()?.toString() todo - figure out how to convert the body to json()

        val req = LogDataModel.Request(
            url = request.url().toString(),
            method = request.method(),
            body = JsonObject()
        )

        // replay request
        val requestHash = req.hash()
        if (cachedRequests.contains(requestHash)) {
            val body = RequestBody.create(MEDIA_TYPE_JSON, ReplayBodyModel(requestHash).json())
            val builder = request.newBuilder()
            builder.url(PATH_REPLAY)
            builder.post(body)
            return builder.build()
        }

        return request
    }

    fun log(response: Response): Response {
//        publisher.onNext(response.toLogData())
        return response
    }

    private fun syncConfig() {
        val request = Request.Builder()
                .url(PATH_CONFIG)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    response.body()?.string()?.let {
                        val config = gson.fromJson(it, ConfigResponse::class.java)
                        cachedRequests.clear()
                        cachedRequests.addAll(config.replay)
                        saveConfig()
                    }
                }
            }
        })
    }

    private fun saveConfig() {
        preferences.put("cached_requests", cachedRequests)
    }

    private fun restoreConfig(): ArrayList<String> {
        return preferences.get("cached_requests", arrayListOf<String>())
    }

    private fun createClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(logging)
        builder.connectTimeout(30, TimeUnit.SECONDS)
        builder.readTimeout(30, TimeUnit.SECONDS)
        builder.writeTimeout(30, TimeUnit.SECONDS)
        return builder.build()
    }

    companion object {
        lateinit var client: OkHttpClient
    }
}

