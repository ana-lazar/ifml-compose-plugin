package views.dialogs

import com.intellij.openapi.ui.DialogWrapper
import java.awt.GridBagLayout
import javax.swing.*
import utils.constraintsLeft
import utils.constraintsRight

class AddActivityDialog(private val create: (String) -> Unit) : DialogWrapper(false) {
    private val nameTextField = JTextField()

    init {
        title = "New Activity"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(
            JPanel().apply {
                panel.layout = GridBagLayout()
                panel.add(JLabel("Name: "), constraintsLeft(0, 1))
                panel.add(nameTextField, constraintsRight(1, 1))
            }
        )
        return panel
    }

    override fun doOKAction() {
        val name = nameTextField.text
        if (name != "") {
            create(name)
        }
        super.doOKAction()
    }
}
