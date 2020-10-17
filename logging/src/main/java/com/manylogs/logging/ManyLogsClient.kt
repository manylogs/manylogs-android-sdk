package com.manylogs.logging

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.reactivex.rxjava3.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ManyLogsClient(
    context: Context,
    host: String,
    apiKey: String
) {

    private val publisher: PublishSubject<LogDataModel> = PublishSubject.create()
    private val preferences = getPreferences(context)
    private var repository: ApiRepository

    private val PATH_REPLAY = "$host/replay"

    init {
        preferences.put("host", host)
        preferences.put("api_key", apiKey)

        repository = ApiRepository.createFromPreferences(context)

        initPublishers(context)
    }

    fun queue(data: LogDataModel) {
        publisher.onNext(data)
    }

    private fun initPublishers(context: Context) {
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

}