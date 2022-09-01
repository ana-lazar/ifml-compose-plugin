package services.parsers

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.references.KtSimpleNameReferenceDescriptorsImpl
import org.jetbrains.kotlin.psi.*
import editor.Diagram
import editor.edges.ActionEdge
import editor.nodes.Action
import editor.nodes.StateParameterElement
import editor.nodes.ViewContainer

class ParameterParser {
    private fun parameterNamesMap(valueArguments: KtValueArgumentList?): MutableMap<Int, String> {
        val map = mutableMapOf<Int, String>()

        // find reference from value arguments
        val referenceExpression = PsiTreeUtil.getPrevSiblingOfType(valueArguments, KtReferenceExpression::class.java)
        if (referenceExpression?.references != null) {
            for (reference in referenceExpression.references) {
                if (reference is KtSimpleNameReferenceDescriptorsImpl) {
                    // resolve reference
                    val element = reference.resolve()
                    val parameterList = PsiTreeUtil.findChildOfType(element, KtParameterList::class.java)

                    // build parameter map
                    var count = 0
                    var parameter = PsiTreeUtil.findChildOfType(parameterList, KtParameter::class.java)
                    while (parameter != null) {
                        map[count] = parameter.firstChild.text
                        parameter = PsiTreeUtil.getNextSiblingOfType(parameter, KtParameter::class.java)
                        count++
                    }
                }
            }
        }
        return map
    }

    fun buildParameters(valueExpression: KtValueArgumentList?, view: ViewContainer, diagram: Diagram, navMap: MutableMap<String, PsiElement>? = null, navEdges: MutableSet<Triple<Action, String, String>>? = null, destinations: MutableList<Pair<String, Action>>? = null) {
        var nameMap: MutableMap<Int, String>? = null
        var eventParameterList: List<String> = mutableListOf()
        var index = 0

        // go through all arguments
        var valueArgument = PsiTreeUtil.findChildOfType(valueExpression, KtValueArgument::class.java)
        while (valueArgument != null) {
            var name: String
            var value = ""
            val firstExpression = valueArgument.firstChild

            if (firstExpression is KtValueArgumentName) {
                name = firstExpression.text
                val valueExpr = PsiTreeUtil.getNextSiblingOfType(valueArgument.firstChild, KtExpression::class.java)
                if (valueExpr != null) {
                    value = valueExpr.text
                }

                // lambda action argument
                val lambda = PsiTreeUtil.getNextSiblingOfType(valueArgument.firstChild, KtLambdaExpression::class.java)
                if (lambda != null) {
                    if (eventParameterList.isEmpty()) {
                        eventParameterList = findEventParameters(view.psiElement)
                    }
                    if (name in eventParameterList) {
                        val blockExpression = PsiTreeUtil.findChildOfType(valueExpr, KtBlockExpression::class.java)
                        value = blockExpression?.text ?: value
                        val action = Action(value)
                        val parent = view.findParentRecursively()
                        action.psiElement = valueExpr
                        action.position(parent.x + 100, parent.y + parent.height * 2)
                        diagram.addNode(action)
                        diagram.addEdge(ActionEdge(view, action, name))

                        // check if state parameters are used
                        val identifiers = findAssignedIdentifiers(lambda, mutableListOf())
                        var currentParent = view.parent
                        while (currentParent != null) {
                            for (parameter in currentParent.parameters) {
                                if (parameter is StateParameterElement && parameter.originalName in identifiers) {
                                    diagram.addEdge(ActionEdge(action, currentParent, parameter.originalName))
                                }
                            }
                            currentParent = currentParent.parent
                        }

                        // check if handler involves navigation with composables
                        val composableDestinations = findComposableDestinations(lambda, mutableListOf())
                        for (destination in composableDestinations) {
                            val destinationElement = navMap?.get("\"$destination\"")
                            val parameterList = PsiTreeUtil.getNextSiblingOfType(destinationElement?.firstChild, KtParameterList::class.java)
                            val id = parameterList?.prevSibling?.text
                            if (id != null) {
                                navEdges?.add(Triple(action, id, name))
                            }
                        }

                        // check if handler involves navigation with other activities
                        findActivityDestinations(lambda, destinations, action)

                        valueArgument = PsiTreeUtil.getNextSiblingOfType(valueArgument, KtValueArgument::class.java)
                        continue
                    }
                }
            }
            else {
                if (nameMap == null) {
                    nameMap = parameterNamesMap(valueExpression)
                }

                // update name and value by index
                name = nameMap[index]!!
                value = firstExpression.text
            }

            view.addParameter(name, value)
            valueArgument = PsiTreeUtil.getNextSiblingOfType(valueArgument, KtValueArgument::class.java)
            index += 1
        }
    }

    private fun findEventParameters(psiElement: PsiElement?): List<String> {
        val referenceExpression = PsiTreeUtil.findChildOfType(psiElement, KtReferenceExpression::class.java)
        for (reference in referenceExpression!!.references) {
            if (reference is KtSimpleNameReferenceDescriptorsImpl) {
                val function = reference.resolve()
                return eventParameterList(function as KtNamedFunction)
            }
        }
        return mutableListOf()
    }

    private fun findAssignedIdentifiers(expression: KtExpression, identifiers: MutableList<String>): MutableList<String> {
        expression.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)

                // if identifier is on the left side of an assignment
                if (element is KtReferenceExpression && element.nextSibling != null && element.parent is KtBinaryExpression) {
                    identifiers.add(element.text)
                }

                // if a function is called
//                else if (element is KtReferenceExpression) {
//                    for (reference in element.references) {
//                        if (reference is KtSimpleNameReferenceDescriptorsImpl) {
//                            val e = reference.resolve()
//                            val block = PsiTreeUtil.findChildOfType(e, KtBlockExpression::class.java)
//                            block?.let { findAssignedIdentifiers(it, identifiers) }
//                        }
//                    }
//                }
            }
        })

        return identifiers
    }

    // TODO fix to work even with .value DotExpression
    fun stateProperties(property: KtProperty, view: ViewContainer) {
        // get property identifier
        val child = property.firstChild.nextSibling.nextSibling
        val id = child.text

        // find call expression definition
        val callExpression = PsiTreeUtil.findChildOfType(property, KtCallExpression::class.java)
        if (callExpression != null) {
            // is state definition
            val referenceExpression = callExpression.firstChild
            if (referenceExpression.text == "remember") {
                val constantExpression = PsiTreeUtil.findChildOfType(callExpression, KtConstantExpression::class.java)
                if (constantExpression != null) {
                    view.addStateParameter(id, constantExpression.text)
                }
            }
        }
    }

    private fun findComposableDestinations(expression: KtExpression, destinations: MutableList<String>): MutableList<String> {
        expression.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)

                // if navController.navigate is called
                val child = element.firstChild
                if (element is KtDotQualifiedExpression) {
                    if (child != null && child is KtReferenceExpression && child.text == "navController") {
                        val callExpr = child.nextSibling.nextSibling.firstChild
                        if (callExpr != null && callExpr is KtReferenceExpression && callExpr.text == "navigate") {
                            val destination =  PsiTreeUtil.findChildOfType(callExpr.nextSibling, KtLiteralStringTemplateEntry::class.java)?.text
                            destination?.let { destinations.add(it) }
                        }
                    }
                }

                // if a function is called
//                else if (element is KtReferenceExpression) {
//                    for (reference in element.references) {
//                        if (reference is KtSimpleNameReferenceDescriptorsImpl) {
//                            val e = reference.resolve()
//                            val block = PsiTreeUtil.findChildOfType(e, KtBlockExpression::class.java)
//                            block?.let { findComposableDestinations(it, destinations) }
//                        }
//                    }
//                }
            }
        })

        return destinations
    }

    private fun findActivityDestinations(expression: KtExpression, destinations: MutableList<Pair<String, Action>>?, action: Action) {
        expression.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)

                // if startActivity is called
                val child = element.firstChild
                if (element is KtCallExpression) {
                    if (child != null && child is KtReferenceExpression && child.text == "startActivity") {
                        val valueArguments = PsiTreeUtil.getNextSiblingOfType(child, KtValueArgumentList::class.java)
                        val intentArgument = PsiTreeUtil.findChildOfType(valueArguments, KtReferenceExpression::class.java)
                        if (intentArgument != null) {
                            for (reference in intentArgument.references) {
                                if (reference is KtSimpleNameReferenceDescriptorsImpl) {
                                    // resolve property
                                    val property = reference.resolve()
                                    // find call expression
                                    val callExpr = PsiTreeUtil.findChildOfType(property, KtCallExpression::class.java)
                                    val firstArgument = PsiTreeUtil.findChildOfType(callExpr, KtValueArgument::class.java)
                                    val secondArgument = PsiTreeUtil.getNextSiblingOfType(firstArgument, KtValueArgument::class.java)
                                    val destinationExpr = PsiTreeUtil.findChildOfType(secondArgument, KtReferenceExpression::class.java)
                                    destinationExpr?.let { destinations?.add(Pair(destinationExpr.text, action)) }
                                }
                            }
                        }
                    }
                }

                // if a function is called
//                else if (element is KtReferenceExpression) {
//                    for (reference in element.references) {
//                        if (reference is KtSimpleNameReferenceDescriptorsImpl) {
//                            val e = reference.resolve()
//                            val block = PsiTreeUtil.findChildOfType(e, KtBlockExpression::class.java)
//                            block?.let { findActivityDestinations(it, destinations, action) }
//                        }
//                    }
//                }
            }
        })
    }

    fun parameterList(function: KtNamedFunction): List<Pair<String, String>> {
        val parameters = mutableListOf<Pair<String, String>>()
        val parameterList = PsiTreeUtil.findChildOfType(function, KtParameterList::class.java)
        var parameter: PsiElement? = PsiTreeUtil.findChildOfType(parameterList, KtParameter::class.java)
        while (parameter != null) {
            if (parameter is KtParameter) {
                val id: String = parameter.firstChild.text
                val type: String = PsiTreeUtil.findChildOfType(parameter, KtTypeReference::class.java)?.text!!
                parameters.add(Pair(id, type))
            }
            parameter = parameter.nextSibling
        }
        return parameters
    }

    fun eventParameterList(function: KtNamedFunction): List<String> {
        val parameters = mutableListOf<String>()
        val parameterList = PsiTreeUtil.findChildOfType(function, KtParameterList::class.java)
        var parameter: PsiElement? = PsiTreeUtil.findChildOfType(parameterList, KtParameter::class.java)
        while (parameter != null) {
            if (parameter is KtParameter) {
                if (PsiTreeUtil.findChildOfType(parameter, KtFunctionType::class.java) != null &&
                    PsiTreeUtil.findChildOfType(parameter, KtModifierList::class.java) == null) {
                    parameters.add(parameter.firstChild.text)
                }
            }
            parameter = parameter.nextSibling
        }
        return parameters
    }

    companion object {
        fun getInstance(project: Project): ParameterParser? {
            return project.getService(ParameterParser::class.java)
        }
    }
}
