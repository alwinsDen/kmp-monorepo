package com.alwinsden.leptosagent

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform