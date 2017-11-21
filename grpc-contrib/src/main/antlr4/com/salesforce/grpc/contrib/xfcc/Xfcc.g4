grammar Xfcc;

/*
 * Parser Rules for x-forwarded-client-cert HTTP header.
 * https://www.envoyproxy.io/docs/envoy/latest/configuration/http_conn_man/headers.html#config-http-conn-man-headers-x-forwarded-client-cert
 */

header         : (element COMMA)* element ;
element        : (kvp SEMOCOLON)* kvp ;
kvp            : key value ;
key            : BY | HASH | SAN | SUBJECT ;
value          : TEXT | QUOTED_TEXT ;


/*
 * Lexer Rules
 */

fragment A     : ('A'|'a') ;
fragment B     : ('B'|'b') ;
fragment C     : ('C'|'c') ;
fragment E     : ('E'|'e') ;
fragment H     : ('H'|'h') ;
fragment J     : ('J'|'j') ;
fragment N     : ('N'|'n') ;
fragment S     : ('S'|'s') ;
fragment T     : ('T'|'t') ;
fragment U     : ('U'|'u') ;
fragment Y     : ('Y'|'y') ;

// Case insensitive keys
BY             : B Y ;
HASH           : H A S H ;
SAN            : S A N ;
SUBJECT        : S U B J E C T ;

// Un-quoted text runs up to the next seperator
TEXT
    : EQUALS ~[;=,]+
    {setText(getText().substring(1));}
    ;

// Quoted text runs up to the first unescaped closing quote
QUOTED_TEXT
    : EQUALS QUOTE (~('"') | BACKSLASH QUOTE)* QUOTE
    {setText(getText().substring(2, getText().length()-1).replace("\\\"", "\""));}
    ;

SEMOCOLON      : ';' ;
COMMA          : ',' ;
EQUALS         : '=' ;
QUOTE          : '"' ;
BACKSLASH      : '\\' ;