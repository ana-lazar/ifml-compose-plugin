package views.dialogs

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import java.awt.GridBagLayout
import javax.swing.*
import kotlin.reflect.KFunction5
import editor.nodes.ViewContainer
import utils.constraintsLeft
import utils.constraintsRight

class AddParameterDialog(
    types: List<String>,
    private val viewContainer: ViewContainer,
    private val create: KFunction5<String, String, String, Boolean, ViewContainer, Unit>
) : DialogWrapper(false) {
    private val nameTextField = JTextField()
    private val valueTextField = JTextField()
    private val typeComboBox = ComboBox<String>()
    private val stateCheckBox = JCheckBox()

    init {
        for (type in types) {
            typeComboBox.addItem(type)
        }
        title = "New Parameter"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(
            JPanel().apply {
                panel.layout = GridBagLayout()
                panel.add(JLabel("Name: "), constraintsLeft(0, 0))
                panel.add(nameTextField, constraintsRight(1, 0))
                panel.add(JLabel("Value: "), constraintsLeft(0, 1))
                panel.add(valueTextField, constraintsRight(1, 1))
                panel.add(JLabel("IsState: "), constraintsLeft(0, 2))
                panel.add(stateCheckBox, constraintsRight(1, 2))
                panel.add(JLabel("Type: "), constraintsLeft(0, 3))
                panel.add(typeComboBox, constraintsRight(1, 3))
            }
        )
        return panel
    }

    override fun doOKAction() {
        val name = nameTextField.text
        val value = valueTextField.text
        val type = typeComboBox.selectedItem as String
        val isSelected = stateCheckBox.isSelected
        if (name != "" && value != "" && type != "") {
            create(name, value, type, isSelected, viewContainer)
        }
        super.doOKAction()
    }
}
