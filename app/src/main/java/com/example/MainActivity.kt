package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.engine.fs.LocalFileManager
import com.example.engine.omniroot.service.OmniRootProxyService
import com.example.ui.OmniRootApp
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.LogKeeper

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    LocalFileManager.init(applicationContext)
    LogKeeper.init(applicationContext)
    
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
}
