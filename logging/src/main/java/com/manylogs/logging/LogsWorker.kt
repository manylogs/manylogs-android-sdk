package com.manylogs.logging

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import timber.log.Timber
import java.io.File

class LogsWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private var file: File? = null

    private var repository: ApiRepository = ApiRepository.createFromPreferences(context)

    init {
        inputData.getString("filename")?.let {
            file = context.getFileStreamPath(it)
            Timber.d("Worker: filename: $it")
        }
    }

    override fun doWork(): Result {
        file?.bufferedReader()?.useLines { lines ->
            val export = lines.fold("") { some, text ->
                "$some$text"
            }.trim()
            val logs: List<LogDataModel> = export.fromJsonList()
            repository.postLogs(LogsBody(logs))
        }

        file?.let {
            if (it.delete()) return Result.success()
        }

        return Result.failure()
    }
}