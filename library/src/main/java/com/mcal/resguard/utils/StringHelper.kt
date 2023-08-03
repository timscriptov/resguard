package com.mcal.resguard.utils

import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest


object StringHelper {
    @JvmStatic
    fun getFileExtension(filePath: String): String {
        val file = File(filePath)
        val fileName = file.name
        val dotIndex = fileName.lastIndexOf(".")
        return if (dotIndex != -1) {
            fileName.substring(dotIndex + 1)
        } else {
            ""
        }
    }

    @JvmStatic
    fun md5(str: String): ByteArray =
        MessageDigest.getInstance("MD5").digest(str.toByteArray(StandardCharsets.UTF_8))

    @JvmStatic
    fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }

    @JvmStatic
    fun generateRandomString(len: Int = 15): String {
        val alphanumerics = CharArray(26) { (it + 97).toChar() }.toSet()
            .union(CharArray(9) { (it + 48).toChar() }.toSet())
        return (0 until len).map {
            alphanumerics.toList().random()
        }.joinToString("")
    }
}
