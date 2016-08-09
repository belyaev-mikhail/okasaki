package ru.spbstu.collections.persistent

import java.util.*

data class BinomialHeapNode<E>(val max: E, val subNodes: SList<BinomialHeapNode<E>>? = null)

data class BinomialHeap<E> internal constructor(
        val nodes: SList<BinomialHeapNode<E>?>? = null,
        internal val cmpOpt: Comparator<E?>,
        val max: E? = nodes.foldLeft(null as E?) { e, t -> cmpOpt.max(e, t?.max) }
) {
    internal operator fun E.compareTo(that: E) = cmpOpt.compare(this, that)
    internal fun BinomialHeapNode<E>.asHeap() =
            if (subNodes == null) BinomialHeap(cmpOpt = cmpOpt)
            else BinomialHeap(subNodes.reverse(), cmpOpt)

    internal infix fun BinomialHeapNode<E>.merge(that: BinomialHeapNode<E>) =
            if (this.max > that.max) this.copy(subNodes = SList(that, subNodes))
            else that.copy(subNodes = SList(this, subNodes))

    internal infix fun BinomialHeapNode<E>?.mergeWithCarry(that: BinomialHeapNode<E>?):
            Pair<BinomialHeapNode<E>?, BinomialHeapNode<E>?> =
            if (this == null) Pair(that, null)
            else if (that == null) Pair(this, null)
            else Pair(null, this merge that)

}

infix fun <E> BinomialHeap<E>.merge(that: BinomialHeap<E>): BinomialHeap<E> {
    this.max ?: return that
    that.max ?: return this

    var nodes: SList<BinomialHeapNode<E>?>? = null
    var carry: BinomialHeapNode<E>? = null
    val thisIt = this.nodes.iterator()
    val thatIt = that.nodes.iterator()
    while (thisIt.hasNext() || thatIt.hasNext()) {
        val thisVal = thisIt.nextOrNull()
        val thatVal = thisIt.nextOrNull()
        var (sum0, carry0) = thisVal mergeWithCarry thatVal
        var (sum1, carry1) = sum0 mergeWithCarry carry
        nodes = SList(sum1, nodes)
        carry = (carry0 mergeWithCarry carry1).first
    }
    return copy(max = cmpOpt.max(this.max, that.max), nodes = nodes.reverse())
}

fun <E> binomialHeapOf(element: E, cmp: Comparator<E>) =
        BinomialHeap(SList(BinomialHeapNode(element)), cmp.nullsFirst())

fun <E> binomialHeapOf(cmp: Comparator<E>) =
        BinomialHeap(sListOf(), cmp.nullsFirst())

fun <E> binomialHeapOf(element: E, cmp: (E, E) -> Int) =
        BinomialHeap(sListOf(BinomialHeapNode(element)), Comparator(cmp).nullsFirst())

fun <E> binomialHeapOf(vararg element: E, cmp: Comparator<E>) =
        element.map { binomialHeapOf(it, cmp) }.reduce { lh, rh -> lh merge rh }

fun <E: Comparable<E>> binomialHeapOf(element: E) = binomialHeapOf(element, cmp = Comparator.naturalOrder())

fun <E: Comparable<E>> binomialHeapOf() = binomialHeapOf(cmp = Comparator.naturalOrder<E>())

fun <E: Comparable<E>> binomialHeapOf(vararg element: E) = binomialHeapOf(*element, cmp = Comparator.naturalOrder<E>())

fun <E> BinomialHeap<E>.add(element: E) = this merge BinomialHeap(null, cmpOpt, element)

fun <E> BinomialHeap<E>.addAll(that: BinomialHeap<E>) = this merge that

fun <E> BinomialHeap<E>.popMax(): BinomialHeap<E> {
    this.max ?: return this

    val maxTree = nodes.foldLeft<BinomialHeapNode<E>?, BinomialHeapNode<E>?>(null) { acc, t ->
        when {
            acc == null -> t
            t == null -> acc
            (t.max > acc.max) -> t
            else -> acc
        }
    }
    maxTree ?: return BinomialHeap(cmpOpt = cmpOpt)
    val maxless = BinomialHeap(nodes = nodes.removeAt { it === maxTree }, cmpOpt = cmpOpt)
    val maxHeap = maxTree.asHeap()

    return maxless merge maxHeap
}
