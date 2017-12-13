package ru.spbstu.collections.persistent

import ru.spbstu.collections.persistent.impl.ImmutableQueue
import ru.spbstu.collections.persistent.slist.SList
import ru.spbstu.collections.persistent.slist.plus
import ru.spbstu.collections.persistent.slist.reverse

class AmortizedQueue<E> private constructor(val inputs: SList<E>?, val outputs: SList<E>?): ImmutableQueue<E> {

    constructor() : this(null, null)

    fun copy(inputs: SList<E>? = this.inputs, outputs: SList<E>? = this.outputs) =
            AmortizedQueue(inputs, outputs)

    override fun push(value: E) =
            when (outputs) {
                null -> copy(outputs = SList(value))
                else -> copy(inputs = SList(value, inputs))
            }

    override fun pop() =
            when (outputs?.tail) {
                null -> copy(outputs = inputs.reverse())
                else -> copy(outputs = outputs.tail)
            }

    override val empty
        get() = outputs == null
    override val top: E
        get() = outputs!!.head

    fun toSList() = inputs + outputs.reverse()

    override fun toString() = "$inputs><$outputs"

    companion object
}

