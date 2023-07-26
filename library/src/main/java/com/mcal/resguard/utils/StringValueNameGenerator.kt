package com.mcal.resguard.utils

import com.mcal.resguard.utils.RefactorUtil.generateUniqueName
import com.reandroid.arsc.chunk.TableBlock
import com.reandroid.arsc.model.ResourceEntry
import com.reandroid.arsc.value.Entry
import com.reandroid.arsc.value.ResTableEntry
import com.reandroid.arsc.value.ValueType
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.Locale
import java.util.regex.Pattern

class StringValueNameGenerator(
    private val tableBlock: TableBlock
    ) {
    private val mGeneratedNames: MutableSet<String> = HashSet()
    private val mSkipIds: MutableSet<Int> = HashSet()

    fun refactor() {
        val resourceEntryMap = mapResourceEntries()
        val nameMap = generate()
        for ((key, name) in nameMap) {
            resourceEntryMap[key]?.name = name
        }
    }

    private fun isGenerated(resourceEntry: ResourceEntry): Boolean {
        val generated = generateUniqueName(
            resourceEntry.type,
            resourceEntry.resourceId
        )
        return generated == resourceEntry.name
    }

    private fun generate(): Map<Int, String> {
        mGeneratedNames.clear()
        mSkipIds.clear()
        val results = HashMap<Int, String>()
        val skipIds = mSkipIds
        val resourceEntryList = listResources()
        for (resourceEntry in resourceEntryList) {
            if (!isGenerated(resourceEntry)) {
                skipIds.add(resourceEntry.resourceId)
            }
        }
        for (resourceEntry in resourceEntryList) {
            val resourceId = resourceEntry.resourceId
            if (results.containsKey(resourceId) || skipIds.contains(resourceId)) {
                continue
            }
            val entry = getEnglishOrDefault(resourceEntry) ?: continue
            val resValue = (entry.tableEntry as ResTableEntry).value
            val text = resValue.valueAsString
            val name = generate(resourceId, text)
            if (name != null) {
                results[resourceId] = name
                mGeneratedNames.add(name)
            }
        }
        return results
    }

    private fun getEnglishOrDefault(resourceEntry: ResourceEntry): Entry? {
        var def: Entry? = null
        for (entry in resourceEntry) {
            if (entry == null) {
                continue
            }
            val tableEntry = entry.tableEntry as? ResTableEntry ?: continue
            val resValue = tableEntry.value
            if (resValue.valueType != ValueType.STRING) {
                continue
            }
            if (entry.resConfig.isDefault) {
                def = entry
            }
            val lang = entry.resConfig.language ?: continue
            if (lang == "en") {
                return entry
            }
        }
        return def
    }

    private fun listResources(): List<ResourceEntry> {
        return ArrayList(mapResourceEntries().values)
    }

    private fun mapResourceEntries(): Map<Int, ResourceEntry> {
        val results: MutableMap<Int, ResourceEntry> = HashMap()
        for (packageBlock in tableBlock.listPackages()) {
            val specTypePair = packageBlock.getSpecTypePair(TYPE) ?: continue
            val itr = specTypePair.resources
            while (itr.hasNext()) {
                val resourceEntry = itr.next()
                if (resourceEntry.isEmpty) {
                    continue
                }
                results[resourceEntry.resourceId] = resourceEntry
            }
        }
        return results
    }

    private fun generate(resourceId: Int, text: String?): String? {
        if (text == null) {
            return null
        }
        var name = generateEnglish(text) ?: return null
        if (!mGeneratedNames.contains(name)) {
            return name
        }
        name = name + "_" + String.format("%04x", 0xffff and resourceId)
        if (!mGeneratedNames.contains(name)) {
            return name
        }
        var i = 0
        while (i < 10) {
            val numberedName = name + "_" + i
            if (!mGeneratedNames.contains(numberedName)) {
                return numberedName
            }
            i++
        }
        return null
    }

    private fun generateEnglish(text: String): String? {
        var name = getPathDataName(text)
        if (name == null) {
            name = getUrlName(text)
        }
        if (name == null) {
            name = getDefaultName(text)
        }
        return name
    }

    private fun getPathDataName(str: String): String? {
        val matcher = PATTERN_PATH.matcher(str)
        return if (!matcher.find()) {
            null
        } else {
            PATH_DATA_NAME
        }
    }

    private fun getUrlName(str: String): String? {
        val matcher = PATTERN_URL.matcher(str)
        if (!matcher.find()) {
            return null
        }
        val builder = StringBuilder()
        builder.append("url_")
        var dom = matcher.group(3)
        dom = dom.replace('.', '/')
        var path = matcher.group(4)
        if (path != null) {
            path = path.replace('?', '/')
            path = path.replace('=', '/')
            dom += path
        }
        var len = 0
        val allPaths = dom.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val max = allPaths.size
        var appendOnce = false
        for (i in 0 until max) {
            val sub = allPaths[i]
            if (!isAToZName(sub)) {
                continue
            }
            val subLen = sub.length
            if (len + subLen > MAX_NAME_LEN) {
                if (!appendOnce) {
                    continue
                }
                break
            }
            if (appendOnce) {
                builder.append('_')
            }
            builder.append(sub)
            appendOnce = true
            len += subLen
        }
        return if (!appendOnce) {
            null
        } else {
            builder.toString()
        }
    }

    private fun getDefaultName(str: String): String? {
        var name = str
        name = name.replace("'".toRegex(), "")
        name = name.replace("%([0-9]+\\$)?s".toRegex(), " STR ")
        name = name.replace("%([0-9]+\\$)?d".toRegex(), " NUM ")
        name = name.replace("&amp;".toRegex(), " and ")
        name = name.replace("&(lt|gt);".toRegex(), " ")
        name = replaceNonAZ(name)
        val allWords = name.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var len = 0
        var appendOnce = false
        val builder = StringBuilder()
        val max = allWords.size
        for (i in 0 until max) {
            var sub = allWords[i]
            if (!isAToZName(sub)) {
                if (i + 1 < max) {
                    continue
                }
                if (!isNumber(sub) || !appendOnce) {
                    continue
                }
            }
            val subLen = sub.length
            if (len + subLen > MAX_NAME_LEN) {
                if (!appendOnce) {
                    continue
                }
                break
            }
            if (appendOnce) {
                builder.append('_')
                len++
            } else {
                sub = sub.lowercase(Locale.getDefault())
            }
            builder.append(sub)
            appendOnce = true
            len += subLen
        }
        return if (!appendOnce || len < 3) {
            null
        } else builder.toString()
    }

    private fun replaceNonAZ(fullStr: String): String {
        var str: String
        var num: String? = null
        val matcher = PATTERN_END_NUMBER.matcher(fullStr)
        if (matcher.find()) {
            str = matcher.group(1)
            num = matcher.group(2)
        } else {
            str = fullStr
        }
        str = str.replace("[^A-Za-z]+".toRegex(), " ")
        val builder = StringBuilder()
        builder.append(str)
        if (num != null) {
            builder.append(' ')
            builder.append(num)
        }
        builder.append(' ')
        return builder.toString()
    }

    companion object {
        private fun isAToZName(str: String): Boolean {
            return PATTERN_EN.matcher(str).find()
        }

        private fun isNumber(str: String): Boolean {
            return PATTERN_NUMBER.matcher(str).find()
        }

        private val PATTERN_EN = Pattern.compile("^[A-Za-z]{2,15}(_[A-Za-z]{1,15})*[0-9]*$")
        private val PATTERN_PATH =
            Pattern.compile("^M[0-9.]+[\\s,]+[0-9\\-ACLHMSVZaclhmsvz,.\\s]+$")
        private val PATTERN_URL = Pattern.compile("^(https?://)(www\\.)?([^/]+)(/.*)?$")
        private val PATTERN_END_NUMBER =
            Pattern.compile("^([^0-9]+)([0-9]+)\\s*([^a-zA-Z0-9]{0,2})\\s*$")
        private val PATTERN_NUMBER = Pattern.compile("^[0-9]+$")
        private const val MAX_NAME_LEN = 40
        private const val PATH_DATA_NAME = "vector_path_data"
        private const val TYPE = "string"
    }
}