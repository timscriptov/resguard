package com.mcal.resguard.utils

import com.reandroid.apk.AndroidFrameworks
import com.reandroid.apk.ApkModule
import com.reandroid.arsc.chunk.PackageBlock
import com.reandroid.arsc.chunk.TableBlock
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.chunk.xml.ResXmlAttribute
import com.reandroid.arsc.chunk.xml.ResXmlDocument
import com.reandroid.arsc.chunk.xml.ResXmlElement
import com.reandroid.arsc.item.TypeString
import com.reandroid.arsc.model.ResourceEntry
import com.reandroid.arsc.value.AttributeDataFormat
import com.reandroid.arsc.value.Entry
import com.reandroid.arsc.value.ResTableMapEntry
import com.reandroid.arsc.value.ValueType
import com.reandroid.arsc.value.array.ArrayBag
import com.reandroid.arsc.value.attribute.AttributeBag
import com.reandroid.arsc.value.plurals.PluralsBag
import java.io.IOException

class TypeNameRefactor(
    private val apkModule: ApkModule
) {
    private var mTypeStrings: HashMap<Int, TypeString> = HashMap()
    private val refactoredTypeMap = TypeNameMap()

    @Throws(IOException::class)
    fun refactor() {
        println("Refactoring types ...")
        loadTypeStrings(apkModule.tableBlock)
        println("Refactoring from AndroidManifest ...")
        val manifestBlock = apkModule.androidManifestBlock
        scanXml(manifestBlock, 0)
        scanResFiles()
        if (!isFinished) {
            scanTableEntries(apkModule.tableBlock)
        }
        logFinished()
    }

    private fun logFinished() {
        val log = StringBuilder()
        log.append("Finished type rename=")
        log.append(refactoredTypeMap.count())
        if (refactoredTypeMap.count() > 0) {
            log.append("\n")
            val element = refactoredTypeMap.toXMLDocument().documentElement
            element.name = "renamed"
            element.setAttribute("count", refactoredTypeMap.count().toString())
            log.append(element.toText(true, false))
        }
        val remain = TypeNameMap()
        for ((key, value) in mTypeStrings) {
            remain.add(key, value.get())
        }
        if (remain.count() > 0) {
            log.append("\n")
            val xmlDocument = remain.toXMLDocument()
            val element = xmlDocument.documentElement
            element.name = "remain"
            log.append(xmlDocument.toText(true, false))
        }
        println(log.toString())
    }

    private fun scanTableEntries(tableBlock: TableBlock) {
        println("Refactoring from TableBlock ...")
        for (packageBlock in tableBlock.listPackages()) {
            if (isFinished) {
                break
            }
            scanPackageEntries(packageBlock)
        }
    }

    private fun scanPackageEntries(packageBlock: PackageBlock) {
        val itr = packageBlock.resources
        while (itr.hasNext() && !isFinished) {
            val resourceEntry = itr.next()
            checkEntryGroup(resourceEntry)
        }
    }

    private fun checkEntryGroup(resourceEntry: ResourceEntry) {
        val resourceId = resourceEntry.resourceId
        if (hasRefactoredId(resourceId)) {
            return
        }
        val renameOk = checkBag(resourceEntry)
        if (renameOk) {
            return
        }
    }

    private fun checkBag(resourceEntry: ResourceEntry): Boolean {
        if (!hasRefactoredName("style") || !hasRefactoredName("attr")) {
            return false
        }
        var hasBagEntry = false
        val itr: Iterator<Entry> = resourceEntry.iterator(true)
        while (itr.hasNext()) {
            val entryBlock = itr.next()
            if (!entryBlock.isComplex) {
                return false
            }
            hasBagEntry = true
            val resValueBag = entryBlock.tableEntry as ResTableMapEntry
            if (checkPlurals(resValueBag)) {
                return true
            }
            if (checkArray(resValueBag)) {
                return true
            }
        }
        return hasBagEntry
    }

    private fun checkArray(resValueBag: ResTableMapEntry): Boolean {
        val name = "array"
        if (hasRefactoredName(name)) {
            return false
        }
        if (resValueBag.value.childesCount() < 2) {
            return false
        }
        if (!ArrayBag.isArray(resValueBag.parentEntry)) {
            return false
        }
        val resourceId = resValueBag.parentEntry.resourceId
        rename(resourceId, name)
        return true
    }

    private fun checkPlurals(resValueBag: ResTableMapEntry): Boolean {
        val name = "plurals"
        if (hasRefactoredName(name)) {
            return false
        }
        if (resValueBag.value.childesCount() < 2) {
            return false
        }
        if (!PluralsBag.isPlurals(resValueBag.parentEntry)) {
            return false
        }
        val resourceId = resValueBag.parentEntry.resourceId
        rename(resourceId, name)
        return true
    }

    @Throws(IOException::class)
    private fun scanResFiles() {
        println("Refactoring from resource files ...")
        for (resFile in apkModule.listResFiles()) {
            if (isFinished) {
                break
            }
            if (resFile.isBinaryXml) {
                val resXmlDocument = ResXmlDocument()
                resXmlDocument.readBytes(resFile.inputSource.openStream())
                scanXml(resXmlDocument, resFile.pickOne().resourceId)
            }
        }
    }

    private fun loadTypeStrings(tableBlock: TableBlock) {
        mTypeStrings = HashMap()
        for (packageBlock in tableBlock.listPackages()) {
            val pkgId = packageBlock.id
            for (typeString in packageBlock.typeStringPool.listStrings()) {
                val pkgTypeId = pkgId shl 24 or (0xff and typeString.id shl 16)
                mTypeStrings[pkgTypeId] = typeString
            }
        }
    }

    private fun scanXml(xmlBlock: ResXmlDocument, resourceId: Int) {
        val isManifest = xmlBlock is AndroidManifestBlock
        if (!isManifest && resourceId != 0 && !hasRefactoredId(resourceId)) {
            var renameOk = checkLayout(xmlBlock, resourceId)
            if (renameOk) {
                return
            }
            renameOk = checkDrawable(xmlBlock, resourceId)
            if (renameOk) {
                return
            }
            renameOk = checkAnimator(xmlBlock, resourceId)
            if (renameOk) {
                return
            }
            renameOk = checkMenu(xmlBlock, resourceId)
            if (renameOk) {
                return
            }
            renameOk = checkXml(xmlBlock, resourceId)
            if (renameOk) {
                return
            }
            renameOk = checkAnim(xmlBlock, resourceId)
            if (renameOk) {
                return
            }
            renameOk = checkInterpolator(xmlBlock, resourceId)
            if (renameOk) {
                return
            }
        }
        val attributeList = listAttributes(xmlBlock.resXmlElement)
        for (attribute in attributeList) {
            scanAttribute(attribute, isManifest)
        }
    }

    private fun scanAttribute(attribute: ResXmlAttribute, isManifest: Boolean) {
        var renameOk: Boolean
        if (isManifest) {
            renameOk = checkString(attribute)
            if (!renameOk) {
                renameOk = checkStyle(attribute)
            }
            return
        }
        renameOk = checkAttr(attribute)
        if (hasRefactoredId(attribute.data)) {
            return
        }
        if (!renameOk) {
            renameOk = checkId(attribute)
        }
        if (!renameOk) {
            renameOk = checkDimen(attribute)
        }
        if (!renameOk) {
            renameOk = checkInteger(attribute)
        }
        if (!renameOk) {
            renameOk = checkColor(attribute)
        }
        if (!renameOk) {
            renameOk = checkBool(attribute)
        }
    }

    private fun checkInterpolator(resXmlDocument: ResXmlDocument, resourceId: Int): Boolean {
        val name = "interpolator"
        if (hasRefactoredName(name)) {
            return false
        }
        val root = resXmlDocument.resXmlElement ?: return false
        val tag = root.name
        return if ("pathInterpolator" != tag && "linearInterpolator" != tag) {
            false
        } else {
            rename(resourceId, name)
        }
    }

    private fun checkAnim(resXmlDocument: ResXmlDocument, resourceId: Int): Boolean {
        val name = "anim"
        if (hasRefactoredName(name)) {
            return false
        }
        if (!hasRefactoredName("animator")) {
            return false
        }
        val root = resXmlDocument.resXmlElement ?: return false
        if ("alpha" != root.name) {
            return false
        }
        val fromAlpha = 0x010101ca
        return if (root.searchAttributeByResourceId(fromAlpha) == null) {
            false
        } else {
            rename(resourceId, name)
        }
    }

    private fun checkXml(resXmlDocument: ResXmlDocument, resourceId: Int): Boolean {
        val name = "xml"
        if (hasRefactoredName(name)) {
            return false
        }
        val root = resXmlDocument.resXmlElement ?: return false
        return if (!isXml(root)) {
            false
        } else {
            rename(resourceId, name)
        }
    }

    private fun checkMenu(resXmlDocument: ResXmlDocument, resourceId: Int): Boolean {
        val name = "menu"
        if (hasRefactoredName(name)) {
            return false
        }
        val root = resXmlDocument.resXmlElement ?: return false
        if ("menu" != root.name) {
            return false
        }
        return if (root.listElements("item").size == 0) {
            false
        } else {
            rename(resourceId, name)
        }
    }

    private fun checkAnimator(resXmlDocument: ResXmlDocument, resourceId: Int): Boolean {
        val name = "animator"
        if (hasRefactoredName(name)) {
            return false
        }
        val root = resXmlDocument.resXmlElement ?: return false
        if ("selector" != root.name) {
            return false
        }
        val stateEnabled = 0x0101009e
        var hasObjectAnimator = false
        for (itemElement in root.listElements("item")) {
            if (itemElement.searchAttributeByResourceId(stateEnabled) == null) {
                continue
            }
            hasObjectAnimator = itemElement.listElements("objectAnimator").size > 0
            if (hasObjectAnimator) {
                break
            }
        }
        return if (!hasObjectAnimator) {
            false
        } else {
            rename(resourceId, name)
        }
    }

    private fun checkDrawable(resXmlDocument: ResXmlDocument, resourceId: Int): Boolean {
        val name = "drawable"
        if (hasRefactoredName(name)) {
            return false
        }
        val root = resXmlDocument.resXmlElement ?: return false
        if ("vector" != root.name) {
            return false
        }
        val pathData = 0x01010405
        var hasPathData = false
        for (element in root.listElements("path")) {
            if (element.searchAttributeByResourceId(pathData) != null) {
                hasPathData = true
            }
        }
        return if (!hasPathData) {
            false
        } else {
            rename(resourceId, name)
        }
    }

    private fun checkLayout(resXmlDocument: ResXmlDocument, resourceId: Int): Boolean {
        val name = "layout"
        if (hasRefactoredName(name)) {
            return false
        }
        val root = resXmlDocument.resXmlElement ?: return false
        return if ("LinearLayout" != root.name) {
            false
        } else {
            rename(resourceId, name)
        }
    }

    private fun checkAttr(attribute: ResXmlAttribute): Boolean {
        val name = "attr"
        return if (hasRefactoredName(name)) {
            false
        } else {
            rename(attribute.nameResourceID, name)
        }
    }

    private fun checkColor(attribute: ResXmlAttribute): Boolean {
        val name = "color"
        if (hasRefactoredName(name)) {
            return false
        }
        if (!hasRefactoredName("drawable")) {
            return false
        }
        val textColor = 0x01010098
        val nameId = attribute.nameResourceID
        if (nameId != textColor) {
            return false
        }
        if (attribute.valueType != ValueType.REFERENCE) {
            return true
        }
        rename(attribute.data, name)
        return true
    }

    private fun checkBool(attribute: ResXmlAttribute): Boolean {
        return checkWithAndroidAttribute(
            "bool",
            attribute, AttributeDataFormat.BOOL
        )
    }

    private fun checkInteger(attribute: ResXmlAttribute): Boolean {
        return checkWithAndroidAttribute(
            "integer",
            attribute, AttributeDataFormat.INTEGER
        )
    }

    private fun checkWithAndroidAttribute(
        name: String,
        attribute: ResXmlAttribute,
        attributeValueType: AttributeDataFormat
    ): Boolean {
        if (hasRefactoredName(name)) {
            return false
        }
        val nameId = attribute.nameResourceID
        if (nameId == 0) {
            return false
        }
        if (attribute.valueType != ValueType.REFERENCE) {
            return true
        }
        if (!isEqualAndroidAttributeType(nameId, attributeValueType)) {
            return false
        }
        rename(attribute.data, name)
        return true
    }

    private fun checkDimen(attribute: ResXmlAttribute): Boolean {
        val name = "dimen"
        if (hasRefactoredName(name)) {
            return false
        }
        val layoutWidth = 0x010100f4
        val layoutHeight = 0x010100f5
        val nameId = attribute.nameResourceID
        if (nameId != layoutWidth && nameId != layoutHeight) {
            return false
        }
        if (attribute.valueType != ValueType.REFERENCE) {
            return true
        }
        rename(attribute.data, name)
        return true
    }

    private fun checkId(attribute: ResXmlAttribute): Boolean {
        val name = "id"
        if (hasRefactoredName(name)) {
            return false
        }
        if (attribute.nameResourceID != AndroidManifestBlock.ID_id) {
            return false
        }
        if (attribute.valueType != ValueType.REFERENCE) {
            return true
        }
        rename(attribute.data, name)
        return true
    }

    private fun checkStyle(attribute: ResXmlAttribute): Boolean {
        val name = "style"
        if (hasRefactoredName(name)) {
            return false
        }
        if (attribute.nameResourceID != AndroidManifestBlock.ID_theme) {
            return false
        }
        if (attribute.valueType != ValueType.REFERENCE) {
            return true
        }
        rename(attribute.data, name)
        return true
    }

    private fun checkString(attribute: ResXmlAttribute): Boolean {
        val name = "string"
        if (hasRefactoredName(name)) {
            return false
        }
        if (attribute.nameResourceID != AndroidManifestBlock.ID_label) {
            return false
        }
        if (attribute.valueType != ValueType.REFERENCE) {
            return true
        }
        rename(attribute.data, name)
        return true
    }

    private fun isXml(root: ResXmlElement): Boolean {
        if (isPaths(root)) {
            return true
        }
        return isPreferenceScreen(root)
    }

    private fun isPreferenceScreen(root: ResXmlElement): Boolean {
        if ("PreferenceScreen" != root.name) {
            return false
        }
        for (element in root.listElements()) {
            val tag = element.name
            if ("PreferenceCategory" == tag) {
                return true
            }
            if ("CheckBoxPreference" == tag) {
                return true
            }
        }
        return false
    }

    private fun isPaths(root: ResXmlElement): Boolean {
        if ("paths" != root.name) {
            return false
        }
        for (element in root.listElements()) {
            val tag = element.name
            if ("files-path" == tag || "cache-path" == tag) {
                return true
            }
            if ("external-path" == tag || "root-path" == tag) {
                return true
            }
            if ("external-files-path" == tag || "external-cache-path" == tag) {
                return true
            }
        }
        return false
    }

    private fun rename(resourceId: Int, name: String): Boolean {
        val typeString = getTypeString(resourceId) ?: return false
        removeTypeString(resourceId)
        addRefactored(resourceId, name)
        if (name == typeString.get()) {
            return true
        }
        println("Renamed: '" + typeString.get() + "' --> '" + name + "'")
        typeString.set(name)
        return true
    }

    private fun isEqualAndroidAttributeType(
        attributeResourceId: Int,
        attributeValueType: AttributeDataFormat
    ): Boolean {
        val frameworkApk = AndroidFrameworks.getCurrent() ?: return false
        val frameworkTable = frameworkApk.tableBlock ?: return false
        val resourceEntry = frameworkTable.getResource(attributeResourceId) ?: return false
        val entryBlock = resourceEntry.get()
        if (entryBlock == null || !entryBlock.isComplex) {
            return false
        }
        val attributeBag =
            AttributeBag.create((entryBlock.tableEntry as ResTableMapEntry).value) ?: return false
        return if (attributeBag.isFlag || attributeBag.isEnum) {
            false
        } else {
            attributeBag.format
                .isEqualType(attributeValueType)
        }
    }

    private fun addRefactored(id: Int, name: String) {
        refactoredTypeMap.add(id, name)
    }

    private val isFinished: Boolean
        get() = mTypeStrings.size == 0

    private fun hasRefactoredName(name: String): Boolean {
        return refactoredTypeMap.contains(name)
    }

    private fun hasRefactoredId(resourceId: Int): Boolean {
        return refactoredTypeMap.contains(resourceId)
    }

    private fun getTypeString(resourceId: Int): TypeString? {
        return mTypeStrings[resourceId and -0x10000]
    }

    private fun removeTypeString(resourceId: Int) {
        mTypeStrings.remove(resourceId and -0x10000)
    }

    private fun listAttributes(element: ResXmlElement?): List<ResXmlAttribute> {
        if (element == null) {
            return ArrayList()
        }
        val results: MutableList<ResXmlAttribute> = ArrayList(element.listAttributes())
        for (child in element.listElements()) {
            results.addAll(listAttributes(child))
        }
        return results
    }
}
