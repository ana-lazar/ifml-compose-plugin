package editor.edges

import java.awt.Graphics
import java.awt.Color
import java.awt.geom.Line2D
import java.awt.geom.Point2D
import java.io.Serializable
import editor.nodes.Action
import editor.nodes.Node
import editor.nodes.ViewContainer

enum class EdgeType(var edgeType: String) {
    BASIC_EDGE("Basic edge"), ACTION_EDGE("Action edge");

    override fun toString(): String {
        return edgeType
    }
}

open class Edge(var node1: Node, var node2: Node) : Serializable {
	var edgeType: EdgeType = EdgeType.BASIC_EDGE

    open fun draw(g: Graphics) {
        g.color = Color.BLACK

        val line = line()
        val a = intersect(node1, line)
        val b = intersect(node2, line)

        g.drawLine(a.x.toInt(), a.y.toInt(), b.x.toInt(), b.y.toInt())
    }

    fun line(): Line2D {
        var center1: Point2D = Point2D.Double(0.0, 0.0)
        var center2: Point2D = Point2D.Double(0.0, 0.0)
        if (node1 is ViewContainer) {
            center1 = Point2D.Double((node1.x + node1.width / 2).toDouble(), (node1.y + node1.height / 2).toDouble())
        }
        else if (node1 is Action) {
            center1 = Point2D.Double(node1.x.toDouble(), node1.y.toDouble())
        }
        if (node2 is ViewContainer) {
            center2 = Point2D.Double((node2.x + node2.width / 2).toDouble(), (node2.y + node2.height / 2).toDouble())
        }
        else if (node2 is Action) {
            center2 = Point2D.Double(node2.x.toDouble(), node2.y.toDouble())
        }
        return Line2D.Double(center1, center2)
    }

    fun intersect(node: Node, line: Line2D): Point2D {
        val i = node.intersections(line)
        var xb = node.x
        var yb = node.y
        if (i.isNotEmpty()) {
            xb = i.first().x.toInt()
            yb = i.first().y.toInt()
        }
        return Point2D.Double(xb.toDouble(), yb.toDouble())
    }

    companion object {
        private const val serialVersionUID = -6972652167790425200L
    }
}
