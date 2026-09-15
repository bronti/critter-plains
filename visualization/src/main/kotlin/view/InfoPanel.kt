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

    fun render(selectedCritter: CritterStateView?, chronicle: String = "") {
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

        if (chronicle.isNotBlank()) {
            drawer.fontMap = font
            drawer.fill = textColor
            val lines = wrap(chronicle)
            val chronicleTop = height - padding - lineHeight * (lines.size - 1)
            lines.forEachIndexed { i, line ->
                drawer.text(line, xOffset + padding, chronicleTop + lineHeight * i)
            }
        }
    }

    private fun wrap(text: String, maxCharsPerLine: Int = 22): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            if (current.isNotEmpty() && current.length + 1 + word.length > maxCharsPerLine) {
                lines += current.toString()
                current = StringBuilder()
            }
            if (current.isNotEmpty()) current.append(" ")
            current.append(word)
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }
}
