package com.mcal.resguard.utils

import com.reandroid.xml.XMLDocument
import com.reandroid.xml.XMLElement
import java.util.*

class TypeNameMap : Comparator<TypeNameMap.TypeName> {
    private val map: MutableMap<Int, TypeName> = HashMap()

    operator fun contains(name: String?): Boolean {
        for (typeName in map.values) {
            if (name == typeName.name) {
                return true
            }
        }
        return false
    }

    operator fun contains(id: Int): Boolean {
        return map.containsKey(id and -0x10000)
    }

    fun toXMLDocument(): XMLDocument {
        val xmlDocument = XMLDocument()
        val documentElement = XMLElement("resources")
        xmlDocument.documentElement = documentElement
        for (typeName in ArrayList(map.values)) {
            documentElement.add(typeName.toXMLElement())
        }
        return xmlDocument
    }

    fun count(): Int {
        return map.size
    }

    fun add(xmlDocument: XMLDocument) {
        val documentElement = xmlDocument.documentElement ?: return
        val iterator = documentElement.elements
        while (iterator.hasNext()) {
            add(TypeName.fromXMLElement(iterator.next()))
        }
    }

    fun add(id: Int, name: String) {
        add(TypeName(id, name))
    }

    fun add(typeName: TypeName) {
        map.remove(typeName.packageTypeId)
        map[typeName.packageTypeId] = typeName
    }

    override fun compare(typeName1: TypeName, typeName2: TypeName): Int {
        return typeName1.compareTo(typeName2)
    }

    class TypeName(packageTypeId: Int, val name: String) : Comparable<TypeName> {
        val packageTypeId = packageTypeId and -0x10000

        fun toXMLElement(): XMLElement {
            val element = XMLElement("type")
            element.setAttribute("id", hexId)
            element.setAttribute("name", name)
            return element
        }

        val hexId: String
            get() = String.format("0x%08x", packageTypeId)

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other == null || javaClass != other.javaClass) {
                return false
            }
            val typeName = other as TypeName
            return packageTypeId == typeName.packageTypeId
        }

        override fun hashCode(): Int {
            return Objects.hash(packageTypeId)
        }

        override fun toString(): String {
            return "$hexId:$name"
        }

        override operator fun compareTo(other: TypeName): Int {
            return (packageTypeId shr 16).compareTo(other.packageTypeId shr 16)
        }

        companion object {
            fun fromXMLElement(element: XMLElement): TypeName {
                val id = decodeHex(element.getAttributeValue("id"))
                val name = element.getAttributeValue("name")
                return TypeName(id, name)
            }
        }
    }

    companion object {
        private fun decodeHex(hex: String): Int {
            return java.lang.Long.decode(hex).toInt()
        }
    }
}
