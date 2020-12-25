package com.manylogs.logging

import android.annotation.SuppressLint

object Encodings {
    @SuppressLint("DefaultLocale")
    fun encodeRequest(method: String, url: String) = "$url$method".toLowerCase().crc32()
}