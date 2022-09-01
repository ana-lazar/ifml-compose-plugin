package editor.nodes

import com.intellij.psi.PsiElement
import editor.GraphicElement
import java.awt.Graphics
import java.awt.Color
import java.awt.Point
import java.awt.geom.Line2D
import java.awt.geom.Point2D
import java.awt.geom.RoundRectangle2D

enum class NodeType(var nodeType: String) {
    BASIC_NODE("Basic node"), VIEW_CONTAINER("View Container");

    override fun toString(): String {
        return nodeType
    }
}

abstract class Node(text: String) : GraphicElement() {
    var id = ""
    var nodeType: NodeType = NodeType.BASIC_NODE
    var name = NamedElement(text)
    var psiElement: PsiElement? = null
    var isSelected: Boolean = false
    private val arch = 10
    open var x = 0
    open var y = 0
    open var height = 0
    open var width = 0
    val padding = 5

    open fun position(setX: Int, setY: Int) {
        x = setX
        y = setY
    }

    override fun draw(g: Graphics) {
        g.color = Color.WHITE
        g.fillRoundRect(x, y, width, height, arch, arch)
        if (isSelected) {
            g.color = Color.BLUE
        }
        else {
            g.color = Color.BLACK
        }
        g.drawRoundRect(x, y, width, height, arch, arch)
    }

    open fun isUnderCursor(mx: Int, my: Int): Boolean {
        val rectangle = RoundRectangle2D.Double(x.toDouble(), y.toDouble(), width.toDouble(),
            height.toDouble(), arch.toDouble(), arch.toDouble()
        )
        return rectangle.contains(mx.toDouble(), my.toDouble())
    }

    open fun move(dx: Int, dy: Int) {
        x += dx
        y += dy
    }

    open fun findChildByName(name: String): ViewContainer? {
        return null
    }

    open fun findChildById(id: String): ViewContainer? {
        return null
    }

    open fun findChildByElement(element: PsiElement): ViewContainer? {
        return null
    }

    abstract fun intersections(l: Line2D): Set<Point2D>

    fun linesIntersection(l1: Line2D, l2: Line2D): Point2D? {
        var result: Point2D? = null
        val s1X = l1.x2 - l1.x1
        val s1Y = l1.y2 - l1.y1
        val s2X = l2.x2 - l2.x1
        val s2Y = l2.y2 - l2.y1
        val s = (-s1Y * (l1.x1 - l2.x1) + s1X * (l1.y1 - l2.y1)) / (-s2X * s1Y + s1X * s2Y)
        val t = (s2X * (l1.y1 - l2.y1) - s2Y * (l1.x1 - l2.x1)) / (-s2X * s1Y + s1X * s2Y)
        if (s in 0.0..1.0 && t >= 0 && t <= 1) {
            result = Point(
                (l1.x1 + t * s1X).toInt(),
                (l1.y1 + t * s1Y).toInt()
            )
        }
        return result
    }

    abstract fun getPath(): String
}
