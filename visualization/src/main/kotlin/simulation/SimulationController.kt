package simulation

import ai.Chronicler
import critters.CritterStateView
import critters.Game
import critters.GameStateView
import critters.world.Position
import critters.world.Territory
import org.openrndr.math.Vector2

class SimulationController(
    private val game: Game,
    initialFps: Int,
    initialTime: Double,
    initialCellSize: Int,
    val viewportWidthPx: Int,
    val viewportHeightPx: Int,
    private val chronicler: Chronicler = Chronicler(),
) {

    private val mapWidth: Int get() = game.mapWidth
    private val mapHeight: Int get() = game.mapHeight

    private val camera = Camera(mapWidth, mapHeight, viewportWidthPx, viewportHeightPx, initialCellSize.toDouble())

    internal val visibleCols: Int get() = camera.visibleCols
    internal val visibleRows: Int get() = camera.visibleRows

    val cellSize: Double get() = camera.cellSize
    val viewportX: Double get() = camera.viewportX
    val viewportY: Double get() = camera.viewportY

    var state: GameStateView = game.stateView()
        private set

    var selectedCritter: CritterStateView? = null
        private set

    val chronicle: String get() = chronicler.latestChronicle

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

    fun deselect() {
        selectedCritter = null
    }

    fun togglePause() {
        paused = !paused
    }

    fun update(seconds: Double) {
        if (!paused && seconds - lastUpdateTime >= updateInterval) {
            game.tick()
            state = game.stateView()
            selectedCritter = selectedCritter?.let { state.critter(it.name) }
            lastUpdateTime = seconds
            chronicler.maybeUpdate(game.stats())
        }
    }

    fun scroll(dx: Int, dy: Int) = camera.pan(dx, dy)

    fun zoom(anchorScreenPos: Vector2, factor: Double) = camera.zoomAt(anchorScreenPos, factor)

    fun mapPosition(pos: Vector2): Position? = camera.mapPosition(pos)
}
