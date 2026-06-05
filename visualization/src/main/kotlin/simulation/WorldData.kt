package simulation

import critters.world.Territory
import org.openrndr.math.Vector2

interface WorldData {
    fun territory(pos: Vector2): Territory
    fun occupied(pos: Vector2): Boolean
}
