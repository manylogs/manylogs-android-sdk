package com.manylogs.logging

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

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

data class ConfigResponse(
    @SerializedName("data_settings")
    val dataSettings: DataSettingsModel,
    @SerializedName("replay_ids")
    val replayIds: List<Long>
)

data class DataSettingsModel(
    @SerializedName("is_replay_enabled")
    val isReplayEnabled: Boolean = true,
    @SerializedName("is_data_stream_enabled")
    val isDataStreamEnabled: Boolean = true
)

data class ReplayBodyModel(val requestHash: Long)