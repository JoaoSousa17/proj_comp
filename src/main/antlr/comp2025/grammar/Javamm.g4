grammar Javamm;

@header {
    package pt.up.fe.comp2025;
}

// Tokens
CLASS : 'class';
PUBLIC : 'public';
STATIC : 'static';
VOID : 'void';
RETURN : 'return';
IF : 'if';
ELSE : 'else';
WHILE : 'while';
IMPORT : 'import';
BOOLEAN : 'boolean';
INT : 'int';
STRING : 'String';
TRUE : 'true';
FALSE : 'false';
NEW : 'new';
THIS : 'this';

// Literals and identifiers
INTEGER : '0' | [1-9][0-9]*;
ID : [a-zA-Z_][a-zA-Z0-9_]*;

// Whitespace and comments
WS : [ \t\n\r\f]+ -> skip;
COMMENT : '//' ~[\r\n]* -> skip;
BLOCK_COMMENT : '/*' .*? '*/' -> skip;

// Grammar Rules
program : importDecl* classDecl EOF;

importDecl : IMPORT ID ('.' ID)* ';';

classDecl : CLASS className=ID ('extends' superName=ID)? '{' varDecl* (methodDecl)* '}';

varDecl : type varName=ID ';';

type : INT '[' ']'    #ArrayType
     | INT '...'      #VarArgsType
     | INT            #IntType
     | BOOLEAN        #BooleanType
     | STRING         #StringType
     | value=ID             #ClassType
     | VOID           #VoidType
     | STRING '[' ']' #StringArrayType;


// Regular method declaration
methodDecl : PUBLIC? STATIC? type methodName=ID '(' paramList? ')' '{' varDecl* stmt* '}';


paramList : param (',' param)*;
param : type (paramName=ID | '...' paramName=ID);  // Only allow '...' in params

// Statements
stmt : '{' stmt* '}'                       #BlockStmt
     | ID '=' expr ';'                     #AssignStmt
     | ID '[' expr ']' '=' expr ';'        #ArrayAssignStmt
     | IF '(' expr ')' stmt (ELSE stmt)?   #IfStmt
     | WHILE '(' expr ')' stmt             #WhileStmt
     | RETURN expr ';'                     #ReturnStmt
     | expr ';'                            #ExprStmt;

// Expressions
expr
    : '(' expr ')'                         #Parenthesis
    | expr '[' expr ']'                    #ArrayAccess
    | expr '.' value=ID '(' (expr (',' expr)*)? ')' #MethodCall
    | expr '.' op='length'                 #LengthOp
    | 'new' name=ID '(' ')'                #GeneralDeclaration
    | 'new' 'int' '[' expr ']'             #IntArrayDeclaration
    | op='!' expr                         #UnaryOp
    | expr op=('*' | '/') expr             #BinaryOp
    | expr op=('+' | '-') expr            #BinaryOp
    | expr op='<' expr                    #BinaryOp
    | expr op='&&' expr                   #BinaryOp
    | value=('true' | 'false')            #Boolean
    | 'this'                              #This
    | value=INTEGER                       #Integer
    | value=ID                            #Identifier
    | '[' (expr (',' expr)*)? ']'         #ArrayInitializer;
