package ru.spbstu.collections.persistent.impl

abstract class AbstractCollection<E>: Collection<E> {
    override fun containsAll(elements: Collection<E>) = elements.all { contains(it) }
    override fun isEmpty() = size == 0

    override fun toString() = asSequence().joinToString(prefix = "[", postfix = "]")
    override fun contains(element: E): Boolean = asSequence().any { it == element }
}