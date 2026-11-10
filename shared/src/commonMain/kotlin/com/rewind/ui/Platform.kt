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
