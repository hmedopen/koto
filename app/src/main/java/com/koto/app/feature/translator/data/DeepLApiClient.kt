package com.koto.app.feature.translator.data

import com.koto.app.feature.translator.model.TranslationLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

open class DeepLApiClient(
    private val endpointUrl: String = "https://api-free.deepl.com/v2/translate",
    private val connectTimeoutMs: Int = 4000,
    private val readTimeoutMs: Int = 4000,
) {

    open suspend fun translate(
        text: String,
        sourceLanguage: TranslationLanguage,
        targetLanguage: TranslationLanguage,
        apiKey: String,
    ): String? = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val cleanText = text.trim()
        if (cleanKey.isBlank() || cleanText.isBlank()) {
            return@withContext null
        }

        val targetLangCode = when (targetLanguage) {
            TranslationLanguage.Japanese -> "JA"
            TranslationLanguage.English -> "EN-US"
        }

        val sourceLangCode = when (sourceLanguage) {
            TranslationLanguage.Japanese -> "JA"
            TranslationLanguage.English -> "EN"
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(endpointUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                setRequestProperty("Authorization", "DeepL-Auth-Key $cleanKey")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("User-Agent", "KotoApp/0.1.0")
            }

            val requestJson = JSONObject().apply {
                val textArray = JSONArray().apply { put(cleanText) }
                put("text", textArray)
                put("target_lang", targetLangCode)
                put("source_lang", sourceLangCode)
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val rootJson = JSONObject(responseText)
                val translations = rootJson.optJSONArray("translations")
                if (translations != null && translations.length() > 0) {
                    val firstTranslation = translations.getJSONObject(0)
                    return@withContext firstTranslation.optString("text").takeIf { it.isNotBlank() }
                }
                null
            } else {
                // Quota exceeded (456), auth failed (403), rate limited (429), or server errors
                // Fail silently so callers seamlessly fall back to on-device ML Kit
                null
            }
        } catch (_: Throwable) {
            // Network loss, timeouts, DNS failures silently route to fallback
            null
        } finally {
            connection?.disconnect()
        }
    }
}
