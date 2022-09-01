package services.parsers

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.references.KtSimpleNameReferenceDescriptorsImpl
import org.jetbrains.kotlin.psi.*
import editor.Diagram
import editor.nodes.Action
import editor.nodes.ViewContainer

class ComponentParser {
    private fun callExpressions(callExpression: KtCallExpression?, parentView: ViewContainer, project: Project, diagram: Diagram, navMap: MutableMap<String, PsiElement>?, navEdges: MutableSet<Triple<Action, String, String>>?, destinations: MutableList<Pair<String, Action>>? = null) {
        // break condition
        if (callExpression == null) {
            return
        }

        // break expression into components
        val referenceExpression = PsiTreeUtil.findChildOfType(callExpression, KtReferenceExpression::class.java)
        val valueExpression = PsiTreeUtil.findChildOfType(callExpression, KtValueArgumentList::class.java)
        val lambdaExpression = PsiTreeUtil.findChildOfType(callExpression, KtLambdaArgument::class.java)

        // add view
        val view = ViewContainer(ManifestParser.getInstance(project)!!.getNameFromKtCallExpression(callExpression))
        view.psiElement = callExpression
        diagram.addChildToContainer(parentView, view)

        // parameters
        ParameterParser.getInstance(project)?.buildParameters(valueExpression, view, diagram, navMap, navEdges, destinations = destinations)

        // go to children
        val nextBlock = PsiTreeUtil.findChildOfType(lambdaExpression, KtBlockExpression::class.java)
        addCallExpressionsToView(nextBlock, view, project, diagram, navMap, navEdges, destinations = destinations)

        // resolve reference
        if (lambdaExpression == null) {
            if (referenceExpression?.references != null) {
                // find reference
                val element = resolveReference(referenceExpression)

                // check if is composable
                val modifiers = PsiTreeUtil.findChildOfType(element, KtModifierList::class.java)
                val isComposable = PsiTreeUtil
                    .findChildrenOfType(modifiers, KtReferenceExpression::class.java)
                    .any { it.text == "Composable" }

                if (isComposable) {
                    val block = PsiTreeUtil.findChildOfType(element, KtBlockExpression::class.java)
                    addCallExpressionsToView(block, view, project, diagram, navMap, navEdges, destinations = destinations)
                }
            }
        }
    }

    fun addCallExpressionsToView(block: KtBlockExpression?, view: ViewContainer, project: Project, diagram: Diagram, navMap: MutableMap<String, PsiElement>? = null, navEdges: MutableSet<Triple<Action, String, String>>? = null, destinations: MutableList<Pair<String, Action>>? = null) {
        if (block != null) {
            // go to all first siblings
            for (child in block.children) {
                if (child is KtProperty) {
                    ParameterParser.getInstance(project)?.stateProperties(child, view)
                }
                if (child is KtCallExpression) {
                    callExpressions(child, view, project, diagram, navMap, navEdges, destinations = destinations)
                }
            }
        }
    }

    fun resolveReference(referenceExpression: KtReferenceExpression): PsiElement? {
        for (reference in referenceExpression.references) {
            if (reference is KtSimpleNameReferenceDescriptorsImpl) {
                return reference.resolve()
            }
        }
        return null
    }

    fun findEvents(viewContainer: ViewContainer, project: Project): List<String> {
        val psiElement = viewContainer.psiElement
        val events = mutableListOf<String>()
        if (psiElement != null) {
            if (psiElement.firstChild is KtReferenceExpression) {
                val function = resolveReference(psiElement.firstChild as KtReferenceExpression)
                events.addAll(ParameterParser.getInstance(project)?.eventParameterList(function as KtNamedFunction)!!)
            }
        }
        return events
    }

    companion object {
        fun getInstance(project: Project): ComponentParser? {
            return project.getService(ComponentParser::class.java)
        }
    }
}
