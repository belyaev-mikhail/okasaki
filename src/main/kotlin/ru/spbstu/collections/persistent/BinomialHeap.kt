package ru.spbstu.collections.persistent

import java.util.*

data class BinomialHeapNode<E>(val max: E, val subNodes: SList<BinomialHeapNode<E>>? = null, val cmp: Comparator<E>) {
    internal operator fun E.compareTo(that: E) = cmp.compare(this, that)

    internal fun maxx(lhv: E?, rhv: E?) =
        when {
            lhv == null -> rhv
            rhv == null -> lhv
            lhv >= rhv -> lhv
            else -> rhv
        }
    internal fun nextMax(): E? = subNodes.foldLeft<BinomialHeapNode<E>, E?>(null){ acc, heap -> maxx(acc, heap.max) }

    internal fun toHeap(): BinomialHeap<E>? =
            if(subNodes == null) null
            else BinomialHeap(
                    nextMax()!!, // guaranteed not to fail for non-empty subNodes
                    subNodes.reverse(),
                    cmp
            )
}
fun<E> BinomialHeapNode(element: E, cmp: Comparator<E>) = BinomialHeapNode(element, null, cmp)
infix fun<E> BinomialHeapNode<E>.merge(that: BinomialHeapNode<E>) =
        if(this.max > that.max) this.copy(subNodes = SList(that, subNodes))
        else that.copy(subNodes = SList(this, subNodes))

internal infix fun<E> BinomialHeapNode<E>?.mergeWithCarry(that: BinomialHeapNode<E>?): Pair<BinomialHeapNode<E>?, BinomialHeapNode<E>?> =
    if(this == null) Pair(that, null)
    else if(that == null) Pair(this, null)
    else Pair(null, this merge that)

data class BinomialHeap<E>(val max: E, val nodes: SList<BinomialHeapNode<E>?>?, val cmp: Comparator<E>) {
    internal operator fun E.compareTo(that: E) = cmp.compare(this, that)
    internal inline fun maxx(v0: E, v1: E) = if(v0 > v1) v0 else v1
}

internal fun<E> Iterator<E>.nextOrNull(): E? = if(hasNext()) next() else null

infix fun<E> BinomialHeap<E>?.merge(that: BinomialHeap<E>?): BinomialHeap<E>? {
    this ?: return that
    that ?: return this

    var nodes: SList<BinomialHeapNode<E>?>? = null
    var carry: BinomialHeapNode<E>? = null
    val thisIt = this.nodes.iterator()
    val thatIt = that.nodes.iterator()
    while(thisIt.hasNext() || thatIt.hasNext()) {
        val thisVal = thisIt.nextOrNull()
        val thatVal = thisIt.nextOrNull()
        var (sum0, carry0) = thisVal mergeWithCarry thatVal
        var (sum1, carry1) = sum0 mergeWithCarry carry
        nodes = SList(sum1, nodes)
        carry = (carry0 mergeWithCarry carry1).first
    }
    return copy(max = maxx(this.max, that.max), nodes = nodes.reverse())
}

fun<E> BinomialHeap<E>?.popMax(): BinomialHeap<E>? {
    this ?: return null

    val maxTree = nodes.foldLeft<BinomialHeapNode<E>?, BinomialHeapNode<E>?>(null){ acc, t ->
        when {
            acc == null -> t
            t == null -> acc
            (t.max > acc.max) -> t
            else -> acc
        }
    }

    return (this.copy(nodes = nodes.removeAt { it === maxTree }) merge maxTree?.toHeap())
}
