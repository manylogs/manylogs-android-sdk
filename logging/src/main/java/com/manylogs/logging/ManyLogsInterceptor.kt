package com.manylogs.logging

import okhttp3.Interceptor
import okhttp3.Response

class ManyLogsInterceptor(val logger: ManyLogs) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        val request = logger.parseRequest(chain.request())

        val response = logger.log(chain.proceed(request))

        return response
    }
}