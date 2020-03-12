grammar XPath;

NAME: [a-zA-Z0-9_-]+;

FILENAME: [a-zA-Z0-9._-]+;

fileName: FILENAME;

TXT: 'text()';

WS: [ \t\r\n]+ -> skip;
//WS: [ \t\r\n]+;

doc: 'doc("' fileName '")';

ap
	: doc '/' rp                   # ApChildren
	| doc '//' rp                  # ApDescendant
	;

rp
	: NAME                         # Tag
	| '*'                          # AllChildren
	| '.'                          # Current
	| '..'                         # Parent
	| TXT                          # Text
	| '@' NAME                     # Attribute
	| '(' rp ')'                   # RpwithParentheses
	| rp '/' rp                    # RpChildren
	| rp '//' rp                   # RpDescendant
	| rp '[' filter ']'            # RpFilter
	| rp ',' rp                    # RpConcatenation
	;

filter
	: rp                           # FilterRp
	| rp '=' rp                    # FilterEqual
	| rp 'eq' rp                   # FilterEqual
	| rp '==' rp                   # FilterIs
	| rp 'is' rp                   # FilterIs
	| '(' filter ')'               # FilterwithParentheses
	| filter 'and' filter          # FilterAnd
	| filter 'or' filter           # FilterOr
	| 'not' filter                 # FilterNot
	;