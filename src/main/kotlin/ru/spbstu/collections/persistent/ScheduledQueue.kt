package ru.spbstu.collections.persistent

import ru.spbstu.collections.persistent.impl.ImmutableQueue
import ru.spbstu.collections.persistent.slist.SList
import ru.spbstu.collections.persistent.slist.sListOf

internal fun<E> rotate(outputs: ConsStream<E>?, inputs: SList<E>, accumulator: ConsStream<E>? = null): ConsStream<E>? =
        when(outputs) {
            null -> ConsStream(inputs.head, accumulator)
            else -> ConsStream(outputs.head) {
                rotate(outputs.tail, inputs.tail!!, inputs.head + accumulator)
            }
        }

class ScheduledQueue<E> private constructor(
        private val inputs: SList<E>?,
        private val outputs: ConsStream<E>?,
        private val schedule: ConsStream<E>? = outputs): ImmutableQueue<E> {

    constructor() : this(null, null, null)

    internal fun copy(
            inputs: SList<E>? = this.inputs,
            outputs: ConsStream<E>? = this.outputs,
            schedule: ConsStream<E>? = this.schedule
    ) =
            ScheduledQueue(inputs, outputs, schedule)

    internal fun exec(): ScheduledQueue<E> = when {
        schedule != null -> copy(schedule = schedule.tail)
        inputs == null -> this
        else -> {
            val rotated = rotate(outputs, inputs)
            ScheduledQueue(sListOf(), rotated, rotated)
        }
    }

    override fun push(value: E) =
            when (outputs) {
                null -> copy(outputs = ConsStream<E>(value, outputs)).exec()
                else -> copy(inputs = SList(value, inputs)).exec()
            }

    override fun pop() =
            copy(outputs = outputs?.tail).exec()

    override val empty: Boolean
        get() = outputs == null
    override val top: E
        get() = outputs!!.head

    override fun toString() = "$inputs><$outputs"

    companion object
}

