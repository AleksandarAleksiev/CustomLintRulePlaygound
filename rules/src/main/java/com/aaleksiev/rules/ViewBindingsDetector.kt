package com.aaleksiev.rules

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.kotlin.KotlinUBlockExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor

@Suppress("UnstableApiUsage")
class ViewBindingsDetector : Detector(), Detector.UastScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement?>> {
        return listOf(UMethod::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitMethod(node: UMethod) {
                val noteString = node.asRenderString()
                println("CustomLint: visited $noteString")
                val nodeName = node.name
                if (nodeName.equals("onSaveInstanceState", ignoreCase = true)) {
                    val expression = node.uastBody as? KotlinUBlockExpression
                    expression?.accept(object : AbstractUastVisitor() {
                        override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression): Boolean {
                            if (!isKotlin(node.sourcePsi)) return true
                            node.resolve()?.let { element ->
                                //node.getExpressionType()?.isConvertibleFrom(psiType)
                                val psiType = PsiType.getTypeByName("androidx.viewbinding.ViewBinding", element.project, element.resolveScope)
                                if ((element as? KtLightMethodImpl)?.returnTypeElement?.type?.isConvertibleFrom(psiType) == true) {
                                    //report an issue
                                    return false
                                }
                            }
                            return true
                        }
                    })
                }
            }
        }
    }

    companion object {
        /**
         * Issue describing the problem and pointing to the detector
         * implementation.
         */
        @JvmField
        val ISSUE: Issue = Issue.create(
            // ID: used in @SuppressLint warnings etc
            id = "ShortUniqueId",
            // Title -- shown in the IDE's preference dialog, as category headers in the
            // Analysis results window, etc
            briefDescription = "Lint Mentions",
            // Full explanation of the issue; you can use some markdown markup such as
            // `monospace`, *italic*, and **bold**.
            explanation = """
                    This check highlights string literals in code which mentions the word `lint`. \
                    Blah blah blah.
                    Another paragraph here.
                    """, // no need to .trimIndent(), lint does that automatically
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                ViewBindingsDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}