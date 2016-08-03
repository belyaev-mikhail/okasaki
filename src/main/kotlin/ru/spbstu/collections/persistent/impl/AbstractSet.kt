package ru.spbstu.collections.persistent.impl

import java.util.Objects.*

abstract class AbstractSet<E>: Set<E>, AbstractCollection<E>() {
    override fun equals(other: Any?): Boolean {
        if(other === this) return true
        if(other !is Set<*>) return false
        return (size == other.size) && containsAll(other) // set equality
    }

    override fun hashCode(): Int {
        return asSequence().sumBy { hashCode(it) } // set hashcode
    }
}