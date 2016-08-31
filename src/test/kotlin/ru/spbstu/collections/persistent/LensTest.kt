package ru.spbstu.collections.persistent

import org.junit.Test
import ru.spbstu.lens.Lenser
import ru.spbstu.lens.*
import kotlin.test.assertEquals

class LensTest {
    @Test
    fun testSimple() {
        val array = arrayOf(Triple(Pair(2, "Wadawad"), null, 3.15), Triple(Pair(4, "Hello"), 22, 3.15))

        val array2 = Lenser(array)[1].first.second.mutate { it + " world!" }

        assertEquals("Hello", array[1].first.second)
        assertEquals("Hello world!", array2[1].first.second)

        val array3 = Lenser(array)[0].run { third zip first.first }.mutate { Pair(it.first + 1.0, it.second - 5) }

        assertEquals(3.15, array[0].third)
        assertEquals(2, array[0].first.first)

        assertEquals(4.15, array3[0].third)
        assertEquals(-3, array3[0].first.first)

        val array4 = Lenser(array)[1].first.second[1].mutate { it.toUpperCase() }
        assertEquals("HEllo", array4[1].first.second)
    }

    @Test
    fun testPrisms() {
        val array = arrayOf(1,2,3,4,5)

        val array2 = Lenser(array).getOrNull(8).set(20)

        assertEquals(listOf(1,2,3,4,5,20,20,20,20), array2.asList())

        val array3 = Lenser(array).getOrNull(3).set(null)

        assertEquals(listOf(1,2,3,5), array3.asList())

        val array4 = Lenser(array).insertionAt(3).set(42)

        assertEquals(listOf(1,2,3,42,4,5), array4.asList())

        val array5 = Lenser(array).insertionAt(8).set(20)

        assertEquals(listOf(1,2,3,4,5,20,20,20,20), array5.asList())

    }
}