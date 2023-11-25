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
    private val globals = Environment().apply {
        define(
            "clock",
            object : LoxCallable {
                override fun arity(): Int = 0

                override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
                    return System.currentTimeMillis().toDouble() / 1000.0
                }

                override fun toString(): String = "<native fn>"
            },
        )
    }

    private val locals = hashMapOf<Expr, Int>()

    private var environment = globals

    fun interpret(statements: List<Stmt>) {
        try {
            for (statement in statements) {
                execute(statement)
            }
        } catch (e: RuntimeError) {
            Lox.runtimeError(e)
        }
    }

    private fun evaluate(expr: Expr): Any? {
        return expr.accept(this)
    }

    private fun execute(stmt: Stmt) {
        stmt.accept(this)
    }

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
    }

    fun executeBlock(statements: List<Stmt>, environment: Environment) {
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

    override fun visitClassStmt(stmt: Stmt.Class) {
        environment.define(stmt.name.lexeme, null)
        val methods = stmt.methods.associate { method ->
            val isInitializer = method.name.lexeme == "init"
            method.name.lexeme to LoxFunction(method, environment, isInitializer)
        }
        val klass = LoxClass(stmt.name.lexeme, methods)
        environment.assign(stmt.name, klass)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val function = LoxFunction(stmt, environment, false)
        environment.define(stmt.name.lexeme, function)
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        environment.define(stmt.name.lexeme, stmt.initializer?.let { evaluate(it) })
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expr)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expr)
        println(stringify(value))
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        val value = stmt.value?.let { evaluate(it) }
        throw Return(value)
    }

    override fun visitBlockStmt(block: Stmt.Block) {
        executeBlock(block.statements, Environment(environment))
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

        // environment.assign(expr.name, value)
        val distance = locals[expr]
        if (distance != null) {
            environment.assignAt(distance, expr.name, value)
        } else {
            globals.assign(expr.name, value)
        }

        return value
    }

    override fun visitCallExpr(expr: Expr.Call): Any? {
        val function = evaluate(expr.callee) as? LoxCallable
            ?: throw RuntimeError(expr.paren, "Can only call functions and classes.")
        if (expr.arguments.size != function.arity()) {
            throw RuntimeError(expr.paren, "Expected ${function.arity()} arguments but got ${expr.arguments.size}")
        }
        val arguments = expr.arguments.map { evaluate(it) }
        return function.call(this, arguments)
    }

    override fun visitGetExpr(expr: Expr.Get): Any? {
        val obj = evaluate(expr.obj)
        if (obj is LoxInstance) {
            return obj.get(expr.name)
        }
        throw RuntimeError(expr.name, "Only instances have properties")
    }

    override fun visitSetExpr(expr: Expr.Set): Any? {
        val obj = evaluate(expr.obj)
        if (obj !is LoxInstance) {
            throw RuntimeError(expr.name, "Only instances have fields.")
        }
        val value = evaluate(expr.value)
        obj.set(expr.name, value)
        return value
    }

    override fun visitThisExpr(expr: Expr.This): Any? {
        return lookupVariable(expr.keyword, expr)
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
                        "Operands must be two numbers or two strings: left=$left, right=$right",
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
        // return environment.get(expr.name)
        return lookupVariable(expr.name, expr)
    }

    private fun lookupVariable(name: Token, expr: Expr): Any? {
        val distance = locals[expr] ?: return globals.get(name)
        return environment.getAt(distance, name.lexeme)
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