package com.manylogs.android

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.manylogs.logging.ManyLogsClient
import com.manylogs.logging.ManyLogsInterceptor
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val API_URL = "https://jsonplaceholder.typicode.com"
    private val pathPosts = "$API_URL/posts"
    private val pathUsers = "$API_URL/users"

    private lateinit var client: OkHttpClient
    private lateinit var manyLogsClient: ManyLogsClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initClients()

        findViewById<Button>(R.id.btnPosts)?.setOnClickListener { getPosts() }
        findViewById<Button>(R.id.btnUsers)?.setOnClickListener { getUsers() }
    }

    private fun initClients() {
        manyLogsClient = ManyLogsClient(
            context = this,
            host = getString(R.string.host),
            apiKey = getString(R.string.api_key)
        )
        client = createClient()
    }

    private fun createClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        val manyLogsInterceptor = ManyLogsInterceptor(client = manyLogsClient)
        logging.level = HttpLoggingInterceptor.Level.BODY
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(logging)
        builder.addInterceptor(manyLogsInterceptor)
        builder.connectTimeout(30, TimeUnit.SECONDS)
        builder.readTimeout(30, TimeUnit.SECONDS)
        builder.writeTimeout(30, TimeUnit.SECONDS)
        return builder.build()
    }

    private fun getPosts() {
        val request: Request = Request.Builder()
            .url(pathPosts)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
            }

            override fun onResponse(call: Call, response: Response) {
            }
        })
    }
    private fun getUsers() {
        val request: Request = Request.Builder()
            .url(pathUsers)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
            }

            override fun onResponse(call: Call, response: Response) {
            }
        })
    }
}