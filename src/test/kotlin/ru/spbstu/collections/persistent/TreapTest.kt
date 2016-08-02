package ru.spbstu.collections.persistent

import org.junit.Test
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TreapTest {

    fun testSimple(t0: Treap<Int, Unit>?, t1: Treap<Int, Unit>?) {
        val intersection0 = t0 intersect t1
        for(e in intersection0) assert(e in t0 && e in t1)

        val intersection1 = t1 intersect t0
        for(e in intersection1) assert(e in t0 && e in t1)

        assertEquals(intersection0, intersection1)
        assertEquals(intersection0, intersection0 intersect intersection1)
        assertEquals(intersection0, intersection0 union intersection1)
        assertEquals(null, intersection0 difference intersection1)

        for(e in t0) assert(e in intersection0 && e in t0 && e in t1 || e !in intersection0)
        for(e in t1) assert(e in intersection0 && e in t0 && e in t1 || e !in intersection0)

        val union0 = t0 union t1
        for(e in union0) assert(e in t0 || e in t1)

        val union1 = t1 union t0
        for(e in union1) assert(e in t0 || e in t1)

        for(e in t0) assert(e in union0)
        for(e in t1) assert(e in union1)

        assertEquals(union0, union1)
        assertEquals(union1, (union0 intersect union1))
        assertEquals(union1, (union0 union union1))
        assertEquals(null, (union0 difference union1))

        for(e in intersection0) assert(e in union0)
        assertEquals(t0 intersect union0, t0)
        assertEquals(t1 intersect union0, t1)

        val difference0 = t0 difference t1
        for(e in difference0) assert(e in t0 && e !in t1)
        val difference1 = t1 difference t0
        for(e in difference1) assert(e in t1 && e !in t0)

        assertEquals(null, difference0 intersect difference1)
        assertEquals(null, (difference0 + difference1) intersect intersection0)
        assertEquals(difference0 + difference1 + intersection0, union0)

        val t0max = t0.max() ?: 0
        for(e in t0) assert(t0max >= e)

        val t0min = t0.min() ?: 0
        for(e in t0) assert(t0min <= e)
    }

    @Test
    fun testRandom() {
        val rand = Random()
        testSimple(null, null)
        testSimple(Treap(2,2,2,2,2), Treap(2))
        10.times{
            val r = rand.nextInt()
            testSimple(Treap(r), Treap(r))
            testSimple(Treap(r), Treap(-r))
        }

        10.times{
            val threshold = rand.nextInt(5000) + 1 // should not be zero
            val data0 = rand.ints(threshold.toLong(), -threshold, threshold).toArray()
            val data1 = rand.ints(threshold.toLong(), -threshold, threshold).toArray()
            val t0 = data0.fold(Treap<Int, Unit>()){ t, e -> t.add(e) }
            val t1 = data1.fold(Treap<Int, Unit>()){ t, e -> t.add(e) }

            for(e in data0) { assert(e in t0) }
            for(e in data1) { assert(e in t1) }
            testSimple(t0, t1)

            testSimple(t0, null)
            testSimple(null, t1)
            testSimple(t0, Treap(rand.nextInt(threshold)))
            testSimple(t0, Treap(rand.nextInt()))
            testSimple(t0, t1 - t0)
            testSimple(t0, t1 + t0)
        }

        10.times {
            val pool = Executors.newFixedThreadPool(8)
            val threshold = rand.nextInt(5000000) + 1 // should not be zero
            val data0 = rand.ints(threshold.toLong(), -threshold, threshold).toArray()
            val data1 = rand.ints(threshold.toLong(), -threshold, threshold).toArray()
            val t0 = data0.fold(Treap<Int, Unit>()){ t, e -> t.add(e) }
            val t1 = data1.fold(Treap<Int, Unit>()){ t, e -> t.add(e) }

            measureTimeMillis { t0.union(t1) }.let { println(it) }
            measureTimeMillis { t0.punion(t1, pool) }.let { println(it) }

            //val t3 = t0.pintersect(t1)
        }
    }

    @Test
    fun testHandCrafted() {
        val t = Treap(1,2,2,2,3,2)
        assertEquals(Treap(3,2,1), t)
        assertEquals(null, t - t)
        assertEquals(t, t + t)
        assertEquals(t, t intersect t)
        assert(2 in t)
        assert(5 !in t)

        val t23 = t - Treap(1)
        assert(1 !in t23)

        assertNotEquals(Treap(1,2,3,4), Treap(1,2,3))

    }

}