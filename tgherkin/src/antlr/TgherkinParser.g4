parser grammar TgherkinParser;

options { tokenVocab=TgherkinLexer; }

import WidgetConditionParser;

document : execOptions? feature+ EOF;

execOptions : execOptionExclude execOptionInclude? | execOptionInclude execOptionExclude?;
execOptionExclude : OPTION_KEYWORD_EXCLUDE tags+=TAGNAME+ EOL*;
execOptionInclude : OPTION_KEYWORD_INCLUDE tags+=TAGNAME+ EOL*;

feature:
	tags+=tagname* 
	FEATURE_KEYWORD
	title EOL+
	narrative+=narrativeLine*
	selection?	
	oracle?
	background?	
	scenarioDefiniton+;

background:
	BACKGROUND_KEYWORD 
	title? EOL+ 
	narrative+=narrativeLine*
	selection?	
	oracle?
	steps+=step+;

scenarioDefiniton:	
	(scenario|scenarioOutline);		
	
scenario:
	tags+=tagname* 
	SCENARIO_KEYWORD
	title EOL+
	narrative+=narrativeLine*
	selection?	
	oracle?
	steps+=step+;

scenarioOutline:
	tags+=tagname* 
	SCENARIO_OUTLINE_KEYWORD
	title EOL+
	narrative+=narrativeLine*
	selection?	
	oracle?
	steps+=step+
	examples;

examples:
	EXAMPLES_KEYWORD 
	title? EOL+
	narrative+=narrativeLine* 	
	table;
	
table:
	rows+=TABLE_ROW+ EOL*;

title : WS* ~(EOL)+;

narrativeLine: 
   ~(
      TAGNAME
    | FEATURE_KEYWORD 
    | BACKGROUND_KEYWORD 
    | SCENARIO_KEYWORD  
    | SCENARIO_OUTLINE_KEYWORD 
    | EXAMPLES_KEYWORD 
    | SELECTION_KEYWORD	
    | ORACLE_KEYWORD		
	| STEP_KEYWORD
	| STEP_RANGE_KEYWORD	
    | STEP_GIVEN_KEYWORD 
    | STEP_WHEN_KEYWORD 
    | STEP_THEN_KEYWORD
    | STEP_ALSO_KEYWORD 
    | STEP_EITHER_KEYWORD
	| TABLE_ROW
	| EOL
    )
  ~EOL*
   EOL+;
   
tagname : TAGNAME EOL*;	

selection:
 SELECTION_KEYWORD	
 conditional_gestures+=conditional_gesture+;

oracle:
 ORACLE_KEYWORD	
 widget_tree_condition;
 
step : 
 STEP_KEYWORD
 title EOL+
 stepRange?
 givenClause?
 whenClause
 thenClause?;

givenClause : STEP_GIVEN_KEYWORD widget_tree_condition;
 
whenClause : STEP_WHEN_KEYWORD conditional_gestures+=conditional_gesture+;

thenClause : STEP_THEN_KEYWORD widget_tree_condition;

stepRange:
 STEP_RANGE_KEYWORD
 from=INTEGER_NUMBER
 to=INTEGER_NUMBER;

conditional_gesture:
 widget_condition? 
 gesture; 
 
gesture:
   parameterlessGesture
 | typeGesture
 | clickGesture
 | doubleClickGesture
 | tripleClickGesture
 | anyGesture
 | hitKeyGesture
 | dragDropGesture; 
 
typeGesture: TYPE_NAME LPAREN (STRING | PLACEHOLDER)? RPAREN; 

clickGesture: CLICK_NAME LPAREN (FALSE | TRUE | PLACEHOLDER)? RPAREN;

doubleClickGesture: DOUBLE_CLICK_NAME LPAREN (FALSE | TRUE | PLACEHOLDER)? RPAREN;

tripleClickGesture: TRIPLE_CLICK_NAME LPAREN (FALSE | TRUE | PLACEHOLDER)? RPAREN;

anyGesture: ANY_NAME LPAREN (FALSE | TRUE | PLACEHOLDER)? RPAREN;

hitKeyGesture: HIT_KEY_NAME LPAREN hitKeyArgument* RPAREN;

hitKeyArgument: KB_KEY_NAME | PLACEHOLDER;

dragDropGesture: DRAG_DROP_NAME LPAREN widget_condition? RPAREN;

gestureName:  DRAG_SLIDER_NAME | RIGHT_CLICK_NAME | MOUSE_MOVE_NAME | DROP_DOWN_AT_NAME;

parameterlessGesture: gestureName LPAREN RPAREN; 