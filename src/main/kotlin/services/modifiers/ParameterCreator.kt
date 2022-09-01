package services.modifiers

import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*
import editor.nodes.ViewContainer
import utils.Parameter
import utils.ComponentUtils
import utils.WriteActionDispatcher
import services.parsers.ComponentParser

class ParameterCreator(private val project: Project) {
    private val dispatcher = WriteActionDispatcher(project)

    fun create(parameter: Parameter, viewContainer: ViewContainer) {
        val factory = KtPsiFactory(project)
        val codeStylist = CodeStyleManager.getInstance(project)
        val parentElement = viewContainer.psiElement
        if (parameter.type == "String") {
            parameter.value = "\"${parameter.value}\""
        }
        if (!parameter.isState) {
            // check if value argument list if defined
            var valueArguments = parentElement?.firstChild
            while (valueArguments != null && valueArguments !is KtValueArgumentList) {
                valueArguments = valueArguments.nextSibling
            }
            // add parameter definition to custom Composable
            val composables = ComponentUtils.predefinedComposables.map { it.name }
            if (viewContainer.name.text !in composables) {
                // resolve reference expression
                val referenceExpression = viewContainer.psiElement?.firstChild as KtReferenceExpression
                val function = ComponentParser.getInstance(project)?.resolveReference(referenceExpression)
                val valueParameters = PsiTreeUtil.findChildOfType(function, KtParameterList::class.java)
                val isEmpty = PsiTreeUtil.findChildOfType(valueParameters, KtParameter::class.java) == null
                val newParameter = factory.createParameter("${parameter.name}: ${parameter.type}")
                dispatcher.dispatch {
                    if (!isEmpty) {
                        valueParameters?.addBefore(factory.createComma(), valueParameters.lastChild)
                    }
                    valueParameters?.addBefore(newParameter, valueParameters.lastChild)
                }
            }
            if (valueArguments != null) {
                val psiElement = factory.createArgument(", ${parameter.name} = ${parameter.value}")
                val isEmpty = PsiTreeUtil.findChildOfType(valueArguments, KtValueArgument::class.java) == null
                dispatcher.dispatch {
                    if (!isEmpty) {
                        valueArguments.addBefore(factory.createComma(), valueArguments.lastChild)
                    }
                    valueArguments.addBefore(psiElement, valueArguments.lastChild)
                    valueArguments.replace(codeStylist.reformat(valueArguments))
                }
            }
            else {
                val psiElement = factory.createCallArguments("${parameter.name} = ${parameter.value}")
                dispatcher.dispatch {
                    parentElement?.add(psiElement)
                }
            }
        }
        else {
            // check if lambda expression is defined
            var lambdaArgument = parentElement?.firstChild
            while (lambdaArgument != null && lambdaArgument !is KtLambdaArgument) {
                lambdaArgument = lambdaArgument.nextSibling
            }
            if (lambdaArgument != null) {
                val blockExpression = PsiTreeUtil.findChildOfType(lambdaArgument, KtBlockExpression::class.java)
                val stateProperty = factory.createProperty("val ${parameter.name} by remember { mutableStateOf(${parameter.value}) }")
                dispatcher.dispatch {
                    blockExpression?.addBefore(stateProperty, blockExpression.firstChild)
                    blockExpression?.addAfter(factory.createNewLine(), blockExpression.firstChild)
                }
            }
            else {
                val newElement = factory.createExpression("f() {  }").lastChild
                val psiElement = codeStylist.reformat(factory.createLambdaExpression("", "val ${parameter.name} by remember { mutableStateOf(${parameter.value}) }"))
                dispatcher.dispatch {
                    newElement.firstChild.replace(psiElement)
                    parentElement?.add(newElement)
                }
            }
        }
    }
}
