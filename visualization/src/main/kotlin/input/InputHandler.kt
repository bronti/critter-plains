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

// `KeyEvent.key` has no named Int constant for printable/symbol keys in org.openrndr's Keys.kt
// (v0.4.5, verified via javap against the installed jar — only special keys like KEY_SPACEBAR
// get one). Discrete +/- zoom therefore matches on `KeyEvent.name`, the same GLFW-key-name
// string KeyTracker uses. glfwGetKeyName reports the unshifted base character, so the main
// "+/=" key reports "=" even with Shift held; the numpad add/subtract keys report "+"/"-"
// directly. Both spellings are matched per direction to cover both keys.
private const val KEY_NAME_ZOOM_IN_MAIN = "="
private const val KEY_NAME_ZOOM_IN_KEYPAD = "+"
private const val KEY_NAME_ZOOM_OUT = "-"

// Discrete zoom step per +/- press or scroll notch (one factor step, not held-continuous —
// AD-1 excludes key-based zoom from the per-frame KeyTracker poll).
private const val KEY_ZOOM_FACTOR = 1.25
private const val SCROLL_ZOOM_FACTOR = 1.1

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

// Pure key-name -> zoom-factor mapping for discrete +/- zoom, mirroring panDelta above so it's
// unit-testable without an OPENRNDR window. Null means the key isn't a zoom key.
internal fun zoomFactorForKey(keyName: String): Double? = when (keyName) {
    KEY_NAME_ZOOM_IN_MAIN, KEY_NAME_ZOOM_IN_KEYPAD -> KEY_ZOOM_FACTOR
    KEY_NAME_ZOOM_OUT -> 1.0 / KEY_ZOOM_FACTOR
    else -> null
}

// Pure scroll-direction -> zoom-factor mapping, same rationale. Null means no vertical scroll.
internal fun zoomFactorForScroll(verticalRotation: Double): Double? = when {
    verticalRotation > 0 -> SCROLL_ZOOM_FACTOR
    verticalRotation < 0 -> 1.0 / SCROLL_ZOOM_FACTOR
    else -> null
}

// Pure boundary check, same rationale: Camera's screen-space convention is the map-viewport
// rectangle only (0,0 to viewportWidthPx x viewportHeightPx, per AD-1) — the info panel sits
// to the right of it, so scroll-wheel zoom must not fire there.
// Intentionally x-only: the info panel is a vertical strip to the right, and the map region
// spans the full window height, so there is no vertical panel to guard against.
internal fun isOverMapViewport(x: Double, viewportWidthPx: Int): Boolean = x < viewportWidthPx

class InputHandler(private val program: Program, private val controller: SimulationController) {

    private val keyTracker = KeyTracker(program.keyboard)

    fun setup() {
        program.keyboard.keyDown.listen { event ->
            when (event.key) {
                KEY_SPACEBAR -> controller.togglePause()
            }
            zoomFactorForKey(event.name)?.let { factor -> controller.zoom(viewportCenter(), factor) }
        }

        program.mouse.moved.listen { event ->
            onMouseMoved(event.position)
        }

        program.mouse.buttonDown.listen { event ->
            onMouseClicked(event.position)
        }

        program.mouse.scrolled.listen { event ->
            onMouseScrolled(event.position, event.rotation.y)
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

    private fun onMouseScrolled(position: Vector2, verticalRotation: Double) {
        if (!isOverMapViewport(position.x, controller.viewportWidthPx)) return
        zoomFactorForScroll(verticalRotation)?.let { factor -> controller.zoom(position, factor) }
    }

    private fun viewportCenter() = Vector2(controller.viewportWidthPx / 2.0, controller.viewportHeightPx / 2.0)
}
