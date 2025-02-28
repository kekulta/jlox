package tech.kekulta.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tech.kekulta.lox.TokenType.*;

class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<Token>();
  private final static Map<String, TokenType> keywords;

  private int start = 0;
  private int current = 0;
  private int line = 1;

  static {
    keywords = new HashMap<String, TokenType>();

    keywords.put("and",     AND);
    keywords.put("class",   CLASS);
    keywords.put("else",    ELSE);
    keywords.put("false",   FALSE);
    keywords.put("for",     FOR);
    keywords.put("fun",     FUN);
    keywords.put("if",      IF);
    keywords.put("nil",     NIL);
    keywords.put("or",      OR);
    keywords.put("print",   PRINT);
    keywords.put("return",  RETURN);
    keywords.put("super",   SUPER);
    keywords.put("this",    THIS);
    keywords.put("true",    TRUE);
    keywords.put("var",     VAR);
    keywords.put("while",   WHILE);
    keywords.put("break",   BREAK);
    keywords.put("continue",CONTINUE);
  }

  Scanner(String source) {
    this.source = source;
  }

  List<Token> scanTokens() {
    while(!isAtEnd()) {
      start = current;
      scanToken();
    }

    tokens.add(new Token(EOF, "", null, line));
    return tokens;
  }

  private void scanToken() {
    char c = advance();
    switch(c) {
      case '(': addToken(LEFT_PAREN); break;
      case ')': addToken(RIGHT_PAREN); break;
      case '{': addToken(LEFT_BRACE); break;
      case '}': addToken(RIGHT_BRACE); break;
      case ',': addToken(COMMA); break;
      case '.': addToken(DOT); break;
      case ';': addToken(SEMICOLON); break;
      case '*': addToken(STAR); break;
      case '?': addToken(QUESTION); break;
      case ':': addToken(COLON); break;

      case '-': addToken(match('-') ? MINUS_MINUS : MINUS); break;
      case '+': addToken(match('+') ? PLUS_PLUS : PLUS); break;
      case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
      case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
      case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
      case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;

      case ' ':
      case '\r':
      case '\t':
        break;

      case '\n':
        line++;
        break;

      case '/':
        if(match('/')) {
            singleLineComment();
        } else if(match('*')) {
            multiLineComment();
        } else {
          addToken(SLASH);
        }
        break;

      case '"': string(); break;

      default:
        if(isDigit(c)) {
          number();
        } else if(isAlpha(c)) {
          identifier();
        } else {
          Lox.error(line, "Unexpected character: '" + c + "'");
        }
        break;
    }
  }

  private void multiLineComment() {
      int level = 1;
      while(!isAtEnd()) {
          if(match('\n')) {
              line++;
          }
          if(match('/') && match('*')) {
              level++;
          }

          if(match('*') && match('/')) {
              level--;
              if(level == 0) {
                  return; 
              }
          }

          if(!isAtEnd()) {
              advance();
          }
      };
      
      Lox.error(line, "Unterminated multi-line comment");
  }

  private void singleLineComment() {
      while(peek() != '\n' && !isAtEnd()) advance();
      if(!isAtEnd()) line++;
  }

  private void identifier() {
    while(isAlphaNumeric(peek())) advance();

    String text = source.substring(start, current);
    TokenType type = keywords.get(text);
    if(type == null) type = IDENTIFIER;

    addToken(type);
  }

  private void number() {
    while(isDigit(peek())) advance();

    if(peek() == '.' && isDigit(peekNext())) {
      advance();

      while(isDigit(peek())) advance();
    }

    addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
  }

  private void string() {
    while(peek() != '"' && !isAtEnd()) {
      if(peek() == '\n') line++;
      advance();
    }

    if(isAtEnd()) {
       Lox.error(line, "Unterminated string.");
       return;
    }

    advance();

    String value = source
        .substring(start + 1, current - 1);

    EscapedString escaped = new EscapedString(value);

    try {
        value = escaped.escape();
    } catch(IllegalArgumentException e) {
       Lox.error(line, e.getMessage());
       return;
    }

    addToken(STRING, value);
  }

  private boolean isAlpha(char c) {
    return  (c >= 'a' && c <= 'z') ||
            (c >= 'A' && c <= 'Z') ||
            (c == '_');
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  private char advance() {
    current++;
    return source.charAt(current - 1);
  }

  private char peek() {
    if(isAtEnd()) return '\0';
    return source.charAt(current);
  }

  private char peekNext() {
    if(current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  }

  private boolean match(char expected) {
    if(isAtEnd()) return false;
    if(expected != source.charAt(current)) return false;

    current++;
    return true;
  }

  private void addToken(TokenType type) {
    addToken(type, null);
  }

  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }

  private boolean isAtEnd() {
    return current >= source.length();
  }
}
