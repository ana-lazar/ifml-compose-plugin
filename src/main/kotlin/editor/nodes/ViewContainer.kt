package editor.nodes

import com.intellij.psi.PsiElement
import java.awt.*
import java.awt.geom.Line2D
import java.awt.geom.Point2D

class ViewContainer(text: String) : Node(text) {
    var parameters = mutableListOf<ParameterElement>()
    var views = mutableListOf<ViewContainer>()
    var parent: ViewContainer? = null

    private var ySeparator1 = 0
    private var ySeparator2 = 0

    init {
        nodeType = NodeType.VIEW_CONTAINER
    }

    private fun computePositions(dx: Int = 0, dy: Int = 0) {
        val fontPlain = Font("default", Font.PLAIN, 12)
        val fmPlain = Canvas().getFontMetrics(fontPlain)

        val fontBold = Font("default", Font.BOLD, 12)
        val fmBold = Canvas().getFontMetrics(fontBold)

        val lastHeight = height
        val lastWidth = width
        height = padding
        val nameY = y + fmBold.ascent + height
        height += fmBold.ascent + padding
        width = fmBold.stringWidth(name.text)

        if (parameters.size == 0) {
            ySeparator1 = 0
        }
        else {
            ySeparator1 = y + height
            height += padding
        }

        for (parameter in parameters) {
            height += fmPlain.height
            parameter.position(x + padding, y + height)
            width = maxOf(width, fmPlain.stringWidth(parameter.text))
            height += padding
        }

        height += padding
        ySeparator2 = y + height
        height += padding

        for (view in views) {
            if (dx != 0 || dy != 0) {
                view.moveThis(dx, dy)
            }
            width = maxOf(width, view.width + view.x - x - padding)
            height = maxOf(height, view.height + view.y - y)
        }

        width += 2 * padding
        height += padding

        width = maxOf(width, lastWidth)
        height = maxOf(height, lastHeight)

        val nameX = x + width / 2 - fmBold.stringWidth(name.text) / 2
        name.position(nameX, nameY)
    }

    override fun position(setX: Int, setY: Int) {
        super.position(setX, setY)
        computePositions()
    }

    override fun draw(g: Graphics) {
        super.draw(g)

        if (isSelected) {
            g.color = Color.BLUE
        }
        else {
            g.color = Color.BLACK
        }
        g.font = Font("default", Font.BOLD, 12)
        g.drawString(name.text, name.x, name.y)
        if (parameters.size != 0) {
            g.drawLine(x, ySeparator1, x + width, ySeparator1)
        }
        g.drawLine(x, ySeparator2, x + width, ySeparator2)
        g.font = Font("default", Font.PLAIN, 12)
        g.color = Color.BLACK
        for (parameter in parameters) {
            parameter.draw(g)
        }
        for (view in views) {
            view.draw(g)
        }
    }

    private fun recomputeParents() {
        parent?.computePositions()
        parent?.recomputeParents()
    }

    private fun updateParentsDimensions(dx: Int, dy: Int) {
        width += dx
        height += dy

        val fontBold = Font("default", Font.BOLD, 12)
        val fmBold = Canvas().getFontMetrics(fontBold)
        val nameX = x + width / 2 - fmBold.stringWidth(name.text) / 2
        name.position(nameX, name.y)

        parent?.updateParentsDimensions(dx, dy)
    }

    fun addParameter(name: String, type: String) {
        parameters.add(ParameterElement(name, type))
        val fontPlain = Font("default", Font.PLAIN, 12)
        val fmPlain = Canvas().getFontMetrics(fontPlain)
        computePositions(0, fmPlain.height + padding)
        recomputeParents()
    }

    fun addStateParameter(name: String, type: String) {
        parameters.add(StateParameterElement(name, type))
        val fontPlain = Font("default", Font.PLAIN, 12)
        val fmPlain = Canvas().getFontMetrics(fontPlain)
        computePositions(0, fmPlain.height + padding)
        recomputeParents()
    }

    fun addChild(view: ViewContainer, nx: Int? = null, ny: Int? = null) {
        if (nx == null && ny == null) {
            view.position(x + padding, y + height)
        }
        else {
            view.position(nx!!, ny!!)
        }
        view.parent = this
        views.add(view)
        computePositions()
        recomputeParents()
    }

    fun removeChild(view: ViewContainer) {
        views.remove(view)
        computePositions()
        recomputeParents()
    }

    override fun findChildByName(name: String): ViewContainer? {
        if (this.name.text == name) {
            return this
        }
        for (view in views) {
            if (view.name.text == name) {
                return view
            }
            val child = view.findChildByName(name)
            if (child != null) {
                return child
            }
        }
        return super.findChildByName(name)
    }

    override fun findChildById(id: String): ViewContainer? {
        if (this.id == id) {
            return this
        }
        for (view in views) {
            if (view.id == id) {
                return view
            }
            val child = view.findChildById(id)
            if (child != null) {
                return child
            }
        }
        return super.findChildById(id)
    }

    override fun findChildByElement(element: PsiElement): ViewContainer? {
        if (psiElement == element) {
            return this
        }
        for (view in views) {
            if (view.psiElement == element) {
                return view
            }
            val child = view.findChildByElement(element)
            if (child != null) {
                return child
            }
        }
        return super.findChildByElement(element)
    }

    fun findParentRecursively(): ViewContainer {
        if (parent == null) {
            return this
        }
        return parent!!.findParentRecursively()
    }

    fun findChildViewUnderCursor(dx: Int, dy: Int): ViewContainer? {
        for (view in views) {
            if (view.isUnderCursor(dx, dy)) {
                return view
            }
        }
        return null
    }

    private fun moveThis(dx: Int, dy: Int) {
        super.move(dx, dy)
        computePositions(dx, dy)
    }

    override fun move(dx: Int, dy: Int) {
        if (parent != null) {
            if (x + dx < parent!!.padding + parent!!.x || y + dy < parent!!.ySeparator2 + parent!!.padding) {
                return
            }
            else if (x + dx + width + padding > parent!!.width + parent!!.x || y + dy + height + padding > parent!!.height + parent!!.y) {
                super.move(dx, dy)
                computePositions(dx, dy)
                val px = if (x + width + padding > parent!!.width + parent!!.x) x + width + padding - parent!!.width - parent!!.x else 0
                val py = if (y + height + padding > parent!!.height + parent!!.y) y + height + padding - parent!!.height - parent!!.y else 0
                parent!!.updateParentsDimensions(px, py)
                return
            }
        }
        super.move(dx, dy)
        computePositions(dx, dy)
    }

    private fun addIntersection(line1: Line2D, line2: Line2D, intersections: MutableSet<Point2D>) {
        val intersection = linesIntersection(line1, line2)
        if (intersection != null) {
            intersections.add(intersection)
        }
    }

    override fun intersections(l: Line2D): Set<Point2D> {
        val intersections: MutableSet<Point2D> = HashSet()
        addIntersection(l, Line2D.Double(x.toDouble(), y.toDouble(), (x + width).toDouble(), y.toDouble()), intersections)
        addIntersection(l, Line2D.Double(x.toDouble(), y.toDouble(), x.toDouble(), (y + height).toDouble()), intersections)
        addIntersection(l, Line2D.Double((x + width).toDouble(), y.toDouble(), (x + width).toDouble(), (y + height).toDouble()), intersections)
        addIntersection(l, Line2D.Double(x.toDouble(), (y + height).toDouble(), (x + width).toDouble(), (y + height).toDouble()), intersections)
        return intersections
    }

    override fun getPath(): String {
        if (parent == null) {
            return id
        }
        return "${parent!!.getPath()}/$id"
    }

    companion object {
        private const val serialVersionUID = 5396347452110584370L
    }
}
