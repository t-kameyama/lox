package lox

class AstPrinter : Expr.Visitor<String> {
    fun print(expr: Expr): String = expr.accept(this)

    override fun visitAssignExpr(expr: Expr.Assign): String {
        TODO("Not yet implemented")
    }

    override fun visitCallExpr(expr: Expr.Call): String {
        TODO("Not yet implemented")
    }

    override fun visitGetExpr(expr: Expr.Get): String {
        TODO("Not yet implemented")
    }

    override fun visitSetExpr(expr: Expr.Set): String {
        TODO("Not yet implemented")
    }

    override fun visitThisExpr(expr: Expr.This): String {
        TODO("Not yet implemented")
    }

    override fun visitSuperExpr(expr: Expr.Super): String {
        TODO("Not yet implemented")
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

    override fun visitLogicalExpr(expr: Expr.Logical): String {
        TODO("Not yet implemented")
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
