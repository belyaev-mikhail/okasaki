package ru.spbstu.collections.persistent.impl

abstract class AbstractSet<E>: Set<E>, AbstractCollection<E>() {
    override fun equals(other: Any?) =
        when {
            other === this -> true
            other !is Set<*> -> false
            else -> (size == other.size) && containsAll(other)
        }

    override fun hashCode() = sumBy { it.hashCode() } // set hashcode
}
