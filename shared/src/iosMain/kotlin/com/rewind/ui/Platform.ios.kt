package com.rewind.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
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
    // Lo gestiona SwiftUI (.preferredColorScheme / statusBarHidden).
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
