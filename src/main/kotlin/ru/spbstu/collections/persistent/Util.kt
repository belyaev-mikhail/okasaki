package ru.spbstu.collections.persistent

import java.util.*

/**
 * Created by belyaev on 7/20/16.
 */
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


object Bits {
    inline operator fun Int.get(at: Int): Int = (this shr at) and 1
    inline operator fun Int.get(start: Int, end: Int): Int {
        val mask = ((1 shl (end - start)) - 1) shl start
        return (this and mask) shr start
    }

    inline operator fun Long.get(at: Int): Long = (this shr at) and 1
    inline operator fun Long.get(start: Long, end: Long): Long {
        val mask = ((1L shl (end - start).toInt()) - 1L) shl start.toInt()
        return (this and mask) shr start.toInt()
    }
}