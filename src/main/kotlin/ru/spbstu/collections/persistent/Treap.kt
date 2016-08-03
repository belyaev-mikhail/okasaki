package ru.spbstu.collections.persistent

import kotlinx.Warnings
import ru.spbstu.collections.persistent.impl.Wrapper
import java.util.*
import java.util.concurrent.*

@Suppress(Warnings.NOTHING_TO_INLINE, Warnings.UNCHECKED_CAST)
inline fun forceCastToTreap(t: Any?) = t as Treap<Comparable<Any?>, Any?>

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
            iteratorEquals(iterator(), forceCastToTreap(other).iterator())
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

fun<E: Comparable<E>, P> Treap<E, P>?.getSubTree(x: E): Treap<E, P>? =
        when{
            this == null       -> null
            key == x           -> this
            key > x            -> left.getSubTree(x)
            else /* key > x */ -> right.getSubTree(x)
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

val<E: Comparable<E>, P> Treap<E, P>?.height: Int
    get() =
        when(this) {
            null -> 0
            else -> Math.max(left.height, right.height) + 1
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

@Suppress(Warnings.NOTHING_TO_INLINE)
inline operator fun<E> ExecutorService.invoke(noinline c: () -> E): Future<E> = submit(c)
@Suppress(Warnings.NOTHING_TO_INLINE)
inline operator fun<E> ForkJoinPool.invoke(noinline c: () -> E): ForkJoinTask<E> = submit(c)

fun<E: Comparable<E>, P> Treap<E, P>?.punion(that: Treap<E, P>?,
                                             pool: ExecutorService = Executors.newCachedThreadPool(),
                                             factor: Int = Runtime.getRuntime().availableProcessors() / 2) : Treap<E, P>? {
    if(factor == 0) return this union that

    this ?: return that
    that ?: return this

    val (T1, T2) =
            if(this.priority < that.priority) Pair(that, this)
            else Pair(this, that)

    val (L, R) = T2.split(T1.key)
    val leftTask = pool.invoke { T1.left.punion(L, pool, factor/2) }
    val right = T1.right.punion(R, pool, factor/2)

    return T1.copy(left = leftTask.get(), right = right)
}

fun<E: Comparable<E>, P> Treap<E, P>?.pintersect(that: Treap<E, P>?,
                                                 pool: ExecutorService = Executors.newCachedThreadPool(),
                                                 factor: Int = Runtime.getRuntime().availableProcessors() / 2) : Treap<E, P>? {
    this ?: return null
    that ?: return null

    if(this.priority < that.priority) return that intersect this

    val (L, R, Dup) = that.split(this.key)
    val leftTask = pool.invoke { this.left.pintersect(L, pool, factor/2) }
    val right = this.right.pintersect(R, pool, factor/2)

    if(!Dup) {
        return leftTask.get() merge right
    } else {
        return this.copy(left = leftTask.get(), right = right)
    }
}

fun<E: Comparable<E>, P> pdifference(left: Treap<E, P>?, right: Treap<E, P>?, rightFromLeft: Boolean,
                                     pool: ExecutorService = Executors.newCachedThreadPool(),
                                     factor: Int = Runtime.getRuntime().availableProcessors() / 2) : Treap<E, P>? {
    if(left == null || right == null) {
        return if(rightFromLeft) left else right
    }

    if(left.priority < right.priority) return pdifference(right, left, !rightFromLeft, pool)

    val(L, R, Dup) = right.split(left.key)

    val leftTask = pool.invoke { pdifference(left.left, L, rightFromLeft, pool, factor/2)  }
    val right = pdifference(left.right, R, rightFromLeft, pool, factor/2)

    if(!Dup && rightFromLeft) {
        return left.copy(left = leftTask.get(), right = right)
    } else {
        return leftTask.get() merge right
    }
}

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

class TreapSet<E: Comparable<E>>(override val inner: Treap<E, Unit>? = null):
        ru.spbstu.collections.persistent.impl.AbstractSet<E>(),
        Wrapper<Treap<E, Unit>?> {
    override val size: Int by lazy { inner.size }

    override fun contains(element: E) = withInner { contains(element) }

    override fun containsAll(elements: Collection<E>) = withInner {
        when(elements) {
            is TreapSet<E> -> this pge elements.inner
            else -> super.containsAll(elements)
        }
    }

    override fun iterator() = withInner { iterator() }
}

class TreapMap<K: Comparable<K>, V>(override val inner: Treap<K, V>? = null):
        ru.spbstu.collections.persistent.impl.AbstractMap<K, V>(),
        Wrapper<Treap<K, V>?> {
    override fun containsKey(key: K) = key in inner

    override fun get(key: K): V? = inner.getSubTree(key)?.payload

    override val size: Int by lazy { inner.size }

}
