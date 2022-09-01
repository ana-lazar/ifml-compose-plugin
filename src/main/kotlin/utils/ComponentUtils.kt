package utils

data class Composable(
    val name: String,
    val importStatement: String,
    val hasContent: Boolean,
    val codeTemplate: String
) {
    override fun toString() = name
}

data class Parameter(
    val name: String,
    val type: String,
    var value: String,
    val isState: Boolean
) {
    override fun toString() = name
}

object ComponentUtils {
    val predefinedComposables = listOf(
        Composable("Text", "import androidx.compose.material.Text\n", false, CodeTemplates.TEXT_COMPONENT),
        Composable("Button", "import androidx.compose.material.Button\n", false, CodeTemplates.BUTTON_COMPONENT),
        Composable("Scaffold", "import androidx.compose.material.Scaffold\n", false, CodeTemplates.SCAFFOLD_COMPONENT),
        Composable("Column", "import androidx.compose.material.Column\n", false, CodeTemplates.COLUMN_COMPONENT),
        Composable("Checkbox", "import androidx.compose.material.Checkbox\n", false, CodeTemplates.CHECKBOX_COMPONENT),
        Composable("RadioButton", "import androidx.compose.material.RadioButton\n", false, CodeTemplates.RADIO_BUTTON_COMPONENT),
        Composable("TextField", "import androidx.compose.material.TextField\n", false, CodeTemplates.TEXT_FIELD_COMPONENT),
        Composable("Card", "import androidx.compose.material.Card\n", false, CodeTemplates.CARD_COMPONENT),
        )
    val parameterTypes = listOf("String", "Int", "Boolean", "Event")
}
