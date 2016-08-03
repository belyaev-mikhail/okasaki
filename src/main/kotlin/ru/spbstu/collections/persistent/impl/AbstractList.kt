package ru.spbstu.collections.persistent.impl

import ru.spbstu.collections.persistent.andReturn
import ru.spbstu.collections.persistent.butAlso
import java.util.*

abstract class AbstractList<E>: List<E>, AbstractCollection<E>() {
    override fun contains(element: E): Boolean {
        for(e in this) if(e == element) return true
        return false
    }

    override fun iterator(): Iterator<E> = listIterator()

    override fun indexOf(element: E): Int {
        var ix = 0
        for(e in this) {
            if(e == element) return ix
            ++ix
        }
        return -1
    }

    override fun lastIndexOf(element: E): Int {
        var ix = 0
        var ret = -1
        for(e in this) {
            if(e == element) {
                ret = ix
            }
            ++ix
        }
        return ret
    }

    override fun listIterator() = listIterator(0)

    data class DefaultListIterator<E>(val data: List<E>, var index: Int = 0): ListIterator<E> {
        override fun hasNext() = index < data.size
        override fun hasPrevious() = index > 0
        override fun next() = data[index] butAlso { ++index }
        override fun nextIndex() = index
        override fun previous() = { --index } andReturn data[index]
        override fun previousIndex() = index - 1
    }

    override fun listIterator(index: Int): ListIterator<E> = DefaultListIterator(this, index)

    class DefaultSubList<E>(val inner: List<E>, val fromIndex: Int, val toIndex: Int): AbstractList<E>() {
        override val size: Int get() = toIndex - fromIndex
        override fun get(index: Int): E = get(fromIndex + index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<E> = DefaultSubList<E>(this, fromIndex, toIndex)

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is List<*>) return false

        val e1 = iterator()
        val e2 = other.iterator()
        while (e1.hasNext() && e2.hasNext()) {
            val o1 = e1.next()
            val o2 = e2.next()
            if (o1 != o2) return false
        }
        return !(e1.hasNext() || e2.hasNext())
    }

    override fun hashCode(): Int {
        var hashCode = 1
        for (e in this)
            hashCode = 31 * hashCode + Objects.hashCode(e)
        return hashCode
    }
}