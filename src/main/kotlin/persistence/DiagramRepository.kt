package persistence

import com.intellij.openapi.project.Project
import editor.Diagram
import editor.nodes.ViewContainer

class DiagramRepository(private val project: Project) {
    fun updateDiagram(id: String, diagram: Diagram) {
        val diagramData = buildDiagram(diagram)
        DiagramStateComponent.getInstance(project).run {
            this.diagramState.diagrams[id] = diagramData
        }
    }

    private fun buildDiagram(diagram: Diagram): DiagramData {
        val nodes = mutableListOf<NodeData>()
        for (node in diagram.nodes) {
            val nodeData = NodeData(node.id, node.x, node.y, node.height, node.width)
            if (node is ViewContainer) buildNode(node, nodeData)
            nodeData.path = node.id
            nodes.add(nodeData)
        }
        return DiagramData(nodes)
    }

    private fun buildNode(container: ViewContainer, nodeData: NodeData) {
        for (view in container.views) {
            val newData = NodeData(view.id, view.x, view.y, view.height, view.width)
            newData.path = "${container.getPath()}/${view.id}"
            nodeData.children.add(newData)
            buildNode(view, newData)
        }
    }

    private fun findNodeChildren(node: NodeData, id: String, children: MutableList<NodeData>) {
        if (node.id == id) {
            children.add(node)
        }
        for (child in node.children) {
            findNodeChildren(child, id, children)
        }
    }

    fun findNodeInDiagram(nodeId: String, nodePath: String, diagramId: String): NodeData? {
        val diagram = loadDiagramsPositions().diagrams.filter { it.key == diagramId }
        if (diagram.isNotEmpty()) {
            for (node in diagram[diagramId]!!.nodes) {
                val children = mutableListOf<NodeData>()
                findNodeChildren(node, nodeId, children)
                val child = children.filter { it.path == nodePath }
                if (child.isNotEmpty()) {
                    return child[0]
                }
            }
        }
        return null
    }

    private fun loadDiagramsPositions() = DiagramStateComponent.getInstance(project).diagramState
}
