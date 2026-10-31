package com.alwinsden.leptosagent

import com.alwinsden.leptosagent.shared.ConversationSubmitRequest
import com.alwinsden.leptosagent.shared.ConversationSubmitResponse
import com.alwinsden.leptosagent.shared.HealthResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal expect fun platformHttpClientEngine(): io.ktor.client.engine.HttpClientEngine

class ApiClient(
    private val baseUrl: String
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = HttpClient(platformHttpClientEngine()) {
        install(ContentNegotiation) {
            json(this@ApiClient.json)
        }
    }

    suspend fun getHealth(): HealthResponse =
        client.get("$baseUrl/health").body()

    suspend fun submitConversation(request: ConversationSubmitRequest): ConversationSubmitResponse =
        client.post("$baseUrl/premium/submit") {
            println(request)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun submitConversationStream(
        request: ConversationSubmitRequest,
        onToken: (ConversationSubmitResponse) -> Unit,
    ) {
        client.preparePost("$baseUrl/premium/submit") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Text.EventStream)
            setBody(request)
        }.execute { response ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readLine() ?: break
                val trimmed = line.trim()
                if (!trimmed.startsWith("data")) continue
                val payload = trimmed.removePrefix("data: ").trim()
                try {
                    onToken(json.decodeFromString<ConversationSubmitResponse>(payload))
                } catch (_: SerializationException) {
                    
                }
            }
        }
    }

    fun close() {
        client.close()
    }
}
