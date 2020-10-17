package com.manylogs.logging

import com.google.gson.JsonElement

data class LogDataModel(
    val request: Request,
    val response: Response
) {
    data class Request(
            val headers: List<String>,
            val url: String,
            val method: String,
            val body: JsonElement?
    )

    data class Response(
            val headers: List<String>,
            val code: String,
            val body: JsonElement?
    )
}

data class LogsBody(
        val data: List<LogDataModel>
)

data class ConfigResponse(val replay: List<String>)

data class ReplayBodyModel(val requestHash: String)