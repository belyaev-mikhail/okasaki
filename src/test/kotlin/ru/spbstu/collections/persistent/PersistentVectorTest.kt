package ru.spbstu.collections.persistent

import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class PersistentVectorTest {
    fun testSimple(size: Int, t: PersistentVector<Int>) {
        // TODO
        println(t[0])
        println(t[size-1])
    }

    @Test
    fun testRandom() {
        val rand = Random()

        testSimple(3, PersistentVector.ofCollection(listOf(1,2,3)))

        20.times{
            val size = rand.nextInt(50) + 1
            val t = rand.ints(size.toLong()).toArray()

            val sl = PersistentVector.ofCollection(t.toList())

            for(i in (0..size-1)) {
                println(t[i])
                println(sl[i])
                assertEquals(t[i], sl[i])
            }

            testSimple(size, sl)
        }


    }

}