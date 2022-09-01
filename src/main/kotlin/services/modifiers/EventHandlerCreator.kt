package services.modifiers

import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*
import editor.nodes.ViewContainer
import utils.ComponentUtils
import utils.WriteActionDispatcher
import services.parsers.ComponentParser

class EventHandlerCreator(private val project: Project) {
    private val dispatcher = WriteActionDispatcher(project)

    fun create(event: String, isCustom: Boolean, viewContainer: ViewContainer) {
        val factory = KtPsiFactory(project)
        val codeStylist = CodeStyleManager.getInstance(project)
        val psiElement = viewContainer.psiElement
        if (psiElement != null) {
            // check if value argument list if defined
            var valueArguments = psiElement.firstChild
            while (valueArguments != null && valueArguments !is KtValueArgumentList) {
                valueArguments = valueArguments.nextSibling
            }
            if (isCustom) {
                val composables = ComponentUtils.predefinedComposables.map { it.name }
                if (viewContainer.name.text !in composables) {
                    // resolve reference expression
                    val referenceExpression = viewContainer.psiElement?.firstChild as KtReferenceExpression
                    val function = ComponentParser.getInstance(project)?.resolveReference(referenceExpression)
                    val valueParameters = PsiTreeUtil.findChildOfType(function, KtParameterList::class.java)
                    val isEmpty = PsiTreeUtil.findChildOfType(valueParameters, KtParameter::class.java) == null
                    val newEvent = factory.createParameter("$event: () -> Unit")
                    dispatcher.dispatch {
                        if (!isEmpty) {
                            valueParameters?.addBefore(factory.createComma(), valueParameters.lastChild)
                        }
                        valueParameters?.addBefore(newEvent, valueParameters.lastChild)
                    }
                }
            }
            if (valueArguments != null) {
                val element = factory.createArgument(", $event = { }")
                val isAEmpty = PsiTreeUtil.findChildOfType(valueArguments, KtValueArgument::class.java) == null
                dispatcher.dispatch {
                    if (!isAEmpty) {
                        valueArguments.addBefore(factory.createComma(), valueArguments.lastChild)
                    }
                    valueArguments.addBefore(element, valueArguments.lastChild)
                    valueArguments.replace(codeStylist.reformat(valueArguments))
                }
            }
            else {
                val element = factory.createCallArguments("$event = { }")
                dispatcher.dispatch {
                    psiElement.add(element)
                }
            }
        }
    }
}
