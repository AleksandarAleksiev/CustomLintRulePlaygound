package com.aaleksiev.rules

import org.jetbrains.uast.UMethod
import org.jetbrains.uast.kotlin.KotlinUBlockExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor

@Suppress("UnstableApiUsage")
internal class MethodVisitor(private val methodsToVisit: List<String>,
                             private val visitor: (UMethod) -> AbstractUastVisitor) : AbstractUastVisitor() {
    override fun visitMethod(node: UMethod): Boolean {
        if (node.name isAnyOf methodsToVisit) {
            val expression = node.uastBody as? KotlinUBlockExpression
            expression?.accept(visitor.invoke(node))
        }
        return super.visitMethod(node)
    }

    private infix fun String.isAnyOf(strings: List<String>): Boolean {
        return strings.contains(this)
    }
}