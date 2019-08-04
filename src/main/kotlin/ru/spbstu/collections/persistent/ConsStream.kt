package ru.spbstu.collections.persistent


data class ConsStream<E>(val head: E, val lazyTail: Lazy<ConsStream<E>?>) {
    constructor(head: E, tail: ConsStream<E>?): this(head, lazyOf(tail))
    constructor(head: E, lazyTail: () -> ConsStream<E>?): this(head, lazy(lazyTail))

    val tail: ConsStream<E>? by lazyTail

    companion object {
        fun<E> repeat(v: E): ConsStream<E> =
                ConsStream(v){ repeat(v) }

        fun<E> walk(init: E, f: (E) -> E): ConsStream<E> =
                ConsStream(init){ walk(f(init), f) }

        fun<E> recurse(head: E, tail: (ConsStream<E>) -> ConsStream<E>?): ConsStream<E> {
            var ret: ConsStream<E>? = null
            ret = ConsStream(head){ ret?.let(tail) }
            return ret
        }

        fun<E> ofIterator(iterator: Iterator<E>): ConsStream<E>? =
            if(iterator.hasNext()) ConsStream(iterator.next()){ ofIterator(iterator) }
            else null

        operator fun<E> invoke(seq: Sequence<E>) = ofIterator(seq.iterator())
        operator fun<E> invoke(seq: Iterable<E>) = ofIterator(seq.iterator())
    }
}

operator fun<E> ConsStream<E>?.get(index: Int): E {
    var acc: ConsStream<E>? = this
    var ix = index
    while(acc != null) {
        if(ix == 0) return acc.head
        acc = acc.tail
        --ix
    }
    throw IndexOutOfBoundsException()
}

infix fun<A, B> ConsStream<A>?.zip(that: ConsStream<B>?): ConsStream<Pair<A, B>>? =
        when {
            this == null || that == null -> null
            else -> ConsStream(Pair(this.head, that.head)){
                val thisTail = this.tail
                val thatTail = that.tail
                when {
                    thisTail == null || thatTail == null -> null
                    else -> thisTail zip thatTail
                }
            }
        }

fun<A, B> ConsStream.Companion.zip(lhv: ConsStream<A>?, rhv: ConsStream<B>?) = lhv zip rhv
fun<A, B, R> ConsStream.Companion.zip(lhv: ConsStream<A>?, rhv: ConsStream<B>?, z: (A, B) -> R): ConsStream<R>? =
        when {
            lhv == null || rhv == null -> null
            else -> ConsStream(z(lhv.head, rhv.head)) {
                val lhvTail = lhv.tail
                val rhvTail = rhv.tail
                when {
                    lhvTail == null || rhvTail == null -> null
                    else -> zip(lhvTail, rhvTail, z)
                }
            }
        }

fun<E> ConsStream<E>?.add(index: Int, e: E): ConsStream<E>? =
        when(index) {
            0 -> ConsStream(e, this)
            else -> this?.copy(lazyTail = lazy { tail.add(index - 1, e) })
        }
fun<E> ConsStream<E>.addAll(index: Int, elements: ConsStream<E>): ConsStream<E> =
        when(index) {
            0 -> elements.addAll(this)
            else -> copy(lazyTail = lazy { tail!!.addAll(index - 1, elements) })
        }

fun<E> ConsStream<E>?.add(e: E): ConsStream<E>? =
        when {
            this == null -> ConsStream(e){ null }
            else -> copy(lazyTail = lazy { tail.add(e) })
        }

fun<E> ConsStream<E>.addAll(e: ConsStream<E>): ConsStream<E> =
        when(val tail = tail) {
            null -> e
            else -> copy(lazyTail = lazy { tail.addAll(e) })
        }

operator fun<E> ConsStream<E>.plus(e: ConsStream<E>) = addAll(e)
operator fun<E> E.plus(cs: ConsStream<E>?) = cs.add(0, this)
operator fun<E> ConsStream<E>.plus(e: E) = add(e)

fun<E> ConsStream<E>.asSequence() = ConsStreamSeq<E>(this)
operator fun<E> ConsStream<E>.iterator() = asSequence().iterator()

data class ConsStreamSeq<E>(var stream: ConsStream<E>?): Sequence<E>, Iterator<E> {
    override fun hasNext() = stream != null
    override fun next() =
            stream.let { stream ->
                if(stream == null) throw NoSuchElementException()
                else stream.head.also { this.stream = stream.tail }
            }

    override fun iterator() = this
}
