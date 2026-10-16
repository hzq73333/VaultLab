package com.vaultlab.app.data

import kotlinx.coroutines.delay
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * VirusTotal API v3. The user's own key is passed in for every call; it is never bundled in the app.
 * Free keys allow about 4 requests per minute, so polling is spaced out.
 */
object VirusTotalApi {
    private const val BASE = "https://www.virustotal.com/api/v3"
    const val MAX_FILE_BYTES = 32L * 1024 * 1024

    data class Stats(val malicious: Int, val suspicious: Int, val harmless: Int, val undetected: Int)

    suspend fun scanUrl(apiKey: String, url: String): Stats {
        val form = FormBody.Builder().add("url", url).build()
        val request = Request.Builder()
            .url("$BASE/urls")
            .header("x-apikey", apiKey)
            .post(form)
            .build()
        return awaitAnalysis(apiKey, submittedId(request))
    }

    suspend fun scanFile(apiKey: String, fileName: String, bytes: ByteArray): Stats {
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                fileName,
                bytes.toRequestBody("application/octet-stream".toMediaType()),
            )
            .build()
        val request = Request.Builder()
            .url("$BASE/files")
            .header("x-apikey", apiKey)
            .post(multipart)
            .build()
        return awaitAnalysis(apiKey, submittedId(request))
    }

    private suspend fun submittedId(request: Request): String =
        JSONObject(Net.client.executeForBody(request)).getJSONObject("data").getString("id")

    private suspend fun awaitAnalysis(apiKey: String, id: String): Stats {
        repeat(12) {
            delay(15_000)
            val request = Request.Builder()
                .url("$BASE/analyses/$id")
                .header("x-apikey", apiKey)
                .build()
            val attributes = JSONObject(Net.client.executeForBody(request))
                .getJSONObject("data")
                .getJSONObject("attributes")
            if (attributes.optString("status") == "completed") {
                val s = attributes.getJSONObject("stats")
                return Stats(
                    malicious = s.optInt("malicious"),
                    suspicious = s.optInt("suspicious"),
                    harmless = s.optInt("harmless"),
                    undetected = s.optInt("undetected"),
                )
            }
        }
        throw ApiException(408, "analysis timed out")
    }
}
