package simulation

import critters.CritterStateView
import critters.Game
import critters.GameStateView
import critters.world.Position
import critters.world.Territory
import org.openrndr.math.Vector2

class SimulationController(private val game: Game, initialFps: Int, initialTime: Double, private val cellSize: Int) {

    val mapWidth: Int get() = game.mapWidth
    val mapHeight: Int get() = game.mapHeight

    var state: GameStateView = game.stateView()
        private set

    var selectedCritter: CritterStateView? = null
        private set

    var paused: Boolean = false
        private set
    var fps: Int = initialFps
        private set(value) {
            require(value > 0) { "fps $fps must be positive." }
            field = value
        }

    private val updateInterval: Double get() = 1.0 / fps
    private var lastUpdateTime: Double = initialTime

    fun selectAt(pos: Vector2) {
        selectedCritter = mapPosition(pos)?.let { state.occupant(it) }
    }

    fun togglePause() {
        paused = !paused
    }

    fun update(seconds: Double) {
        if (!paused && seconds - lastUpdateTime >= updateInterval) {
            game.tick()
            state = game.stateView()
            lastUpdateTime = seconds
        }
    }

    fun mapPosition(pos: Vector2): Position? {
        val cellX = (pos.x / cellSize).toInt()
        val cellY = (pos.y / cellSize).toInt()
        return if (cellX in 0 until mapWidth && cellY in 0 until mapHeight)
            Position(cellX, cellY)
        else null
    }
}
