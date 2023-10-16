package lox

import org.junit.jupiter.api.Test

class LoxTest {
    @Test
    fun scope() {
        val source = """
            var a = "global a";
            var b = "global b";
            var c = "global c";
            {
              var a = "outer a";
              var b = "outer b";
              {
                var a = "inner a";
                print a;
                print b;
                print c;
              }
              print a;
              print b;
              print c;
            }
            print a;
            print b;
            print c;
        """.trimIndent()

        Lox.run(source)
    }
}