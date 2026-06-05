package view

import critters.CritterStateView
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.FontImageMap

class InfoPanel(
    private val drawer: Drawer,
    private val font: FontImageMap,
    private val xOffset: Double,
    private val width: Double,
    private val height: Double
) {
    private val background = ColorRGBa.fromHex("#1a1a2e")
    private val textColor = ColorRGBa.fromHex("#e0e0e0")
    private val padding = 16.0
    private val lineHeight = 28.0

    fun render(selectedCritter: CritterStateView?) {
        drawer.stroke = null
        drawer.fill = background
        drawer.rectangle(xOffset, 0.0, width, height)

        // todo: critter death
        if (selectedCritter != null) {
            drawer.fontMap = font
            drawer.fill = textColor
            drawer.text("Name:   ${selectedCritter.name}", xOffset + padding, padding + lineHeight)
            drawer.text("Hunger: ${selectedCritter.hunger}", xOffset + padding, padding + lineHeight * 2)
        }
    }
}
