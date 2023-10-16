package lox

class AstPrinter : Expr.Visitor<String> {
    fun print(expr: Expr): String = expr.accept(this)

    override fun visitAssignExpr(expr: Expr.Assign): String {
        TODO()
    }

    override fun visitBinaryExpr(expr: Expr.Binary): String {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): String {
        return parenthesize("group", expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): String {
        return expr.value?.toString() ?: "nil"
    }

    override fun visitUnaryExpr(expr: Expr.Unary): String {
        return parenthesize(expr.operator.lexeme, expr.right)
    }

    override fun visitVariableExpr(expr: Expr.Variable): String {
        return expr.name.lexeme
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val visitor = this
        return buildString {
            append("($name")
            exprs.forEach { expr ->
                append(" ")
                append(expr.accept(visitor))
            }
            append(")")
        }
    }
}
