package critters

import critters.critter.Critter
import critters.critter.CritterNameGenerator
import critters.world.*

// todo: make the seed fixed
// todo: setup logging
// todo: store world snapshots
class Game(val mapWidth: Int, val mapHeight: Int, critterCount: Int) {
    private val nameGenerator = CritterNameGenerator()
    private val world: WorldState

    // todo: remove World generation logic from Game class
    init {
        fun randomXCoordinate() = (0 until mapWidth).random()
        fun randomYCoordinate() = (0 until mapHeight).random()
        fun randomPosition() = Position(randomXCoordinate(), randomYCoordinate())

        val initialTerrains = listOf(Terrain.SOIL, Terrain.SOIL, Terrain.SOIL, Terrain.FOOD)

        // todo: generate map via separate builder
        val map = GameMapBuilder(mapWidth, mapHeight)
            .apply {
                repeat(20) {
                    addBlob(initialTerrains.random())
                }
            }.build()
        val critters = (1..critterCount)
            .map { randomPosition() }
            .distinct()
            .associateBy { Critter(nameGenerator.nextName(), it) }
        world = WorldState(map, critters)
    }

    fun tick() = world.tick()

    fun stats() = world.stats()

    fun stateView() = object : GameStateView {
        override fun territory(pos: Position) = world.territory(pos)
        override fun occupant(pos: Position) = world.occupant(pos)?.view()
    }
}
