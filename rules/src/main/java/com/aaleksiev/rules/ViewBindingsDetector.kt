package com.aaleksiev.rules

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.KotlinUBlockExpression
import org.jetbrains.uast.kotlin.KotlinUField
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
                    val parameters = node.uastParameters.toString()
                    val body = node.uastBody?.asRenderString()
                    val expression = node.uastBody as? KotlinUBlockExpression
                    expression?.accept(object : AbstractUastVisitor() {
                        override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression): Boolean {
                            (node.getExpressionType() as PsiClassReferenceType)
                            if (node.identifier == "binding") {
                                val typeReference = node.sourcePsi
                                    ?.children
                                    ?.firstOrNull { it is KtTypeReference } as? KtTypeReference
                                val typeArgument =
                                    typeReference?.typeElement?.typeArgumentsAsTypes?.singleOrNull()
                                if (typeArgument != null) {
                                    return false
                                }
                                val expression = node.sourcePsi
                                    ?.children
                                    ?.firstOrNull { it is KtCallExpression } as? KtCallExpression
                                val typeRef =
                                    expression?.typeArguments?.singleOrNull()?.typeReference
                                //(node.getExpressionType() as PsiClassReferenceType).superTypes.any { it is KtTypeReference }
                            }
                            return true
                        }
                    })
                }
            }
        }
    }

    private fun getFieldTypeReference(element: KotlinUField): KtTypeReference? {
        // If field has type reference, we need to use type reference
        // Given the field `val liveDataField: MutableLiveData<Boolean> = MutableLiveData()`
        // reference: `MutableLiveData<Boolean>`
        // argument: `Boolean`
        val typeReference = element.sourcePsi
            ?.children
            ?.firstOrNull { it is KtTypeReference } as? KtTypeReference
        val typeArgument = typeReference?.typeElement?.typeArgumentsAsTypes?.singleOrNull()
        if (typeArgument != null) {
            return typeArgument
        }

        // We need to extract type from the call expression
        // Given the field `val liveDataField = MutableLiveData<Boolean>()`
        // expression: `MutableLiveData<Boolean>()`
        // argument: `Boolean`
        val expression = element.sourcePsi
            ?.children
            ?.firstOrNull { it is KtCallExpression } as? KtCallExpression
        return expression?.typeArguments?.singleOrNull()?.typeReference
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