package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.example.engine.fs.LocalFileManager
import com.example.engine.omniroot.service.OmniRootProxyService
import com.example.ui.OmniRootApp
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.LogKeeper
import com.example.utils.VoiceManager

class MainActivity : ComponentActivity() {

    companion object {
        val currentIntentData = mutableStateOf<Uri?>(null)
        val currentIntentAction = mutableStateOf<String?>(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalFileManager.init(applicationContext)
        LogKeeper.init(applicationContext)
        VoiceManager.init(applicationContext)
        com.example.engine.EngineRegistry.registerTool(com.example.engine.tools.KnowledgeBitsTool(applicationContext))
        
        handleIncomingIntent(intent)
        
        val serviceIntent = Intent(this, OmniRootProxyService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                OmniRootApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val uri = intent.data
        val action = intent.getStringExtra("action") ?: intent.action
        currentIntentData.value = uri
        currentIntentAction.value = action
        LogKeeper.log("MainActivity", "IntentReceived", "URI: $uri, Action: $action")
    }
}
