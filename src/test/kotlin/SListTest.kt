import org.junit.Test
import ru.spbstu.collections.persistent.*
import java.util.*
import kotlin.test.assertEquals

/**
 * Created by belyaev on 7/20/16.
 */
class SListTest {

    fun testSimple(size: Int, t: SList<Int>?) {
        assertEquals(t?.head, t[0])
        assertEquals(size, t.size)
        assertEquals(t, t.reverse().reverse())
        assertEquals(t, t.splitRevAt(43).let { mergeRev(it.first, it.second) })
        assertEquals(t[size - 1], t.reverse()?.head)

        val tr = t.reverse()
        (0..(size-1)).forEach { assertEquals(t[it], tr[size - 1 - it]) }

        assertEquals(t.addAll(size, t), t + t)
    }

    @Test
    fun testRandom() {
        val rand = Random()

        20.times{
            val size = rand.nextInt(3) + 1
            val t = rand.ints(size.toLong()).toArray()

            val sl = SList.ofCollection(t.toList())

            testSimple(size, sl)
        }

        testSimple(0, null)
        testSimple(1, SList(2))


    }
}