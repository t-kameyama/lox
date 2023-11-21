package lox

sealed interface Expr {
    fun <R> accept(visitor: Visitor<R>): R

    data class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr {
        override fun <R> accept(visitor: Visitor<R>) = visitor.visitBinaryExpr(this)
    }

    data class Grouping(val expression: Expr) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitGroupingExpr(this)
    }

    data class Literal(val value: Any?) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitLiteralExpr(this)
    }

    data class Logical(val left: Expr, val operator: Token, val right: Expr) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitLogicalExpr(this)
    }

    data class Unary(val operator: Token, val right: Expr) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitUnaryExpr(this)
    }

    data class Variable(val name: Token) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitVariableExpr(this)
    }

    data class Assign(val name: Token, val value: Expr) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitAssignExpr(this)
    }

    data class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visitCallExpr(this)
    }

    interface Visitor<R> {
        fun visitBinaryExpr(expr: Binary): R
        fun visitGroupingExpr(expr: Grouping): R
        fun visitLiteralExpr(expr: Literal): R
        fun visitLogicalExpr(expr: Logical): R
        fun visitUnaryExpr(expr: Unary): R
        fun visitVariableExpr(expr: Variable): R
        fun visitAssignExpr(expr: Assign): R
        fun visitCallExpr(expr: Call): R
    }
}