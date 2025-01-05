package tech.kekulta.lox;

import java.util.List;
import java.util.function.Supplier;

import static tech.kekulta.lox.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {};

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        } catch(ParseError e) {
            return null;
        }
    }

    private Expr expression() {
        return conditional();
    }

    private Expr conditional() {
        Expr condition = comma();

        if(match(QUESTION)) {
            Expr thenBranch = expression();
            consume(COLON,
                    "Expect ':' after then branch of conditional expression.");
            Expr elseBranch = conditional();

            return new Expr.Conditional(condition, thenBranch, elseBranch);
        }

        return condition;
    }

    private Expr comma() {
        return leftAssociative(() -> (equality()), COMMA);
    }

    private Expr equality() {
        return leftAssociative(() -> (comparison()), BANG_EQUAL, EQUAL_EQUAL);
    }

    private Expr comparison() {
        return leftAssociative(() -> (term()),
                GREATER, GREATER_EQUAL, LESS, LESS_EQUAL);
    }

    private Expr term() {
        return leftAssociative(() -> (factor()), MINUS, PLUS);
    }

    private Expr factor() {
        return leftAssociative(() -> (unary()), SLASH, STAR);
    }

    private Expr unary() {
        if(match(BANG, MINUS, MINUS_MINUS, PLUS_PLUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return postfix();
    }

    private Expr postfix() {
        Expr expr = primary();

        if(match(MINUS_MINUS, PLUS_PLUS)) {
            Token operator = previous();
            return new Expr.Postfix(expr, operator);
        }

        return expr;
    }

    private Expr primary() {
        if(match(FALSE)) return new Expr.Literal(false);
        if(match(TRUE)) return new Expr.Literal(true);
        if(match(NIL)) return new Expr.Literal(null);

        if(match(NUMBER, STRING)) return new Expr.Literal(previous().literal);
        if(match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after an expression.");
            return new Expr.Grouping(expr);
        }

        if(errorPrefix(() -> (equality()), BANG_EQUAL, EQUAL_EQUAL) != null) {
            return null;
        }

        if(errorPrefix(() -> (comparison()),
                    GREATER, GREATER_EQUAL, LESS, LESS_EQUAL) != null) {
            return null;
        }

        if(errorPrefix(() -> (term()), PLUS) != null) {
            return null;
        }

        if(errorPrefix(() -> (factor()), SLASH, STAR) != null) {
            return null;
        }

        throw error(peek(), "Expect expression.");
    }

    private Expr errorPrefix(
            Supplier<Expr> exprSupplier, TokenType... types) {
        if(match(types)) {
            error(previous(), "Missing left-hand operand.");
            return exprSupplier.get();
        }

        return null;
    }

    private Expr leftAssociative(
            Supplier<Expr> exprSupplier, TokenType... types) {
        Expr expr = exprSupplier.get();

        while(match(types)) {
            Token operator = previous();
            Expr right = exprSupplier.get();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private boolean match(TokenType... types) {
        if(check(types)) {
            advance();
            return true;
        }

        return false;
    }

    private Token advance() {
        if(!isAtEnd()) current++;
        return previous();
    }

    private boolean checkNext(TokenType... types) {
        if(isAtEnd()) return false;

        for(TokenType type : types) {
            if(peekNext().type == type) {
                return true;
            }
        }

        return false;
    }

    private boolean check(TokenType... types) {
        if(isAtEnd()) return false;

        for(TokenType type : types) {
            if(peek().type == type) {
                return true;
            }
        }

        return false;
    }

    private boolean check(TokenType type) {
        if(isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token peekNext() {
        if(isAtEnd()) return peek();
        return tokens.get(current + 1);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token consume(TokenType type, String message) {
        if(check(type)) return advance();

        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        if(!isAtEnd() && previous().type == SEMICOLON) return;

        while(isAtEnd()) {
            switch(peek().type) {
                case CLASS:
                case FOR:
                case FUN:
                case IF:
                case PRINT:
                case RETURN:
                case VAR:
                case WHILE:
                    return;
            }
        }

        advance();
    }
}
