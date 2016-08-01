package ru.spbstu.collections.persistent

import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class PersistentVectorTest {
    val rand = Random()

    fun testSimple(size: Int, t: PersistentVector<Int>) {
        assertEquals(size, t.size)

        assertEquals(size + 2, t.add(50).add(13).size)
    }

    @Test
    fun testRandom() {
        testSimple(3, PersistentVector.ofCollection(listOf(1,2,3)))

        20.times{
            val size = rand.nextInt(50000) + 1
            val t = rand.ints(size.toLong()).toArray()

            val sl = PersistentVector.ofCollection(t.toList())

            for(i in (0..size-1)) {
                assertEquals(t[i], sl[i])
            }

            testSimple(size, sl)
        }


    }

}