package input

import org.openrndr.KEY_SPACEBAR
import org.openrndr.Program
import org.openrndr.math.Vector2
import simulation.SimulationController

class InputHandler(private val program: Program, private val controller: SimulationController) {

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

    private fun onMouseMoved(position: Vector2) {
        // todo: hover highlight
    }

    private fun onMouseClicked(position: Vector2) {
        controller.selectAt(position.x, position.y)
    }
}
