package ru.spbstu.collections.persistent

import ru.spbstu.collections.persistent.log2ceil

// clojure-style persistent vector is just an implicit segment tree with branching factor of 32
internal inline fun log32ceil(v: Int) = (log2ceil(v) - 1) / 5 + 1
internal inline fun log32floor(v: Int) = log2floor(v) / 5
internal inline fun pow32(v: Int) = 1 shl (v * 5)
internal val Int.greaterPowerOf32: Int
    get() = pow32(log32ceil(this))
internal val Int.lesserPowerOf32: Int
    get() = if(this == 0) 0 else pow32(log32floor(this))

internal fun<E> Array<E>.immSet(index: Int, element: E) =
    this.copyOf().apply { set(index, element) }

data class PersistentVector<E>(val size: Int, val root: PersistentVectorNode<E>) {
    val capacity: Int
        get() = size.greaterPowerOf32
    val depth: Int
        get() = log32ceil(capacity)

    fun resize(newSize: Int) =
            copy(size = newSize, root = root.getLeftNodeOfSize(capacity, newSize))

    fun set(index: Int, value: E) = copy(root = root.set(index, value, depth - 1))
    operator inline fun get(index: Int) = root.get(index, depth - 1)
}

data class PersistentVectorNode<E>(val data: Array<Any?> = Array<Any?>(32){null}) {
    internal fun getNode(index: Int): PersistentVectorNode<E> {
        val ret = data[index]
        if(ret == null) data[index] = PersistentVectorNode<E>()
        return ret as PersistentVectorNode<E>
    }

    internal fun getElement(index: Int): E? {
        return data[index] as? E?
    }

    internal fun getLeftNodeOfSize(yourSize: Int, size: Int): PersistentVectorNode<E> = with(this) {
        val adjustedSize = size.greaterPowerOf32
        if(yourSize == adjustedSize) this
        else if(yourSize <= adjustedSize) getNode(0).getLeftNodeOfSize(yourSize/32, size)
        else PersistentVectorNode<E>(data = Array(32){ if(it == 0) this else null })
    }

    fun get(index: Int, depthToGo: Int, curShift: Int = 0): E? =
            with(Bits) {
                val localIx = index[curShift, curShift + 5 - 1]
                assert(localIx < 32)
                if(data[localIx] == null) null
                else if(depthToGo == 0) getElement(localIx)
                else getNode(localIx).get(index, depthToGo - 1, curShift + 5)
            }

    fun set(index: Int, element: E, depthToGo: Int, curShift: Int = 0): PersistentVectorNode<E> =
            with(Bits) {
                val localIx = index[curShift, curShift + 5 - 1]
                assert(localIx < 32)
                if(depthToGo == 0) copy(data = data.immSet(localIx, element))
                else {
                    val node = getNode(localIx).set(index, element, depthToGo - 1, curShift + 5)
                    copy(data = data.immSet(localIx, node))
                }
            }
}