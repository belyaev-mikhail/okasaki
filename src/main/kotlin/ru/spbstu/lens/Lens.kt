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

infix fun <A, B, C> Lens<A, B>.zip(rhv: Lens<A, C>): Lens<A, Pair<B, C>> = this.let { lhv ->
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
                get = { lens.map { it(this) } },
                set = { params ->
                    lens.zip(params).fold(this) { thisRef, lp ->
                        val (l, p) = lp
                        l(thisRef, p)
                    }
                }
        )

inline fun <A, B> Lens<A, B>.choice(alt: Lens<A, B>, crossinline func: (A) -> Boolean): Lens<A, B> =
        this.let { main ->
            Lens(
                    get = { if (func(this)) main(this) else alt(this) },
                    set = { newValue ->
                        if (func(this)) main(this, newValue) else alt(this, newValue)
                    }
            )
        }

inline fun <A> Lens<A, A>.doWhile(crossinline predicate: A.() -> Boolean): Lens<A, A> =
        this.let { outer ->
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
        this.let { outer ->
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

fun <A, B> Lens<A, B>.preserveUniques() = this.let { lens ->
    lens.copy(
            set = { param -> if (lens(this) === param) this else lens(this, param) }
    )
}

fun <A, B> Lens<A, B>.nullable(): Lens<A?, B?> = this.let { outer ->
    Lens(
            get = { if (this != null) outer(this) else null },
            set = { p -> if (this != null && p != null) outer(this, p) else null }
    )
}

fun <T> idLens(): Lens<T, T> = Lens(get = { this }, set = { it })
val incLens: Lens<Int, Int> = Lens(get = { this + 1 }, set = { it - 1 })
