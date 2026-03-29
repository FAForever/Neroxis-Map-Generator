package com.faforever.neroxis.lua;

import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("NullableProblems")
public class LuaParserVisitorImpl extends AbstractParseTreeVisitor<Lua> implements LuaParserVisitor<Lua> {


    @Override
    public Lua.Statement.Block visitStart(LuaParser.StartContext ctx) {
        return visitBlock(ctx.block());
    }

    @Override
    public Lua.Statement.Block visitBlock(LuaParser.BlockContext ctx) {
        List<Lua.Statement> statements = new ArrayList<>();

        ctx.statement().stream().map(this::visitStatement).forEach(statements::add);

        LuaParser.LastStatementContext lastStatement = ctx.lastStatement();
        if (lastStatement != null) {
            statements.add(visitLastStatement(lastStatement));
        }

        return new Lua.Statement.Block(statements);
    }

    @Override
    public Lua.Statement.Assignment visitAssignmentStatement(LuaParser.AssignmentStatementContext ctx) {
        List<Lua.Variable> receivers = ctx.varList().var().stream().map(this::visitVar).toList();
        List<Lua.Expression> values = ctx.expressionList().expression().stream().map(this::visitExpression).toList();
        return new Lua.Statement.Assignment(receivers, values);
    }

    @Override
    public Lua.Statement.FunctionCall visitFunctionCallStatement(LuaParser.FunctionCallStatementContext ctx) {
        return visitFunctionCall(ctx.functionCall());
    }

    @Override
    public Lua.Statement.Do visitDoStatement(LuaParser.DoStatementContext ctx) {
        Lua.Block block = visitBlock(ctx.block());
        return new Lua.Statement.Do(block);
    }

    @Override
    public Lua.Statement.While visitWhileStatement(LuaParser.WhileStatementContext ctx) {
        Lua.Expression condition = visitExpression(ctx.expression());
        Lua.Block block = visitBlock(ctx.block());
        return new Lua.Statement.While(condition, block);
    }

    @Override
    public Lua.Statement.Repeat visitRepeatStatement(LuaParser.RepeatStatementContext ctx) {
        Lua.Block block = visitBlock(ctx.block());
        Lua.Expression condition = visitExpression(ctx.expression());
        return new Lua.Statement.Repeat(block, condition);
    }

    @Override
    public Lua.Statement.If visitIfStatement(LuaParser.IfStatementContext ctx) {
        List<Lua.Expression> expressions = ctx.expression().stream().map(this::visitExpression).toList();
        List<Lua.Block> bodies = ctx.block().stream().map(this::visitBlock).toList();
        List<Lua.Statement.If.Condition> conditions = new ArrayList<>();
        for (int i = 0; i < expressions.size(); i++) {
            conditions.add(new Lua.Statement.If.Condition.Expression(expressions.get(i), bodies.get(i)));
        }

        if (bodies.size() > expressions.size()) {
            conditions.add(new Lua.Statement.If.Condition.Else(bodies.getLast()));
        }

        return new Lua.Statement.If(conditions);
    }

    @Override
    public Lua.Statement.NumericFor visitNumericForStatement(LuaParser.NumericForStatementContext ctx) {
        Lua.Expression start = visitExpression(ctx.startValue);
        Lua.Expression end = visitExpression(ctx.endValue);
        Lua.Expression step = ctx.stepValue == null ? new Lua.Value.Number(1) : visitExpression(ctx.stepValue);
        Lua.Block body = visitBlock(ctx.block());
        return new Lua.Statement.NumericFor(ctx.NAME().getText(), start, end, step, body);
    }

    @Override
    public Lua.Statement visitGenericForStatement(LuaParser.GenericForStatementContext ctx) {
        List<String> variables = ctx.nameList().NAME().stream().map(TerminalNode::getText).toList();
        List<Lua.Expression> iterators = ctx.expressionList().expression().stream().map(this::visitExpression).toList();
        Lua.Block body = visitBlock(ctx.block());
        return new Lua.Statement.GenericFor(variables, iterators, body);
    }

    @Override
    public Lua.Statement.Function visitFunctionStatement(LuaParser.FunctionStatementContext ctx) {
        Lua.FunctionBody body = visitFunctionBody(ctx.functionBody());
        String name = ctx.functionName().getText();
        return new Lua.Statement.Function(name, body);
    }

    @Override
    public Lua.Statement.LocalFunction visitLocalFunctionStatement(LuaParser.LocalFunctionStatementContext ctx) {
        Lua.FunctionBody body = visitFunctionBody(ctx.functionBody());
        String name = ctx.NAME().getText();
        return new Lua.Statement.LocalFunction(name, body);
    }

    @Override
    public Lua.Statement.LocalAssignment visitLocalAssignmentStatement(LuaParser.LocalAssignmentStatementContext ctx) {
        List<String> names = ctx.nameList()
                                .NAME()
                                .stream()
                                .map(TerminalNode::getText)
                                .toList();
        LuaParser.ExpressionListContext expressionListContext = ctx.expressionList();
        List<Lua.Expression> values = expressionListContext == null ? List.of() : expressionListContext.expression()
                                                                                                       .stream()
                                                                                                       .map(this::visitExpression)
                                                                                                       .toList();
        return new Lua.Statement.LocalAssignment(names, values);
    }

    public Lua.Statement visitStatement(LuaParser.StatementContext ctx) {
        return switch (ctx) {
            case LuaParser.AssignmentStatementContext assignmentStatementContext ->
                    visitAssignmentStatement(assignmentStatementContext);
            case LuaParser.FunctionCallStatementContext functionCallStatementContext ->
                    visitFunctionCallStatement(functionCallStatementContext);
            case LuaParser.DoStatementContext doStatementContext -> visitDoStatement(doStatementContext);
            case LuaParser.WhileStatementContext whileStatementContext -> visitWhileStatement(whileStatementContext);
            case LuaParser.RepeatStatementContext repeatStatementContext ->
                    visitRepeatStatement(repeatStatementContext);
            case LuaParser.IfStatementContext ifStatementContext -> visitIfStatement(ifStatementContext);
            case LuaParser.NumericForStatementContext forStatementContext ->
                    visitNumericForStatement(forStatementContext);
            case LuaParser.GenericForStatementContext forInStatementContext ->
                    visitGenericForStatement(forInStatementContext);
            case LuaParser.FunctionStatementContext functionStatementContext ->
                    visitFunctionStatement(functionStatementContext);
            case LuaParser.LocalFunctionStatementContext localFunctionStatementContext ->
                    visitLocalFunctionStatement(localFunctionStatementContext);
            case LuaParser.LocalAssignmentStatementContext localAssignmentStatementContext ->
                    visitLocalAssignmentStatement(localAssignmentStatementContext);
            case LuaParser.StatementContext statementContext -> throw new UnsupportedOperationException(
                    "Unable to handle statement of type %s".formatted(statementContext.getClass().getCanonicalName()));
        };
    }

    @Override
    public Lua.Statement.Break visitBreakStatement(LuaParser.BreakStatementContext ctx) {
        return new Lua.Statement.Break();
    }

    @Override
    public Lua.Statement.Continue visitContinueStatement(LuaParser.ContinueStatementContext ctx) {
        return new Lua.Statement.Continue();
    }

    @Override
    public Lua.Statement.Return visitReturnStatement(LuaParser.ReturnStatementContext ctx) {
        List<Lua.Expression> values = ctx.expressionList().expression().stream().map(this::visitExpression).toList();
        return new Lua.Statement.Return(values);
    }

    public Lua.Statement visitLastStatement(LuaParser.LastStatementContext ctx) {
        return switch (ctx) {
            case LuaParser.BreakStatementContext breakStatementContext -> visitBreakStatement(breakStatementContext);
            case LuaParser.ContinueStatementContext continueStatementContext ->
                    visitContinueStatement(continueStatementContext);
            case LuaParser.ReturnStatementContext returnStatementContext ->
                    visitReturnStatement(returnStatementContext);
            case LuaParser.LastStatementContext lastStatementContext -> throw new UnsupportedOperationException(
                    "Unable to handle last statement of type %s".formatted(
                            lastStatementContext.getClass().getCanonicalName()));
        };
    }

    @Override
    public Lua.Variable.Named visitVariableLiteral(LuaParser.VariableLiteralContext ctx) {
        return new Lua.Variable.Named(ctx.NAME().getText(), List.of());
    }

    @Override
    public Lua.Variable.Named visitMemberAccessLiteral(LuaParser.MemberAccessLiteralContext ctx) {
        List<Lua.MemberAccessor> memberAccessors = ctx.memberAccess().stream().map(this::visitMemberAccess).toList();
        return new Lua.Variable.Named(ctx.NAME().getText(), memberAccessors);
    }

    @Override
    public Lua.Variable.Expression visitExpressionAccessLiteral(LuaParser.ExpressionAccessLiteralContext ctx) {
        Lua.Expression expression = visitExpression(ctx.expression());
        List<Lua.MemberAccessor> memberAccessors = ctx.memberAccess().stream().map(this::visitMemberAccess).toList();
        return new Lua.Variable.Expression(expression, memberAccessors);
    }

    @Override
    public Lua.Expression visitParenthesizedExpressionLiteral(LuaParser.ParenthesizedExpressionLiteralContext ctx) {
        return visitExpression(ctx.expression());
    }

    @Override
    public Lua.Variable.Function visitFunctionAccessLiteral(LuaParser.FunctionAccessLiteralContext ctx) {
        Lua.FunctionCall functionCall = visitFunctionCall(ctx.functionCall());
        List<Lua.MemberAccessor> memberAccessors = ctx.memberAccess().stream().map(this::visitMemberAccess).toList();
        return new Lua.Variable.Function(functionCall, memberAccessors);
    }

    @Override
    public Lua.FunctionCall visitFunctionCallLiteral(LuaParser.FunctionCallLiteralContext ctx) {
        return visitFunctionCall(ctx.functionCall());
    }

    @Override
    public Lua.UnaryOperator visitExpressionUnary(LuaParser.ExpressionUnaryContext ctx) {
        Lua.Expression expression = visitExpression(ctx.expression());
        return switch (ctx.operator.getText()) {
            case "not" -> new Lua.UnaryOperator.Not(expression);
            case "#" -> new Lua.UnaryOperator.Length(expression);
            case "-" -> new Lua.UnaryOperator.Negate(expression);
            case "~" -> new Lua.UnaryOperator.BitNot(expression);
            case String string ->
                    throw new UnsupportedOperationException("Unable to handle unary operator %s".formatted(string));
        };
    }

    @Override
    public Lua.BinaryOperator.Power visitExpressionPower(LuaParser.ExpressionPowerContext ctx) {
        Lua.Expression left = visitExpression(ctx.left);
        Lua.Expression right = visitExpression(ctx.right);
        return new Lua.BinaryOperator.Power(left, right);
    }

    @Override
    public Lua.BinaryOperator visitExpressionBitwise(LuaParser.ExpressionBitwiseContext ctx) {
        Lua.Expression left = visitExpression(ctx.left);
        Lua.Expression right = visitExpression(ctx.right);
        return switch (ctx.operator.getText()) {
            case "&" -> new Lua.BinaryOperator.BitAnd(left, right);
            case "|" -> new Lua.BinaryOperator.BitOr(left, right);
            case "~" -> new Lua.BinaryOperator.BitXor(left, right);
            case "<<" -> new Lua.BinaryOperator.LeftShift(left, right);
            case ">>" -> new Lua.BinaryOperator.RightShift(left, right);
            case String string ->
                    throw new UnsupportedOperationException("Unable to handle bitwise operator %s".formatted(string));
        };
    }

    @Override
    public Lua.Value.Nil visitNilLiteral(LuaParser.NilLiteralContext ctx) {
        return new Lua.Value.Nil();
    }

    @Override
    public Lua.Value.Boolean visitTrueLiteral(LuaParser.TrueLiteralContext ctx) {
        return new Lua.Value.Boolean(true);
    }

    @Override
    public Lua.Value.VarArg visitVarargLiteral(LuaParser.VarargLiteralContext ctx) {
        return new Lua.Value.VarArg();
    }

    @Override
    public Lua.BinaryOperator visitExpressionAdditive(LuaParser.ExpressionAdditiveContext ctx) {
        Lua.Expression left = visitExpression(ctx.left);
        Lua.Expression right = visitExpression(ctx.right);
        return switch (ctx.operator.getText()) {
            case "+" -> new Lua.BinaryOperator.Add(left, right);
            case "-" -> new Lua.BinaryOperator.Subtract(left, right);
            case String string ->
                    throw new UnsupportedOperationException("Unable to handle additive operator %s".formatted(string));
        };
    }

    @Override
    public Lua.BinaryOperator visitExpressionMultiplicative(LuaParser.ExpressionMultiplicativeContext ctx) {
        Lua.Expression left = visitExpression(ctx.left);
        Lua.Expression right = visitExpression(ctx.right);
        return switch (ctx.operator.getText()) {
            case "*" -> new Lua.BinaryOperator.Multiply(left, right);
            case "/" -> new Lua.BinaryOperator.Divide(left, right);
            case "%" -> new Lua.BinaryOperator.Modulo(left, right);
            case "//" -> new Lua.BinaryOperator.FloorDivide(left, right);
            case String string -> throw new UnsupportedOperationException(
                    "Unable to handle multiplicative operator %s".formatted(string));
        };
    }

    @Override
    public Lua.BinaryOperator.Concat visitExpressionConcat(LuaParser.ExpressionConcatContext ctx) {
        Lua.Expression left = visitExpression(ctx.left);
        Lua.Expression right = visitExpression(ctx.right);
        return new Lua.BinaryOperator.Concat(left, right);
    }

    @Override
    public Lua.Value.Table visitTableLiteral(LuaParser.TableLiteralContext ctx) {
        return visitTableConstructor(ctx.tableConstructor());
    }

    @Override
    public Lua.Value.Boolean visitFalseLiteral(LuaParser.FalseLiteralContext ctx) {
        return new Lua.Value.Boolean(false);
    }

    @Override
    public Lua.Value.String visitStringLiteral(LuaParser.StringLiteralContext ctx) {
        return visitString(ctx.string());
    }

    @Override
    public Lua.BinaryOperator visitExpressionComparative(LuaParser.ExpressionComparativeContext ctx) {
        Lua.Expression left = visitExpression(ctx.left);
        Lua.Expression right = visitExpression(ctx.right);
        return switch (ctx.operator.getText()) {
            case "<" -> new Lua.BinaryOperator.LessThan(left, right);
            case ">" -> new Lua.BinaryOperator.GreaterThan(left, right);
            case "<=" -> new Lua.BinaryOperator.LessThanOrEqual(left, right);
            case ">=" -> new Lua.BinaryOperator.GreaterThanOrEqual(left, right);
            case "==" -> new Lua.BinaryOperator.Equal(left, right);
            case "~=", "!=" -> new Lua.BinaryOperator.NotEqual(left, right);
            case String string -> throw new UnsupportedOperationException(
                    "Unable to handle comparative operator %s".formatted(string));
        };
    }

    @Override
    public Lua.BinaryOperator.And visitExpressionAnd(LuaParser.ExpressionAndContext ctx) {
        Lua.Expression left = visitExpression(ctx.left);
        Lua.Expression right = visitExpression(ctx.right);
        return new Lua.BinaryOperator.And(left, right);
    }

    @Override
    public Lua.BinaryOperator.Or visitExpressionOr(LuaParser.ExpressionOrContext ctx) {
        Lua.Expression left = visitExpression(ctx.left);
        Lua.Expression right = visitExpression(ctx.right);
        return new Lua.BinaryOperator.Or(left, right);
    }

    @Override
    public Lua.Value.Function visitFunctionLiteral(LuaParser.FunctionLiteralContext ctx) {
        return new Lua.Value.Function(visitFunctionBody(ctx.functionBody()));
    }

    @Override
    public Lua.Value.Number visitNumberLiteral(LuaParser.NumberLiteralContext ctx) {
        return switch (ctx.number()) {
            case LuaParser.HexFloatNumberContext hexFloatNumberContext -> visitHexFloatNumber(hexFloatNumberContext);
            case LuaParser.FloatNumberContext floatNumberContext -> visitFloatNumber(floatNumberContext);
            case LuaParser.IntNumberContext intNumberContext -> visitIntNumber(intNumberContext);
            case LuaParser.HexNumberContext hexNumberContext -> visitHexNumber(hexNumberContext);
            case LuaParser.NumberContext numberContext -> throw new UnsupportedOperationException(
                    "Unable to handle number of type %s".formatted(numberContext.getClass().getCanonicalName()));
        };
    }

    public Lua.Expression visitExpression(LuaParser.ExpressionContext ctx) {
        return switch (ctx) {
            case LuaParser.NilLiteralContext nilLiteralContext -> visitNilLiteral(nilLiteralContext);
            case LuaParser.FalseLiteralContext falseLiteralContext -> visitFalseLiteral(falseLiteralContext);
            case LuaParser.TrueLiteralContext trueLiteralContext -> visitTrueLiteral(trueLiteralContext);
            case LuaParser.NumberLiteralContext numberLiteralContext -> visitNumberLiteral(numberLiteralContext);
            case LuaParser.StringLiteralContext stringLiteralContext -> visitStringLiteral(stringLiteralContext);
            case LuaParser.VarargLiteralContext varargLiteralContext -> visitVarargLiteral(varargLiteralContext);
            case LuaParser.FunctionLiteralContext functionLiteralContext ->
                    visitFunctionLiteral(functionLiteralContext);
            case LuaParser.VariableLiteralContext variableLiteralContext ->
                    visitVariableLiteral(variableLiteralContext);
            case LuaParser.MemberAccessLiteralContext memberAccessLiteralContext ->
                    visitMemberAccessLiteral(memberAccessLiteralContext);
            case LuaParser.FunctionAccessLiteralContext functionAccessLiteralContext ->
                    visitFunctionAccessLiteral(functionAccessLiteralContext);
            case LuaParser.FunctionCallLiteralContext functionCallLiteralContext ->
                    visitFunctionCallLiteral(functionCallLiteralContext);
            case LuaParser.ExpressionAccessLiteralContext expressionAccessLiteralContext ->
                    visitExpressionAccessLiteral(expressionAccessLiteralContext);
            case LuaParser.ParenthesizedExpressionLiteralContext parenthesizedExpressionLiteralContext ->
                    visitParenthesizedExpressionLiteral(parenthesizedExpressionLiteralContext);
            case LuaParser.TableLiteralContext tableLiteralContext -> visitTableLiteral(tableLiteralContext);
            case LuaParser.ExpressionPowerContext expressionPowerContext ->
                    visitExpressionPower(expressionPowerContext);
            case LuaParser.ExpressionUnaryContext expressionUnaryContext ->
                    visitExpressionUnary(expressionUnaryContext);
            case LuaParser.ExpressionMultiplicativeContext expressionMultiplicativeContext ->
                    visitExpressionMultiplicative(expressionMultiplicativeContext);
            case LuaParser.ExpressionAdditiveContext expressionAdditiveContext ->
                    visitExpressionAdditive(expressionAdditiveContext);
            case LuaParser.ExpressionConcatContext expressionConcatContext ->
                    visitExpressionConcat(expressionConcatContext);
            case LuaParser.ExpressionComparativeContext expressionComparativeContext ->
                    visitExpressionComparative(expressionComparativeContext);
            case LuaParser.ExpressionAndContext expressionAndContext -> visitExpressionAnd(expressionAndContext);
            case LuaParser.ExpressionOrContext expressionOrContext -> visitExpressionOr(expressionOrContext);
            case LuaParser.ExpressionBitwiseContext expressionBitwiseContext ->
                    visitExpressionBitwise(expressionBitwiseContext);
            case LuaParser.ExpressionContext expressionContext -> throw new UnsupportedOperationException(
                    "Unable to handle expression of type %s".formatted(
                            expressionContext.getClass().getCanonicalName()));
        };
    }

    @Override
    public Lua.Variable.Named visitMemberVar(LuaParser.MemberVarContext ctx) {
        List<Lua.MemberAccessor> memberAccessors = ctx.memberAccess().stream().map(this::visitMemberAccess).toList();
        return new Lua.Variable.Named(ctx.NAME().getText(), memberAccessors);
    }

    @Override
    public Lua.Variable.Function visitFunctionVar(LuaParser.FunctionVarContext ctx) {
        Lua.FunctionCall functionCall = visitFunctionCall(ctx.functionCall());
        List<Lua.MemberAccessor> memberAccessors = ctx.memberAccess().stream().map(this::visitMemberAccess).toList();
        return new Lua.Variable.Function(functionCall, memberAccessors);
    }

    @Override
    public Lua.Variable.Expression visitExpressionVar(LuaParser.ExpressionVarContext ctx) {
        Lua.Expression expression = visitExpression(ctx.expression());
        List<Lua.MemberAccessor> memberAccessors = ctx.memberAccess().stream().map(this::visitMemberAccess).toList();
        return new Lua.Variable.Expression(expression, memberAccessors);
    }

    public Lua.Variable visitVar(LuaParser.VarContext ctx) {
        return switch (ctx) {
            case LuaParser.MemberVarContext memberVarContext -> visitMemberVar(memberVarContext);
            case LuaParser.FunctionVarContext functionVarContext -> visitFunctionVar(functionVarContext);
            case LuaParser.ExpressionVarContext expressionVarContext -> visitExpressionVar(expressionVarContext);
            case LuaParser.VarContext varContext -> throw new UnsupportedOperationException(
                    "Unable to handle var of type %s".formatted(varContext.getClass().getCanonicalName()));
        };
    }

    @Override
    public Lua.FunctionCall.Direct visitDirectFunctionCall(LuaParser.DirectFunctionCallContext ctx) {
        List<Lua.MemberAccessor> memberAccessors = ctx.memberAccess().stream().map(this::visitMemberAccess).toList();
        Lua.Variable receiver = new Lua.Variable.Named(ctx.receiver.getText(), memberAccessors);
        List<Lua.Expression> args = extractArgs(ctx.args());
        return new Lua.FunctionCall.Direct(receiver, args);
    }

    @Override
    public Lua.FunctionCall.Self visitDirectSelfCall(LuaParser.DirectSelfCallContext ctx) {
        List<Lua.MemberAccessor> memberAccessors = ctx.memberAccess().stream().map(this::visitMemberAccess).toList();
        Lua.Variable receiver = new Lua.Variable.Named(ctx.receiver.getText(), memberAccessors);
        List<Lua.Expression> args = extractArgs(ctx.args());
        return new Lua.FunctionCall.Self(receiver, ctx.methodName.getText(), args);
    }

    @Override
    public Lua.FunctionCall.Direct visitExpressionFunctionCall(LuaParser.ExpressionFunctionCallContext ctx) {
        Lua.Expression receiverExpression = visitExpression(ctx.expression());
        List<Lua.MemberAccessor> memberAccessors = ctx.memberAccess().stream().map(this::visitMemberAccess).toList();
        Lua.Variable receiver = new Lua.Variable.Expression(receiverExpression, memberAccessors);
        List<Lua.Expression> args = extractArgs(ctx.args());
        return new Lua.FunctionCall.Direct(receiver, args);
    }

    @Override
    public Lua.FunctionCall.Self visitExpressionSelfCall(LuaParser.ExpressionSelfCallContext ctx) {
        Lua.Expression receiverExpression = visitExpression(ctx.expression());
        List<Lua.MemberAccessor> memberAccessors = ctx.memberAccess().stream().map(this::visitMemberAccess).toList();
        Lua.Variable receiver = new Lua.Variable.Expression(receiverExpression, memberAccessors);
        List<Lua.Expression> args = extractArgs(ctx.args());
        return new Lua.FunctionCall.Self(receiver, ctx.methodName.getText(), args);
    }

    @Override
    public Lua.FunctionCall.Direct visitNestedFunctionCall(LuaParser.NestedFunctionCallContext ctx) {
        Lua.FunctionCall receiverFunction = visitFunctionCall(ctx.functionCall());
        List<Lua.MemberAccessor> memberAccessors = ctx.memberAccess().stream().map(this::visitMemberAccess).toList();
        Lua.Variable receiver = new Lua.Variable.Function(receiverFunction, memberAccessors);
        List<Lua.Expression> args = extractArgs(ctx.args());
        return new Lua.FunctionCall.Direct(receiver, args);
    }

    @Override
    public Lua.FunctionCall.Self visitNestedSelfCall(LuaParser.NestedSelfCallContext ctx) {
        Lua.FunctionCall receiverFunction = visitFunctionCall(ctx.functionCall());
        List<Lua.MemberAccessor> memberAccessors = ctx.memberAccess().stream().map(this::visitMemberAccess).toList();
        Lua.Variable receiver = new Lua.Variable.Function(receiverFunction, memberAccessors);
        List<Lua.Expression> args = extractArgs(ctx.args());
        return new Lua.FunctionCall.Self(receiver, ctx.methodName.getText(), args);
    }

    public Lua.FunctionCall visitFunctionCall(LuaParser.FunctionCallContext ctx) {
        return switch (ctx) {
            case LuaParser.DirectFunctionCallContext directFunctionCallContext ->
                    visitDirectFunctionCall(directFunctionCallContext);
            case LuaParser.NestedFunctionCallContext nestedFunctionCallContext ->
                    visitNestedFunctionCall(nestedFunctionCallContext);
            case LuaParser.ExpressionFunctionCallContext expressionFunctionCallContext ->
                    visitExpressionFunctionCall(expressionFunctionCallContext);
            case LuaParser.DirectSelfCallContext directSelfCallContext -> visitDirectSelfCall(directSelfCallContext);
            case LuaParser.NestedSelfCallContext nestedSelfCallContext -> visitNestedSelfCall(nestedSelfCallContext);
            case LuaParser.ExpressionSelfCallContext expressionSelfCallContext ->
                    visitExpressionSelfCall(expressionSelfCallContext);
            case LuaParser.FunctionCallContext functionCallContext -> throw new UnsupportedOperationException(
                    "Unable to handle function call of type %s".formatted(
                            functionCallContext.getClass().getCanonicalName()));
        };
    }

    private List<Lua.Expression> extractArgs(LuaParser.ArgsContext ctx) {
        return switch (ctx) {
            case LuaParser.StringArgumentContext stringArgumentContext ->
                    List.of(visitStringArgument(stringArgumentContext));
            case LuaParser.TableArgumentContext tableArgumentContext ->
                    List.of(visitTableArgument(tableArgumentContext));
            case LuaParser.ExpressionArgumentsContext expressionArgumentsContext -> {
                LuaParser.ExpressionListContext expressionListContext = expressionArgumentsContext.expressionList();
                yield expressionListContext == null ? List.of() : expressionListContext
                                                                  .expression()
                                                                  .stream()
                                                                  .map(this::visitExpression)
                                                                  .toList();
            }
            case LuaParser.ArgsContext argsContext -> throw new UnsupportedOperationException(
                    "Unable to handle argument of type %s".formatted(argsContext.getClass().getCanonicalName()));
        };
    }

    public Lua.MemberAccessor visitMemberAccess(LuaParser.MemberAccessContext ctx) {
        return switch (ctx) {
            case LuaParser.ExpressionAccessContext expressionAccessContext ->
                    visitExpressionAccess(expressionAccessContext);
            case LuaParser.NamedAccessContext namedAccessContext -> visitNamedAccess(namedAccessContext);
            case LuaParser.MemberAccessContext memberAccessContext -> throw new UnsupportedOperationException(
                    "Unable to handle member access of type %s".formatted(
                            memberAccessContext.getClass().getCanonicalName()));
        };
    }

    @Override
    public Lua.MemberAccessor.Expression visitExpressionAccess(LuaParser.ExpressionAccessContext ctx) {
        return new Lua.MemberAccessor.Expression(visitExpression(ctx.expression()));
    }

    @Override
    public Lua.MemberAccessor.Named visitNamedAccess(LuaParser.NamedAccessContext ctx) {
        return new Lua.MemberAccessor.Named(ctx.NAME().getText());
    }

    @Override
    public Lua.Value.Table visitTableArgument(LuaParser.TableArgumentContext ctx) {
        return visitTableConstructor(ctx.tableConstructor());
    }

    @Override
    public Lua.Value.String visitStringArgument(LuaParser.StringArgumentContext ctx) {
        return visitString(ctx.string());
    }

    @Override
    public Lua.FunctionBody visitFunctionBody(LuaParser.FunctionBodyContext ctx) {
        List<Lua.Arg> arguments = switch (ctx.parameterList()) {
            case LuaParser.PopulatedParListContext populatedParListContext -> {
                List<Lua.Arg> args = new ArrayList<>();
                populatedParListContext.nameList()
                                       .NAME()
                                       .stream()
                                       .map(ParseTree::getText)
                                       .map(Lua.Arg.Named::new)
                                       .forEach(args::add);
                if (populatedParListContext.vararg != null) {
                    args.add(new Lua.Arg.Var());
                }
                yield args;
            }
            case LuaParser.VarargParListContext _ -> List.of(new Lua.Arg.Var());
            case LuaParser.EmptyParListContext _ -> List.of();
            case LuaParser.ParameterListContext parlistContext -> throw new UnsupportedOperationException(
                    "Unable to handle par list of type %s".formatted(parlistContext.getClass().getCanonicalName()));
        };

        Lua.Statement.Block block = visitBlock(ctx.block());

        return new Lua.FunctionBody(arguments, block);
    }

    @Override
    public Lua visitPopulatedParList(LuaParser.PopulatedParListContext ctx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Lua visitVarargParList(LuaParser.VarargParListContext ctx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Lua visitEmptyParList(LuaParser.EmptyParListContext ctx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Lua.Value.Table visitTableConstructor(LuaParser.TableConstructorContext ctx) {
        LuaParser.FieldListContext fieldListContext = ctx.fieldList();
        if (fieldListContext == null) {
            return new Lua.Value.Table(Map.of());
        }
        Map<Lua.Expression, Lua.Expression> values = new HashMap<>();
        int index = 1;
        for (LuaParser.FieldContext field : fieldListContext.field()) {
            Lua.Expression value = visitExpression(switch (field) {
                case LuaParser.ExpressionFieldAssignmentContext expressionFieldAssignmentContext ->
                        expressionFieldAssignmentContext.value;
                case LuaParser.DirectFieldAssignmentContext directFieldAssignmentContext ->
                        directFieldAssignmentContext.value;
                case LuaParser.IndexFieldAssingmentContext indexFieldAssingmentContext ->
                        indexFieldAssingmentContext.value;
                case LuaParser.FieldContext fieldContext -> throw new UnsupportedOperationException(
                        "Unable to handle field assignments of type %s".formatted(
                                fieldContext.getClass().getCanonicalName()));
            });

            Lua.Expression key = switch (field) {
                case LuaParser.ExpressionFieldAssignmentContext expressionFieldAssignmentContext ->
                        visitExpression(expressionFieldAssignmentContext.key);
                case LuaParser.DirectFieldAssignmentContext directFieldAssignmentContext ->
                        new Lua.Value.String(directFieldAssignmentContext.NAME().getText());
                case LuaParser.IndexFieldAssingmentContext _ -> new Lua.Value.Number(index++);
                case LuaParser.FieldContext fieldContext -> throw new UnsupportedOperationException(
                        "Unable to handle field assignments of type %s".formatted(
                                fieldContext.getClass().getCanonicalName()));
            };

            values.put(key, value);
        }

        return new Lua.Value.Table(values);
    }

    @Override
    public Lua.Value.Number visitIntNumber(LuaParser.IntNumberContext ctx) {
        return new Lua.Value.Number(Double.parseDouble(ctx.getText()));
    }

    @Override
    public Lua.Value.Number visitHexNumber(LuaParser.HexNumberContext ctx) {
        return new Lua.Value.Number(Double.parseDouble(ctx.getText()));
    }

    @Override
    public Lua.Value.Number visitFloatNumber(LuaParser.FloatNumberContext ctx) {
        return new Lua.Value.Number(Double.parseDouble(ctx.getText()));
    }

    @Override
    public Lua.Value.Number visitHexFloatNumber(LuaParser.HexFloatNumberContext ctx) {
        return new Lua.Value.Number(Double.parseDouble(ctx.getText()));
    }

    @Override
    public Lua.Value.String visitNormalString(LuaParser.NormalStringContext ctx) {
        String text = ctx.getText();
        return new Lua.Value.String(text.substring(1, text.length() - 1));
    }

    @Override
    public Lua.Value.String visitCharString(LuaParser.CharStringContext ctx) {
        String text = ctx.getText();
        return new Lua.Value.String(text.substring(1, text.length() - 1));
    }

    @Override
    public Lua.Value.String visitLongString(LuaParser.LongStringContext ctx) {
        String text = ctx.getText();
        return new Lua.Value.String(text.substring(2, text.length() - 2));
    }

    public Lua.Value.String visitString(LuaParser.StringContext ctx) {
        return switch (ctx) {
            case LuaParser.NormalStringContext normalStringContext -> visitNormalString(normalStringContext);
            case LuaParser.CharStringContext charStringContext -> visitCharString(charStringContext);
            case LuaParser.LongStringContext longStringContext -> visitLongString(longStringContext);
            case LuaParser.StringContext stringContext -> throw new UnsupportedOperationException(
                    "Unable to handle string of type %s".formatted(stringContext.getClass().getCanonicalName()));
        };
    }

    @Override
    public Lua visitFunctionName(LuaParser.FunctionNameContext ctx) {
        throw new UnsupportedOperationException("Function names are not supported independently");
    }

    @Override
    public Lua visitVarList(LuaParser.VarListContext ctx) {
        throw new UnsupportedOperationException("Var lists are not supported independently");
    }

    @Override
    public Lua visitNameList(LuaParser.NameListContext ctx) {
        throw new UnsupportedOperationException("Name lists are not supported independently");
    }

    @Override
    public Lua visitExpressionList(LuaParser.ExpressionListContext ctx) {
        throw new UnsupportedOperationException("Expression lists are not supported independently");
    }

    @Override
    public Lua visitExpressionArguments(LuaParser.ExpressionArgumentsContext ctx) {
        throw new UnsupportedOperationException("Expression arguments are not supported independently");
    }

    @Override
    public Lua visitFieldList(LuaParser.FieldListContext ctx) {
        throw new UnsupportedOperationException("Field lists are not supported independently");
    }

    @Override
    public Lua visitExpressionFieldAssignment(LuaParser.ExpressionFieldAssignmentContext ctx) {
        throw new UnsupportedOperationException("Expression field assignments are not supported independently");
    }

    @Override
    public Lua visitDirectFieldAssignment(LuaParser.DirectFieldAssignmentContext ctx) {
        throw new UnsupportedOperationException("Direct field assignments are not supported independently");
    }

    @Override
    public Lua visitIndexFieldAssingment(LuaParser.IndexFieldAssingmentContext ctx) {
        throw new UnsupportedOperationException("Index field assignments are not supported independently");
    }
}
