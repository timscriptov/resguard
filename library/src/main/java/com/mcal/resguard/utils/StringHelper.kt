package com.mcal.resguard.utils

import java.nio.charset.StandardCharsets
import java.security.MessageDigest


object StringHelper {
    @JvmStatic
    fun md5(str: String): ByteArray = MessageDigest.getInstance("MD5").digest(str.toByteArray(StandardCharsets.UTF_8))

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

    fun trueOrNull(value: Boolean?): String? {
        return if (value == null) {
            null
        } else {
            trueOrNull(value)
        }
    }

    fun trueOrNull(value: Boolean): String? {
        return if (!value) {
            null
        } else {
            true.toString()
        }
    }

    fun sortAscending(nameList: List<String>): List<String> {
        nameList.sortedWith { s1, s2 -> s1.compareTo(s2) }
        return nameList
    }

    fun printNameAndValues(builder: StringBuilder, tab: String, totalWidth: Int, objTable: Array<Array<Any?>?>?) {
        printNameAndValues(builder, tab, "", totalWidth, objTable)
    }

    fun printNameAndValues(
        builder: StringBuilder,
        tab: String,
        separator: String,
        totalWidth: Int,
        objTable: Array<Array<Any?>?>?
    ) {
        val table = convertNameAndValue(objTable) ?: return
        var leftWidth = 0
        for (col in table) {
            val len = col[0]!!.length
            if (len > leftWidth) {
                leftWidth = len
            }
        }
        val bnColumns = 0
        leftWidth += bnColumns
        val maxRight = totalWidth - leftWidth
        for (i in table.indices) {
            val col = table[i]
            if (i != 0) {
                builder.append("\n")
            }
            printRow(false, builder, tab, leftWidth, maxRight, col[0], separator, col[1])
        }
    }

    private fun convertNameAndValue(table: Array<Array<Any?>?>?): Array<Array<String?>>? {
        if (table == null) {
            return null
        }
        val results: MutableList<Array<String?>> = ArrayList()
        for (objRow in table) {
            val row = convertNameAndValueRow(objRow)
            if (row != null) {
                results.add(row)
            }
        }
        return if (results.size == 0) {
            null
        } else results.toTypedArray<Array<String?>>()
    }

    private fun convertNameAndValueRow(objRow: Array<Any?>?): Array<String?>? {
        if (objRow == null) {
            return null
        }
        val len = objRow.size
        if (len != 2) {
            return null
        }
        if (objRow[0] == null || objRow[1] == null) {
            return null
        }
        val result = arrayOfNulls<String>(len)
        result[0] = objRow[0].toString()
        result[1] = objRow[1].toString()
        return if (result[0] == null || result[1] == null) {
            null
        } else result
    }

    fun printTwoColumns(builder: StringBuilder, tab: String, totalWidth: Int, table: Array<Array<String>>) {
        printTwoColumns(builder, tab, "  ", totalWidth, table)
    }

    fun printTwoColumns(
        builder: StringBuilder,
        tab: String,
        columnSeparator: String,
        totalWidth: Int,
        table: Array<Array<String>>
    ) {
        var leftWidth = 0
        for (col in table) {
            val len = col[0].length
            if (len > leftWidth) {
                leftWidth = len
            }
        }
        val maxRight = totalWidth - leftWidth
        for (i in table.indices) {
            val col = table[i]
            if (i != 0) {
                builder.append("\n")
            }
            printRow(true, builder, tab, leftWidth, maxRight, col[0], columnSeparator, col[1])
        }
    }

    private fun printRow(
        indentLeft: Boolean,
        builder: StringBuilder,
        tab: String,
        leftWidth: Int,
        maxRight: Int,
        left: String?,
        separator: String,
        right: String?
    ) {
        builder.append(tab)
        if (indentLeft) {
            builder.append(left)
        }
        fillSpace(builder, leftWidth - left!!.length)
        if (!indentLeft) {
            builder.append(left)
        }
        builder.append(separator)
        val rightChars = right!!.toCharArray()
        var rightWidth = 0
        var spacePrefixSeen = false
        for (i in rightChars.indices) {
            val ch = rightChars[i]
            if (i == 0) {
                builder.append(ch)
                rightWidth++
                continue
            }
            if (ch == '\n' || rightWidth > 0 && rightWidth % maxRight == 0) {
                builder.append('\n')
                builder.append(tab)
                fillSpace(builder, leftWidth + separator.length)
                rightWidth = 0
                spacePrefixSeen = false
            }
            if (ch != '\n') {
                val skipFirstSpace = rightWidth == 0 && ch == ' '
                if (!skipFirstSpace || spacePrefixSeen) {
                    builder.append(ch)
                    rightWidth++
                } else {
                    spacePrefixSeen = true
                }
            }
        }
    }

    private fun fillSpace(builder: StringBuilder, count: Int) {
        for (i in 0 until count) {
            builder.append(' ')
        }
    }
}