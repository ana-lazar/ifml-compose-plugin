package editor.nodes

import java.awt.*
import java.awt.geom.Line2D
import java.awt.geom.PathIterator
import java.awt.geom.Point2D

open class Action(text: String) : Node(text) {
    override var x = 0
    override var y = 0
    private var polygon = Polygon()

    private fun computePositions() {
        polygon.reset()

        val fontBold = Font("default", Font.BOLD, 12)
        val fmBold = Canvas().getFontMetrics(fontBold)

        val lines = name.text.split("\n")
        val r = maxOf(lines.size * fmBold.ascent, 30)

        var textWidth = 0
        for (line in lines) {
            textWidth = maxOf(textWidth, fmBold.stringWidth(line.trim()))
        }

        var xP = 0.0
        var yP: Double
        for (i in 0..6) {
            when (i) {
                0, 1, 5, 6 -> {
                    xP = x + r * kotlin.math.cos(i * 2 * Math.PI / 6) + textWidth / 2
                }
                in 2..4 -> {
                    xP = x + r * kotlin.math.cos(i * 2 * Math.PI / 6) - textWidth / 2
                }
            }
            yP = y + r * kotlin.math.sin(i * 2 * Math.PI / 6)
            polygon.addPoint(xP.toInt(), yP.toInt())
        }

        height = (r * kotlin.math.sqrt(2.0)).toInt()

        var tY = y + fmBold.descent
        if (lines.size == 1) {
            tY += padding
        }
        name.position(x, tY)
    }

    override fun position(setX: Int, setY: Int) {
        x = setX
        y = setY
        computePositions()
    }

    override fun draw(g: Graphics) {
        g.color = Color.WHITE
        g.fillPolygon(polygon)
        if (isSelected) {
            g.color = Color.BLUE
        }
        else {
            g.color = Color.BLACK
        }
        g.drawPolygon(polygon)
        g.color = Color.BLACK

        val fm = g.getFontMetrics(Font("default", Font.BOLD, 12))
        val lines = name.text.split("\n")
        var lineY = name.y - lines.size * fm.ascent / 2
        g.font = Font("default", Font.BOLD, 12)
        for (line in lines) {
            // TODO strip string
            g.drawString(line.trim(), name.x - fm.stringWidth(line) / 2, lineY + fm.descent)
            lineY += fm.height
        }
    }

    override fun isUnderCursor(mx: Int, my: Int): Boolean {
        return polygon.contains(mx, my)
    }

    override fun move(dx: Int, dy: Int) {
        x += dx
        y += dy
        computePositions()
    }

    override fun intersections(l: Line2D): Set<Point2D> {
        val polyIt = polygon.getPathIterator(null)
        val coords = DoubleArray(6)
        val firstCoords = DoubleArray(2)
        val lastCoords = DoubleArray(2)
        val intersections: MutableSet<Point2D> = HashSet()

        polyIt.currentSegment(firstCoords)
        lastCoords[0] = firstCoords[0]
        lastCoords[1] = firstCoords[1]
        polyIt.next()

        while (!polyIt.isDone) {
            when (polyIt.currentSegment(coords)) {
                PathIterator.SEG_LINETO -> {
                    val currentLine = Line2D.Double(lastCoords[0], lastCoords[1], coords[0], coords[1])
                    if (currentLine.intersectsLine(l)) linesIntersection(currentLine, l)?.let { intersections.add(it) }
                    lastCoords[0] = coords[0]
                    lastCoords[1] = coords[1]
                }
                PathIterator.SEG_CLOSE -> {
                    val currentLine = Line2D.Double(coords[0], coords[1], firstCoords[0], firstCoords[1])
                    if (currentLine.intersectsLine(l)) linesIntersection(currentLine, l)?.let { intersections.add(it) }
                }
                else -> {
                    throw Exception("Unsupported PathIterator segment type.")
                }
            }
            polyIt.next()
        }
        return intersections
    }

    override fun getPath(): String {
        return id
    }
}
