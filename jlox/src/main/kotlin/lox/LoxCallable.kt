package lox

interface LoxCallable {
    fun arity(): Int
    fun call(interpreter: Interpreter, arguments: List<Any?>): Any?
}

class LoxFunction(private val declaration: Stmt.Function, private val closure: Environment) : LoxCallable {
    override fun arity(): Int = declaration.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        declaration.params.zip(arguments).forEach { (param, arg) ->
            environment.define(param.lexeme, arg)
        }
        return try {
            interpreter.executeBlock(declaration.body, environment)
            null
        } catch (e: Return) {
            e.value
        }
    }

    override fun toString(): String = "<fn ${declaration.name.lexeme}>"
}