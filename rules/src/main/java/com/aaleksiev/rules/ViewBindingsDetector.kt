package com.aaleksiev.rules

import com.android.SdkConstants
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.kotlin.KotlinUBlockExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor

@Suppress("UnstableApiUsage")
class ViewBindingsDetector : Detector(), SourceCodeScanner {
    private val methodsToVisit by lazy {
        listOf("onCreate", "onCreateView", "onSaveInstanceState", "onDestroy")
    }

    override fun applicableSuperClasses(): List<String> = listOf(SdkConstants.CLASS_FRAGMENT, SdkConstants.CLASS_V4_FRAGMENT.newName(), SdkConstants.CLASS_V4_FRAGMENT.oldName())

    override fun visitClass(context: JavaContext, declaration: UClass) {
        val viewBindingPsiType = PsiType.getTypeByName(
                VIEW_BINDING_CLASS,
                declaration.project,
                declaration.resolveScope
        )

        declaration.accept(MethodVisitor(methodsToVisit) { visitedMethod ->
            object : AbstractUastVisitor() {
                override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression): Boolean {
                    if (isKotlin(node.sourcePsi)) {
                        node.resolve()?.let { element ->
                            val methodImpl = element as? KtLightMethodImpl
                            if (methodImpl.isDescendantOf(viewBindingPsiType)) {
                                //report an issue
                                context.report(
                                        ISSUE,
                                        visitedMethod,
                                        context.getLocation(node),
                                        "[${visitedMethod.name}] Should not attempt to access view bindings after Fragment view was destroyed."
                                )
                            }
                        }
                    }
                    return super.visitSimpleNameReferenceExpression(node)
                }
            }
        })
    }

    private fun KtLightMethodImpl?.isDescendantOf(psiType: PsiType): Boolean = when {
        this == null -> false
        else -> returnType?.isAssignableFrom(psiType) == true ||
                returnType?.superTypes
                        ?.filter { superType ->
                            !superType.canonicalText.equals(
                                    "java.lang.Object",
                                    ignoreCase = true
                            )
                        }
                        ?.any { superType -> superType.isAssignableFrom(psiType) } == true
    }

    companion object {
        private const val VIEW_BINDING_CLASS = "androidx.viewbinding.ViewBinding"
        /**
         * Issue describing the problem and pointing to the detector
         * implementation.
         */
        val ISSUE = Issue.create(
                id = "AccessDestroyedView",
                briefDescription = "Accessing Fragment view after it was destroyed",
                explanation = "This check ensures that Fragment View's are not accessed before onCreateView() or after onDestroyView().",
                category = Category.INTEROPERABILITY_KOTLIN,
                severity = Severity.FATAL,
                implementation = Implementation(
                        ViewBindingsDetector::class.java,
                        Scope.JAVA_FILE_SCOPE
                ),
                androidSpecific = true
        )
    }
}