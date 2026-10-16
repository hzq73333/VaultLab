package com.vaultlab.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ApiException(val code: Int, val body: String) : Exception("HTTP $code")

object Net {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
}

/** Runs the request off the main thread and returns the body, or throws [ApiException] on non-2xx. */
suspend fun OkHttpClient.executeForBody(request: Request): String = withContext(Dispatchers.IO) {
    newCall(request).execute().use { response ->
        val text = response.body?.string().orEmpty()
        if (!response.isSuccessful) throw ApiException(response.code, text)
        text
    }
}
