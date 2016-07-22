package ru.spbstu.collections.persistent

import java.util.*

data class Treap<E: Comparable<E>, out P>(
        val key: E,
        val payload: P,
        val left: Treap<E, P>? = null,
        val right: Treap<E, P>? = null,
        val random: Random = Random(),
        val priority: Int = random.nextInt()
) {
    companion object{}

    fun copy(): Treap<E, P> = this

    override fun equals(other: Any?): Boolean =
        if(other is Treap<*,*>)
            iteratorEquals(iterator(), (other as Treap<Comparable<Any?>, *>).iterator())
        else false

    override fun hashCode() =
        iteratorHash(iterator())

    override fun toString(): String =
            iterator().asSequence().joinToString(prefix = "{", postfix = "}")
}

infix fun<E: Comparable<E>, P> Treap<E, P>?.merge(that: Treap<E, P>?): Treap<E, P>? =
    when {
        this == null                              -> that
        that == null                              -> this
        this.priority > that.priority             -> this.copy(right = this.right merge that)
        else /* this.priority <= that.priority */ -> that.copy(left  = this merge that.left)
    }

fun<E: Comparable<E>, P> Treap<E, P>.split(onKey: E): Triple<Treap<E, P>?, Treap<E, P>?, Boolean> {
    if(key == onKey) return Triple(left, right, true)
    else if(key < onKey) {
        right ?: return Triple(this, null, false)
        val(RL, R, Dup) = right.split(onKey)
        return Triple(this.copy(right = RL), R, Dup)
    } else /* if key > onKey */ {
        left ?: return Triple(null, this, false)
        val(L, LR, Dup) = left.split(onKey)
        return Triple(L, this.copy(left = LR), Dup)
    }
}

fun<E: Comparable<E>, P> Treap<E, P>?.add(x: E, p: P): Treap<E, P> {
    this ?: return Treap(x, p)
    val(L, R) = split(x)
    val M = Treap(x, p, random = random)
    return (L merge M merge R) ?: M
}

fun<E: Comparable<E>> Treap<E, Unit>?.add(x: E) = add(x, Unit)

fun<E: Comparable<E>, P> Treap<E, P>?.remove(x: E): Treap<E, P>? {
    this ?: return null
    val(L, R) = split(x)
    return (L merge R)
}

operator fun<E: Comparable<E>, P> Treap<E, P>?.contains(x: E): Boolean =
    when{
        this == null       -> false
        key == x           -> true
        key > x            -> left.contains(x)
        else /* key > x */ -> right.contains(x)
    }
fun<E: Comparable<E>, P> Treap<E, P>?.min(): E? =
        when(this){
            null -> null
            else -> left.min() ?: key
        }
fun<E: Comparable<E>, P> Treap<E, P>?.max(): E? =
        when(this){
            null -> null
            else -> right.max() ?: key
        }

val<E: Comparable<E>, P> Treap<E, P>?.size: Int
    get() =
        when(this) {
            null -> 0
            else -> 1 + left.size + right.size
        }

infix fun<E: Comparable<E>, P> Treap<E, P>?.union(that: Treap<E, P>?) : Treap<E, P>? {
    this ?: return that
    that ?: return this

    val (T1, T2) =
            if(this.priority < that.priority) Pair(that, this)
            else Pair(this, that)

    val (L, R) = T2.split(T1.key)
    return T1.copy(left = T1.left union L, right = T1.right union R)
}

infix fun<E: Comparable<E>, P> Treap<E, P>?.intersect(that: Treap<E, P>?) : Treap<E, P>? {
    this ?: return null
    that ?: return null

    if(this.priority < that.priority) return that intersect this

    val (L, R, Dup) = that.split(this.key)
    val Lres = this.left intersect L
    val Rres = this.right intersect R

    if(!Dup) {
        return Lres merge Rres
    } else {
        return this.copy(left = Lres, right = Rres)
    }
}

fun<E: Comparable<E>, P> difference(left: Treap<E, P>?, right: Treap<E, P>?, rightFromLeft: Boolean) : Treap<E, P>? {
    if(left == null || right == null) {
        return if(rightFromLeft) left else right
    }

    if(left.priority < right.priority) return difference(right, left, !rightFromLeft)

    val(L, R, Dup) = right.split(left.key)

    val Lres = difference(left.left, L, rightFromLeft)
    val Rres = difference(left.right, R, rightFromLeft)

    if(!Dup && rightFromLeft) {
        return left.copy(left = Lres, right = Rres)
    } else {
        return Lres merge Rres
    }
}

infix fun<E: Comparable<E>, P> Treap<E, P>?.difference(that: Treap<E, P>?) = difference(this, that, true)

operator fun<E: Comparable<E>, P> Treap<E, P>?.plus(that: Treap<E, P>?) = this union that
operator fun<E: Comparable<E>, P> Treap<E, P>?.minus(that: Treap<E, P>?) = this difference that

infix fun<E: Comparable<E>, P> Treap<E, P>?.symDiff(that: Treap<E, P>?) = (this + that) - (this intersect that)
infix fun<E: Comparable<E>, P> Treap<E, P>?.pge(that: Treap<E, P>?) = (that - this) == null
infix fun<E: Comparable<E>, P> Treap<E, P>?.plt(that: Treap<E, P>?) = !(this pge that)

data class TreapIterator<E: Comparable<E>, P>(var data: Treap<E, P>?, val nav: Stack<Treap<E, P>> = Stack()): Iterator<E> {
    override fun hasNext() = nav.isNotEmpty() || data != null

    override fun next(): E {
        while(data != null) {
            nav.push(data)
            data = data?.left
        }
        data = nav.pop()
        val ret = data!!.key
        data = data?.right
        return ret
    }
}

operator fun<E: Comparable<E>, P> Treap<E, P>?.iterator() = TreapIterator(this)

operator fun<E: Comparable<E>, P> Treap.Companion.invoke(): Treap<E, P>? = null
operator fun<E: Comparable<E>> Treap.Companion.invoke(e: E): Treap<E, Unit>? = Treap(e, Unit)
operator fun<E: Comparable<E>> Treap.Companion.invoke(vararg e: E): Treap<E, Unit>?
        = e.fold(invoke()){ t, e -> t.add(e) }

data class TreapSet<E: Comparable<E>>(val inner: Treap<E, Unit>? = null): Set<E> {
    override val size: Int by lazy { inner.size }

    override fun contains(element: E) = inner.contains(element)

    override fun containsAll(elements: Collection<E>) =
            when(elements) {
                is TreapSet<E> -> this.inner pge elements.inner
                else -> elements.all { contains(it) }
            }

    override fun isEmpty()
        = size == 0

    override fun iterator() = TreapIterator(inner)
}
