package tech.kekulta.lox;

class EscapedString {
    private String source;
    private int current = 0;
    private StringBuilder builder = new StringBuilder();

    EscapedString(String source) {
        this.source = source;
    }

    String escape() {
        builder.setLength(0);

        while(!isAtEnd()) {
            if(match('\\')) {
                switch(peek()) {
                    case '\\':
                        builder.append('\\');
                        break;
                    case 'n':
                        builder.append('\n');
                        break;
                    case 't':
                        builder.append('\t');
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Unknown escape code: '\\" + peek() + "'");
                }

                advance();
            } else {
                builder.append(advance());
            }
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    private char peekNext() {
        if(current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private char peek() {
        return source.charAt(current);
    }

    private boolean match(char c) {
        if(check(c)) {
            advance();
            return true;
        }

        return false;
    }

    private boolean check(char c) {
        return peek() == c;
    }

    private char advance() {
        return source.charAt(current++);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }
}
