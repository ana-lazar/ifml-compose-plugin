package services.modifiers

import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import editor.Diagram
import editor.nodes.ViewContainer
import utils.Composable
import org.jetbrains.kotlin.psi.*
import services.parsers.ActivityParser
import utils.CodeTemplates
import utils.WriteActionDispatcher

class ViewCreator(private val project: Project, private val diagram: Diagram) {
    private val dispatcher = WriteActionDispatcher(project)

    fun create(name: String, type: Composable, viewContainer: ViewContainer) {
        val factory = KtPsiFactory(project)
        val parentElement = viewContainer.psiElement
        var composable = type
        if (parentElement != null) {
            // add composable function definition to activity file, then composable
            if (type.name == "Custom") {
                val definition = CodeTemplates.COMPOSABLE.replace("%composableName%", name)
                val customFunction = factory.createFunction(definition)
                dispatcher.dispatch {
                    parentElement.containingFile.add(customFunction)
                }
                composable = Composable(name, "", true, "$name()")
            }
            // check if is activity
            if (viewContainer.parent == null) {
                val onCreateFunctions =
                    PsiTreeUtil.findChildrenOfType(viewContainer.psiElement, KtNamedFunction::class.java)
                if (onCreateFunctions.isEmpty()) return
                val onCreateFunction = onCreateFunctions.single { it.name == "onCreate" }
                // find setContent call
                val setContentCalls = PsiTreeUtil.findChildrenOfType(onCreateFunction, KtCallExpression::class.java)
                if (setContentCalls.isEmpty()) return
                val setContentCall = setContentCalls.single { it.firstChild.text == "setContent" }
                // find setContent block
                val setContentBlock = PsiTreeUtil.findChildOfType(setContentCall, KtBlockExpression::class.java)
                val psiElement = factory.createLambdaExpression("", "\n${composable.codeTemplate}\n")
                val callExpression = PsiTreeUtil.findChildOfType(psiElement, KtCallExpression::class.java)
                dispatcher.dispatch {
                    setContentBlock?.add(factory.createNewLine())
                    callExpression?.let { setContentBlock?.add(it) }
                }
            }
            else {
                // check if is in file
                val namedFunction = ActivityParser.getInstance(project)?.isComposableInFile(project, viewContainer.psiElement!!.containingFile, viewContainer.name.text)
                if (namedFunction != null) {
                    // add to definition
                    val blockExpression = PsiTreeUtil.getNextSiblingOfType(namedFunction.firstChild, KtBlockExpression::class.java)
                    val codeStylist = CodeStyleManager.getInstance(project)
                    val psiElement = codeStylist.reformat(factory.createLambdaExpression("", "\n${composable.codeTemplate}\n"))
                    val callExpression = PsiTreeUtil.findChildOfType(psiElement, KtCallExpression::class.java)
                    dispatcher.dispatch {
                        blockExpression?.addBefore(factory.createNewLine(), blockExpression.lastChild)
                        callExpression?.let { blockExpression?.addBefore(it, blockExpression.lastChild) }
                    }
                }
                else {
                    // check if element has lambda argument child
                    var lambdaArgument = parentElement.firstChild
                    while (lambdaArgument != null && lambdaArgument !is KtLambdaArgument) {
                        lambdaArgument = lambdaArgument.nextSibling
                    }
                    if (lambdaArgument != null) {
                        // add call expression to lambda expression
                        val lambdaExpression = lambdaArgument.firstChild
                        val codeStylist = CodeStyleManager.getInstance(project)
                        val psiElement = codeStylist.reformat(factory.createLambdaExpression("", "\n${composable.codeTemplate}\n"))
                        val callExpression = PsiTreeUtil.findChildOfType(psiElement, KtCallExpression::class.java)
                        // check if there are other expressions
                        val blockExpression = PsiTreeUtil.findChildOfType(lambdaExpression, KtBlockExpression::class.java)
                        val isEmpty = PsiTreeUtil.findChildOfType(blockExpression, KtExpression::class.java) == null
                        dispatcher.dispatch {
                            if (isEmpty) {
                                lambdaExpression?.replace(psiElement)
                            }
                            else {
                                blockExpression?.add(factory.createNewLine())
                                callExpression?.let { blockExpression?.add(it) }
                            }
                        }
                    }
                    else {
                        // add lambda expression
                        val codeStylist = CodeStyleManager.getInstance(project)
                        val newElement = factory.createExpression("f() { }").lastChild
                        val lambdaExpression = codeStylist.reformat(factory.createLambdaExpression("", "\n${composable.codeTemplate}\n"))
                        dispatcher.dispatch {
                            newElement.firstChild.replace(lambdaExpression)
                            parentElement.add(newElement)
                        }
                    }
                }
            }

            val childContainer = ViewContainer(name)
            diagram.addChildToContainer(viewContainer, childContainer)
        }
    }
}
