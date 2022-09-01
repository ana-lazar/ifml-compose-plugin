package editor.edges

import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import kotlin.math.atan2
import editor.nodes.NamedElement
import editor.nodes.Node

class ActionEdge(a: Node, b: Node, name: String) : Edge(a, b) {
    var name = NamedElement(name)
    var intersection1: Point2D = Point2D.Double(0.0, 0.0)
    var intersection2: Point2D = Point2D.Double(0.0, 0.0)
    var h: Int = 0
    private var arrowHead = Polygon()

    init {
        edgeType = EdgeType.ACTION_EDGE
    }

    fun computePositions() {
        val line = line()
        intersection1 = intersect(node1, line)
        intersection2 = intersect(node2, line)

        h = minOf(node1.height / 5, 20)
        arrowHead.reset()
        arrowHead.addPoint(0, 0)
        arrowHead.addPoint(-h / 2, -h / 2)
        arrowHead.addPoint(-h / 2, h / 2)

        val font = Font("default", Font.PLAIN, 12)
        val fm = Canvas().getFontMetrics(font)
        positionText(intersection1, intersection2, fm)
    }

    override fun draw(g: Graphics) {
        super.draw(g)

        val rotate = atan2((intersection2.y - intersection1.y), (intersection2.x - intersection1.x))
        val transform = AffineTransform()
        transform.translate(intersection2.x, intersection2.y)
        transform.rotate(rotate)
        val g2d = g as Graphics2D
        g2d.fill(transform.createTransformedShape(arrowHead))

        g.color = Color.WHITE
        g.fillOval((intersection1.x - h / 2).toInt(), (intersection1.y - h / 2).toInt(), h, h)
        g.color = Color.BLACK
        g.drawOval((intersection1.x - h / 2).toInt(), (intersection1.y - h / 2).toInt(), h, h)

        g.drawString(name.text, name.x, name.y)
    }

    private fun positionText(a: Point2D, b: Point2D, fm: FontMetrics) {
        var angle = Math.toDegrees(atan2(b.y - a.y, b.x - a.x))
        if (angle < 0){
            angle += 360
        }
        if (angle < 90) {
            name.x = (a.x - fm.stringWidth(name.text) - h / 2).toInt()
            name.y = (a.y + fm.ascent + h / 2).toInt()
            if (angle < 45) {
                name.x += fm.stringWidth(name.text) + h
                name.y = (a.y - fm.ascent).toInt()
            }
        }
        else if (angle < 180) {
            name.x = (a.x + h / 2).toInt()
            name.y = (a.y + fm.ascent + h / 2).toInt()
            if (angle > 135) {
                name.x -= fm.stringWidth(name.text) + h
                name.y = (a.y - fm.ascent).toInt()
            }
        }
        else if (angle < 270) {
            name.x = (a.x + h / 2).toInt()
            name.y = (a.y - fm.ascent).toInt()
            if (angle < 225) {
                name.x -= fm.stringWidth(name.text) + h
                name.y = (a.y + fm.ascent + h / 2).toInt()
            }
        }
        else if (angle < 360) {
            name.x = (a.x - fm.stringWidth(name.text) - h / 2).toInt()
            name.y = (a.y - fm.ascent).toInt()
            if (angle > 315) {
                name.x += fm.stringWidth(name.text) + h
                name.y = (a.y + fm.ascent + h / 2).toInt()
            }
        }
    }

    companion object {
        private const val serialVersionUID = -2550804644989255961L
    }
}
