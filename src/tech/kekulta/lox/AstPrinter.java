package tech.kekulta.lox;

class AstPrinter implements Expr.Visitor<String> {
    public static void main(String[] args) {
        Expr expr = new Expr.Binary(
                new Expr.Unary(
                    new Token(TokenType.MINUS, "-", null, 0),
                    new Expr.Literal("100")
                    ),
                new Token(TokenType.STAR, "*", null, 0),
                new Expr.Grouping(new Expr.Literal("90"))
                );

        Print.println(new AstPrinter().print(expr));
    }

    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitConditionalExpr(Expr.Conditional expr) {
        return "(" + expr.condition.accept(this)
            + " ? " + expr.thenBranch.accept(this) 
            + " : " + expr.elseBranch.accept(this) 
            + ")";
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if(expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitPostfixExpr(Expr.Postfix expr) {
        StringBuilder builder = new StringBuilder();

        builder.append("(");
        builder.append(expr.left.accept(this));
        builder.append(" ");
        builder.append(expr.operator.lexeme);
        builder.append(")");

        return builder.toString();
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        for(Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }
}
