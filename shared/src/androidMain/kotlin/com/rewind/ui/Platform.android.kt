package com.rewind.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Modo inmersivo total (sin barras; reaparecen con un deslizamiento del borde) y pantalla siempre encendida. */
@Composable
actual fun SystemBarsEffect(dark: Boolean) {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

class SharedPrefsStore(context: Context) : KeyValueStore {
    private val p = context.getSharedPreferences("rewind", Context.MODE_PRIVATE)
    override fun getString(key: String): String? = p.getString(key, null)
    override fun getBoolean(key: String, default: Boolean) = p.getBoolean(key, default)
    override fun getLong(key: String, default: Long) = p.getLong(key, default)
    override fun putString(key: String, value: String) = p.edit().putString(key, value).apply()
    override fun putBoolean(key: String, value: Boolean) = p.edit().putBoolean(key, value).apply()
    override fun putLong(key: String, value: Long) = p.edit().putLong(key, value).apply()
}

class FileImageStore(private val dir: java.io.File) : ImageStore {
    private fun file(name: String) = java.io.File(dir, "$name.img")
    override fun save(name: String, bytes: ByteArray) { file(name).writeBytes(bytes) }
    override fun load(name: String): ByteArray? = file(name).takeIf { it.exists() }?.readBytes()
    override fun delete(name: String) { file(name).delete() }
}

actual fun decodeImage(bytes: ByteArray): androidx.compose.ui.graphics.ImageBitmap? =
    runCatching { android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()

@Composable
actual fun rememberImagePicker(onPicked: (ByteArray) -> Unit): () -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    val current = androidx.compose.runtime.rememberUpdatedState(onPicked)
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { current.value(it.readBytes()) }
        }
    }
    return { launcher.launch("image/*") }
}

actual fun platformLanguage(): String = java.util.Locale.getDefault().language

@Composable
actual fun rememberVibrate(): () -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    return androidx.compose.runtime.remember(context) {
        {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            }
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(40, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}

@Composable
actual fun ProximityEffect(enabled: Boolean, onWave: () -> Unit, onStatus: (ProximityStatus) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val current = androidx.compose.runtime.rememberUpdatedState(onWave)
    val status = androidx.compose.runtime.rememberUpdatedState(onStatus)
    DisposableEffect(enabled) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val sensor = manager.getDefaultSensor(android.hardware.Sensor.TYPE_PROXIMITY)
        if (!enabled) return@DisposableEffect onDispose { }
        if (sensor == null) {
            status.value(ProximityStatus.UNAVAILABLE)
            return@DisposableEffect onDispose { }
        }
        val wave = com.rewind.time.ProximityWave()
        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(event: android.hardware.SensorEvent) {
                val near = event.values[0] < sensor.maximumRange
                status.value(if (near) ProximityStatus.NEAR else ProximityStatus.FAR)
                if (wave.update(near)) current.value()
            }
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
        }
        manager.registerListener(listener, sensor, android.hardware.SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { manager.unregisterListener(listener) }
    }
}

/**
 * Puente entre la actividad (que recibe las teclas) y la interfaz Compose. Mientras haya un [handler] registrado,
 * `MainActivity` le pasa las pulsaciones de volumen y las consume (el sistema no cambia el volumen).
 */
object VolumeKeyBridge {
    @Volatile
    var handler: (() -> Unit)? = null
}

actual fun platformSupportsVolumeKeys(): Boolean = true

@Composable
actual fun VolumeKeyEffect(enabled: Boolean, onPress: () -> Unit) {
    val current = androidx.compose.runtime.rememberUpdatedState(onPress)
    DisposableEffect(enabled) {
        if (enabled) VolumeKeyBridge.handler = { current.value() }
        onDispose { VolumeKeyBridge.handler = null }
    }
}
