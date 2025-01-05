package tech.kekulta.lox;

import java.util.Objects;

import static tech.kekulta.lox.TokenType.*;

class Interpreter implements Expr.Visitor<Object> {

    void interpret(Expr expr) {
        try {
            Object value = evaluate(expr);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
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
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                if(left instanceof Double && right instanceof Double)
                    return (double)left + (double)right;
                if(left instanceof String && right instanceof String)
                    return (String)left + (String)right;
                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");

            case EQUAL_EQUAL:
                return isEqual(left, right);
            case BANG_EQUAL:
                return !isEqual(left, right);

            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            
            case COMMA:
                return right;
        }

        return null;
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
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            case MINUS_MINUS:
                checkNumberOperand(expr.operator, right);
                return (double)right - 1;
            case PLUS_PLUS:
                checkNumberOperand(expr.operator, right);
                return (double)right + 1;
        }

        return null;
    }

    @Override
    public Object visitPostfixExpr(Expr.Postfix expr) {
        Object left = evaluate(expr.left);

        switch(expr.operator.type) {
            case MINUS_MINUS:
                checkNumberOperand(expr.operator, left);
                return (double)left;
            case PLUS_PLUS:
                checkNumberOperand(expr.operator, left);
                return (double)left;
        }

        return null;
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

    private void checkNumberOperands(
            Token operator, Object left, Object right) {
        if(left instanceof Double && right instanceof Double) return;

        throw new RuntimeError(operator, "Operand must be numbers");
    }

    private void checkNumberOperand(Token operator, Object object) {
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
}
