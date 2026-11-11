package com.rewind.ui

import androidx.compose.runtime.Composable

/** Barras del sistema / modo inmersivo según la plataforma. */
@Composable
expect fun SystemBarsEffect(dark: Boolean)

/** Almacenamiento de imágenes (capturas A/B) en el almacenamiento interno de la app. */
interface ImageStore {
    fun save(name: String, bytes: ByteArray)
    fun load(name: String): ByteArray?
    fun delete(name: String)
}

/** Decodifica una imagen (PNG/JPEG...) a un bitmap de Compose; null si no se puede. */
expect fun decodeImage(bytes: ByteArray): androidx.compose.ui.graphics.ImageBitmap?

/** Devuelve una función que abre la galería del sistema; al elegir una foto llama a [onPicked] con sus bytes. */
@Composable
expect fun rememberImagePicker(onPicked: (ByteArray) -> Unit): () -> Unit

/** Código de idioma del sistema ("es", "en"...), para elegir el formato de fecha por defecto. */
expect fun platformLanguage(): String

/** Devuelve una función que da una vibración corta de confirmación (háptica en iOS). */
@Composable
expect fun rememberVibrate(): () -> Unit

/** Lo que informa el sensor de proximidad (para diagnosticar). */
enum class ProximityStatus { UNAVAILABLE, FAR, NEAR }

/**
 * Mientras [enabled], vigila el sensor de proximidad y llama a [onWave] cuando se completa una pasada de mano, es decir,
 * al RETIRAR la mano tras taparlo (ver `ProximityWave`). Mientras está tapado, el sistema apaga la pantalla.
 * El sensor se mantiene activo todo el tiempo que [enabled] es true (no se apaga al disparar ni al reiniciar: en iOS,
 * volver a activarlo dejaba el estado "pegado" en cerca). [onStatus] informa del estado actual del sensor
 * (no disponible / lejos / cerca) para diagnosticar.
 */
@Composable
expect fun ProximityEffect(enabled: Boolean, onWave: () -> Unit, onStatus: (ProximityStatus) -> Unit = {})

/** true si la plataforma permite usar las teclas de volumen como disparador (Android sí; iOS no ofrece forma fiable). */
expect fun platformSupportsVolumeKeys(): Boolean

/**
 * Mientras [enabled], las pulsaciones de las teclas de volumen llaman a [onPress] y no cambian el volumen
 * ni muestran el aviso del sistema. En plataformas sin soporte no hace nada.
 */
@Composable
expect fun VolumeKeyEffect(enabled: Boolean, onPress: () -> Unit)
