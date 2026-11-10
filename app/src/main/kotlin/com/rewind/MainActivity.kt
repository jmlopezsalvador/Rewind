package com.rewind

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rewind.ui.FileImageStore
import com.rewind.ui.RewindRoot
import com.rewind.ui.SettingsStore
import com.rewind.ui.SharedPrefsStore
import com.rewind.ui.VolumeKeyBridge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val store = SettingsStore(SharedPrefsStore(applicationContext))
        setContent { RewindRoot(store, FileImageStore(filesDir)) }
    }

    // Con el disparador de volumen activo, las teclas de volumen disparan el efecto y no llegan al sistema.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (isVolumeKey(keyCode)) {
            val handler = VolumeKeyBridge.handler
            if (handler != null) {
                if (event.repeatCount == 0) handler()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (isVolumeKey(keyCode) && VolumeKeyBridge.handler != null) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun isVolumeKey(keyCode: Int) =
        keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
}
