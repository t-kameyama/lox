package lox

import org.junit.jupiter.api.Test

class AstPrinterTest {
    @Test
    fun test() {
        // -123 * (45.67)
        val expression = Expr.Binary(
            left = Expr.Unary(
                operator = Token(type = TokenType.MINUS, lexeme = "-", literal = null, line = 0),
                right = Expr.Literal(value = 123)
            ),
            operator = Token(type = TokenType.STAR, lexeme = "*", literal = null, line = 1),
            right = Expr.Grouping(
                expression = Expr.Literal(value = 45.67)
            )
        )
        println(AstPrinter().print(expression))
    }
}