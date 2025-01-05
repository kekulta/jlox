expression ->   conditional;
conditional ->  comma ( "?" expression ":" conditional)? ;
comma ->        equality ( "," equality )*;
equality ->     comparison ( ( "!=" | "==" ) comparison )* ;
comparison ->   term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term ->         factor ( ( "-" | "+" ) factor )* ;
factor ->       unary ( ( "/" | "*" ) unary )* ;
unary ->        ( "!" | "-" | "++" | "--") unary | postfix ;
postfix ->      primary ( "!" | "-" | "++" | "--") | primary ;
primary ->      NUMBER 
                | STRING 
                | "true" 
                | "false" 
                | "nil" 
                | "(" expression ")" 
                // Error productions...
                | ( "!=" | "==" ) equality
                | ( ">" | ">=" | "<" | "<=" ) comparison
                | ( "+" ) term
                | ( "/" | "*" ) factor ;
