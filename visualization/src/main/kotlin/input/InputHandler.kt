package input

import org.openrndr.KEY_SPACEBAR
import org.openrndr.KeyTracker
import org.openrndr.Program
import org.openrndr.math.Vector2
import simulation.SimulationController

// Per-frame pan step (tiles). Applied once per rendered frame while a direction key
// is held, so this is deliberately small — unlike a one-shot keypress delta.
private const val SCROLL_SPEED = 1

// Key names as reported by OPENRNDR's KeyTracker (org.openrndr.Keys.kt, v0.4.5):
// letters come from GLFW.glfwGetKeyName and are lowercase; arrows are OPENRNDR's own
// hardcoded names. Both are layout-independent for the keys used here.
private const val KEY_NAME_W = "w"
private const val KEY_NAME_A = "a"
private const val KEY_NAME_S = "s"
private const val KEY_NAME_D = "d"
private const val KEY_NAME_ARROW_UP = "arrow-up"
private const val KEY_NAME_ARROW_DOWN = "arrow-down"
private const val KEY_NAME_ARROW_LEFT = "arrow-left"
private const val KEY_NAME_ARROW_RIGHT = "arrow-right"

// Pure key-names-held -> (dx, dy) mapping, kept separate from KeyTracker/SimulationController
// so it's unit-testable without an OPENRNDR window. Opposing keys held together cancel out;
// perpendicular keys combine, giving diagonal movement without special-casing.
internal fun panDelta(heldKeys: Set<String>, speed: Int): Pair<Int, Int> {
    var dx = 0
    var dy = 0
    if (KEY_NAME_W in heldKeys || KEY_NAME_ARROW_UP in heldKeys) dy -= speed
    if (KEY_NAME_S in heldKeys || KEY_NAME_ARROW_DOWN in heldKeys) dy += speed
    if (KEY_NAME_A in heldKeys || KEY_NAME_ARROW_LEFT in heldKeys) dx -= speed
    if (KEY_NAME_D in heldKeys || KEY_NAME_ARROW_RIGHT in heldKeys) dx += speed
    return dx to dy
}

class InputHandler(private val program: Program, private val controller: SimulationController) {

    private val keyTracker = KeyTracker(program.keyboard)

    fun setup() {
        program.keyboard.keyDown.listen { event ->
            when (event.key) {
                KEY_SPACEBAR -> controller.togglePause()
            }
        }

        program.mouse.moved.listen { event ->
            onMouseMoved(event.position)
        }

        program.mouse.buttonDown.listen { event ->
            onMouseClicked(event.position)
        }
    }

    fun pollHeldKeys() {
        val (dx, dy) = panDelta(keyTracker.pressedKeys, SCROLL_SPEED)
        if (dx != 0 || dy != 0) controller.scroll(dx, dy)
    }

    private fun onMouseMoved(position: Vector2) {
        // todo: hover highlight
    }

    private fun onMouseClicked(position: Vector2) {
        controller.selectAt(position)
    }
}
