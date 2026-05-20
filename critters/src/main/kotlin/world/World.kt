package critters.world

import critters.critter.CritterName
import critters.critter.TimeStamp
import critters.critter.Critter
import critters.critter.act

interface World {
    interface Observable {
        fun territory(pos: Position): Territory
        fun occupant(pos: Position): CritterName?
        fun occupied(pos: Position) = occupant(pos) != null
    }

    interface Interactive {
        fun moveIfPossible(critter: Critter, target: Position): Boolean
        fun gatherIf(pos: Position, filter: (Terrain) -> Boolean): Terrain?
    }
}

class WorldState(initMap: MutableGameMap, initialPlacement: Map<Critter, Position>) {
    var time: TimeStamp = 0
        private set

    private var gameMap = MutableGameMap(initMap)
    private val placement = MutablePlacement(initialPlacement)

    fun territory(pos: Position) = gameMap.territory(pos)
    fun occupant(pos: Position) = placement.occupant(pos)

    fun tick() {
        ++time

        val worldBefore = observable()
        placement.forEach { (position, critter) ->
            check(critter.isAlive) { "The dead are not alive" }
            critter.experience(worldBefore, position)
            critter.tick()
        }

        val currentWorld = interactive()
        for (critter in placement.values.toList()) {
            if (critter.isAlive) {
                act(critter.intent(), critter, placement.position(critter)!!, currentWorld)
            }
        }

        placement.rip()
    }

    // todo: invalidate it after usage
    private fun WorldState.interactive() = object : World.Interactive {
        override fun moveIfPossible(critter: Critter, target: Position): Boolean {
            if (placement.empty(target) && gameMap.traversable(target)) {
                placement.move(critter, target)
                return true
            }
            return false
        }

        override fun gatherIf(pos: Position, filter: (Terrain) -> Boolean): Terrain? {
            val territory = gameMap.territory(pos)
            if (territory is Terrain && filter(territory)) {
                gameMap.replace(pos, Terrain.ground)
                return territory
            }
            return null
        }
    }
}

// todo: invalidate it after usage
private fun WorldState.observable() = object : World.Observable {
    override fun territory(pos: Position) = this@observable.territory(pos)
    override fun occupant(pos: Position) = this@observable.occupant(pos)?.name
}
