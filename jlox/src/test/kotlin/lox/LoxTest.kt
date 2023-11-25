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

    @Test
    fun `if`() {
        val source = """
            var x = false;
            if (x) print 1; else print 2;
        """.trimIndent()
        Lox.run(source)
    }

    @Test
    fun and() {
        val source = """
            var x = true;
            var y = false;
            if (x and y) print 1; else print 2;
        """.trimIndent()
        Lox.run(source)
    }

    @Test
    fun or() {
        val source = """
            var x = false;
            var y = true;
            if (x or y) {
                print 1; 
            } else {
                print 2;
            }
        """.trimIndent()
        Lox.run(source)
    }

    @Test
    fun `while`() {
        val source = """
            var x = 1;
            while (x < 3) {
                print x;
                x = x + 1;
            }
        """.trimIndent()
        Lox.run(source)
    }

    @Test
    fun `for`() {
        val source = """
            for (var i = 0; i < 10; i = i + 1) print i;
        """.trimIndent()
        Lox.run(source)
    }

    @Test
    fun function() {
        val source = """
            fun sayHi(first, last) {
                print "Hi, " + first + " " + last + "!";
            }
            sayHi("Dear", "Reader");
            print sayHi;
        """.trimIndent()
        Lox.run(source)
    }

    @Test
    fun fib() {
        val source = """
            fun fib(n) {
                if (n <= 1) return n;
                return fib(n - 2) + fib(n - 1);
            }
            for (var i = 0; i < 20; i = i + 1) {
                print fib(i);
            }
        """.trimIndent()
        Lox.run(source)
    }

    @Test
    fun closure() {
        val source = """
            fun makeCounter() {
                var i = 0;
                fun count() {
                    i = i + 1;
                    print i;
                }
                return count;
            }
            
            var counter = makeCounter();
            counter();
            counter();
        """.trimIndent()
        Lox.run(source)
    }

    @Test
    fun binding() {
        val source = """
            var a = "global";
            {
                fun showA() {
                    print a;
                }
                showA();
                var a = "block";
                showA();
            }
        """.trimIndent()
        Lox.run(source)
    }

    @Test
    fun duplicateDefinition() {
        val source = """
            fun bad() {
                var a = "first";
                var a = "second";
            }
        """.trimIndent()
        Lox.run(source)
    }

    @Test
    fun topLevelReturn() {
        val source = """
            return "at top level";
        """.trimIndent()
        Lox.run(source)
    }
}
