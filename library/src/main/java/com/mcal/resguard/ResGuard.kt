package com.mcal.resguard

import com.mcal.resguard.utils.AutoRefactor
import com.mcal.resguard.utils.PublicXmlRefactor
import com.mcal.resguard.utils.StringValueNameGenerator
import com.mcal.resguard.utils.TypeNameRefactor
import com.reandroid.apk.ApkModule
import java.io.File
import java.io.IOException

class ResGuard(
    private val inputApkFile: File,
    private val outputApkFile: File,
    private val fixTypeNames: Boolean = false,
    private val publicXml: File? = null
) {
    fun confuse() {
        val module = ApkModule.loadApkFile(inputApkFile)
        module.setPreferredFramework(33)
        if (!module.hasTableBlock()) {
            throw IOException("Don't have resources.arsc")
        }
        if (fixTypeNames) {
            TypeNameRefactor(module).refactor()
        }
        AutoRefactor(module).refactor()
        StringValueNameGenerator(module.tableBlock).refactor()
        if (publicXml != null) {
            PublicXmlRefactor(module, publicXml).refactor()
        }
        module.writeApk(outputApkFile, null)
    }
}