package critters.critter

import critters.world.Position
import critters.world.Terrain
import critters.world.World
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// todo: make into a driver class
fun act(intent: Intention, critter: Critter, from: Position, world: World.Interactive) {
    val memory = critter.memory
    require(memory.position == from) // todo
    when (intent) {
        Intention.EXPLORE -> exploreAction(critter, from, world)
        Intention.EAT -> {
            val closestFood = memory.closestFiltered { _, location, occupant ->
                location is Terrain && critter.edible(location) && (occupant == null || occupant == critter.name)
            }
            if (closestFood == null) exploreAction(critter, from, world)
            else goEatAction(critter, from, closestFood, world)
        }
        else -> throw NotImplementedError()
    }
}

fun goEatAction(critter: Critter, from: Position, closestFood: Position, world: World.Interactive) {
    if (from == closestFood) {
        world
            .gatherIf(from, critter::edible)
            ?.let { critter.eat(it) }
    } else {
        moveOneStep(critter, from, closestFood, world)
    }
}

fun exploreAction(critter: Critter, from: Position, world: World.Interactive) {
    val coordinateShifts = listOf(-1, +1)
    val positions = coordinateShifts
        .flatMap { shift ->
            listOf(
                Position(from.x + shift, from.y),
                Position(from.x, from.y + shift),
            )
        }
        .shuffled()
    moveToOneOf(critter, positions, world)
}

// todo: pathfinding
private fun moveOneStep(critter: Critter, from: Position, to: Position, world: World.Interactive) {
    if (to == from) return
    val xDist = abs(to.x - from.x)
    val yDist = abs(to.y - from.y)
    val alternatives = listOf(shiftByX(from, to), shiftByY(from, to))
        .apply { if (xDist <= yDist) reversed() }
    moveToOneOf(critter, alternatives, world)
}

private fun moveToOneOf(critter: Critter, to: List<Position>, world: World.Interactive) {
    to.firstOrNull { critter.memory.traversable(it) }
        ?.let {
            world.moveIfPossible(critter, it)
        }
}

private fun shiftTowards(from: Int, to: Int) = if (to > from) min(to, from + 1) else max(to, from - 1)

private fun shiftByX(from: Position, to: Position) = Position(shiftTowards(from.x, to.x), from.y)

private fun shiftByY(from: Position, to: Position) = Position(from.x, shiftTowards(from.y, to.y))
