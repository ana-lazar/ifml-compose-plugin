package views

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.awt.Cursor
import java.awt.Graphics
import java.awt.event.*
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import editor.*
import editor.nodes.Node
import editor.nodes.ViewContainer
import persistence.DiagramRepository
import services.EditorCaretManager
import services.parsers.ActivityParser
import views.actions.*

class ActivityPanel(
    private val project: Project,
    private val file: PsiFile,
    private val diagramRepository: DiagramRepository?
) : Panel(), MouseListener, MouseMotionListener, ComponentListener {
    private val fileName: String
    private var diagram: Diagram
    private var mouseLeftButton = false
    private var mouseRightButton = false
    private var mouseX = 0
    private var mouseY = 0
    private var nodeUnderCursor: Node? = null
    private var selectedNode: Node? = null
    private val panelActions: List<PanelAction>
    private val viewActions: List<ViewAction>

    init {
        isFocusable = true
        requestFocus()
        addMouseMotionListener(this)
        addMouseListener(this)
        addComponentListener(this)
        fileName = file.name.split(".").toTypedArray()[0]
        diagram = Diagram(fileName, diagramRepository)
        panelActions = listOf(
            AddActivityAction("New activity", project, diagram)
        )
        viewActions = listOf(
            AddViewAction("Components", project, diagram),
            AddParameterAction("Parameters", project, diagram),
            AddEventHandlerAction("Event handlers", project, diagram),
            RemoveNodeAction("Delete", project, diagram)
        )
        try {
            buildDiagram()
        }
        catch (_: Exception) { }
    }

    fun buildDiagram() {
        diagram = Diagram(fileName, diagramRepository)
        repaint()
        val mainView = ViewContainer(fileName)
        mainView.psiElement = file
        mainView.position(50, 50)
        diagram.addNode(mainView)
        ActivityParser.getInstance(project)?.buildActivity(project, file, diagram, mainView, null)
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        diagram.draw(g)
    }

    override fun mouseDragged(e: MouseEvent) {
        if (mouseLeftButton) {
            moveGraphDrag(e.x, e.y)
        }
        else {
            setMouseCursor(e)
        }
    }

    override fun mouseMoved(e: MouseEvent) {
        setMouseCursor(e)
    }

    override fun mouseClicked(e: MouseEvent) {
        if (e.button == MouseEvent.BUTTON1) {
            selectNode(e)
        }
    }

    override fun mousePressed(e: MouseEvent) {
        if (e.button == MouseEvent.BUTTON1) {
            mouseLeftButton = true
            selectNode(e)
        }
        if (e.button == MouseEvent.BUTTON3) {
            mouseRightButton = true
        }
        setMouseCursor(e)
    }

    private fun selectNode(e: MouseEvent) {
        nodeUnderCursor = diagram.findNodeUnderCursor(e.x, e.y)
        if (nodeUnderCursor != null) {
            selectedNode?.isSelected = false
            selectedNode = nodeUnderCursor
            diagram.selectNode(nodeUnderCursor!!)
            EditorCaretManager.getInstance(project)?.moveEditorCaret(nodeUnderCursor!!.psiElement)
            repaint()
        }
    }

    fun selectElement(element: PsiElement) {
        val viewContainer = diagram.findByElement(element)
        if (viewContainer != null) {
            selectedNode?.isSelected = false
            selectedNode = viewContainer
            diagram.selectNode(viewContainer)
            repaint()
        }
    }

    override fun mouseReleased(e: MouseEvent) {
        if (e.button == MouseEvent.BUTTON1) {
            mouseLeftButton = false
        }
        if (e.button == MouseEvent.BUTTON3) {
            mouseRightButton = false
            if (nodeUnderCursor != null) {
                createViewPopupMenu(e, nodeUnderCursor!!)
            }
            else {
                createPanelPopupMenu(e)
            }
        }
        setMouseCursor(e)
    }

    private fun addMenuItemToPopup(popupMenu: JPopupMenu, text: String, func: () -> Unit) {
        val newMenuItem = JMenuItem(text)
        popupMenu.add(newMenuItem)
        newMenuItem.addActionListener { func() }
    }

    private fun createPanelPopupMenu(e: MouseEvent) {
        val popupMenu = JPopupMenu()
        for (action in panelActions) {
            addMenuItemToPopup(popupMenu, action.name) { action.execute(e.x, e.y, this) }
        }
        popupMenu.show(e.component, e.x, e.y)
    }

    private fun createViewPopupMenu(e: MouseEvent, node: Node) {
        val popupMenu = JPopupMenu()
        for (action in viewActions) {
            addMenuItemToPopup(popupMenu, action.name) { action.execute(node, this) }
        }
        popupMenu.show(e.component, e.x, e.y)
    }

    private fun setMouseCursor(e: MouseEvent?) {
        if (e != null) {
            nodeUnderCursor = diagram.findNodeUnderCursor(e.x, e.y)
            mouseX = e.x
            mouseY = e.y
        }
        val mouseCursor: Int = if (nodeUnderCursor != null) {
            Cursor.HAND_CURSOR
        }
        else if (mouseLeftButton) {
            Cursor.MOVE_CURSOR
        }
        else {
            Cursor.DEFAULT_CURSOR
        }
        cursor = Cursor.getPredefinedCursor(mouseCursor)
    }

    private fun moveGraphDrag(mx: Int, my: Int) {
        val dx = mx - mouseX
        val dy = my - mouseY
        if (nodeUnderCursor != null) {
            nodeUnderCursor!!.move(dx, dy)
            diagramRepository?.updateDiagram(fileName, diagram)
        }
        else {
            diagram.moveGraph(dx, dy)
            diagramRepository?.updateDiagram(fileName, diagram)
        }
        mouseX = mx
        mouseY = my
        repaint()
    }

    override fun componentHidden(e: ComponentEvent) { }

    override fun componentMoved(e: ComponentEvent) { }

    override fun componentResized(e: ComponentEvent) { }

    override fun componentShown(e: ComponentEvent) { }

    override fun mouseEntered(e: MouseEvent) { }

    override fun mouseExited(e: MouseEvent) { }

    companion object {
        private const val serialVersionUID = 3544581658578869882L
    }
}
