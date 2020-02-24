grammar XQuery;
import XPath;

var : '$' NAME;

forClause: 'for' var 'in' xq (',' var 'in' xq)*;

letClause: 'let' var ':=' xq (',' var ':=' xq)*;

whereClause: 'where' cond ;

returnClause: 'return' xq;

StringConstant: '"'+[a-zA-Z0-9,.!?; ''""-]+'"';

xq
	: var													        # Variable
	| StringConstant											   	# StringConstant
	| ap													        # XqAp
	| '(' xq ')'													# XqwithParentheses
	| xq ',' xq 											   		# TwoXq
	| xq '/' rp													    # XqRp
	| xq '//' rp 												    # XqDescendantRp
	| '<' NAME '>' '{' xq '}' '<' '/' NAME '>'						# XqConstructor
	| forClause letClause? whereClause? returnClause    			# FLWR
	| letClause xq 												    # XqLet
	;


cond
	: xq '=' xq 											         # XqEqual
	| xq 'eq' xq 											         # XqEqual
	| xq '==' xq 											         # XqIs
	| xq 'is' xq 											         # XqIs
	| 'empty' '(' xq ')' 		 							 		 # XqEmpty
	| 'some' var 'in' xq (',' var 'in' xq)* 'satisfies' cond 		 # XqSome
	| '(' cond ')' 											         # XqCondwithParentheses
	| cond 'and' cond 											     # XqCondAnd
	| cond 'or' cond 										 	     # XqCondOr
	| 'not' cond 											         # XqCondNot
	;
