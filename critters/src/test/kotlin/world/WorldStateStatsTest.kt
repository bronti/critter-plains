package world

import critters.critter.Critter
import critters.world.GameMapBuilder
import critters.world.Position
import critters.world.WorldState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldStateStatsTest {

    private fun blankMap(size: Int = 12) = GameMapBuilder(size, size).build()

    @Test
    fun `stats on an empty world reports zero population and zero hunger`() {
        val world = WorldState(blankMap(), initialPlacement = mapOf())
        val stats = world.stats()
        assertEquals(0, stats.time)
        assertEquals(0, stats.population)
        assertEquals(0.0, stats.avgHunger, 0.0)
    }

    @Test
    fun `stats reports population size and hunger within the critters' initial range`() {
        val critters = (0 until 5).associate { i -> Critter(name = i, pos = Position(i, 0)) to Position(i, 0) }
        val world = WorldState(blankMap(), critters)

        val stats = world.stats()
        assertEquals(0, stats.time)
        assertEquals(5, stats.population)
        assertTrue("avgHunger ${stats.avgHunger} should be within the 1..20 initial hunger range", stats.avgHunger in 1.0..20.0)
    }

    @Test
    fun `time advances after a tick`() {
        val critters = mapOf(Critter(name = 0, pos = Position(0, 0)) to Position(0, 0))
        val world = WorldState(blankMap(), critters)

        world.tick()

        assertEquals(1, world.stats().time)
    }
}
