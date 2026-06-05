package input

import org.openrndr.KEY_SPACEBAR
import org.openrndr.Program
import org.openrndr.math.Vector2
import simulation.SimulationController

private const val SCROLL_SPEED = 3

// GLFW key codes for WASD — equal to ASCII value of the uppercase letter,
// independent of the OS input language or keyboard layout.
private const val KEY_W = 'W'.code
private const val KEY_A = 'A'.code
private const val KEY_S = 'S'.code
private const val KEY_D = 'D'.code

class InputHandler(private val program: Program, private val controller: SimulationController) {

    fun setup() {
        program.keyboard.keyDown.listen { event ->
            when (event.key) {
                KEY_SPACEBAR -> controller.togglePause()
                KEY_W -> controller.scroll(0, -SCROLL_SPEED)
                KEY_S -> controller.scroll(0, +SCROLL_SPEED)
                KEY_A -> controller.scroll(-SCROLL_SPEED, 0)
                KEY_D -> controller.scroll(+SCROLL_SPEED, 0)
            }
        }

        program.mouse.moved.listen { event ->
            onMouseMoved(event.position)
        }

        program.mouse.buttonDown.listen { event ->
            onMouseClicked(event.position)
        }
    }

    private fun onMouseMoved(position: Vector2) {
        // todo: hover highlight
    }

    private fun onMouseClicked(position: Vector2) {
        controller.selectAt(position)
    }
}
