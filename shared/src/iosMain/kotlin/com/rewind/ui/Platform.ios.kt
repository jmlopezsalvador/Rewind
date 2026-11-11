@file:OptIn(ExperimentalForeignApi::class)

package com.rewind.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject
import platform.posix.memcpy

@Composable
actual fun SystemBarsEffect(dark: Boolean) {
    // Barra de estado e indicador de inicio: los oculta SwiftUI (ver ContentView.swift). Aquí: la pantalla no se apaga sola.
    DisposableEffect(Unit) {
        UIApplication.sharedApplication.idleTimerDisabled = true
        onDispose { UIApplication.sharedApplication.idleTimerDisabled = false }
    }
}

class UserDefaultsStore : KeyValueStore {
    private val d = NSUserDefaults.standardUserDefaults
    override fun getString(key: String): String? = d.stringForKey(key)
    override fun getBoolean(key: String, default: Boolean) =
        if (d.objectForKey(key) == null) default else d.boolForKey(key)
    override fun getLong(key: String, default: Long) =
        if (d.objectForKey(key) == null) default else d.integerForKey(key)
    override fun putString(key: String, value: String) = d.setObject(value, key)
    override fun putBoolean(key: String, value: Boolean) = d.setBool(value, key)
    override fun putLong(key: String, value: Long) = d.setInteger(value, key)
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val out = ByteArray(length.toInt())
    if (out.isNotEmpty()) out.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return out
}

class FileImageStore : ImageStore {
    private val dir: String =
        NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
    private fun path(name: String) = "$dir/$name.img"
    override fun save(name: String, bytes: ByteArray) { bytes.toNSData().writeToFile(path(name), true) }
    override fun load(name: String): ByteArray? = NSData.dataWithContentsOfFile(path(name))?.toByteArray()
    override fun delete(name: String) { NSFileManager.defaultManager.removeItemAtPath(path(name), null) }
}

actual fun decodeImage(bytes: ByteArray): ImageBitmap? =
    runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()

private class PickerDelegate(private val onPicked: (ByteArray) -> Unit) :
    NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        val data = image?.let { UIImagePNGRepresentation(it) }
        picker.dismissViewControllerAnimated(true, null)
        data?.let { onPicked(it.toByteArray()) }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
    }
}

@Composable
actual fun rememberImagePicker(onPicked: (ByteArray) -> Unit): () -> Unit {
    val current = rememberUpdatedState(onPicked)
    val delegate = remember { PickerDelegate { current.value(it) } } // se retiene: el picker solo guarda una referencia débil
    return remember {
        {
            val picker = UIImagePickerController()
            picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
            picker.delegate = delegate
            UIApplication.sharedApplication.keyWindow?.rootViewController
                ?.presentViewController(picker, true, null)
        }
    }
}

actual fun platformLanguage(): String = NSLocale.currentLocale.languageCode

@Composable
actual fun rememberVibrate(): () -> Unit = remember {
    {
        UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium).impactOccurred()
    }
}

@Composable
actual fun ProximityEffect(enabled: Boolean, onNear: () -> Unit, onStatus: (ProximityStatus) -> Unit) {
    val current = rememberUpdatedState(onNear)
    val status = rememberUpdatedState(onStatus)
    // Se apaga la monitorización al salir (mientras está tapado, iOS apaga la pantalla).
    DisposableEffect(enabled) {
        onDispose { if (enabled) UIDevice.currentDevice.proximityMonitoringEnabled = false }
    }
    // En vez de fiarse de la notificación del sistema (que podía no llegar si se activaba antes de que la app estuviera
    // activa), se lee el estado del sensor unas 10 veces por segundo y se reafirma la monitorización.
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        val device = UIDevice.currentDevice
        var sawFar = false
        var last: Boolean? = null
        while (true) {
            if (!device.proximityMonitoringEnabled) device.proximityMonitoringEnabled = true
            if (!device.proximityMonitoringEnabled) {
                status.value(ProximityStatus.UNAVAILABLE) // el sistema la rechaza: este iPhone/iPad no tiene sensor
            } else {
                val near = device.proximityState
                status.value(if (near) ProximityStatus.NEAR else ProximityStatus.FAR)
                if (near != last) {
                    if (!near) sawFar = true else if (sawFar) { sawFar = false; current.value() }
                    last = near
                }
            }
            delay(100)
        }
    }
}

// En iOS no hay forma oficial de interceptar las teclas de volumen: la opción se descarta en esta plataforma.
actual fun platformSupportsVolumeKeys(): Boolean = false

@Composable
actual fun VolumeKeyEffect(enabled: Boolean, onPress: () -> Unit) {
    // sin soporte
}
