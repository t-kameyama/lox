package lox

import lox.TokenType.*

class Parser(private val tokens: List<Token>) {
    class ParserError : RuntimeException()

    private var current: Int = 0

    fun parse(): Expr? {
        return try {
            expression()
        } catch (error: ParserError) {
            null
        }
    }

    private fun expression(): Expr {
        // equality ;
        return equality()
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
        //  NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
        if (match(FALSE)) return Expr.Literal(false)
        if (match(TRUE)) return Expr.Literal(true)
        if (match(NIL)) return Expr.Literal(null)
        if (match(NUMBER, STRING)) return Expr.Literal(previous().literal)
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
