package com.rewind

import androidx.compose.ui.window.ComposeUIViewController
import com.rewind.ui.RewindRoot
import com.rewind.ui.SettingsStore
import com.rewind.ui.UserDefaultsStore
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    RewindRoot(SettingsStore(UserDefaultsStore()))
}
