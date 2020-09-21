package com.manylogs.logging

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.Request
import okhttp3.RequestBody
import timber.log.Timber
import java.io.File

class LogsWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private var file: File? = null
    private val client = ManyLogs.client

    init {
        val filename = inputData.getString("filename")
        file = context.getFileStreamPath(filename)

        Timber.d("Worker: filename: $filename")
    }

    override fun doWork(): Result {
        file?.bufferedReader()?.useLines { lines ->
            val export = lines.fold("") { some, text ->
                "$some$text"
            }.trim()
            val logs: List<LogDataModel> = export.fromJsonList()
            uploadLogs(LogsBody(logs))
        }

        file?.let {
            if (it.delete()) return Result.success()
        }

        return Result.failure()
    }

    private fun uploadLogs(body: LogsBody) {

        val reqBody = RequestBody
                .create(MEDIA_TYPE_JSON, body.json())

        val request = Request.Builder()
                .url(PATH_LOGS)
                .post(reqBody)
                .build()

        client.newCall(request).execute()
    }
}