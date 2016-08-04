@file:Suppress(Warnings.NOTHING_TO_INLINE)

package ru.spbstu.collections.persistent

import kotlinx.Warnings
import ru.spbstu.collections.persistent.HamtScope.BINARY_DIGITS
import java.util.*

import ru.spbstu.collections.persistent.HamtScope.DIGITS_MASK
import ru.spbstu.collections.persistent.HamtScope.popcount
import ru.spbstu.collections.persistent.HamtScope.immInsert
import ru.spbstu.collections.persistent.HamtScope.immSet

object HamtScope {
    // clojure-style persistent vector is just an implicit segment tree with branching factor of 32
    internal const val BF = 32
    internal const val BINARY_DIGITS = 5 // == log2(BF); number of binary digits needed for one BF digit
    internal const val DIGITS_MASK = (BF - 1) // == '1' bit repeated BINARY_DIGITS times; 0x1F for BF = 32

    internal inline fun logBFceil(v: Int) = (log2ceil(v) - 1) / BINARY_DIGITS + 1
    internal inline fun logBFfloor(v: Int) = log2floor(v) / BINARY_DIGITS
    internal inline fun powBF(v: Int) = 1 shl (v * BINARY_DIGITS)

    internal val Int.greaterPowerOfBF: Int
        get() = powBF(logBFceil(this))
    internal val Int.lesserPowerOfBF: Int
        get() = if (this == 0) 0 else powBF(logBFfloor(this))

    internal fun <E> Array<E>.immSet(index: Int, element: E) = this[index assignTo element]
    internal fun <E> Array<E>.immInsert(index: Int, element: E) = copyOf(size + 1).apply {
        this[index] = element
        System.arraycopy(this@immInsert, index, this, index + 1, size - index - 1)
    }

    internal val Int.popcount: Int
            get() = Integer.bitCount(this)
}

data class Hamt<E> internal constructor(val root: HamtNode<E>, val size: Int) {}

internal data class HamtNode<E>(
        val bitMask: Int = 0,
        val storage: Array<Any?> = Array(0){ null }
) {

    internal inline fun Int.digit(index: Int) = (this ushr (index * BINARY_DIGITS)) and DIGITS_MASK
    internal inline fun Int.toIndex() = (bitMask and (this - 1)).popcount
    internal inline fun Int.bitpos(index: Int) = 1 shl digit(index)

    @Suppress(Warnings.UNCHECKED_CAST)
    private inline fun forceNode(v: HamtNode<*>) = v as HamtNode<E>

    fun calcHash(e: E?) = Objects.hashCode(e)

    fun find(key: E, depth: Int, hash: Int = calcHash(key)): E? = with(Bits){
        val bit = 0.setBit(hash.digit(depth))
        if(bitMask and bit != 0) {
            val thing = storage[bit.toIndex()]
            when(thing) {
                is HamtNode<*> -> forceNode(thing).find(key, depth - 1, hash)
                else -> @Suppress(Warnings.UNCHECKED_CAST)(thing as? E)
            }
        }
        else null
    }

    fun put(key: E, depth: Int, hash: Int = calcHash(key)): HamtNode<E> = with(Bits){
        val bit = 0.setBit(hash.digit(depth))
        val index = bit.toIndex()
        if(bitMask and bit != 0) {
            val thing = storage[index]
            when(thing) {
                is HamtNode<*> ->
                    copy(
                        storage = storage.immSet(index, forceNode(thing).put(key, depth - 1, hash))
                    )
                else -> {
                    val existing = @Suppress(Warnings.UNCHECKED_CAST) (thing as E)
                    if(existing == key) this@HamtNode
                    else HamtNode<E>().put(key, depth - 1, hash).put(existing, depth - 1)
                }
            }
        } else {
            copy(bitMask = bitMask or bit, storage = storage.immInsert(index, key))
        }
    }

    override fun equals(other: Any?) = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}