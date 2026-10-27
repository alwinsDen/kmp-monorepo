package com.alwinsden.leptosagent

import com.alwinsden.leptosagent.shared.ConversationSubmitRequest
import com.alwinsden.leptosagent.shared.ConversationSubmitResponse
import com.alwinsden.leptosagent.shared.HealthResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

internal expect fun platformHttpClientEngine(): io.ktor.client.engine.HttpClientEngine

class ApiClient(
    private val baseUrl: String = "http://localhost:8080",
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

    fun close() {
        client.close()
    }
}
