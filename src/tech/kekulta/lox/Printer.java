package tech.kekulta.lox;

class Printer {
    private static boolean isNewLine = true;

    static boolean isNewLine() {
        return isNewLine;
    }

    static void print(Object m) {
        String ms = m.toString();
        System.out.print(ms);
        isNewLine = ms.endsWith("\n");
    }

    static void println(Object m) {
        print(m + "\n");
    }

    static void printf(String pat, Object... args) {
        print(String.format(pat, args));
    }

    static void eprint(Object m) {
        String ms = m.toString();
        System.err.print(ms);
        isNewLine = ms.endsWith("\n");
    }

    static void eprintln(Object m) {
        eprint(m + "\n");
    }

    static void eprintf(String pat, Object... args) {
        eprint(String.format(pat, args));
    }
}
