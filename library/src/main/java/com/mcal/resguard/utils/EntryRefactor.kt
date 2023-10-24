package com.mcal.resguard.utils

import com.reandroid.identifiers.ResourceIdentifier
import com.reandroid.identifiers.TypeIdentifier

class EntryRefactor(
    private val mTypeIdentifier: TypeIdentifier
) {
    fun refactorAll(): Int {
        var result = 0
        for (ri in mTypeIdentifier.items) {
            if (!ri.isGeneratedName) {
                continue
            }
            val renamed = refactor(ri)
            if (renamed) {
                result++
            }
        }
        return result
    }

    private fun refactor(entry: ResourceIdentifier): Boolean {
        return refactorByValue(entry)
    }

    /*
     * TODO: implement refactoring from TableBlock entry value
     *   e.g-1: <string name="***">No internet connection</string>
     *      ==> <string name="no_internet_connection">No internet connection</string>
     *   e.g-2: <color name="***">#FF0000</color>
     *      ==> <color name="red">#FF0000</color>
     */
    private fun refactorByValue(resourceIdentifier: ResourceIdentifier): Boolean {
        return false
    }
}
