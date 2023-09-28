package lox

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ScannerTest {
    @Test
    fun test() {
        val source = """
            var foo = "string";
            var bar = foo == 99;
        """.trimIndent()
        val tokens = Scanner(source).scanTokens()

        assertEquals(13, tokens.size)
        assertEquals(TokenType.VAR, tokens[0].type)
        assertEquals(TokenType.IDENTIFIER, tokens[1].type)
        assertEquals(TokenType.EQUAL, tokens[2].type)
        assertEquals(TokenType.STRING, tokens[3].type)
        assertEquals(TokenType.SEMICOLON, tokens[4].type)
        assertEquals(TokenType.VAR, tokens[5].type)
        assertEquals(TokenType.IDENTIFIER, tokens[6].type)
        assertEquals(TokenType.EQUAL, tokens[7].type)
        assertEquals(TokenType.IDENTIFIER, tokens[8].type)
        assertEquals(TokenType.EQUAL_EQUAL, tokens[9].type)
        assertEquals(TokenType.NUMBER, tokens[10].type)
        assertEquals(TokenType.SEMICOLON, tokens[11].type)
        assertEquals(TokenType.EOF, tokens[12].type)

        assertEquals("string", tokens[3].literal)
        assertEquals(99.0, tokens[10].literal)

        assertEquals(1, tokens[0].line)
        assertEquals(2, tokens[11].line)
    }
}
