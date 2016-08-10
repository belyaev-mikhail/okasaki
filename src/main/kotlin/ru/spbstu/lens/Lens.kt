package ru.spbstu.lens

import java.util.*


data class Lens<O, R>(val get: O.() -> R, val set: O.(R) -> O) {
    inline fun O.mutate(f: (R) -> R) = set(f(get()))
    operator fun invoke(receiver: O) = receiver.get()
    operator fun invoke(receiver: O, value: R) = receiver.set(value)
    operator fun invoke(receiver: O, mutator: (R) -> R) = receiver.mutate(mutator)
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

fun <T> idLens(): Lens<T, T> = Lens(get = { this }, set = { it })
val incLens: Lens<Int, Int> = Lens(get = { this + 1 }, set = { it - 1 })

class Lenser<O, R>(val lens: Lens<O, R>){
    val set = lens.set
    val get = lens.get
}

class PairLenser<A, B> {
    val first: Lens<Pair<A, B>, A>  = Lens(get = { first },  set = { copy(first = it) })
    val second: Lens<Pair<A, B>, B> = Lens(get = { second }, set = { copy(second = it) })
}

val<O, A, B> Lenser<O, Pair<A, B>>.pairFirst: Lenser<O, A>
    get() = Lenser(lens + Lens(get = { first },  set = { copy(first = it) }))
val<O, A, B> Lenser<O, Pair<A, B>>.pairSecond: Lenser<O, B>
    get() = Lenser(lens + Lens(get = { second },  set = { copy(second = it) }))


class TripleLenser<A, B, C> {
    val first: Lens<Triple<A, B, C>, A>  = Lens(get = { first },  set = { copy(first = it) })
    val second: Lens<Triple<A, B, C>, B> = Lens(get = { second }, set = { copy(second = it) })
    val third: Lens<Triple<A, B, C>, C>  = Lens(get = { third },  set = { copy(third = it) })
}

val<O, A, B, C> Lenser<O, Triple<A, B, C>>.tripleFirst: Lenser<O, A>
    get() = Lenser(lens + Lens(get = { first },  set = { copy(first = it) }))
val<O, A, B, C> Lenser<O, Triple<A, B, C>>.tripleSecond: Lenser<O, B>
    get() = Lenser(lens + Lens(get = { second },  set = { copy(second = it) }))
val<O, A, B, C> Lenser<O, Triple<A, B, C>>.tripleThird: Lenser<O, C>
    get() = Lenser(lens + Lens(get = { third },  set = { copy(third = it) }))


class ArrayLenser<A> {
    operator fun get(index: Int): Lens<Array<A>, A> =
            Lens(
                    get = { this[index] },
                    set = { newElement -> this.copyOf().apply { set(index, newElement) } }
            )
}
