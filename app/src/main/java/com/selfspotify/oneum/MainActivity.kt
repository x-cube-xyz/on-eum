package com.selfspotify.oneum

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.selfspotify.oneum.domain.YouTubeLink
import com.selfspotify.oneum.ui.OneumApp
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val sharedUrl = MutableStateFlow<String?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readShare(intent)
        val model = ViewModelProvider(this)[MainViewModel::class.java]
        setContent { OneumApp(model, sharedUrl) { sharedUrl.value = null } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readShare(intent)
    }

    private fun readShare(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            sharedUrl.value = YouTubeLink.fromSharedText(intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty())?.url
        }
    }
}
