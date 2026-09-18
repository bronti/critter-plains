package critters.critter

import critters.world.Position
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
            val closestFood = memory.closestEdible()
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
    critter.memory.pointsOfInterest
        .map { it to critter.memory.lastSeen(it) }
        .sortedWith(
            compareBy<Pair<Position, TimeStamp?>> { it.second }
                .thenBy { from.distance(it.first) }
        )
        .lastOrNull()
        ?.let { moveOneStep(critter, from, it.first, world) }
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
    to.filter { critter.memory.traversable(it) }
        .randomOrNull()
        ?.let {
            world.moveIfPossible(critter, it)
        }
}

private fun shiftTowards(from: Int, to: Int) = if (to > from) min(to, from + 1) else max(to, from - 1)

private fun shiftByX(from: Position, to: Position) = Position(shiftTowards(from.x, to.x), from.y)

private fun shiftByY(from: Position, to: Position) = Position(from.x, shiftTowards(from.y, to.y))
