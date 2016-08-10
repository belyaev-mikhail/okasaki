package ru.spbstu.lens

import java.util.*


data class Lens<O, R>(val get: O.() -> R, val set: O.(R) -> O) {
    inline fun O.mutate(f: (R) -> R) = set(f(get()))
    operator fun invoke(receiver: O) = receiver.get()
    operator fun invoke(receiver: O, value: R) = receiver.set(value)
    inline operator fun invoke(receiver: O, mutator: (R) -> R) = receiver.mutate(mutator)
}

operator fun <A, B, C> Lens<A, B>.plus(inner: Lens<B, C>): Lens<A, C> = this.let { outer ->
    Lens(
            get = { this.(outer.get)().(inner.get)() },
            set = { innerValue ->
                val outerValue = outer(this)
                outer(this, inner(outerValue, innerValue))
            }
    )
}

fun <A, B, R> Lens<A, B>.bimap(fwd: (B) -> R, bwd: (R) -> B) = this.let { outer ->
    Lens<A, R>(
            get = { fwd(outer(this)) },
            set = { param -> outer(this, bwd(param)) }
    )
}

infix fun <A, B, C> Lens<A, B>.zip(rhv: Lens<A, C>): Lens<A, Pair<B, C>> = this.let {
    lhv ->
        Lens(
                get = { Pair(lhv(this), rhv(this)) },
                set = { pair ->
                    val (l, r) = pair
                    rhv(lhv(this, l), r)
                }
        )
}

fun <A, B> group(vararg lens: Lens<A, B>): Lens<A, List<B>> =
        Lens(
                get = { lens.map { (it.get)() } },
                set = { params ->
                    lens.zip(params).fold(this) {
                        thisRef, lp ->
                        val (l, p) = lp
                        thisRef.(l.set)(p)
                    }
                }
        )

inline fun <A, B> Lens<A, B>.choice(alt: Lens<A, B>, crossinline func: (A) -> Boolean): Lens<A, B> =
        let { main ->
            Lens(
                    get = { if (func(this)) (main.get)() else (alt.get)() },
                    set = { newValue ->
                        if (func(this)) (main.set)(newValue) else (alt.set)(newValue)
                    }
            )
        }

inline fun <A> Lens<A, A>.doWhile(crossinline predicate: A.() -> Boolean): Lens<A, A> =
        let { outer ->
            Lens(
                    get = {
                        var self = this
                        while (self.predicate()) self = self.get()
                        self
                    },
                    set = { newValue ->
                        val backStack: Stack<A> = Stack()
                        var self = this
                        while (self.predicate()) {
                            backStack.push(self)
                            self = self.get()
                        }
                        self.set(newValue)
                        while (!backStack.empty()) {
                            self = backStack.pop().set(self)
                        }
                        self
                    }
            )
        }

inline fun <A> Lens<A, A>.doWhileWithIndex(crossinline predicate: A.(Int) -> Boolean): Lens<A, A> =
        let { outer ->
            Lens(
                    get = {
                        var self = this
                        var index = 0
                        while (self.predicate(index)) {
                            self = self.get()
                            ++index
                        }
                        self
                    },
                    set = { newValue ->
                        val backStack: Stack<A> = Stack()
                        var self = this
                        var index = 0
                        while (self.predicate(index)) {
                            backStack.push(self)
                            self = self.get()
                            index++
                        }
                        self.set(newValue)
                        while (!backStack.empty()) {
                            self = backStack.pop().set(self)
                        }
                        self
                    }
            )
        }

fun <A, B> Lens<A, B>.preserveUniques() =
        Lens(
                get = this.get,
                set = { param -> get().let { if (it === param) this else this@preserveUniques(this, param) } }
        )

fun <A, B> Lens<A, B>.nullable(): Lens<A?, B?> = this.let{outer ->
        Lens(
                get = { if(this != null) outer(this) else null },
                set = { p -> if(this != null && p != null) outer(this, p) else null }
        )
}

fun <T> idLens(): Lens<T, T> = Lens(get = { this }, set = { it })
val incLens: Lens<Int, Int> = Lens(get = { this + 1 }, set = { it - 1 })

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
