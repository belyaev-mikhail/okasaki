package ru.spbstu.collections.persistent.impl

fun <A, B> iteratorEquals(l: Iterator<A>, r: Iterator<B>): Boolean {
    while (l.hasNext() && r.hasNext()) {
        if (l.next() != r.next()) return false
        if (l.hasNext() != r.hasNext()) return false
    }
    return true
}

fun <E> iteratorHash(i: Iterator<E>): Int {
    var hashCode = 1
    for (e in i.asSequence())
        hashCode = 31 * hashCode + e.hashCode()
    return hashCode
}

data class MappedIterator<E, R>(val inner: Iterator<E>, val f: (E) -> R): Iterator<R> {
    override fun hasNext() = inner.hasNext()
    override fun next() = f(inner.next())
}

fun<E, R> mapIterator(i: Iterator<E>, f: (E) -> R): Iterator<R> = MappedIterator(i, f)
