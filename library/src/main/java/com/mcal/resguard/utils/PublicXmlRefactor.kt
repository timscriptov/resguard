package com.mcal.resguard.utils

import com.reandroid.apk.ApkModule
import com.reandroid.identifiers.TableIdentifier
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.IOException

class PublicXmlRefactor(
    private val apkModule: ApkModule,
    private val pubXmlFile: File
) {
    @Throws(IOException::class)
    fun refactor() {
        println("Loading: $pubXmlFile")
        val tableIdentifier = TableIdentifier()
        try {
            tableIdentifier.loadPublicXml(pubXmlFile)
        } catch (ex: XmlPullParserException) {
            throw IOException(ex)
        }
        println("Applying from public xml ...")
        tableIdentifier.setTableBlock(apkModule.tableBlock)
        val count = tableIdentifier.renameSpecs()
        if (count == 0) {
            println("Nothing renamed !")
        }
        println("Renamed: $count")
        apkModule.tableBlock.removeUnusedSpecs()
    }
}