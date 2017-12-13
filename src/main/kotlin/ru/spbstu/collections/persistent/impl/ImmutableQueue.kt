package ru.spbstu.collections.persistent.impl

interface ImmutableQueue<E> {
    val top: E
    val empty: Boolean

    fun push(value: E): ImmutableQueue<E>
    fun pop(): ImmutableQueue<E>
}
