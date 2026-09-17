package simulation

import critters.CritterStateView
import critters.world.Territory
import org.openrndr.math.Vector2

interface World {
    fun territory(pos: Vector2): Territory
    fun occupied(pos: Vector2): Boolean
    fun occupant(pos: Vector2): CritterStateView?
}

val SimulationController.world: World
    get() = object : World {
        override fun territory(pos: Vector2): Territory =
            mapPosition(pos)?.let { state.territory(it) } ?: critters.world.VOID

        override fun occupied(pos: Vector2): Boolean =
            mapPosition(pos)?.let { state.occupant(it) != null } ?: false

        override fun occupant(pos: Vector2): CritterStateView? =
            mapPosition(pos)?.let { state.occupant(it) }
    }
