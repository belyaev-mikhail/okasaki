package ru.spbstu.collections.persistent

import ru.spbstu.collections.persistent.impl.ImmutableQueue
import ru.spbstu.collections.persistent.slist.SList
import ru.spbstu.collections.persistent.slist.sListOf

// rotate is essentially a scheduled reverse() of `inputs`
// precondition: outputs.size == (inputs.size - 1)
// accumulator is the result of the rotation of `input`
// logically, it returns outputs ++ inputs.reverse(), but using smart lazy unwrapping of things
// start with the contents of outputs, then, when it runs out, continue with inputs.head
// (that is guaranteed to be the only item in inputs left if any),
// then the accumulator
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
        // schedule serves two purposes:
        // first, on every operation we need to force a single element of output
        // but we can't, cause it may not be first element.
        // schedule initially contains all the output elements, but we gradually force-remove them one by one
        // and force them _inside_ output not even touching it.
        // second, the size is (genius!) always equal to (outputs.size - inputs.size), so, when it reaches zero,
        // it's `rotate` time!
        private val schedule: ConsStream<E>? = outputs): ImmutableQueue<E> {

    constructor() : this(null, null, null)

    internal fun copy(
            inputs: SList<E>? = this.inputs,
            outputs: ConsStream<E>? = this.outputs,
            schedule: ConsStream<E>? = this.schedule
    ) =
            ScheduledQueue(inputs, outputs, schedule)

    // exec is the workhorse that is invoked on every operation.
    // it force-removes one element from schedule (tick-tock), and, when it runs empty, invokes `rotate`
    // to do the reversing
    internal fun exec(): ScheduledQueue<E> = when {
        schedule != null -> copy(schedule = schedule.tail)
        inputs == null -> this
        else -> {
            val rotated = rotate(outputs, inputs)
            ScheduledQueue(sListOf(), rotated, rotated)
        }
    }

    override fun push(value: E) =
            copy(inputs = SList(value, inputs)).exec()

    override fun pop() =
            copy(outputs = outputs?.tail).exec()

    override val empty: Boolean
        get() = outputs == null
    override val top: E
        get() = outputs!!.head

    override fun toString() = "$inputs><$outputs"

    companion object
}

