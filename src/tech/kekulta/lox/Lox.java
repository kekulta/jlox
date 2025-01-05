package tech.kekulta.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    static Interpreter interpreter = new Interpreter();

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            Printer.println("Usage: jlox [script]");
        } else if(args.length == 1) {
            runFile(args[0]);
        } else {
            runPromt();
        }
    }

    static void runtimeError(RuntimeError error) {
        Printer.eprintln(
                "[line " 
                + error.token.line 
                + "] RuntimeError: " 
                + error.getMessage());
        hadRuntimeError = true;
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    static void error(Token token, String message) {
        if(token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if(hadError) System.exit(65);
        if(hadRuntimeError) System.exit(70);
    }

    private static void runPromt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for(;;) {
            hadError = false;
            if(Printer.isNewLine()) {
                Printer.print("> ");
            } else {
                Printer.print("\033[7m%\033[0m\n> ");
            }
            String line = reader.readLine();
            Printer.setNewLine();

            if (line == null) continue;

            Scanner scanner = new Scanner(line);
            List<Token> tokens = scanner.scanTokens();

            Parser parser = new Parser(tokens);
            List<Stmt> statements = parser.parseRepl();
            if(hadError) continue;

            if(statements.size() == 1 
                    && statements.get(0) instanceof Stmt.Expression) {
                String result = 
                    interpreter.interpret((Stmt.Expression)statements.get(0));
                if(result != null) {
                    Printer.println(result);
                }
            } else {
                interpreter.interpret(statements);
            }
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();
        if(hadError) return;

        interpreter.interpret(statements);
    }

    private static void report(int line, String where, String message) {
        Printer.eprintf("[line %d] Error%s: %s\n", line, where, message);
        hadError = true;
    }
}
