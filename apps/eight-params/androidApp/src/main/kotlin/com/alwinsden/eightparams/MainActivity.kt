package com.alwinsden.eightparams

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alwinsden.eightparams.health.HealthApp
import com.alwinsden.eightparams.health.HealthDbHolder
import com.alwinsden.eightparams.health.HealthTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val db = HealthDbHolder.get(applicationContext)

        setContent {
            HealthTheme {
                HealthApp(db = db)
            }
        }
    }
}
