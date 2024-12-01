/*

MIT license

Author: Ken Domino, October 2023

Based on previous work of: Kazunori Sakamoto, Alexander Alexeev

*/

// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine false, allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging

parser grammar LuaParser;

options {
    tokenVocab = LuaLexer;
}

start
    : chunk EOF
    ;

chunk
    : block
    ;

block
    : statement* returnStatement?
    ;

statement
    : ';' # emptyStatement
    | varList '=' expressionList # assignmentStatement
    | 'local' attributeNameList ('=' expressionList)? # localAssignmentStatement
    | functionCall # functionCallStatement
    | '::' NAME '::' # labelStatement
    | 'break' # breakStatement
    | 'goto' NAME # gotoStatement
    | 'do' block 'end' # doStatement
    | 'while' expression 'do' block 'end' # whileStatement
    | 'repeat' block 'until' expression # repeatStatement
    | 'if' expression 'then' block ('elseif' expression 'then' block)* ('else' block)? 'end' # ifStatement
    | 'for' NAME '=' startValue=expression ',' endValue=expression (',' stepValue=expression)? 'do' block 'end' # numericForStatement
    | 'for' nameList 'in' expressionList 'do' block 'end' # genericForStatement
    | 'function' functionName functionBody # functionStatement
    | 'local' 'function' NAME functionBody # localFunctionStatement
    ;

attributeNameList
    : attributeName (',' attributeName)*
    ;

attributeName
    : nameValue=NAME ('<' attributeValue=NAME '>')?
    ;

returnStatement
    : ('return' expressionList? | 'break' | 'continue') ';'?
    ;

functionName
    : NAME ('.' NAME)* (':' NAME)?
    ;

varList
    : var (',' var)*
    ;

nameList
    : NAME (',' NAME)*
    ;

expressionList
    : expression (',' expression)*
    ;

expression
    : 'nil' # nilLiteral
    | 'false' # falseLiteral
    | 'true' # trueLiteral
    | number # numberLiteral
    | string # stringLiteral
    | '...' # varargLiteral
    | 'function' functionBody # functionLiteral
    | NAME memberAccess* # memberAccessLiteral
    | functionCall memberAccess* # functionAccessLiteral
    | '(' expression ')' memberAccess* # expressionAccessLiteral
    | tableConstructor # tableLiteral
    | <assoc=right> left=expression ('^') right=expression # expressionPower
    | operator=('not' | '#' | '-' | '~') expression # expressionUnary
    | left=expression operator=('*' | '/' | '%' | '//') right=expression # expressionMultiplicative
    | left=expression operator=('+' | '-') right=expression # expressionAdditive
    | <assoc=right>left=expression ('..') right=expression # expressionConcat
    | left=expression operator=('<' | '>' | '<=' | '>=' | '~=' | '==') right=expression # expressionComparative
    | left=expression operator='and' right=expression # expressionAnd
    | left=expression operator='or' right=expression # expressionOr
    | left=expression operator=('&' | '|' | '~' | '<<' | '>>') right=expression # expressionBitwise
    ;

// var ::=  Name | prefixexp '[' exp ']' | prefixexp '.' Name
var
    : NAME memberAccess* # memberVar
    | functionCall memberAccess+ # functionVar
    | '(' expression ')' memberAccess+ # expressionVar
    ;

// functioncall ::=  prefixexp args | prefixexp ':' Name args;
functionCall
    : receiver=NAME memberAccess* args # directFunctionCall
    | receiver=NAME memberAccess* ':' methodName=NAME args # directSelfCall
    | '(' expression ')' memberAccess* args # expressionFunctionCall
    | '(' expression ')' memberAccess* ':' methodName=NAME args # expressionSelfCall
    | functionCall memberAccess+ args # nestedFunctionCall
    | functionCall memberAccess+ ':' methodName=NAME args # nestedSelfCall
    ;

memberAccess
    :'[' expression ']' # expressionAccess
    | '.' NAME # namedAccess
    ;

args
    : '(' expressionList? ')' # expressionArguments
    | tableConstructor # tableArgument
    | string # stringArgument
    ;

functionBody
    : '(' parameterList ')' block 'end'
    ;

/* lparser.c says "is 'parlist' not empty?"
 * That code does so by checking la(1) == ')'.
 * This means that parlist can derive empty.
 */
parameterList
    : nameList (',' vararg='...')? # populatedParList
    | '...' # varargParList
    | # emptyParList
    ;

tableConstructor
    : '{' fieldList? '}'
    ;

fieldList
    : field ((',' | ';') field)* (',' | ';')?
    ;

field
    : '[' key=expression ']' '=' value=expression # expressionFieldAssignment
    | NAME '=' value=expression # directFieldAssignment
    | value=expression # indexFieldAssingment
    ;

number
    : INT # intNumber
    | HEX # hexNumber
    | FLOAT # floatNumber
    | HEX_FLOAT #hexFloatNumber
    ;

string
    : NORMALSTRING # normalString
    | CHARSTRING # charString
    | LONGSTRING # longString
    ;