program         ->  declaration* EOF ;
declaration     ->  varDecl | statement ;
varDecl         ->  "var" IDENTIFIER ( "=" expression )? ";" ;
statement       ->  exprStmt 
                    | ifStmt
                    | whileStmt
                    | forStmt
                    | printStmt 
                    | block;
                    | break;
                    | continue;
break           ->  "break" ";" ;
continue        ->  "continue" ";" ;
forStmt         -> "for" "(" (varDecl | exprStmt | ";" )
                            expression? ";" 
                            expression? ")" statement ;
whileStmt       ->  "while" "(" expression ")" statement ;
ifStmt          ->  "if" "(" expression ")" statement 
                    ("else" statement)? ;
block           ->  "{" declaration* "}" ;
exprStmt        ->  expression ";" ;
printStmt       ->  "print" expression ";" ;
expression      ->  assignment;
assignment      ->  IDENTIFIER "=" assignment | logicOr ;
logicOr         ->  logicAnd ( "or" logicAnd )* ;
logicAnd        ->  equality ( "and" equality )* ;
conditional     ->  comma ( "?" expression ":" conditional)? ;
comma           ->  equality ( "," equality )*;
equality        ->  comparison ( ( "!=" | "==" ) comparison )* ;
comparison      ->  term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term            ->  factor ( ( "-" | "+" ) factor )* ;
factor          ->  unary ( ( "/" | "*" ) unary )* ;
unary           ->  ( "!" | "-" | "++" | "--") unary | postfix ;
postfix         ->  primary ( "!" | "-" | "++" | "--") | primary ;
primary         ->  NUMBER 
                    | STRING 
                    | "true" 
                    | "false" 
                    | "nil" 
                    | "(" expression ")" 
                    | IDENTIFIER
                    // Error productions...
                    | ( "!=" | "==" ) equality
                    | ( ">" | ">=" | "<" | "<=" ) comparison
                    | ( "+" ) term
                    | ( "/" | "*" ) factor ;
