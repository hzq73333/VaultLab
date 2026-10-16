package com.vaultlab.app.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

data class TempAccount(val address: String, val password: String, val token: String)

data class TempMessage(
    val id: String,
    val from: String,
    val subject: String,
    val intro: String,
)

/** Mail.tm public API. No API key needed. */
object MailTmApi {
    private const val BASE = "https://api.mail.tm"
    private const val CHARS = "abcdefghijklmnopqrstuvwxyz0123456789"
    private val JSON_TYPE = "application/json".toMediaType()

    suspend fun createAccount(): TempAccount {
        val domain = activeDomain()
        val address = randomString(12) + "@" + domain
        val password = randomString(16)

        val body = JSONObject().put("address", address).put("password", password).toString()
        val request = Request.Builder()
            .url("$BASE/accounts")
            .post(body.toRequestBody(JSON_TYPE))
            .header("Accept", "application/json")
            .build()
        Net.client.executeForBody(request)

        return TempAccount(address, password, login(address, password))
    }

    suspend fun messages(token: String): List<TempMessage> {
        val raw = Net.client.executeForBody(authed("$BASE/messages", token))
        val list = members(raw)
        return (0 until list.length()).map { i ->
            val m = list.getJSONObject(i)
            TempMessage(
                id = m.getString("id"),
                from = m.optJSONObject("from")?.optString("address").orEmpty(),
                subject = m.optString("subject"),
                intro = m.optString("intro"),
            )
        }
    }

    suspend fun messageText(token: String, id: String): String {
        val raw = Net.client.executeForBody(authed("$BASE/messages/$id", token))
        return JSONObject(raw).optString("text")
    }

    private suspend fun login(address: String, password: String): String {
        val body = JSONObject().put("address", address).put("password", password).toString()
        val request = Request.Builder()
            .url("$BASE/token")
            .post(body.toRequestBody(JSON_TYPE))
            .header("Accept", "application/json")
            .build()
        return JSONObject(Net.client.executeForBody(request)).getString("token")
    }

    private suspend fun activeDomain(): String {
        val request = Request.Builder()
            .url("$BASE/domains")
            .header("Accept", "application/json")
            .build()
        val list = members(Net.client.executeForBody(request))
        for (i in 0 until list.length()) {
            val d = list.getJSONObject(i)
            if (d.optBoolean("isActive", true)) return d.getString("domain")
        }
        throw ApiException(503, "no active domain")
    }

    /** Collections come back either as a plain array or as a JSON-LD object with "hydra:member". */
    private fun members(raw: String): JSONArray {
        val trimmed = raw.trimStart()
        return if (trimmed.startsWith("[")) JSONArray(raw)
        else JSONObject(raw).optJSONArray("hydra:member") ?: JSONArray()
    }

    private fun authed(url: String, token: String): Request =
        Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $token")
            .build()

    private fun randomString(length: Int): String =
        buildString { repeat(length) { append(CHARS[Random.nextInt(CHARS.length)]) } }
}
