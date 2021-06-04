package com.aaleksiev.rules

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiVariable
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.impl.source.PsiImmediateClassType
import org.jetbrains.kotlin.asJava.elements.KtLightTypeParameter
import org.jetbrains.kotlin.psi.*
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.KotlinUBlockExpression
import org.jetbrains.uast.kotlin.KotlinUField
import org.jetbrains.uast.kotlin.KotlinUSimpleReferenceExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.jetbrains.uast.getUastParentOfType as getUastParentOfType1

@Suppress("UnstableApiUsage")
class ViewBindingsDetector : Detector(), Detector.UastScanner {

    val typesMap = HashMap<String, KtTypeReference>()

    val methods = listOf("setValue", "postValue")

    override fun getApplicableUastTypes(): List<Class<out UElement>>? {
        return listOf(UCallExpression::class.java, UClass::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        return object : UElementHandler() {
            override fun visitClass(node: UClass) {
                for (element in node.uastDeclarations) {
                    if (element is KotlinUField) {
                        getFieldTypeReference(element)?.let {
                            // map the variable name to the type reference of its expression.
                            typesMap.put(element.name, it)
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

            override fun visitCallExpression(node: UCallExpression) {
                if (!isKotlin(node.sourcePsi) ||
                    !context.evaluator.isMemberInSubClassOf(
                        node.resolve()!!, "androidx.viewbinding.ViewBinding", false
                    )
                ) return

                val receiverType = node.receiverType as? PsiClassType
                var liveDataType =
                    if (receiverType != null && receiverType.hasParameters()) {
                        val receiver =
                            (node.receiver as? KotlinUSimpleReferenceExpression)?.resolve()
                        val variable = (receiver as? PsiVariable)
                        val assignment = variable?.let {
                            UastLintUtils.findLastAssignment(it, node)
                        }
                        val constructorExpression = assignment?.sourcePsi as? KtCallExpression
                        constructorExpression?.typeArguments?.singleOrNull()?.typeReference
                    } else {
                        getTypeArg(receiverType)
                    }
                if (liveDataType == null) {
                    liveDataType = typesMap[getVariableName(node)] ?: return
                }
                checkNullability(liveDataType, context, node)
            }

            private fun getVariableName(node: UCallExpression): String? {
                // We need to get the variable this expression is being assigned to
                // Given the assignment `liveDataField.value = null`
                // node.sourcePsi : `value`
                // dot: `.`
                // variable: `liveDataField`
                val dot = node.sourcePsi?.prevSibling
                val variable = dot?.prevSibling?.firstChild
                return variable?.text
            }
        }
    }

    /**
     * Iterates [classType]'s hierarchy to find its [androidx.lifecycle.LiveData] value type.
     *
     * @param classType The [PsiClassType] to search
     * @return The LiveData type argument.
     */
    fun getTypeArg(classType: PsiClassType?): KtTypeReference? {
        if (classType == null) {
            return null
        }
        val cls = classType.resolve().getUastParentOfType1<UClass>()
        val parentPsiType = cls?.superClassType as PsiClassType
        if (parentPsiType.hasParameters()) {
            val parentTypeReference = cls.uastSuperTypes[0]
            val superType = (parentTypeReference.sourcePsi as KtTypeReference).typeElement
            return superType!!.typeArgumentsAsTypes[0]
        }
        return getTypeArg(parentPsiType)
    }

    fun checkNullability(
        liveDataType: KtTypeReference,
        context: JavaContext,
        node: UCallExpression
    ) {
        // ignore generic types
        if (node.isGenericTypeDefinition()) return

        if (liveDataType.typeElement !is KtNullableType) {
            val fixes = mutableListOf<LintFix>()
            if (context.getLocation(liveDataType).file == context.file) {
                // Quick Fixes can only be applied to current file
                fixes.add(
                    fix().name("Change `LiveData` type to nullable")
                        .replace().with("?").range(context.getLocation(liveDataType)).end().build()
                )
            }
            val argument = node.valueArguments[0]
            if (argument.isNullLiteral()) {
                // Don't report null!! quick fix.
            } else if (argument.isNullable()) {
                fixes.add(
                    fix().name("Add non-null asserted (!!) call")
                        .replace().with("!!").range(context.getLocation(argument)).end().build()
                )
            }
        }
    }

    private fun UCallExpression.isGenericTypeDefinition(): Boolean {
        val classType = typeArguments.singleOrNull() as? PsiImmediateClassType
        val resolveGenerics = classType?.resolveGenerics()
        return resolveGenerics?.element is KtLightTypeParameter
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

/**
 * Checks if the [UElement] is nullable. Always returns `false` if the [UElement] is not a
 * [UReferenceExpression] or [UCallExpression].
 *
 * @return `true` if instance is nullable, `false` otherwise.
 */
internal fun UElement.isNullable(): Boolean {
    if (this is UCallExpression) {
        val psiMethod = resolve() ?: return false
        return psiMethod.hasAnnotation(NULLABLE_ANNOTATION)
    } else if (this is UReferenceExpression) {
        return (resolveToUElement() as? UAnnotated)?.findAnnotation(NULLABLE_ANNOTATION) != null
    }
    return false
}

const val NULLABLE_ANNOTATION = "org.jetbrains.annotations.Nullable"