package com.alwinsden.eightparams

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
