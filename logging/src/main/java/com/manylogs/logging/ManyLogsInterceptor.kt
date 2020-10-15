package com.manylogs.logging

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class ManyLogsInterceptor(private val client: ManyLogsClient) : Interceptor {

    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = parse(chain.request())
        val response = log(chain.proceed(request))
        return response
    }

    private fun parse(request: Request): Request {
        return request
    }

    private fun log(response: Response): Response {
        client.queue(response.toLogData())
        return response
    }

    private fun Response.toLogData(): LogDataModel {
        val req = LogDataModel.Request(
            url = request().url().toString(),
            method = request().method(),
            body = JsonObject() // todo - peak body
        )

        val resBody = peekBody(Long.MAX_VALUE).string()

        val res = LogDataModel.Response(
            headers = headers().toString(),
            code = code().toString(),
            body = gson.fromJson(resBody, JsonElement::class.java)
        )

        return LogDataModel(
            request = req,
            response = res
        )
    }
}


