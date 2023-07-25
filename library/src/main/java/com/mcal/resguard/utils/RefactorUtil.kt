package com.mcal.resguard.utils

object RefactorUtil {
    fun generateUniqueName(type: String, resourceId: Int): String {
        return type + "_" + String.format("0x%08x", resourceId)
    }
}