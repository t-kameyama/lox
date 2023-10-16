package lox

import lox.TokenType.BANG
import lox.TokenType.BANG_EQUAL
import lox.TokenType.CLASS
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
        // varDecl | statement ;
        try {
            if (match(VAR)) return varDecl()
            return statement()
        } catch (e: ParserError) {
            synchronize()
            return null
        }
    }

    private fun varDecl(): Stmt.Var {
        // "var" IDENTIFIER ( "=" expression )? ";" ;
        val name = consume(IDENTIFIER, "Expect variable name.")
        val initialize = if (match(EQUAL)) expression() else null
        consume(SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initialize)
    }

    private fun statement(): Stmt {
        // exprStmt | printStmt | block ;
        if (match(PRINT)) return printStmt()
        if (match(LEFT_BRACE)) return blockStmt()
        return exprStmt()
    }

    private fun exprStmt(): Stmt.Expression {
        // expression ";" ;
        val expr = expression()
        consume(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    private fun printStmt(): Stmt.Print {
        // "print" expression ";" ;
        val expr = expression()
        consume(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Print(expr)
    }

    private fun blockStmt(): Stmt.Block {
        val statements = mutableListOf<Stmt>()
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            declaration()?.let { statements.add(it) }
        }
        consume(RIGHT_BRACE, "Expect '} after block.")
        return Stmt.Block(statements)
    }

    private fun expression(): Expr {
        // assignment ;
        return assignment()
    }

    private fun assignment(): Expr {
        // IDENTIFIER "=" assignment | equality ;
        val expr = equality()
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
        // ( "!" | "-" ) unary | primary ;
        return if (match(BANG, MINUS)) {
            Expr.Unary(previous(), unary())
        } else {
            primary()
        }
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
