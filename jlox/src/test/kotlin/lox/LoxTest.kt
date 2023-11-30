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

    @Test
    fun `class`() {
        val source = """
            class Foo {}
            var foo = Foo();
            print foo;
        """.trimIndent()
        Lox.run(source)
    }

    @Test
    fun accessor() {
        val source = """
            class Foo {}
            var foo = Foo();
            foo.bar = 123;
            print foo.bar;
        """.trimIndent()
        Lox.run(source)
    }

    @Test
    fun method() {
        val source = """
            class Bacon {
                eat() {
                    print "Crunch crunch crunch!";
                }
            }
            Bacon().eat();
        """.trimIndent()
        Lox.run(source)
    }

    @Test
    fun `this`() {
        val source = """
            class Cake {
                taste() {
                    var adjective = "delicious";
                    print "The " + this.flavor + " cake is " + adjective + "!";
                }
            }
            var cake = Cake();
            cake.flavor = "German chocolate";
            cake.taste();
        """.trimIndent()
        Lox.run(source)
    }

    @Test
    fun topLevelThis() {
        val source = """
            print this;
        """.trimIndent()
        Lox.run(source)
    }

    @Test
    fun `init`() {
        val source = """
            class Foo {
                init() {
                    print "init";
                }
            }
            var foo = Foo();
            print foo.init();
            
            class Bar {
                init() {
                    return;
                }
            }
            print Bar().init();
        """.trimIndent()
        Lox.run(source)
    }

    @Test
    fun returnValueFromInit() {
        val source = """
            class Baz {
                init() {
                    return "baz";
                }
            }
            print Baz().init();
        """.trimIndent()
        Lox.run(source)
    }

    @Test
    fun inheritance() {
        val source = """
            class A {
              method() {
                print "A method";
              }
            }
            
            class B < A {
              method() {
                print "B method";
              }
            
              test() {
                super.method();
              }
            }
            
            class C < B {}
            
            var c = C();
            c.method();
            c.test();
        """.trimIndent()
        Lox.run(source)
    }

    @Test
    fun invalidSuper() {
        val source = """
        class Eclair {
          cook() {
            super.cook();
            print "Pipe full of crème pâtissière.";
          }
        }
        Eclair().cook();
        """.trimIndent()
        Lox.run(source)
    }
}
