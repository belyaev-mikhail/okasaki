package ru.spbstu.collections.persistent.slist

import ru.spbstu.collections.persistent.impl.IterableWithDefaults

data class SList<out E>(val head: E, val tail: SList<E>? = null) : IterableWithDefaults<E> {

    companion object {}

    override fun iterator() = SListIterator(this)

    override fun toString() = defaultToString()
    override fun equals(other: Any?) = defaultEquals(other)
    override fun hashCode() = defaultHashCode()
}
