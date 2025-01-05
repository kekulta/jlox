package tech.kekulta.lox;

import java.util.Objects;
import java.util.List;

import static tech.kekulta.lox.TokenType.*;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private static class BreakException extends RuntimeException {};
    private static class ContinueException extends RuntimeException {};

    private Environment env = new Environment();

    void interpret(List<Stmt> statements) {
        try {
            for(Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    String interpret(Stmt.Expression stmt) {
        try {
            return stringify(evaluate(stmt.expression));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }

        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        Printer.print(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if(stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        env.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(env));
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if(isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if(stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }

        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while(isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
            } catch(BreakException e) {
                break;
            } catch(ContinueException e) {
                continue;
            }
        }

        return null;
    }

    @Override
    public Void visitForStmt(Stmt.For stmt) {
        if(stmt.initializer != null) execute(stmt.initializer);
        while(stmt.condition == null || isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
                if(stmt.increment != null) evaluate(stmt.increment);
            } catch(BreakException e) {
                break;
            } catch(ContinueException e) {
                if(stmt.increment != null) evaluate(stmt.increment);
                continue;
            }
        }

        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new BreakException();
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        throw new ContinueException();
    }

    @Override
    public Object visitConditionalExpr(Expr.Conditional expr) {
        boolean condition = isTruthy(evaluate(expr.condition));

        if(condition) {
            return evaluate(expr.thenBranch);
        } else {
            return evaluate(expr.elseBranch);
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch(expr.operator.type) {
            case SLASH:
                requireNumberOperands(expr.operator, left, right);
                if((double)right == 0.0) {
                    throw new RuntimeError(
                            expr.operator, "Division by zero!");
                }

                return (double)left / (double)right;
            case STAR:
                requireNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case MINUS:
                requireNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                if(left instanceof String || right instanceof String)
                    return stringify(left) + stringify(right);

                if(isNumberOperands(left, right))
                    return (double)left + (double)right;

                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or strings.");

            case EQUAL_EQUAL:
                return isEqual(left, right);
            case BANG_EQUAL:
                return !isEqual(left, right);

            case GREATER:
                if(isNumberOperands(left, right))
                    return (double)left > (double)right;
                if(isStringOperands(left, right))
                    return compareStrings(expr.operator,
                            (String)left, (String)right) > 0;
                throw new RuntimeError(expr.operator,
                        "Only numbers and Strings can be compared.");
            case GREATER_EQUAL:
                if(isNumberOperands(left, right))
                    return (double)left >= (double)right;
                if(isStringOperands(left, right))
                    return compareStrings(expr.operator,
                            (String)left, (String)right) >= 0;
                throw new RuntimeError(expr.operator,
                        "Only numbers and Strings can be compared.");
            case LESS:
                if(isNumberOperands(left, right))
                    return (double)left < (double)right;
                if(isStringOperands(left, right))
                    return compareStrings(expr.operator,
                            (String)left, (String)right) < 0;
                throw new RuntimeError(expr.operator,
                        "Only numbers and Strings can be compared.");
            case LESS_EQUAL:
                if(isNumberOperands(left, right))
                    return (double)left <= (double)right;
                if(isStringOperands(left, right))
                    return compareStrings(expr.operator,
                            (String)left, (String)right) <= 0;
                throw new RuntimeError(expr.operator,
                        "Only numbers and Strings can be compared.");
            
            case COMMA:
                return right;
        }

        return null;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        switch(expr.operator.type) {
            case OR:
                if(isTruthy(left)) return left;
                break;
            case AND:
                if(!isTruthy(left)) return left;
                break;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch(expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                requireNumberOperand(expr.operator, right);
                return -(double)right;
            case MINUS_MINUS:
                requireNumberOperand(expr.operator, right);
                if(expr.right instanceof Expr.Variable)
                    decrement(((Expr.Variable)expr.right).name);
                
                return (double)right - 1;
            case PLUS_PLUS:
                requireNumberOperand(expr.operator, right);
                if(expr.right instanceof Expr.Variable)
                    increment(((Expr.Variable)expr.right).name);
                return (double)right + 1;
        }

        return null;
    }

    @Override
    public Object visitPostfixExpr(Expr.Postfix expr) {
        Object left = evaluate(expr.left);

        switch(expr.operator.type) {
            case MINUS_MINUS:
                requireNumberOperand(expr.operator, left);
                if(expr.left instanceof Expr.Variable)
                    decrement(((Expr.Variable)expr.left).name);
                return (double)left;
            case PLUS_PLUS:
                requireNumberOperand(expr.operator, left);
                if(expr.left instanceof Expr.Variable)
                    increment(((Expr.Variable)expr.left).name);
                return (double)left;
        }

        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return env.get(expr.name);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        env.assign(expr.name, value);
        return value;
    }

    private int compareStrings(Token operator, String left, String right) {
        if(left == null || right == null) 
            throw new RuntimeError(operator, "Can't compare to null!");

        return left.compareTo(right);
    }

    private void increment(Token name) {
        Object variable = env.get(name);
        env.assign(name, (double)variable+1);
    }

    private void decrement(Token name) {
        Object variable = env.get(name);
        env.assign(name, (double)variable-1);
    }

    private String stringify(Object object) {
        if(object == null) return "nil"; 

        if(object instanceof Double) {
            String text = object.toString();
            if(text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }

            return text;
        }

        return object.toString();
    }

    private boolean isStringOperand(Object value) {
        return value instanceof String;
    }

    private boolean isStringOperands(Object left, Object right) {
        return left instanceof String && right instanceof String;
    }

    private void requireStringOperands(
            Token operator, Object left, Object right) {
        if(left instanceof String && right instanceof String) return;

        throw new RuntimeError(operator, "Operand must be strings");
    }

    private void requireStringOperand(Token operator, Object object) {
        if(object instanceof String) return;
        throw new RuntimeError(operator, "Operand must be a string");
    }

    private boolean isNumberOperand(Object value) {
        return value instanceof Double;
    }

    private boolean isNumberOperands(Object left, Object right) {
        return left instanceof Double && right instanceof Double;
    }

    private void requireNumberOperands(
            Token operator, Object left, Object right) {
        if(left instanceof Double && right instanceof Double) return;

        throw new RuntimeError(operator, "Operand must be numbers");
    }

    private void requireNumberOperand(Token operator, Object object) {
        if(object instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number");
    }

    private boolean isEqual(Object left, Object right) {
        return Objects.equals(left, right);
    }

    private boolean isTruthy(Object object) {
        if(object == null) return false;
        if(object instanceof Boolean) return (boolean) object;

        return true;
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt statement) {
        statement.accept(this);
    }

    private void executeBlock(List<Stmt> statements, Environment newEnv) {
        Environment oldEnv = env;

        try {
            env = newEnv;
            for(Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            env = oldEnv;
        }
    }
}
