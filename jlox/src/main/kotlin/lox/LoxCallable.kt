package lox

interface LoxCallable {
    fun arity(): Int
    fun call(interpreter: Interpreter, arguments: List<Any?>): Any?
}

class LoxFunction(
    private val declaration: Stmt.Function,
    private val closure: Environment,
    private val isInitializer: Boolean,
) : LoxCallable {
    override fun arity(): Int = declaration.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        declaration.params.zip(arguments).forEach { (param, arg) ->
            environment.define(param.lexeme, arg)
        }
        return try {
            interpreter.executeBlock(declaration.body, environment)
            if (isInitializer) closure.getAt(0, "this") else null
        } catch (e: Return) {
            if (isInitializer) closure.getAt(0, "this") else e.value
        }
    }

    override fun toString(): String = "<fn ${declaration.name.lexeme}>"

    fun bind(instance: LoxInstance): LoxFunction {
        val environment = Environment(closure)
        environment.define("this", instance)
        return LoxFunction(declaration, environment, isInitializer)
    }
}

class LoxClass(
    val name: String,
    private val superclass: LoxClass?,
    private val methods: Map<String, LoxFunction>,
) : LoxCallable {
    override fun arity(): Int = findMethods("init")?.arity() ?: 0

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
        val instance = LoxInstance(this)
        findMethods("init")?.bind(instance)?.call(interpreter, arguments)
        return instance
    }

    override fun toString(): String = name

    fun findMethods(name: String): LoxFunction? = methods[name] ?: superclass?.findMethods(name)
}

class LoxInstance(private val klass: LoxClass) {
    private val fields = mutableMapOf<String, Any?>()

    override fun toString(): String = "${klass.name} instance"

    fun get(name: Token): Any? {
        if (fields.containsKey(name.lexeme)) {
            return fields[name.lexeme]
        }
        val method = klass.findMethods(name.lexeme)
        if (method != null) {
            return method.bind(this)
        }
        throw RuntimeError(name, "Undefined property '${name.lexeme}'.")
    }

    fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }
}
