package views.dialogs

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import java.awt.GridBagLayout
import javax.swing.*
import kotlin.reflect.KFunction3
import editor.nodes.ViewContainer
import utils.constraintsLeft
import utils.constraintsRight

class AddEventHandlerDialog(
    events: List<String>?,
    private val create: KFunction3<String, String, ViewContainer, Unit>,
    private val viewContainer: ViewContainer
) : DialogWrapper(false) {
    private val eventTextField = JTextField()
    private val eventComboBox = ComboBox<String>()

    init {
        eventComboBox.addItem("Custom")
        for (event in events!!) {
            eventComboBox.addItem(event)
        }
        title = "New Event Handler"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(
            JPanel().apply {
                panel.layout = GridBagLayout()
                panel.add(JLabel("Existing event: "), constraintsLeft(0, 0))
                panel.add(eventComboBox, constraintsRight(1, 0))
                panel.add(JLabel("Custom event: "), constraintsLeft(0, 1))
                panel.add(eventTextField, constraintsRight(1, 1))
            }
        )
        return panel
    }

    override fun doOKAction() {
        val event = eventTextField.text
        val item = eventComboBox.selectedItem as String
        if (event != "" && item != "") {
            create(event, item, viewContainer)
        }
        super.doOKAction()
    }
}
