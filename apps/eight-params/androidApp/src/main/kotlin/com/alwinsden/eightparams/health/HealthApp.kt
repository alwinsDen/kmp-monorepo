package com.alwinsden.eightparams.health

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Home : Screen
    data class Detail(val type: MetricType) : Screen
}

/**
 * Root of the health tracker: home summary → metric detail, with the
 * Apple Health–style logging sheet available from both.
 */
@Composable
fun HealthApp(db: HealthDatabase) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var loggingMetric by remember { mutableStateOf<MetricType?>(null) }
    val scope = rememberCoroutineScope()

    // One live flow per metric type from the shared Room database.
    val entriesByType: Map<MetricType, List<HealthEntry>> = MetricType.entries.associateWith { type ->
        val flow = remember(type) { db.dao().entriesByType(type.name) }
        flow.collectAsState(initial = emptyList()).value.map { it.toDomain() }
    }

    BackHandler(enabled = loggingMetric != null || screen !is Screen.Home) {
        if (loggingMetric != null) loggingMetric = null else screen = Screen.Home
    }

    HealthTheme {
        Surface(modifier = Modifier.fillMaxSize().background(HealthColors.Background)) {
            when (val current = screen) {
                is Screen.Home -> HomeScreen(
                    entries = entriesByType,
                    onOpen = { screen = Screen.Detail(it) },
                    onAdd = { loggingMetric = it },
                )

                is Screen.Detail -> MetricDetailScreen(
                    type = current.type,
                    entries = entriesByType[current.type].orEmpty(),
                    onBack = { screen = Screen.Home },
                    onAdd = { loggingMetric = current.type },
                    onDelete = { id -> scope.launch { db.dao().delete(id) } },
                )
            }
        }

        loggingMetric?.let { type ->
            AddEntrySheet(
                type = type,
                onDismiss = { loggingMetric = null },
                onSave = { value1, value2, timestampMillis ->
                    scope.launch {
                        db.dao().insert(
                            HealthEntryEntity(
                                type = type.name,
                                value1 = value1,
                                value2 = value2,
                                timestampMillis = timestampMillis,
                            )
                        )
                    }
                    loggingMetric = null
                },
            )
        }
    }
}

/** Convenience accessor for previews/tools that need the DB before composition. */
@Composable
fun rememberHealthDatabase(): HealthDatabase {
    val context = LocalContext.current
    return remember { HealthDbHolder.get(context) }
}
