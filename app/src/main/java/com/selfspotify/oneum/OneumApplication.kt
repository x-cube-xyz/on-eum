package com.selfspotify.oneum

import android.app.Application
import com.selfspotify.oneum.data.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OneumApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val library by lazy { LibraryRepository(this) }
    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { runCatching { library.load() } }
    }
}
