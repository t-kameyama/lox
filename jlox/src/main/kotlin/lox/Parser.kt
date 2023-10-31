package lox

import lox.TokenType.AND
import lox.TokenType.BANG
import lox.TokenType.BANG_EQUAL
import lox.TokenType.CLASS
import lox.TokenType.COMMA
import lox.TokenType.ELSE
import lox.TokenType.EOF
import lox.TokenType.EQUAL
import lox.TokenType.EQUAL_EQUAL
import lox.TokenType.FALSE
import lox.TokenType.FOR
import lox.TokenType.FUN
import lox.TokenType.GREATER
import lox.TokenType.GREATER_EQUAL
import lox.TokenType.IDENTIFIER
import lox.TokenType.IF
import lox.TokenType.LEFT_BRACE
import lox.TokenType.LEFT_PAREN
import lox.TokenType.LESS
import lox.TokenType.LESS_EQUAL
import lox.TokenType.MINUS
import lox.TokenType.NIL
import lox.TokenType.NUMBER
import lox.TokenType.OR
import lox.TokenType.PLUS
import lox.TokenType.PRINT
import lox.TokenType.RETURN
import lox.TokenType.RIGHT_BRACE
import lox.TokenType.RIGHT_PAREN
import lox.TokenType.SEMICOLON
import lox.TokenType.SLASH
import lox.TokenType.STAR
import lox.TokenType.STRING
import lox.TokenType.TRUE
import lox.TokenType.VAR
import lox.TokenType.WHILE

class Parser(private val tokens: List<Token>) {
    class ParserError : RuntimeException()

    private var current: Int = 0

    fun parse(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!isAtEnd()) {
            declaration()?.let { statements.add(it) }
        }
        return statements
    }

    private fun declaration(): Stmt? {
        // function | varDecl | statement ;
        try {
            if (match(FUN)) return function("function")
            if (match(VAR)) return varDecl()
            return statement()
        } catch (e: ParserError) {
            synchronize()
            return null
        }
    }

    private fun function(kind: String): Stmt.Function {
        // "fun" IDENTIFIER "(" parameters? ")" block;
        // parameters -> IDENTIFIER ( "," IDENTIFIER )* ;
        val name = consume(IDENTIFIER, "Expect $kind name.")
        consume(LEFT_PAREN, "Expect '(' after $kind name.")
        val parameters = mutableListOf<Token>()
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    error(peek(), "Can't have more than 255 parameters.")
                }
                parameters.add(
                    consume(IDENTIFIER, "Expect parameter name"),
                )
            } while (match(COMMA))
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.")

        consume(LEFT_BRACE, "Expect '{' before $kind body.")
        val body = block()

        return Stmt.Function(name, parameters, body)
    }

    private fun varDecl(): Stmt.Var {
        // "var" IDENTIFIER ( "=" expression )? ";" ;
        val name = consume(IDENTIFIER, "Expect variable name.")
        val initialize = if (match(EQUAL)) expression() else null
        consume(SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initialize)
    }

    private fun statement(): Stmt {
        // exprStmt | forStmt | ifStmt | printStmt | whileStmt | blockStmt ;
        if (match(FOR)) return forStmt()
        if (match(IF)) return ifStmt()
        if (match(RETURN)) return returnStmt()
        if (match(PRINT)) return printStmt()
        if (match(WHILE)) return whileStmt()
        if (match(LEFT_BRACE)) return blockStmt()
        return exprStmt()
    }

    private fun exprStmt(): Stmt.Expression {
        // expression ";" ;
        val expr = expression()
        consume(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    private fun forStmt(): Stmt {
        // "for" "(" ( varDecl | exprStmt | ";" ) expression? ";" expression? ")" statement ;
        consume(LEFT_PAREN, "Expect '(' after 'for'.")

        val initializer = when {
            match(SEMICOLON) -> null
            match(VAR) -> varDecl()
            else -> exprStmt()
        }

        val condition = if (check(SEMICOLON)) Expr.Literal(true) else expression()
        consume(SEMICOLON, "Expect ';' after loop condition.")

        val increment = if (check(RIGHT_PAREN)) null else Stmt.Expression(expression())
        consume(RIGHT_PAREN, "Expect ')' after for clauses.")

        var body = if (increment != null) {
            Stmt.Block(listOf(statement(), increment))
        } else {
            statement()
        }

        body = Stmt.While(condition, body)

        body = if (initializer != null) {
            Stmt.Block(listOf(initializer, body))
        } else {
            body
        }

        return body
    }

    private fun whileStmt(): Stmt.While {
        // "while" "(" expression ")" statement ;
        consume(LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after while condition.")

        val body = statement()

        return Stmt.While(condition, body)
    }

    private fun ifStmt(): Stmt.If {
        // "if" "(" expression ")" statement ( "else" statement )? ;
        consume(LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after if condition.")

        val thenBranch = statement()
        val elseBranch = if (match(ELSE)) statement() else null

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun printStmt(): Stmt.Print {
        // "print" expression ";" ;
        val expr = expression()
        consume(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Print(expr)
    }

    private fun returnStmt(): Stmt.Return {
        // "return" expression? ";" ;
        val keyword = previous()
        val expression = if (!check(SEMICOLON)) expression() else null
        consume(SEMICOLON, "Expect ';' after return value.")
        return Stmt.Return(keyword, expression)
    }

    private fun blockStmt(): Stmt.Block {
        val statements = block()
        return Stmt.Block(statements)
    }

    private fun block(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            declaration()?.let { statements.add(it) }
        }
        consume(RIGHT_BRACE, "Expect '} after block.")
        return statements
    }

    private fun expression(): Expr {
        // assignment ;
        return assignment()
    }

    private fun assignment(): Expr {
        // IDENTIFIER "=" assignment | or ;
        val expr = or()
        if (match(EQUAL)) {
            val equals = previous()
            val value = assignment()
            if (expr is Expr.Variable) {
                return Expr.Assign(expr.name, value)
            }
            error(equals, "Invalid assignment target.")
        }
        return expr
    }

    private fun or(): Expr {
        // and ( "or" and )* ;
        var expr = and()
        while (match(OR)) {
            val operator = previous()
            val right = and()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

    private fun and(): Expr {
        // equality ( "and" equality )* ;
        var expr = equality()
        while (match(AND)) {
            val operator = previous()
            val right = and()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

    private fun equality(): Expr {
        // comparison ( ( "!=" | "==" ) comparison )* ;
        var expr = comparison()
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            expr = Expr.Binary(expr, previous(), comparison())
        }
        return expr
    }

    private fun comparison(): Expr {
        // term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
        var expr = term()
        while (match(LESS, LESS_EQUAL, GREATER, GREATER_EQUAL)) {
            expr = Expr.Binary(expr, previous(), term())
        }
        return expr
    }

    private fun term(): Expr {
        // factor ( ( "-" | "+" ) factor )* ;
        var expr = factory()
        while (match(MINUS, PLUS)) {
            expr = Expr.Binary(expr, previous(), factory())
        }
        return expr
    }

    private fun factory(): Expr {
        // unary ( ( "/" | "*" ) unary )* ;
        var expr = unary()
        while (match(SLASH, STAR)) {
            expr = Expr.Binary(expr, previous(), unary())
        }
        return expr
    }

    private fun unary(): Expr {
        // ( "!" | "-" ) unary | call ;
        return if (match(BANG, MINUS)) {
            Expr.Unary(previous(), unary())
        } else {
            call()
        }
    }

    private fun call(): Expr {
        // primary ( "(" arguments ")" )* ;
        // arguments -> expression ( "," expression? )* ;
        var expr = primary()
        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr)
            } else {
                break
            }
        }
        return expr
    }

    private fun finishCall(expr: Expr): Expr {
        val arguments = mutableListOf<Expr>()
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size >= 255) {
                    error(peek(), "Can't have more than 255 arguments.")
                }
                arguments.add(expression())
            } while (match(COMMA))
        }
        val paren = consume(RIGHT_PAREN, "Expect ')' after arguments.")
        return Expr.Call(expr, paren, arguments)
    }

    private fun primary(): Expr {
        //  NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER ;
        if (match(FALSE)) return Expr.Literal(false)
        if (match(TRUE)) return Expr.Literal(true)
        if (match(NIL)) return Expr.Literal(null)
        if (match(NUMBER, STRING)) return Expr.Literal(previous().literal)
        if (match(IDENTIFIER)) return Expr.Variable(previous())
        if (match(LEFT_PAREN)) {
            val expr = expression()
            consume(RIGHT_PAREN, "Expect ') after expression.")
            return Expr.Grouping(expr)
        }
        throw error(peek(), "Expect expression.")
    }

    private fun match(vararg types: TokenType): Boolean {
        types.forEach { type ->
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun isAtEnd(): Boolean = peek().type == EOF

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun peek(): Token = tokens[current]

    private fun previous(): Token = tokens[current - 1]

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun error(token: Token, message: String): ParserError {
        Lox.error(token, message)
        return ParserError()
    }

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {
            if (previous().type == EOF) return
            if (peek().type in setOf(VAR, FUN, CLASS, IF, FOR, WHILE, RETURN, PRINT)) return
            advance()
        }
    }
}
