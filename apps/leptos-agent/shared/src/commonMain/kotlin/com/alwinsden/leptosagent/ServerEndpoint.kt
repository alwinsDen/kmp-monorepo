package com.alwinsden.leptosagent

/**
 * Base URL of the leptos-agent Go server, injected at build time from the
 * repo-root .env (SERVER_ENDPOINT) via the generateEnvConfig Gradle task.
 */
internal expect fun serverEndpoint(): String

class kmp_serverEndpoint() {
    fun currentServerUrl(): String = serverEndpoint()
}