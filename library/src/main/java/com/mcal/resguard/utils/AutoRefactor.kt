package com.mcal.resguard.utils

import com.mcal.resguard.utils.StringHelper.toHex
import com.reandroid.apk.ApkModule
import com.reandroid.identifiers.TableIdentifier
import java.io.File
import java.io.IOException

class AutoRefactor(
    private val mApkModule: ApkModule
) {
    @Throws(IOException::class)
    fun refactor() {
        refactorResourceNames()
        refactorFilePaths()
    }

    private fun refactorFilePaths(): Int {
        println("Validating file paths ...")
        var renameCount = 0
        val resFileList = mApkModule.listResFiles()
        for (resFile in resFileList) {
            val path =
                StringHelper.md5(
                    (StringHelper.md5(File(resFile.filePath).name) + StringHelper.md5(
                        StringHelper.generateRandomString(32)
                    )).toHex()
                ).toHex()
            resFile.filePath = path
            renameCount++
        }
        return renameCount
    }

    private fun refactorResourceNames() {
        println("Validating resource names ...")
        val tableIdentifier = TableIdentifier()
        val tableBlock = mApkModule.tableBlock
        tableIdentifier.load(tableBlock)
        val msg = tableIdentifier.validateSpecNames()
        if (msg == null) {
            println("All resource names are valid")
            return
        }
        var count = 0
        for (pi in tableIdentifier.packages) {
            for (ti in pi.list()) {
                val entryRefactor = EntryRefactor(ti)
                count += entryRefactor.refactorAll()
            }
        }
        println(msg)
    }
}