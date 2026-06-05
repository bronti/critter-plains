package critters

import critters.critter.Critter
import critters.critter.CritterName
import critters.critter.Memory
import critters.critter.TimeStamp
import critters.world.Position
import critters.world.Territory

interface GameStateView {
    fun territory(pos: Position): Territory
    fun occupant(pos: Position): CritterStateView?
}

data class CritterStateView(
    val name: CritterName,
    val memory: MemoryStateView,
    val isAlive: Boolean,
    var hunger: Int,
) {
    val isDead = !isAlive
}

abstract class MemoryStateView(
    val position: Position,
    val time: TimeStamp,
) {

    abstract fun territory(pos: Position): Territory?
    abstract fun occupied(pos: Position): Boolean
    abstract fun area(): Set<Position>
}

fun Critter.view() = CritterStateView(name, memory.view(), isAlive, hunger)
fun Memory.view() = object : MemoryStateView(position, time) {
    override fun territory(pos: Position) = this@view.territory(pos)
    override fun occupied(pos: Position) = this@view.occupied(pos)
    override fun area(): Set<Position> = this@view.area()
}
