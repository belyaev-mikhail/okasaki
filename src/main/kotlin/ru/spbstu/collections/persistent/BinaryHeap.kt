package ru.spbstu.collections.persistent

import java.util.*

/**
 * Created by Kopcap on 25.07.2016.
 */
data class BinaryHeapNode<E>(val max: E, val subNodes: SList<BinaryHeapNode<E>>? = null, val cmp: Comparator<E>) {
    internal operator fun E.compareTo(that: E) = cmp.compare(this, that)

    internal fun maxx(lhv: E?, rhv: E?) =
        when {
            lhv == null -> rhv
            rhv == null -> lhv
            lhv >= rhv -> lhv
            else -> rhv
        }
    internal fun nextMax(): E? = subNodes.foldLeft<BinaryHeapNode<E>, E?>(null){ acc, heap -> maxx(acc, heap.max) }

    internal fun toHeap(): BinaryHeap<E>? =
            if(subNodes == null) null
            else BinaryHeap(
                    nextMax()!!, // guaranteed not to fail for non-empty subNodes
                    subNodes.reverse(),
                    cmp
            )
}
fun<E> BinaryHeapNode(element: E, cmp: Comparator<E>) = BinaryHeapNode(element, null, cmp)
infix fun<E> BinaryHeapNode<E>.merge(that: BinaryHeapNode<E>) =
        if(this.max > that.max) this.copy(subNodes = SList(that, subNodes))
        else that.copy(subNodes = SList(this, subNodes))

internal infix fun<E> BinaryHeapNode<E>?.mergeWithCarry(that: BinaryHeapNode<E>?): Pair<BinaryHeapNode<E>?, BinaryHeapNode<E>?> =
    if(this == null) Pair(that, null)
    else if(that == null) Pair(this, null)
    else Pair(null, this merge that)

data class BinaryHeap<E>(val max: E, val nodes: SList<BinaryHeapNode<E>?>?, val cmp: Comparator<E>) {
    internal operator fun E.compareTo(that: E) = cmp.compare(this, that)
    internal inline fun maxx(v0: E, v1: E) = if(v0 > v1) v0 else v1
}

internal fun<E> Iterator<E>.nextOrNull(): E? = if(hasNext()) next() else null

infix fun<E> BinaryHeap<E>?.merge(that: BinaryHeap<E>?): BinaryHeap<E>? {
    this ?: return that
    that ?: return this

    var nodes: SList<BinaryHeapNode<E>?>? = null
    var carry: BinaryHeapNode<E>? = null
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

fun<E> BinaryHeap<E>?.popMax(): BinaryHeap<E>? {
    this ?: return null

    val maxTree = nodes.foldLeft<BinaryHeapNode<E>?, BinaryHeapNode<E>?>(null){ acc, t ->
        when {
            acc == null -> t
            t == null -> acc
            (t.max > acc.max) -> t
            else -> acc
        }
    }

    return (this.copy(nodes = nodes.removeAt { it === maxTree }) merge maxTree?.toHeap())
}
