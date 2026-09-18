package simulation

import ai.Chronicler
import critters.Game
import critters.critter.CritterName
import critters.world.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openrndr.math.Vector2

// Minimum size MutableGameMap allows (width/height must be > 10).
private const val MAP_SIZE = 11
private const val CELL_SIZE = 15
private const val VIEWPORT_PX = MAP_SIZE * CELL_SIZE
private const val FPS = 5
private const val UPDATE_INTERVAL = 1.0 / FPS

class SimulationControllerTest {

    // Chronicler defaults to a disabled one so no test ever depends on ANTHROPIC_API_KEY or
    // makes real network calls, regardless of the local environment/.env.
    private fun newController(critterCount: Int) =
        SimulationController(
            game = Game(MAP_SIZE, MAP_SIZE, critterCount),
            initialFps = FPS,
            initialTime = 0.0,
            initialCellSize = CELL_SIZE,
            viewportWidthPx = VIEWPORT_PX,
            viewportHeightPx = VIEWPORT_PX,
            chronicler = Chronicler(forceDisabled = true),
        )

    // Viewport covers the whole map at CELL_SIZE, so viewportX/Y stay 0 without any pan/zoom,
    // making this the inverse of Camera.mapPosition for viewportX = viewportY = 0.
    private fun screenPositionOf(pos: Position): Vector2 =
        Vector2((pos.x + 0.5) * CELL_SIZE, (pos.y + 0.5) * CELL_SIZE)

    private fun allPositions(): List<Position> =
        (0 until MAP_SIZE).flatMap { x -> (0 until MAP_SIZE).map { y -> Position(x, y) } }

    private fun critterNames(controller: SimulationController): List<CritterName> =
        allPositions().mapNotNull { controller.state.occupant(it)?.name }

    private fun emptyPosition(controller: SimulationController): Position =
        checkNotNull(allPositions().firstOrNull { controller.state.occupant(it) == null }) {
            "No empty tile found on an $MAP_SIZE x $MAP_SIZE map."
        }

    private fun selectByName(controller: SimulationController, name: CritterName) {
        val critter = checkNotNull(controller.state.critter(name)) { "Critter $name not found in current state." }
        controller.selectAt(screenPositionOf(critter.memory.position))
    }

    private fun tick(controller: SimulationController, seconds: Double) = controller.update(seconds)

    @Test
    fun `clicking the only critter selects it`() {
        val controller = newController(critterCount = 1)
        val name = critterNames(controller).single()

        selectByName(controller, name)

        assertEquals(name, controller.selectedCritter?.name)
    }

    @Test
    fun `selected critter's hunger updates on every tick, not just at click time`() {
        val controller = newController(critterCount = 1)
        val name = critterNames(controller).single()
        selectByName(controller, name)
        val hungerAtSelection = checkNotNull(controller.selectedCritter).hunger

        tick(controller, UPDATE_INTERVAL)

        assertNotEquals(hungerAtSelection, controller.selectedCritter?.hunger)
    }

    @Test
    fun `selected critter's hunger mirrors the current world state across ticks`() {
        val controller = newController(critterCount = 1)
        val name = critterNames(controller).single()
        selectByName(controller, name)

        var seconds = 0.0
        repeat(5) {
            seconds += UPDATE_INTERVAL
            tick(controller, seconds)
            val groundTruthHunger = checkNotNull(controller.state.critter(name)) { "Critter $name should still be alive." }.hunger
            assertEquals(groundTruthHunger, controller.selectedCritter?.hunger)
        }
    }

    @Test
    fun `deselect clears an active selection`() {
        val controller = newController(critterCount = 1)
        val name = critterNames(controller).single()
        selectByName(controller, name)
        assertEquals(name, controller.selectedCritter?.name)

        controller.deselect()

        assertNull(controller.selectedCritter)
    }

    @Test
    fun `deselect with nothing selected is a no-op`() {
        val controller = newController(critterCount = 1)
        assertNull(controller.selectedCritter)

        controller.deselect()

        assertNull(controller.selectedCritter)
    }

    @Test
    fun `clicking an empty tile clears the selection`() {
        val controller = newController(critterCount = 1)
        val name = critterNames(controller).single()
        selectByName(controller, name)

        controller.selectAt(screenPositionOf(emptyPosition(controller)))

        assertNull(controller.selectedCritter)
    }

    @Test
    fun `clicking a different critter switches the selection`() {
        val controller = newController(critterCount = 3)
        val names = critterNames(controller)
        check(names.size >= 2) {
            "Need at least two critters for this test; only ${names.size} were placed" +
                " (extremely unlikely random-position collision)."
        }
        val (first, second) = names[0] to names[1]
        selectByName(controller, first)
        assertEquals(first, controller.selectedCritter?.name)

        val secondCritter = checkNotNull(controller.state.critter(second)) { "Critter $second not found in current state." }
        controller.selectAt(screenPositionOf(secondCritter.memory.position))

        assertEquals(second, controller.selectedCritter?.name)
    }

    @Test
    fun `selection auto-clears when the selected critter dies`() {
        val controller = newController(critterCount = 1)
        val name = critterNames(controller).single()
        selectByName(controller, name)
        assertEquals(name, controller.selectedCritter?.name)

        // Food is finite (blobs are placed once at Game init and never regenerate), which bounds
        // the sole critter's worst-case lifespan. Loop with a generous cap and assert termination
        // before it, rather than an exact tick count.
        val maxTicks = 3000
        var seconds = 0.0
        var ticksElapsed = 0
        while (controller.selectedCritter != null && ticksElapsed < maxTicks) {
            seconds += UPDATE_INTERVAL
            tick(controller, seconds)
            ticksElapsed++
        }

        assertNull("selection should auto-clear once the critter dies", controller.selectedCritter)
        assertTrue("expected the critter to die within $maxTicks ticks", ticksElapsed < maxTicks)
    }
}
