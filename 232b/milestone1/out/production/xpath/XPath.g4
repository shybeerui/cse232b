grammar XPath;

NAME: [a-zA-Z0-9_-]+;

TXT: 'text()';

WS: [ \t\r\n]+ -> skip;

doc: 'doc' '(' '"' fileName '"' ')';

fileName: NAME ('.' NAME)?;

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