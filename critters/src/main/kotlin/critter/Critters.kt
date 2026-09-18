package critters.critter

import critters.world.Territory
import critters.world.Terrain
import critters.world.Position
import critters.world.World
import kotlin.math.max
import kotlin.math.min

typealias CritterName = Int
typealias TimeStamp = Int

class CritterNameGenerator {
    private var idCounter = 0
    fun nextName(): CritterName = idCounter++
}

class Critter(val name: CritterName, pos: Position) {

    val memory: Memory = Memory(pos, this)

    var isAlive = true
        private set
    val isDead: Boolean
        get() = !isAlive
    var hunger: Int = (1..20).random()
        private set

    // update info
    fun experience(world: World.Observable, pos: Position) {
        require(isAlive) { "Dead ones dont experience." }
        memory.update(world, pos)
    }

    fun tick() {
        require(isAlive) { "Dead ones dont tick." }
        hunger = min(40, hunger + 1)
        if (hunger >= 40) {
            isAlive = false
        }
    }

    fun edible(terrain: Terrain) = terrain == Terrain.FOOD

    fun eat(terrain: Terrain) {
        check(terrain == Terrain.FOOD) { "Cannot eat $terrain." }
        hunger = max(0, hunger - 10)
    }

    fun intent(): Intention {
        val closestEdible = memory.closestEdible()
        val distanceToEdible = closestEdible?.distance(memory.position)
        return when {
            hunger > 30 -> Intention.EAT
            hunger > 20 && distanceToEdible?.let { it < 3 } ?: false -> Intention.EAT
            distanceToEdible?.let { it > 10 } ?: false -> Intention.EAT
            else -> Intention.EXPLORE
        }
    }
}

// contract: critter always remembers its position correctly
// question: should critter know it's  absolute coordinates?
// if yes (current implementation) ->
//       - it's weird
//       - there is implicit a contract to uphold (the info about its position should always be remembered correctly)
//       - easy to implement
//       - we cannot teleport a critter to a position unknown to it thus invalidating its memory (not fun)
// if not ->
//       - each time a critter moves it needs to change Memory so it tracks where it is in relation to its memorized map
//       - critters real position is hold only in Population = good!
//       - confusing
//       - teleportation works
// todo: uninitialized critter possible leak
class Memory(initialPosition: Position, val critter: Critter) {
    var position: Position = initialPosition
        private set
    // todo: immutable to access
    val trace: MutableList<Position> = mutableListOf(initialPosition)
    var time: TimeStamp = 0
        private set

    // TODO: critter parameter
    private val sightRadius = 7

    private val territories = mutableMapOf<Position, Pair<Territory, TimeStamp>>()
    private val critters = mutableMapOf<Position, Pair<CritterName, TimeStamp>>()

    private val _pointsOfInterest = mutableSetOf<Position>()
    val pointsOfInterest: Set<Position> get() = _pointsOfInterest

    fun area(): Set<Position> = territories.keys + critters.keys

    fun update(world: World.Observable, myPosition: Position) {
        ++time
        position = myPosition
        trace.addLast(myPosition)
        positionsInRadius().forEach { position ->
            updateForPosition(world, position)
            territory(position)?.let { territory ->
                if (territory is Terrain && critter.edible(territory)) {
                    _pointsOfInterest.add(position)
                } else {
                    _pointsOfInterest.remove(position)
                }
                if (occupied(position)) {
                    _pointsOfInterest.add(position)
                    // todo: remove old position
                }
            }
        }
        _pointsOfInterest.remove(position)
    }

    fun territory(pos: Position) = territories[pos]?.first

    fun lastSeen(pos: Position) = territories[pos]?.second

    // todo: stale data
    fun occupied(pos: Position) = critters.containsKey(pos)

    fun traversable(pos: Position) = !occupied(pos) && territory(pos)?.traversable == true

    fun closestEdible(): Position? =
        closestAccessibleFiltered { _, territory, _ ->
            territory is Terrain && critter.edible(territory)
        }

    fun closestAccessibleFiltered(predicate: (Position, Territory, CritterName?) -> Boolean): Position? =
        closestFiltered { position, territory, occupant ->
            predicate(position, territory, occupant) && (occupant == null || occupant == critter.name)
        }

    fun closestFiltered(predicate: (Position, Territory, CritterName?) -> Boolean): Position? =
        territories.entries
            .filter { (pos, data) -> predicate(pos, data.first, critters[pos]?.first) }
            .minByOrNull { position.distance(it.key) }
            ?.key

    private fun updateForPosition(world: World.Observable, target: Position) {
        territories[target] = world.territory(target) to time

        if (world.occupied(target)) {
            val occupant = world.occupant(target)!!
            val outdatedLocation = critters.filter { it.value.first == occupant }.keys.singleOrNull()
            outdatedLocation?.let {
                critters.remove(it)
            }
            critters[target] = occupant to time
        } else {
            critters.remove(target)
        }
    }

    private fun positionsInRadius() =
        (position.x - sightRadius..position.x + sightRadius).flatMap { x ->
            ((position.y - sightRadius..position.y + sightRadius)).map { y -> Position(x, y) }
        }
}

enum class Intention {
    EXPLORE,
    COMMUNICATE,
    EAT,
    PROCREATE
}
