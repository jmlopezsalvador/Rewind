package com.rewind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rewind.ui.FileImageStore
import com.rewind.ui.RewindRoot
import com.rewind.ui.SettingsStore
import com.rewind.ui.SharedPrefsStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val store = SettingsStore(SharedPrefsStore(applicationContext))
        setContent { RewindRoot(store, FileImageStore(filesDir)) }
    }
}
