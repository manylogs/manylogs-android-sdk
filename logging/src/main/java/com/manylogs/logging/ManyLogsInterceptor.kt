package com.manylogs.logging

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import okhttp3.*
import okio.Buffer
import okio.BufferedSource
import timber.log.Timber
import java.io.EOFException
import java.nio.charset.Charset

var UTF8 = Charset.forName("UTF-8")

class ManyLogsInterceptor(private val client: ManyLogsClient) : Interceptor {

    private val gson = Gson()

    @Suppress("UnnecessaryVariable")
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = parse(chain.request())
        val response = log(chain.proceed(request))
        return response
    }

    private fun parse(request: Request): Request {
        val method = request.method()
        val url = request.url().toString()
        val hash = Encodings.encodeRequest(method, url)

        val isReplay = client.hashIds().contains(hash)
        val isReplayEnabled = client.isDataReplyEnabled()

        if (isReplayEnabled && isReplay) {
            Timber.d("Replaying request $method $url")
            return client.createReplayRequest(hash)
        }

        return request
    }

    private fun log(response: Response): Response {
        if (client.isDataSyncEnabled()) client.queue(response.toLogData())
        return response
    }

    private fun Response.toLogData(): LogDataModel {

        val req: LogDataModel.Request = request().run {
            val reqHeaders = headers().toJsonObject()
            val reqUrl = url().toString()
            val reqMethod = method()
            val reqBodyStr = body()?.copyBody()
            val reqBody: JsonElement = if (reqBodyStr != null) gson.fromJson(
                reqBodyStr,
                JsonElement::class.java
            ) else JsonObject()

            LogDataModel.Request(
                headers = reqHeaders,
                url = reqUrl,
                method = reqMethod,
                body = reqBody
            )
        }

        val resHeaders = headers().toJsonObject()
        val resCode = code().toString()
        val resBodyStr = body()?.copyBody()
        val resBody = if (resBodyStr != null) gson.fromJson(
            resBodyStr,
            JsonElement::class.java
        ) else JsonObject()

        val res = LogDataModel.Response(
            headers = resHeaders,
            code = resCode,
            body = resBody
        )

        return LogDataModel(
            request = req,
            response = res
        )
    }

}

fun Headers.bodyHasUnknownEncoding(): Boolean {
    val contentEncoding = this["Content-Encoding"]
    return (contentEncoding != null && !contentEncoding.equals("identity", ignoreCase = true)
            && !contentEncoding.equals("gzip", ignoreCase = true))
}

internal fun Headers.toJsonObject(): JsonObject {
    val o = JsonObject()
    if (size() > 0) {
        for (i in 0 until size()) {
            val name = name(i)
            val values = values(name).joinToString(separator = "; ")
            o.add(name, JsonPrimitive(values))
        }
    }
    return o
}

internal fun RequestBody.copyBody(): String? {

    val buffer = Buffer().also { writeTo(it) }
    val charset = contentType()?.charset(UTF8) ?: UTF8

    if (buffer.isPlainText() && contentLength() != 0L) {
        return buffer.clone().readString(charset)
    }

    return null
}

internal fun ResponseBody.copyBody(): String? {
    val source: BufferedSource = source().apply {
        request(Long.MAX_VALUE)
    }
    val buffer = source.buffer
    val charset = contentType()?.charset(UTF8) ?: UTF8

    if (buffer.isPlainText() && contentLength() != 0L) {
        return buffer.clone().readString(charset)
    }

    return null
}

internal fun Buffer.isPlainText(): Boolean {
    return try {
        val prefix = Buffer()
        val byteCount = if (size() < 64) size() else 64
        copyTo(prefix, 0, byteCount)
        for (i in 0..15) {
            if (prefix.exhausted()) {
                break
            }
            val codePoint = prefix.readUtf8CodePoint()
            if (Character.isISOControl(codePoint) && !Character.isWhitespace(
                    codePoint
                )
            ) {
                return false
            }
        }
        true
    } catch (e: EOFException) {
        false // Truncated UTF-8 sequence.
    }
}


