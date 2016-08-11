package ru.spbstu.lens

data class Lenser<O, R>(val obj: O, val lens: Lens<O, R>){
    fun set(value: R) = lens(obj, value)
    fun get() = lens(obj)
    inline fun mutate(mutator: (R) -> R) = lens(obj, mutator)

    @JvmName("smartCopy")
    fun<X> copy(obj: O = this.obj, lens: Lens<O, X>) = Lenser(obj, lens)
}

fun<O> Lenser(obj: O) = Lenser(obj, idLens())

operator fun<O, A, B> Lenser<O, A>.plus(lens: Lens<A, B>) = copy(lens = this.lens + lens)
operator fun<O, A, B> Lenser<O, A>.plus(that: Lenser<A, B>) = copy(lens = this.lens + that.lens)

infix fun<O, A, B> Lenser<O, A>.zip(that: Lenser<O, B>) = copy(lens = this.lens zip that.lens)

object PairLenses {
    fun<A, B> first(): Lens<Pair<A, B>, A> = Lens(get = { first },  set = { copy(first = it) })
    fun<A, B> second(): Lens<Pair<A, B>, B> = Lens(get = { second },  set = { copy(second = it) })
}

val<O, A, B> Lenser<O, Pair<A, B>>.first: Lenser<O, A>
    @JvmName("pairFirst")
    get() = this + PairLenses.first<A, B>()
val<O, A, B> Lenser<O, Pair<A, B>>.second: Lenser<O, B>
    @JvmName("pairSecond")
    get() = this + PairLenses.second<A, B>()

object TripleLenses {
    fun<A, B, C> first(): Lens<Triple<A, B, C>, A> = Lens(get = { first },  set = { copy(first = it) })
    fun<A, B, C> second(): Lens<Triple<A, B, C>, B> = Lens(get = { second },  set = { copy(second = it) })
    fun<A, B, C> third(): Lens<Triple<A, B, C>, C> = Lens(get = { third },  set = { copy(third = it) })
}

val<O, A, B, C> Lenser<O, Triple<A, B, C>>.first: Lenser<O, A>
    @JvmName("tripleFirst")
    get() = this + TripleLenses.first<A, B, C>()
val<O, A, B, C> Lenser<O, Triple<A, B, C>>.second: Lenser<O, B>
    @JvmName("tripleSecond")
    get() = this + TripleLenses.second<A, B, C>()
val<O, A, B, C> Lenser<O, Triple<A, B, C>>.third: Lenser<O, C>
    @JvmName("tripleThird")
    get() = this + TripleLenses.third<A, B, C>()

@JvmName("arrayGet")
operator fun<O, A> Lenser<O, Array<A>>.get(index: Int): Lenser<O, A> =
        this + Lens(
                get = { this[index] },
                set = { newElement -> this.copyOf().apply { set(index, newElement) }}
        )

object StringLenses {
    fun slice(range: IntRange): Lens<String, String> =
            Lens(get = { slice(range) }, set = { newContents -> replaceRange(range, newContents) })

    fun get(index: Int): Lens<String, Char> =
            Lens(get = { get(index) }, set = { newChar -> replaceRange(index, index + 1, newChar.toString()) })
}

@JvmName("stringSlice")
fun<O> Lenser<O, String>.slice(range: IntRange) = this + StringLenses.slice(range)

@JvmName("stringGet")
operator fun<O> Lenser<O, String>.get(index: Int) = this + StringLenses.get(index)
