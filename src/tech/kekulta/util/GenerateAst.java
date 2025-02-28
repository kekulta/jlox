package tech.kekulta.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if(args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }

        String outputDir = args[0];
        defineAst(outputDir, "Expr", Arrays.asList(
            "Conditional: Expr condition, Expr thenBranch, Expr elseBranch",
            "Binary     : Expr left, Token operator, Expr right",
            "Grouping   : Expr expression",
            "Literal    : Object value",
            "Unary      : Token operator, Expr right",
            "Postfix    : Expr left, Token operator",
            "Variable   : Token name",
            "Assign     : Token name, Expr value",
            "Logical    : Expr left, Token operator, Expr right"
        ));

        defineAst(outputDir, "Stmt", Arrays.asList(
            "Expression : Expr expression",
            "Break      : ",
            "Continue   : ",
            "Print      : Expr expression",
            "Var        : Token name, Expr initializer",
            "Block      : List<Stmt> statements",
            "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
            "While      : Expr condition, Stmt body",
            "For        : Stmt initializer, Expr condition, "
                        + "Expr increment, Stmt body"
        ));
    }   

    private static void defineAst(
            String outputDir, String baseName, List<String> types
            ) throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");
        System.out.printf("Generated: %s\n", path);

        writer.println("package tech.kekulta.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");

        defineVisitor(writer, baseName, types);

        writer.println("  abstract <R> R accept(Visitor<R> visitor);");
        writer.println();

        for(String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();

            defineType(writer, baseName, className, fields);
        }

        writer.println("}");
        writer.close();
    }

    private static void defineVisitor(
            PrintWriter writer, String baseName, List<String> types) {
        writer.println("  interface Visitor<R> {");

        for(String type : types) {
            String typeName = type.split(":")[0].trim();

            writer.println("    R visit" + typeName + baseName + "(" +
                    typeName + " " + baseName.toLowerCase() + ");");
        }
        writer.println("  }");
        writer.println();
    }

    private static void defineType(
            PrintWriter writer, String baseName,
            String className, String fieldsList) {

        writer.println("  static class " + className 
                + " extends " + baseName + " {");
        writer.println("    " + className + "(" + fieldsList + ") {");

        String[] fields;
        if(fieldsList.isEmpty()) {
            fields = new String[0];
        } else {
            fields = fieldsList.split(", ");
        }
        for(String field : fields) {
            String name = field.split(" ")[1];
            writer.println("      this." + name + " = " + name + ";");
        }

        writer.println("    }");

        writer.println();
        writer.println("    @Override");
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.println("      return visitor.visit" + className + baseName + 
                "(this);");
        writer.println("    }");

        writer.println();
        for(String field : fields) {
            writer.println("    final " + field + ";");
        }
        writer.println("  }");
        writer.println();
    }
}

