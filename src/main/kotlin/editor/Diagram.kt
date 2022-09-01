package editor

import com.intellij.psi.PsiElement
import java.awt.Graphics
import java.io.*
import editor.edges.ActionEdge
import editor.edges.Edge
import editor.nodes.Action
import editor.nodes.Node
import editor.nodes.ViewContainer
import persistence.DiagramRepository

open class Diagram(
    val name: String,
    private val diagramRepository: DiagramRepository?
) : Serializable {
    var nodes: MutableList<Node> = mutableListOf()
    private var edges: MutableList<Edge> = mutableListOf()

    fun draw(g: Graphics?) {
        for (node in nodes) {
            node.draw(g!!)
        }
        for (edge in edges) {
            if (edge is ActionEdge) {
                edge.computePositions()
            }
            edge.draw(g!!)
        }
    }

    fun moveGraph(dx: Int, dy: Int) {
        for (node in nodes) {
            node.move(dx, dy)
        }
    }

    fun selectNode(node: Node) {
        node.isSelected = true
    }

    fun addNode(node: Node) {
        var count = -1
        for (el in nodes) {
            if (node is Action && el is Action) {
                count = el.id.split("-")[1].toInt()
            }
            if (node.name.text == el.name.text) {
                count = el.id.split("-")[1].toInt()
            }
        }
        if (node is Action) {
            node.id = "Action-${count + 1}"
        }
        else {
            node.id = "${node.name.text}-${count + 1}"
        }
        // cauta in diagrama salvata view-ul cu name-count+1
        val oldNode = diagramRepository?.findNodeInDiagram(node.id, node.getPath(), name)
        if (oldNode != null) {
            node.position(oldNode.x, oldNode.y)
        }
        nodes.add(node)
    }

    fun addChildToContainer(parent: ViewContainer, child: ViewContainer) {
        // cauta in views ultima componenta cu acelasi nume, avand id name-count
        var count = -1
        for (view in parent.views) {
            if (child.name.text == view.name.text) {
                count = view.id.split("-")[1].toInt()
            }
        }
        // daca exista, id-ul view-ului copil este name-count+1
        val id = "${child.name.text}-${count + 1}"
        child.id = id

        // cauta in diagrama salvata view-ul cu name-count+1
        child.parent = parent
        val oldNode = diagramRepository?.findNodeInDiagram(child.id, child.getPath(), name)
        if (oldNode != null) {
            parent.addChild(child, oldNode.x, oldNode.y)
        }
        else {
            parent.addChild(child)
        }
    }

    fun addEdge(e: Edge) {
        for (edge in edges) {
            if (e == edge) return
        }
        edges.add(e)
    }

    fun findNodeUnderCursor(mx: Int, my: Int): Node? {
        for (node in nodes) {
            if (node is Action && node.isUnderCursor(mx, my)) {
                return node
            }
        }
        for (node in nodes) {
            var np: Node
            if (node is ViewContainer && node.isUnderCursor(mx, my)) {
                np = node
                var child = np.findChildViewUnderCursor(mx, my)
                while (child != null) {
                    np = child
                    child = np.findChildViewUnderCursor(mx, my)
                }
                return np
            }
        }
        return null
    }

    fun findNode(id: String?): Node? {
        for (node in nodes) {
            val child = id?.let { node.findChildByName(it) }
            if (child != null) {
                return child
            }
        }
        return null
    }

    fun findByElement(element: PsiElement?): Node? {
        for (node in nodes) {
            val child = element?.let { node.findChildByElement(it) }
            if (child != null) {
                return child
            }
        }
        return null
    }

    fun findByName(name: String?): Node? {
        for (node in nodes) {
            if (node.name.text == name) {
                return node
            }
        }
        return null
    }

    fun removeNode(nodeUnderCursor: Node) {
        removeAttachedEdges(nodeUnderCursor)
        if (nodeUnderCursor in nodes) {
            nodes.remove(nodeUnderCursor)
        }
        else if (nodeUnderCursor is ViewContainer) {
            var parent = nodeUnderCursor.parent
            while (parent != null && parent !in nodes) {
                parent = parent.parent
            }
            parent?.removeChild(nodeUnderCursor)
        }
    }

    private fun removeAttachedEdges(nodeUnderCursor: Node) {
        edges.removeIf { e: Edge -> e.node1 == nodeUnderCursor || e.node2 == nodeUnderCursor }
    }

    companion object {
        private const val serialVersionUID = 5673009196816218789L
    }
}
