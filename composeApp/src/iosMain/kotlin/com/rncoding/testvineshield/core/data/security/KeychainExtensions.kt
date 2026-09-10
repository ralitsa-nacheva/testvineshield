package com.rncoding.testvineshield.core.data.security

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import platform.CoreFoundation.*
@OptIn(ExperimentalForeignApi::class)
fun Map<Any?, Any>.toCFDictionary(): CFDictionaryRef {
    return CFDictionaryCreate(
        null,
        this.keys.map { it as CFTypeRef? }.toCValues(),
        this.values.map { it as CFTypeRef? }.toCValues(),
        this.size.toLong(),
        null,
        null
    )
}
@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData(): NSData =
    NSData.create(bytes = this.refTo(0), length = size.toULong())
@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    memScoped {
        memcpy(bytes.refTo(0), this@toByteArray.bytes, length)
    }
    return bytes
}
