package ru.spbstu.collections.persistent

import kotlinx.Warnings
import java.util.*

object HamtScope {
    // clojure-style persistent vector is just an implicit segment tree with branching factor of 32
    internal const val BF = 32
    internal const val BINARY_DIGITS = 5 // == log2(BF); number of binary digits needed for one BF digit
    internal const val DIGITS_MASK = (BF - 1) // == '1' bit repeated BINARY_DIGITS times; 0x1F for BF = 32

    @Suppress(Warnings.NOTHING_TO_INLINE)
    internal inline fun logBFceil(v: Int) = (log2ceil(v) - 1) / BINARY_DIGITS + 1
    @Suppress(Warnings.NOTHING_TO_INLINE)
    internal inline fun logBFfloor(v: Int) = log2floor(v) / BINARY_DIGITS
    @Suppress(Warnings.NOTHING_TO_INLINE)
    internal inline fun powBF(v: Int) = 1 shl (v * BINARY_DIGITS)

    internal val Int.greaterPowerOfBF: Int
        get() = powBF(logBFceil(this))
    internal val Int.lesserPowerOfBF: Int
        get() = if (this == 0) 0 else powBF(logBFfloor(this))

    internal fun <E> Array<E>.immSet(index: Int, element: E) =
            this.copyOf().apply { set(index, element) }

    internal val Int.popcount: Int
            get() = Integer.bitCount(this)
}

data class Hamt<E> internal constructor(val root: HamtNode<E>, val size: Int) {
}

internal data class HamtNode<E>(
        val bitMask: Int,
        val storage: Array<Any?> = Array(32){ null }
) {
    override fun equals(other: Any?) = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}