package critter

import critters.critter.Critter
import critters.critter.Intention
import critters.world.Position
import critters.world.Terrain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CritterTest {

    private fun newCritter() = Critter(name = 0, pos = Position(0, 0))

    @Test
    fun `starts alive with hunger in the expected range`() {
        val critter = newCritter()
        assertTrue(critter.isAlive)
        assertFalse(critter.isDead)
        assertTrue(critter.hunger in 1..20)
    }

    @Test
    fun `tick increases hunger by one`() {
        val critter = newCritter()
        val before = critter.hunger
        critter.tick()
        assertEquals(minOf(40, before + 1), critter.hunger)
    }

    @Test
    fun `critter dies once hunger reaches 40`() {
        val critter = newCritter()
        while (critter.isAlive) critter.tick()
        assertEquals(40, critter.hunger)
        assertFalse(critter.isAlive)
    }

    @Test
    fun `dead critters cannot tick again`() {
        val critter = newCritter()
        while (critter.isAlive) critter.tick()
        assertThrows(IllegalArgumentException::class.java) { critter.tick() }
    }

    @Test
    fun `eating food reduces hunger by ten, floored at zero`() {
        val critter = newCritter()
        repeat(5) { critter.tick() } // guarantee hunger >= 6 so the floor isn't trivially hit
        val before = critter.hunger
        critter.eat(Terrain.FOOD)
        assertEquals(maxOf(0, before - 10), critter.hunger)
    }

    @Test
    fun `eating non-food terrain is rejected`() {
        val critter = newCritter()
        assertThrows(IllegalStateException::class.java) { critter.eat(Terrain.SOIL) }
    }

    @Test
    fun `only food terrain is edible`() {
        val critter = newCritter()
        assertTrue(critter.edible(Terrain.FOOD))
        assertFalse(critter.edible(Terrain.SOIL))
    }

    @Test
    fun `intent is EAT when hunger is above ten, EXPLORE otherwise`() {
        val critter = newCritter()
        while (critter.hunger <= 10) critter.tick()
        assertEquals(Intention.EAT, critter.intent())

        critter.eat(Terrain.FOOD)
        while (critter.hunger > 10) critter.eat(Terrain.FOOD)
        assertEquals(Intention.EXPLORE, critter.intent())
    }
}
