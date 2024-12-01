package com.faforever.neroxis.lua;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public sealed interface Lua {

    static Lua parse(InputStream inputStream) {
        try {
            CharStream charStream = CharStreams.fromStream(inputStream);
            LuaLexer expressionLexer = new LuaLexer(charStream);
            CommonTokenStream commonTokenStream = new CommonTokenStream(expressionLexer);
            LuaParser luaParser = new LuaParser(commonTokenStream);
            return new LuaParserVisitorImpl().visitStart(luaParser.start());
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    record Block(List<Statement> statements) implements Lua {
        public Block {
            statements = List.copyOf(statements);
        }
    }

    sealed interface Statement extends Lua {
        record Empty() implements Statement {}

        record Assignment(List<Lua.Receiver> targets, List<Lua.Expression> values) implements Statement {
            public Assignment {
                targets = List.copyOf(targets);
                values = List.copyOf(values);
            }
        }

        record LocalAssignment(List<Lua.AttributeName> targets, List<Lua.Expression> values) implements Statement {
            public LocalAssignment {
                targets = List.copyOf(targets);
                values = List.copyOf(values);
            }
        }

        record Label(String label) implements Statement {}

        record Break() implements Statement {}

        record Goto(String label) implements Statement {}

        record Do(Block block) implements Statement {}

        record While(Lua.Expression condition, Block block) implements Statement {}

        record Repeat(Block block, Lua.Expression condition) implements Statement {}

        record If(List<Condition> conditions) implements Statement {
            public If {
                conditions = List.copyOf(conditions);
            }

            public sealed interface Condition {
                record Expression(Lua.Expression condition, Block block) implements Condition {}

                record Else(Block block) implements Condition {}
            }
        }

        record NumericFor(java.lang.String variable,
                          Lua.Expression start,
                          Lua.Expression end,
                          Lua.Expression step,
                          Block block) implements Statement {}

        record GenericFor(List<java.lang.String> variables, List<Lua.Expression> iterators, Block block) implements
                                                                                                         Statement {}

        record Function(java.lang.String name, FunctionBody body) implements Statement {}

        record LocalFunction(java.lang.String name, FunctionBody body) implements Statement {}

        record Continue() implements Statement {}

        record Return(List<Lua.Expression> values) implements Statement {}
    }

    sealed interface Expression extends Lua {}

    sealed interface Value extends Expression {
        record Nil() implements Value {}

        record Boolean(boolean value) implements Value {}

        record Number(double value) implements Value {}

        record String(java.lang.String value) implements Value {}

        record VarArg() implements Value {}

        record Table(Map<Lua.Expression, Lua.Expression> contents) implements Value {
            public Table {
                contents = Map.copyOf(contents);
            }
        }

        record Function(FunctionBody body) implements Value {}
    }

    sealed interface UnaryOperator extends Expression {
        record Not(Lua.Expression expression) implements UnaryOperator {}

        record BitNot(Lua.Expression expression) implements UnaryOperator {}

        record Negate(Lua.Expression expression) implements UnaryOperator {}

        record Length(Lua.Expression expression) implements UnaryOperator {}
    }

    sealed interface BinaryOperator extends Expression {
        record Add(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record Subtract(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record Multiply(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record Divide(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record FloorDivide(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record Power(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record Modulo(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record Concat(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record And(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record Or(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record LeftShift(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record RightShift(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record BitAnd(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record BitOr(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record BitXor(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record GreaterThan(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record GreaterThanOrEqual(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record LessThan(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record LessThanOrEqual(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record Equal(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}

        record NotEqual(Lua.Expression left, Lua.Expression right) implements BinaryOperator {}
    }

    sealed interface FunctionCall extends Statement, Expression {
        record Direct(Lua.Receiver receiver, List<Lua.Expression> arguments) implements FunctionCall {
            public Direct {
                arguments = List.copyOf(arguments);
            }
        }

        record Self(Lua.Receiver receiver, String methodName, List<Lua.Expression> arguments) implements FunctionCall {
            public Self {
                arguments = List.copyOf(arguments);
            }
        }
    }

    record AttributeName(String name, String attribute) implements Lua {}

    sealed interface Receiver extends Expression {
        record Named(String name, List<MemberAccessor> memberAccessors) implements Receiver {
            public Named {
                memberAccessors = List.copyOf(memberAccessors);
            }
        }

        record Function(Statement.FunctionCall functionCall, List<MemberAccessor> memberAccessors) implements Receiver {
            public Function {
                memberAccessors = List.copyOf(memberAccessors);
            }
        }

        record Expression(Lua.Expression expression, List<MemberAccessor> memberAccessors) implements Receiver {
            public Expression {
                memberAccessors = List.copyOf(memberAccessors);
            }
        }
    }

    sealed interface MemberAccessor extends Lua {
        record Named(String name) implements MemberAccessor {}

        record Expression(Lua.Expression expression) implements MemberAccessor {}
    }

    sealed interface Arg extends Lua {
        record Var() implements Arg {}

        record Named(java.lang.String name) implements Arg {}
    }

    record FunctionBody(List<Arg> arguments, Statement.Block body) implements Lua {
        public FunctionBody {
            arguments = List.copyOf(arguments);
        }
    }
}
