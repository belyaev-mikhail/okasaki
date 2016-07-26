package ru.spbstu.collections.persistent

import ru.spbstu.collections.persistent.Bits.get
import ru.spbstu.collections.persistent.log2ceil

// clojure-style persistent vector is just an implicit segment tree with branching factor of 32
internal inline fun log32ceil(v: Int) = (log2ceil(v) - 1) / 5 + 1

internal inline fun log32floor(v: Int) = log2floor(v) / 5
internal inline fun pow32(v: Int) = 1 shl (v * 5)
internal val Int.greaterPowerOf32: Int
    get() = pow32(log32ceil(this))
internal val Int.lesserPowerOf32: Int
    get() = if (this == 0) 0 else pow32(log32floor(this))

internal fun<E> Array<E>.immSet(index: Int, element: E) =
        this.copyOf().apply { set(index, element) }

data class PersistentVector<E>(val size: Int, val root: PersistentVectorNode<E>) {
    val capacity: Int
        get() = size.greaterPowerOf32
    val depth: Int
        get() = log32ceil(capacity)

    fun resize(newSize: Int) =
            copy(size = newSize, root = root.getLeftNodeOfSize(capacity, newSize))

    fun set(index: Int, value: E) =
            if (index > size) throw IndexOutOfBoundsException()
            else copy(root = root.set(index, value, depth - 1))

    operator inline fun get(index: Int) = root.get(index, depth - 1)
}

data class PersistentVectorNode<E>(val data: Array<Any?> = Array<Any?>(32) { null }) {
    inline fun Int.adjusted() = with(Bits){ this@adjusted and 0x1F }

    internal fun getNode(index: Int): PersistentVectorNode<E> = with(Bits) {
        val realIndex = index.adjusted()
        val ret = data[realIndex]
        if (ret == null) return PersistentVectorNode()
        return ret as PersistentVectorNode<E>
    }

    internal fun getElement(index: Int): E? = with(Bits) { data[index[0, 4]] as? E? }

    internal fun getLeftNodeOfSize(yourSize: Int, size: Int): PersistentVectorNode<E> = with(this) {
        val adjustedSize = size.greaterPowerOf32
        if (yourSize == adjustedSize) this
        else if (yourSize <= adjustedSize) getNode(0).getLeftNodeOfSize(yourSize / 32, size)
        else PersistentVectorNode<E>(data = Array(32) { if (it == 0) this else null })
    }

    fun get(index: Int, depthToGo: Int): E? =
            with(Bits) {
                if (data[index.adjusted()] == null) null
                else if (depthToGo == 0) getElement(index)
                else getNode(index).get(index ushr 5, depthToGo - 1)
            }

    fun set(index: Int, element: E, depthToGo: Int): PersistentVectorNode<E> =
            with(Bits) {
                val localIx = index.adjusted()
                if (depthToGo == 0) copy(data = data.immSet(localIx, element))
                else {
                    val node = getNode(localIx).set(index ushr 5, element, depthToGo - 1)
                    copy(data = data.immSet(localIx, node))
                }
            }
}