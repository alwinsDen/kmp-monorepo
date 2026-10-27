package com.alwinsden.leptosagent

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun platformHttpClientEngine(): HttpClientEngine = OkHttp.create()
