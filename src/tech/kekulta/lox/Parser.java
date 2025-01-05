package tech.kekulta.lox;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;

import static tech.kekulta.lox.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {};

    private boolean allowExpression = false;
    private boolean foundExpression = false;
    private final List<Token> tokens;
    private int current = 0;
    private int loopDepth = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parseRepl() {
        allowExpression = true;
        List<Stmt> statements = new ArrayList<Stmt>();

        while(!isAtEnd()) {
            statements.add(declaration());
            if(foundExpression) break;
            allowExpression = false;
        }
        return statements;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<Stmt>();

        while(!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Stmt declaration() {
        try {
            if(match(VAR)) return varDeclaration();

            return statement();
        } catch (ParseError e) {
            synchronize();
            return null;
        }
    }

    private Stmt varDeclaration() {

        Token name = consume(IDENTIFIER,
                "Expect variable name.");

        Expr initializer = null;
        if(match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");

        return new Stmt.Var(name, initializer);
    }

    private Stmt statement() {
        if(match(IF)) return ifStatement();
        if(match(PRINT)) return printStatement();
        if(match(FOR)) return forStatement();
        if(match(WHILE)) return whileStatement();
        if(match(LEFT_BRACE)) return block();
        if(match(BREAK)) {
            if(loopDepth == 0)
                throw error(previous(),
                        "'break' prohibited outside of a loop.");
            consume(SEMICOLON, "Expect ';' after 'break'.");
            return new Stmt.Break();
        }
        if(match(CONTINUE)) {
            if(loopDepth == 0)
                throw error(previous(),
                        "'continue' prohibited outside of a loop.");
            consume(SEMICOLON, "Expect ';' after 'continue'.");
            return new Stmt.Continue();
        }
        return expressionStatement();
    } 

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if(match(SEMICOLON)) {
            initializer = null;
        } else if(match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if(!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if(!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after increment.");

        Stmt body;
        try {
            loopDepth++;
            body = statement();
        } finally {
            loopDepth--;
        }

        return new Stmt.For(initializer, condition, increment, body);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");

        Stmt body;
        try {
            loopDepth++;
            body = statement();
        } finally {
            loopDepth--;
        }


        return new Stmt.While(condition, body);
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if(match(ELSE)) elseBranch = statement();

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt block() {
        List<Stmt> statements = new ArrayList<Stmt>();

        while(!isAtEnd() && !check(RIGHT_BRACE)) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect ';' after block.");
        return new Stmt.Block(statements);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt expressionStatement() {
        Expr value = expression();
        if(allowExpression && isAtEnd()) {
            foundExpression = true;
        } else {
            consume(SEMICOLON, "Expect ';' after expression.");
        }
        return new Stmt.Expression(value);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();

        if(match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if(expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        return logicLeftAssociative(() -> (and()), OR);
    }

    private Expr and() {
        return logicLeftAssociative(() -> (equality()), AND);
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
        return binaryLeftAssociative(() -> (equality()), COMMA);
    }

    private Expr equality() {
        return binaryLeftAssociative(
                () -> (comparison()), BANG_EQUAL, EQUAL_EQUAL);
    }

    private Expr comparison() {
        return binaryLeftAssociative(() -> (term()),
                GREATER, GREATER_EQUAL, LESS, LESS_EQUAL);
    }

    private Expr term() {
        return binaryLeftAssociative(() -> (factor()), MINUS, PLUS);
    }

    private Expr factor() {
        return binaryLeftAssociative(() -> (unary()), SLASH, STAR);
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
        if(match(IDENTIFIER)) return new Expr.Variable(previous());
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

    private Expr logicLeftAssociative(
            Supplier<Expr> exprSupplier, TokenType... types) {
        Expr expr = exprSupplier.get();

        while(match(types)) {
            Token operator = previous();
            Expr right = exprSupplier.get();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr binaryLeftAssociative(
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

        while(!isAtEnd()) {
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

            advance();
        }
    }
}
