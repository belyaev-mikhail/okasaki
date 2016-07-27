package ru.spbstu.collections.persistent

import java.util.*
import java.util.stream.BaseStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

infix fun Int.times(action: () -> Unit) {
    for(i in 0..this) action()
}

fun<T> Int.times(initial: T, app: (T) -> T): T {
    var imm: T = initial
    for(i in 0..this) imm = app(imm)
    return imm
}

fun<A, B> iteratorEquals(l: Iterator<A>, r: Iterator<B>): Boolean {
    while(l.hasNext() && r.hasNext()) {
        if(l.next() != r.next()) return false
        if(l.hasNext() != r.hasNext()) return false
    }
    return true
}

fun<E> iteratorHash(i: Iterator<E>): Int {
    var res: Int = 0
    while(i.hasNext()) {
        res = Objects.hash(res, i.next())
    }
    return res
}

fun<A, R> fix(f: ((A) -> R, A) -> R): (A) -> R = { x -> f (fix(f), x) }

inline fun<V> fixValue(crossinline f: (Lazy<V>) -> V): Lazy<V> {
    var x: Lazy<V>? = null
    x = lazy { x?.let(f)!! }
    return x
}

data class WrappedComparator<E>(val cmp: Comparator<E>) {
    operator fun E.compareTo(that: E): Int = cmp.compare(this, that)
}
inline fun<E> withCmp(cmp: Comparator<E>, body: WrappedComparator<E>.() -> Unit) = with(WrappedComparator(cmp), body)

object Bits {
    inline operator fun Int.get(at: Int): Int = (this ushr at) and 1
    inline operator fun Int.get(start: Int, end: Int): Int {
        val start_ = start.coerceAtLeast(0)
        val end_ = end.coerceAtMost(Int.SIZE - 1) + 1
        val range = (end_ - start_)
        val mask = if(range == Int.SIZE) return this
                   else ((1 shl (end_ - start_)) - 1) shl start_
        return (this and mask) ushr start_
    }

    inline operator fun Long.get(at: Int): Long = (this shr at) and 1
    inline operator fun Long.get(start: Int, end: Int): Long {
        val start_ = start.coerceAtLeast(0)
        val end_ = end.coerceAtMost(Long.SIZE - 1) + 1
        val range = (end_ - start_)
        if(range == Long.SIZE) return this
        val mask = ((1L shl (end_ - start_)) - 1L) shl start_
        return (this and mask) ushr start_
    }
}

val Int.Companion.SIZE: Int
    get() = java.lang.Integer.SIZE
val Long.Companion.SIZE: Int
    get() = java.lang.Long.SIZE
val Short.Companion.SIZE: Int
    get() = java.lang.Short.SIZE
val Byte.Companion.SIZE: Int
    get() = java.lang.Byte.SIZE

inline fun log2ceil(v: Int) =
        if(v == 0) 0 else Int.SIZE - Integer.numberOfLeadingZeros(v - 1)
inline fun log2floor(v: Int) =
        if(v == 0) 0 else Int.SIZE - 1 - Integer.numberOfLeadingZeros(v)
inline fun pow2(v: Int) = 1 shl v
val Int.greaterPowerOf2: Int
        get() = pow2(log2ceil(this))
val Int.lesserPowerOf2: Int
        get() = if(this == 0) 0 else pow2(log2floor(this))

internal fun <T, S: BaseStream<T, S>> BaseStream<T, S>.asSequence() = iterator().asSequence()
internal fun IntStream.asSequence() = iterator().asSequence()
internal fun LongStream.asSequence() = iterator().asSequence()

fun<E> Comparator<E>.max(le: E, re: E) = if(compare(le, re) > 0) le else re
fun<E> Comparator<E>.max(vararg e: E) = e.maxWith(this)
fun<E, R> Comparator<R>.maxBy(vararg e: E, selector: (E) -> R): E? =
    with(nullsFirst()) {
        var max: E? = null
        var maxValue: R? = null
        for(p in e){
            val pv = selector(p)
            if(compare(pv, maxValue) > 0) {
                max = p
                maxValue = pv
            }
        }
        return max
    }

fun<E> Comparator<E>.nullsFirst(): Comparator<E?> = Comparator.nullsFirst(this) as Comparator<E?>
fun<E> Comparator<E>.nullsLast(): Comparator<E?> = Comparator.nullsLast(this) as Comparator<E?>

fun<E> Iterator<E>.nextOrNull(): E? = if(hasNext()) next() else null
