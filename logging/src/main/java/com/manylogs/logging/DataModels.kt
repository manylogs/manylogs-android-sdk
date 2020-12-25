package com.manylogs.logging

import com.google.gson.JsonElement
import com.google.gson.JsonObject

data class LogDataModel(
    val request: Request,
    val response: Response
) {
    data class Request(
        val headers: JsonObject?,
        val url: String,
        val method: String,
        val body: JsonElement?
    )

    data class Response(
        val headers: JsonObject?,
        val code: String,
        val body: JsonElement?
    )
}

data class LogsBody(
    val data: List<LogDataModel>
)

data class ConfigResponse(val replay: List<String>)

data class ReplayBodyModel(val requestHash: String)