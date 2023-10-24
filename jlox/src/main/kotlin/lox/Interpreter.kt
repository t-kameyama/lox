package lox

import lox.TokenType.AND
import lox.TokenType.BANG
import lox.TokenType.BANG_EQUAL
import lox.TokenType.EQUAL_EQUAL
import lox.TokenType.GREATER
import lox.TokenType.GREATER_EQUAL
import lox.TokenType.LESS
import lox.TokenType.LESS_EQUAL
import lox.TokenType.MINUS
import lox.TokenType.OR
import lox.TokenType.PLUS
import lox.TokenType.SLASH
import lox.TokenType.STAR
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
    private var environment = Environment()

    fun interpret(statements: List<Stmt>) {
        try {
            for (statement in statements) {
                execute(statement)
            }
        } catch (e: RuntimeError) {
            Lox.runtimeError(e)
        }
    }

    private fun execute(stmt: Stmt) {
        stmt.accept(this)
    }

    private fun evaluate(expr: Expr): Any? {
        return expr.accept(this)
    }

    private fun evaluateBlock(statements: List<Stmt>, environment: Environment) {
        val previous = this.environment
        try {
            this.environment = environment
            statements.forEach { execute(it) }
        } finally {
            this.environment = previous
        }
    }

    private fun stringify(value: Any?): String {
        if (value == null) return "nil"

        val text = value.toString()
        if (value is Double) {
            return if (text.endsWith(".0")) text.removeSuffix(".0") else text
        }

        return text
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        environment.define(stmt.name, stmt.initializer?.let { evaluate(it) })
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expr)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expr)
        println(stringify(value))
    }

    override fun visitBlockStmt(block: Stmt.Block) {
        evaluateBlock(block.statements, Environment(environment))
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch)
        }
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body)
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)
        environment.assign(expr.name, value)
        return value
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)
        val operator = expr.operator
        return when (operator.type) {
            PLUS -> {
                when {
                    left is Double && right is Double -> left + right
                    left is String && right is String -> left + right
                    else -> throw RuntimeError(
                        operator,
                        "Operands must be two numbers or two strings: left=$left, right=$right"
                    )
                }
            }

            MINUS -> {
                checkNumberOperand(operator, left, right)
                left.toDouble() - right.toDouble()
            }

            SLASH -> {
                checkNumberOperand(operator, left, right)
                left.toDouble() / right.toDouble()
            }

            STAR -> {
                checkNumberOperand(operator, left, right)
                left.toDouble() * right.toDouble()
            }

            GREATER -> {
                checkNumberOperand(operator, left, right)
                left.toDouble() > right.toDouble()
            }

            GREATER_EQUAL -> {
                checkNumberOperand(operator, left, right)
                left.toDouble() >= right.toDouble()
            }

            LESS -> {
                checkNumberOperand(operator, left, right)
                left.toDouble() < right.toDouble()
            }

            LESS_EQUAL -> {
                checkNumberOperand(operator, left, right)
                left.toDouble() <= right.toDouble()
            }

            BANG_EQUAL -> {
                !isEqual(left, right)
            }

            EQUAL_EQUAL -> {
                isEqual(left, right)
            }

            else -> {
                null
            }
        }
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? {
        return expr.value
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Any? {
        val left = evaluate(expr.left)
        val leftIsTruthy = isTruthy(left)
        val type = expr.operator.type
        return when {
            type == OR && leftIsTruthy -> left
            type == AND && !leftIsTruthy -> left
            else -> evaluate(expr.right)
        }
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)
        val operator = expr.operator
        return when (operator.type) {
            MINUS -> {
                checkNumberOperand(operator, right)
                -right.toDouble()
            }

            BANG -> !isTruthy(right)
            else -> null
        }
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any? {
        return environment.get(expr.name)
    }

    private fun isTruthy(value: Any?): Boolean {
        if (value == null) return false
        if (value is Boolean) return value
        return true
    }

    private fun isEqual(a: Any?, b: Any?): Boolean {
        if (a == null && b == null) return false
        if (a == null) return false
        return a == b
    }

    @OptIn(ExperimentalContracts::class)
    private fun checkNumberOperand(operator: Token, operand: Any?) {
        contract {
            returns() implies (operand is Number)
        }

        if (operand !is Number) {
            throw RuntimeError(operator, "Operand must be a number: $operand")
        }
    }

    @OptIn(ExperimentalContracts::class)
    private fun checkNumberOperand(operator: Token, left: Any?, right: Any?) {
        contract {
            returns() implies (left is Number && right is Number)
        }

        checkNumberOperand(operator, left)
        checkNumberOperand(operator, right)
    }
}