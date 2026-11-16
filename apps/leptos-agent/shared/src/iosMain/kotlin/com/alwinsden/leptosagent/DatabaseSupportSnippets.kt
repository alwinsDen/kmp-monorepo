package com.alwinsden.leptosagent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/*
* This the glue code for iosMain to support read functiosn and other stuff for
* KMP room database.
* */

interface Disposable {
    fun dispose()
}

object UserPrefsRead {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    fun observeAll(onUpdate: (List<ModelPreferencesEntity>) -> Unit) : Disposable {
        val job : Job = scope.launch {
            DatabaseProvider
                .database
                .getDao()
                .getAllAsFlow()
                .collect { rows  -> onUpdate(rows) }
        }
        return object : Disposable {
            override fun dispose() {
                job.cancel()
            }
        }
    }
}