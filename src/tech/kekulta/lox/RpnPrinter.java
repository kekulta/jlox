package tech.kekulta.lox;

// class RpnPrinter implements Expr.Visitor<String> {
//     public static void main(String[] args) {
//         Expr expr = new Expr.Binary(
//                 new Expr.Unary(
//                     new Token(TokenType.MINUS, "-", null, 0),
//                     new Expr.Literal("100")
//                     ),
//                 new Token(TokenType.STAR, "*", null, 0),
//                 new Expr.Grouping(new Expr.Literal("90"))
//                 );
//
//         System.out.println(new AstPrinter().print(expr));
//     }
//
//     String print(Expr expr) {
//         return expr.accept(this);
//     }
//
//     @Override
//     public String visitConditionalExpr(Expr.Conditional expr) {
//         return "(" + expr.condition.accept(this) 
//             + " ? " + expr.thenBranch.accept(this) 
//             + " : " + expr.elseBranch.accept(this) 
//             + ")";
//     }
//
//     @Override
//     public String visitBinaryExpr(Expr.Binary expr) {
//         return rpn(expr.operator.lexeme, expr.left, expr.right);
//     }
//
//     @Override
//     public String visitGroupingExpr(Expr.Grouping expr) {
//         return expr.expression.accept(this);
//     }
//
//     @Override
//     public String visitLiteralExpr(Expr.Literal expr) {
//         if(expr.value == null) return "nil";
//         return expr.value.toString();
//     }
//
//     @Override
//     public String visitUnaryExpr(Expr.Unary expr) {
//         String lexeme = expr.operator.lexeme;
//         if(expr.operator.type == TokenType.MINUS) {
//             lexeme = "~";
//         }
//
//         return rpn(lexeme, expr.right);
//     }
//
//     private String rpn(String name, Expr... exprs) {
//         StringBuilder builder = new StringBuilder();
//
//         for(Expr expr : exprs) {
//             builder.append(expr.accept(this));
//             builder.append(" ");
//         }
//
//         builder.append(name);
//
//         return builder.toString();
//     }
// }
