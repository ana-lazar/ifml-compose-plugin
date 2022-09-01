package views.dialogs

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import java.awt.GridBagLayout
import javax.swing.*
import kotlin.reflect.KFunction3
import editor.nodes.ViewContainer
import utils.Composable
import utils.constraintsLeft
import utils.constraintsRight

class AddViewDialog(
    composables: MutableList<Composable>,
    private val viewContainer: ViewContainer,
    private val create: KFunction3<String, Composable, ViewContainer, Unit>
) : DialogWrapper(false) {
    private val nameTextField = JTextField()
    private val typeComboBox = ComboBox<Composable>()

    init {
        for (composable in composables) {
            typeComboBox.addItem(composable)
        }
        title = "New Component"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(
            JPanel().apply {
                panel.layout = GridBagLayout()
                panel.add(JLabel("Type: "), constraintsLeft(0, 0))
                panel.add(typeComboBox, constraintsRight(1, 0))
                panel.add(JLabel("Name: "), constraintsLeft(0, 1))
                panel.add(nameTextField, constraintsRight(1, 1))
            }
        )
        return panel
    }

    override fun doOKAction() {
        val name = nameTextField.text
        val type = typeComboBox.selectedItem as Composable
        if (type.name == "Custom" && name != "" || type.name != "Custom") {
            create(name, type, viewContainer)
        }
        super.doOKAction()
    }
}
