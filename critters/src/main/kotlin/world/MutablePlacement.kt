package critters.world

import critters.critter.Critter

interface Placement {
    fun occupant(pos: Position): Critter?
    fun position(critter: Critter): Position?
}

fun Placement.empty(pos: Position) = occupant(pos) == null
fun Placement.occupied(pos: Position) = occupant(pos) != null

// todo: fix concurrent modification
class MutablePlacement private constructor(
    private val population: MutableMap<Critter, Position>,
    private val positions: MutableMap<Position, Critter>
) : Placement, Map<Position, Critter> by positions {

    constructor(initial: Map<Critter, Position>) : this(
        initial.toMutableMap(),
        initial.entries.associate { (k, v) -> v to k }.toMutableMap()
    ) {
        require(initial.values.distinct().size == initial.size) { "Two critters in the same place." }
        require(initial.keys.distinct().size == initial.size) { "A critter in two places at once." }
    }

    override fun occupant(pos: Position) = positions[pos]
    override fun position(critter: Critter) = if (critter in population) population[critter] else null

    fun rip() {
        val theDead = filter { it.value.isDead }.values.toList()
        theDead.forEach {
            val pos = population[it]
            population.remove(it)
            positions.remove(pos)
        }
    }

    fun move(critter: Critter, pos: Position) {
        check(critter in population.keys)
        check(empty(pos))

        remove(critter)
        put(pos, critter)
    }

    fun put(pos: Position, critter: Critter) {
        check(critter !in population.keys)
        check(empty(pos))

        population[critter] = pos
        positions[pos] = critter
    }

    fun remove(critter: Critter) {
        check(population.keys.contains(critter))
        positions.remove(population[critter])
        population.remove(critter)
    }
}
