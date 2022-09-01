package services.parsers

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.references.KtSimpleNameReferenceDescriptorsImpl
import org.jetbrains.kotlin.idea.structuralsearch.visitor.KotlinRecursiveElementVisitor
import org.jetbrains.kotlin.psi.*
import editor.Diagram
import editor.edges.ActionEdge
import editor.nodes.Action
import editor.nodes.ViewContainer
import utils.Composable

class ActivityParser {
    private fun buildNavigationMap(lambdaExpression: KtLambdaArgument?, project: Project): MutableMap<String, PsiElement> {
        val navMap = mutableMapOf<String, PsiElement>()
        var callExpression = PsiTreeUtil.findChildOfType(lambdaExpression, KtCallExpression::class.java)
        while (callExpression != null) {
            var routeName = ""
            val referenceExpr = callExpression.firstChild
            val valueArguments = PsiTreeUtil.getNextSiblingOfType(referenceExpr, KtValueArgumentList::class.java)
            val lambdaExpr = PsiTreeUtil.getNextSiblingOfType(valueArguments, KtLambdaArgument::class.java)
            if (referenceExpr is KtReferenceExpression && referenceExpr.text == "composable") {
                val routeArgument = PsiTreeUtil.findChildrenOfType(valueArguments, KtValueArgumentName::class.java).filter { it.text == "route" }
                if (routeArgument.isNotEmpty()) {
                    routeName = PsiTreeUtil.getNextSiblingOfType(routeArgument[0], KtExpression::class.java)?.text ?: ""
                }
            }
            val callExpr = PsiTreeUtil.findChildOfType(lambdaExpr, KtCallExpression::class.java)
            if (callExpr?.firstChild is KtReferenceExpression) {
                val function = ComponentParser.getInstance(project)?.resolveReference(callExpr.firstChild as KtReferenceExpression)
                function?.let { navMap.put(routeName, it) }
            }
            callExpression = PsiTreeUtil.getNextSiblingOfType(callExpression, KtCallExpression::class.java)
        }
        return navMap
    }

    private fun buildNavigationDiagram(navigationHost: PsiElement, mainView: ViewContainer, project: Project, diagram: Diagram, destinations: MutableList<Pair<String, Action>>? = null) {
        // break expression into components
        val valueArguments = PsiTreeUtil.getNextSiblingOfType(navigationHost, KtValueArgumentList::class.java)
        val lambdaExpression = PsiTreeUtil.getNextSiblingOfType(navigationHost, KtLambdaArgument::class.java)

        // find navController parameter
        val controllerArgument = PsiTreeUtil
            .findChildrenOfType(valueArguments, KtValueArgumentName::class.java)
            .filter { it.text == "navController" }
        if (controllerArgument.isEmpty()) return
        val navController = PsiTreeUtil.getNextSiblingOfType(controllerArgument[0], KtReferenceExpression::class.java)
            ?.let { ComponentParser.getInstance(project)?.resolveReference(it) }

        // find startDestination parameter
        val startArgument = PsiTreeUtil
            .findChildrenOfType(valueArguments, KtValueArgumentName::class.java)
            .filter { it.text == "startDestination" }
        if (startArgument.isEmpty()) return
        val startDestination = PsiTreeUtil.getNextSiblingOfType(startArgument[0], KtExpression::class.java)?.text

        // build navigation map
        val navMap = buildNavigationMap(lambdaExpression, project)
        val navEdges = mutableSetOf<Triple<Action, String, String>>()

        // find startDestination composable function
        for (navDest in navMap) {
            val parameterList = PsiTreeUtil.getNextSiblingOfType(navDest.value.firstChild, KtParameterList::class.java)
            val id = parameterList?.prevSibling?.text
            if (id != null) {
                val view = ViewContainer(id)
                diagram.addChildToContainer(mainView, view)
                val block = PsiTreeUtil.findChildOfType(navDest.value, KtBlockExpression::class.java)
                ComponentParser.getInstance(project)?.addCallExpressionsToView(block, view, project, diagram, navMap, navEdges, destinations = destinations)
            }
        }

        // add navigation action edges
        for (triple in navEdges) {
            val node = diagram.findNode(triple.second)
            if (node != null) {
                diagram.addEdge(ActionEdge(triple.first, node, ""))
            }
        }
    }

    private fun navHost(callExpression: KtCallExpression?): PsiElement? {
        if (callExpression == null) {
            return null
        }
        val referenceExpression = PsiTreeUtil.findChildOfType(callExpression, KtReferenceExpression::class.java)
        if (referenceExpression?.text == "NavHost") {
            return referenceExpression
        }
        val lambdaExpression = PsiTreeUtil.findChildOfType(callExpression, KtLambdaArgument::class.java)
        val nextBlock = PsiTreeUtil.findChildOfType(lambdaExpression, KtBlockExpression::class.java)
        findNavHost(nextBlock)
        if (lambdaExpression == null) {
            if (referenceExpression?.references != null) {
                for (reference in referenceExpression.references) {
                    if (reference is KtSimpleNameReferenceDescriptorsImpl) {
                        val element = reference.resolve()
                        val block = PsiTreeUtil.findChildOfType(element, KtBlockExpression::class.java)
                        return findNavHost(block)
                    }
                }
            }
        }
        return null
    }

    private fun findNavHost(block: KtBlockExpression?): PsiElement? {
        if (block == null) return null
        // go to all first siblings
        for (child in block.children) {
            if (child is KtCallExpression) {
                return navHost(child)
            }
        }
        return null
    }

    fun buildActivity(project: Project, file: PsiFile, diagram: Diagram, mainView: ViewContainer, destinations: MutableList<Pair<String, Action>>?) {
        try {
            DumbService
                .getInstance(project).runWhenSmart {
                    // find onCreate function definition
                    val onCreateFunctions =
                        PsiTreeUtil
                            .findChildrenOfType(file, KtNamedFunction::class.java)

                    if (onCreateFunctions.isEmpty()) (return@runWhenSmart)

                    val onCreateFunction = onCreateFunctions
                        .single { it.name == "onCreate" }

                    // find setContent call
                    val setContentCalls =
                        PsiTreeUtil
                            .findChildrenOfType(onCreateFunction, KtCallExpression::class.java)

                    if (setContentCalls.isEmpty()) (return@runWhenSmart)

                    val setContentCall = setContentCalls
                        .single { it.firstChild.text == "setContent" }

                    // find setContent block
                    val setContentBlock =
                        PsiTreeUtil
                            .findChildOfType(setContentCall, KtBlockExpression::class.java)

                    // check if navigation is present
                    val navigationHost = findNavHost(setContentBlock)
                    if (navigationHost == null) {
                        // get call expressions from setContent
                        ComponentParser.getInstance(project)?.addCallExpressionsToView(setContentBlock, mainView, project, diagram, destinations = destinations)
                    }
                    else {
                        // parse navigation host
                        buildNavigationDiagram(navigationHost, mainView, project, diagram, destinations = destinations)
                    }
                }
        }
        catch (exception: Exception) {
            println(exception.message)
        }
    }

    fun getComposablesForFile(project: Project, file: PsiFile): MutableList<Composable> {
        val composables = mutableListOf<Composable>()
        file.accept(object : KotlinRecursiveElementVisitor() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                super.visitNamedFunction(function)
                val modifierList = PsiTreeUtil.findChildOfType(function, KtModifierList::class.java)
                val composableAnnotation = PsiTreeUtil.findChildrenOfType(modifierList, KtReferenceExpression::class.java).filter { it.text == "Composable" }
                if (composableAnnotation.isNotEmpty()) {
                    // value parameter list
                    var parTemplate = ""
                    val parameterList = ParameterParser.getInstance(project)?.parameterList(function)
                    for (parameter in parameterList!!) {
                        if (parTemplate != "") {
                            parTemplate += ", "
                        }
                        val value = when (parameter.second) {
                            "String" -> {
                                "\"\""
                            }
                            "Int" -> {
                                "0"
                            }
                            "Boolean" -> {
                                "false"
                            }
                            else -> {
                                "{ }"
                            }
                        }
                        parTemplate += parameter.first + " = " + value
                    }
                    function.name?.let { composables.add(Composable(it, "", false, "$it($parTemplate)")) }
                }
            }
        })
        return composables
    }

    fun isComposableInFile(project: Project, file: PsiFile, name: String): KtNamedFunction? {
        var namedFunction: KtNamedFunction? = null
        file.accept(object : KotlinRecursiveElementVisitor() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                super.visitNamedFunction(function)
                val modifierList = PsiTreeUtil.findChildOfType(function, KtModifierList::class.java)
                val composableAnnotation = PsiTreeUtil.findChildrenOfType(modifierList, KtReferenceExpression::class.java).filter { it.text == "Composable" }
                if (composableAnnotation.isNotEmpty() && function.name == name) {
                    namedFunction = function
                }
            }
        })
        return namedFunction
    }

    companion object {
        fun getInstance(project: Project): ActivityParser? {
            return project.getService(ActivityParser::class.java)
        }
    }
}
