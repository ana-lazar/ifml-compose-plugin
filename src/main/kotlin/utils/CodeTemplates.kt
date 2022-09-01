package utils

object CodeTemplates {
    const val ACTIVITY_FILE = "package %packageName%\n" +
            "\n" +
            "import android.os.Bundle\n" +
            "import androidx.activity.ComponentActivity\n" +
            "import androidx.activity.compose.setContent\n" +
            "\n" +
            "class %activityName%Activity : ComponentActivity() {\n" +
            "    override fun onCreate(savedInstanceState: Bundle?) {\n" +
            "        super.onCreate(savedInstanceState)\n" +
            "        setContent {\n" +
            "\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
    const val COMPOSABLE = "@Composable\n" +
            "fun %composableName%() {\n" +
            "}"
    const val ACTIVITY_TAG = "<activity android:name=\"%activityPath%Activity\"/>"
    const val TEXT_COMPONENT = "Text(text = \"\")"
    const val BUTTON_COMPONENT = "Button(onClick = { /* TODO */ }) {\n" +
            "                \n" +
            "            }"
    const val SCAFFOLD_COMPONENT =  "Scaffold() {\n" +
            "                \n" +
            "            }"
    const val COLUMN_COMPONENT =  "Column() {\n" +
            "                \n" +
            "            }"
    const val CHECKBOX_COMPONENT =  "Checkbox(checked = false, onCheckedChange = { /*TODO*/ })Radio"
    const val RADIO_BUTTON_COMPONENT =  "RadioButton(selected = false, onClick = { /*TODO*/ })T"
    const val TEXT_FIELD_COMPONENT =  "TextField(value = \"\", onValueChange = { /*TODO*/ })"
    const val CARD_COMPONENT =  "Card() {\n" +
            "                \n" +
            "            }"
}
