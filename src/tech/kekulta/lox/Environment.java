package tech.kekulta.lox;

import java.util.Map;
import java.util.HashMap;

class Environment {
    private final static Object Uninitialized = new Object();

    private final Environment enclosing;
    private final Map<String, Object> values = new HashMap<String, Object>();

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }


    void define(String name, Object value) {
        Object initial = value;
        if(initial == null) {
            initial = Uninitialized;
        }
        values.put(name, initial);
    }

    Object get(Token name) {
        if(values.containsKey(name.lexeme)) {
            if(values.get(name.lexeme) == Uninitialized) {
                throw new RuntimeError(name,
                        "Can't access uninitialized variable '"
                        + name.lexeme + "'.");
            }
            return values.get(name.lexeme);
        }

        if(enclosing != null) return enclosing.get(name);

        throw new RuntimeError(name,
                "Undefined variable '" + name.lexeme + "'.");
    }

    void assign(Token name, Object value) {
        if(values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if(enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name,
                "Undefined variable '" + name.lexeme + "'.");
    }
}
