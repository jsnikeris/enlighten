(*****************************************************************
 * Description:  A Propositional Theorem Prover Using Resolution *
 * Top-Level Fn: isTautology                                     *
 * Written by:   Joe Snikeris                                    *
 * For:          Independent Study Project                       *
 * At:           West Chester University of Pennsylvania         *
 * When:         Spring 2009                                     *
 *****************************************************************)

(******************** Data Representation Code *******************)

datatype formula = P | Q | R | S | T		  (* Variables *)
		 | ~       of formula             (* Negation *)
		 | &       of formula * formula	  (* Conjunction *)
		 | v       of formula * formula	  (* Disjunction *)
		 | -->     of formula * formula	  (* Conditional *)
		 | <->     of formula * formula;  (* Biconditional *)
    
(* left associative, same level of precedence *)
infix &;
infix v;
infix -->;
infix <->;

(* needed for ordering of sets *)
fun toString P = "P"
  | toString Q = "Q"
  | toString R = "R"
  | toString S = "S"
  | toString T = "T"
  | toString (~f) = "~" ^ toString f
  | toString (f1 & f2) = "(" ^ toString f1 ^ " & " ^ toString f2 ^ ")"
  | toString (f1 v f2) = "(" ^ toString f1 ^ " v " ^ toString f2 ^ ")"
  | toString (f1 --> f2) = "(" ^ toString f1 ^ " --> " ^ toString f2 ^ ")"
  | toString (f1 <-> f2) = "(" ^ toString f1 ^ " <-> " ^ toString f2 ^ ")";
    
(* creates a linear ordering on formulas *)
fun compareFormula (f1, f2) = String.compare(toString f1, toString f2);

(* used to represent clauses *)
structure FormulaSet = BinarySetFn (struct
		type ord_key = formula
		val compare = compareFormula
		end);

(* used to represent the database - a set of clauses *)	  
structure FormulaSetSet = BinarySetFn (struct
		type ord_key = FormulaSet.set
		val compare = FormulaSet.compare
		end);

(******************** Debugging Code *******************)

(* set to true to enable printing of database after each resolveOnce *)
val traceOn = false;

(* returns a list of lists representing a database db *)
fun listDb db =
    case FormulaSetSet.find (fn f => true) db of
      NONE        => [] |
      SOME clause => let
	val clauseList = FormulaSet.listItems clause
	val db'        = FormulaSetSet.delete (db, clause)
      in
	clauseList::(listDb db')
      end;

fun printClause c = (
    print "{";
    map (print o toString) c;
    print "}"
)

fun printDb db = (map printClause (listDb db); print "\n")
	  
	  
(******************** CNF Code *******************)

(* remove conditionals and biconditionals *)
fun remConds (f1 v f2)   = (remConds f1) v (remConds f2)
  | remConds (f1 & f2)   = (remConds f1) & (remConds f2)
  | remConds (~f)        = ~(remConds f)
  | remConds (f1 --> f2) = ~(remConds f1) v (remConds f2)
  | remConds (f1 <-> f2) = let
      val f1' = remConds f1
      val f2' = remConds f2
    in
      (f1' & f2') v (~f1' & ~f2')
    end
  | remConds f = f;

(* push negations inwards until they are immediately before variables *)
(* call only after deconditionalizing *)
fun pushNegations (f1 v f2) = (pushNegations f1) v (pushNegations f2)
  | pushNegations (f1 & f2) = (pushNegations f1) & (pushNegations f2)
  | pushNegations (~(f1 & f2)) = (pushNegations (~f1)) v (pushNegations (~f2))
  | pushNegations (~(f1 v f2)) = (pushNegations (~f1)) & (pushNegations (~f2))
  | pushNegations (~(~f)) = pushNegations f
  | pushNegations f = f;

(* remove double-negations *)
(* call only after deconditionalizing *)
fun remDoubNeg (f1 & f2) = (remDoubNeg f1) & (remDoubNeg f2)
  | remDoubNeg (f1 v f2) = (remDoubNeg f1) v (remDoubNeg f2)
  | remDoubNeg (~ (~ f)) = (remDoubNeg f)
  | remDoubNeg (~ f)     = ~(remDoubNeg f)
  | remDoubNeg f = f;

(* distribute conjunctions over disjunctions *)
(* call only after deconditionalizing and pushing negations inward *)
fun distribute (f1 & f2) = (distribute f1) & (distribute f2)
  | distribute (f as f1 v f2) = let
      val f1' = distribute f1
      val f2' = distribute f2
    in
      case (f1', f2') of
	(g1 & g2, g3) => distribute (g1 v g3) & distribute (g2 v g3)
      | (g1, g2 & g3) => distribute (g1 v g2) & distribute (g1 v g3)
      | _             => f
    end
  | distribute f = f;

val cnf = distribute o remDoubNeg o pushNegations o remConds;

    
(******************** Resolution Code *******************)    

fun isLiteral P    = true
  | isLiteral Q    = true
  | isLiteral R    = true
  | isLiteral S    = true
  | isLiteral T    = true
  | isLiteral (~f) = isLiteral f
  | isLiteral _    = false;
    
exception NotAClause of formula;
			
(* takes a clause (as a formula) and returns a set of literals *)
fun clauseToSet (f1 v f2) = FormulaSet.union (clauseToSet f1, clauseToSet f2)
  | clauseToSet f = if isLiteral f then
		      FormulaSet.singleton f
		    else raise NotAClause f
			    
(* returns true iff 'clause' contains a literal and its negation *)
fun containsPOrNotP clause =
    case FormulaSet.find (fn f => true) clause of
      NONE         => false
    | SOME literal => let
	val notLiteral = remDoubNeg (~ literal)
	val clause'    = FormulaSet.delete (clause, literal)
      in
	if FormulaSet.member (clause', notLiteral) then true
	else containsPOrNotP clause'
      end;    
    
fun cnfToDb (f1 & f2)     = FormulaSetSet.union (cnfToDb f1, cnfToDb f2)
  | cnfToDb clauseFormula = let
      val clauseSet = clauseToSet clauseFormula handle
	  NotAClause f => (print (toString f);
			   print " is not a clause";
			   print "\n";
			   FormulaSet.empty
			  )
    in
      if containsPOrNotP clauseSet then
	FormulaSetSet.empty
      else
	FormulaSetSet.singleton (clauseSet)
    end;

(* returns NONE if the clauses cannot be resolved, otherwise 
 *   returns an option containing the resolvent 
 *)
fun resolve (c1, c2) = let
  (* "cdr" down the first set, but keep a copy of the original *)
  fun resolveAux (c1, c1', c2) =
      case FormulaSet.find (fn f => true) c1' of
	NONE         => NONE |	(* c1 is empty set *)
	SOME literal => let
	  val notLiteral = remDoubNeg (~ literal)
	in
	  if FormulaSet.member (c2, notLiteral) then let
	      val ans = (FormulaSet.union (FormulaSet.delete (c1, literal),
					   FormulaSet.delete (c2, notLiteral)))
	    in			
	      if containsPOrNotP ans then (* don't add {P,...,~P,...} *)
		resolveAux (c1, FormulaSet.delete (c1', literal), c2)
	      else SOME ans
	    end
	  else
	    resolveAux (c1, FormulaSet.delete (c1', literal), c2)
	end
in
  resolveAux (c1, c1, c2)
end;

(* try to resolve c1 with each clause in db. *)
(* returns the db after c1 has been resolved through *)
fun resolveOnce (dbOrig, db, c1) =
    case FormulaSetSet.find (fn f => true) db of
      NONE    => dbOrig
    | SOME c2 => let
	val db' = FormulaSetSet.delete (db, c2)
      in
	case resolve (c1, c2) of
	  NONE => resolveOnce (dbOrig, db', c1)
	| SOME resolvent =>
	  resolveOnce (FormulaSetSet.add (dbOrig, resolvent), db', c1)
      end;

(* returns true if the set of formulas in the db is consistent *)
fun isConsistent db =
    case FormulaSetSet.find (fn f => true) db of
      NONE        => true
    | SOME clause =>
      if FormulaSet.isEmpty clause then false else (* empty clause *)
      let
	val db' = FormulaSetSet.delete (db, clause)
      in (
	if traceOn then (
	  printClause (FormulaSet.listItems clause);
	  print "\t";
	  printDb db'
	  )
	else ();
	isConsistent (resolveOnce (db', db', clause))
	)
      end;

(* returns true if the given formula is a tautology *)
val isTautology = not o isConsistent o cnfToDb o cnf  o ~;

    
(******************** Testing Code *******************)

val formulas = [
(* f1 *)  (P, false),
(* f2 *)  (~P, false),
(* f3 *)  (~(~P), false),
(* f4 *)  (~(~(~P)), false),
(* f5 *)  (P --> Q, false),
(* f6 *)  (P v ~P, true),
(* f7 *)  ((P v Q) --> P, false),
(* f8 *)  (P <-> Q, false),
(* f9 *)  ((((P --> Q) --> R) --> S) --> ((Q --> R) --> (P --> S)), true),
(* f10 *) (~S & ~T, false),
(* f11 *) ((P --> Q) & (Q --> R), false),
(* f12 *) (((P --> Q) &  (Q --> R))  -->  (P --> R), true),
(* f13 *) ((P & Q) --> P, true),
(* f14 *) (((P --> Q) --> (~Q --> ~P)), true),
(* f15 *) (((P --> ~Q) v (~P & ~Q)), false),
(* f16 *) ((P --> ~Q) v (P & Q), true),
(* f17 *) ((P --> Q) <-> (~Q --> ~P), true),
(* f18 *) ((P --> Q) --> (~Q --> ~P), true),
(* f19 *) ((~Q --> ~P) --> (P --> Q), true),
(* f20 *) (P --> ((P --> Q) <-> Q), true),
(* f21 *) (((P & Q) --> R) <-> ((P & ~R) --> Q), false),
(* f22 *) ((P --> ~Q) v (~P & ~Q), false),
(* f23 *) (P --> (~P --> Q), true),
(* f24 *) ((~P --> Q) --> (~Q --> P), true),
(* f25 *) ((~P --> P) --> P, true),
(* f26 *) ((~P --> P) --> ~P, false),
(* f27 *) ((~(P --> Q)) --> P, true)
];
    
(* tests all formulas starting with the nth *)
fun testAll n =
    if n >= 0 andalso n < List.length formulas then let
	val formulaPair = List.nth (formulas, n)
	val formula     = #1(formulaPair)
	val answer      = #2(formulaPair)
	val isTaut      = isTautology formula
      in (
	if isTaut = answer then print "CORRECT\t\t"
	else print "INCORRECT\t";
	print ((toString formula) ^ " is ");
	if isTaut then ()
	else print "not ";
	print "a tautology.\n";
	testAll (n + 1)
	)
      end
    else
      ();


(******************** Demo Code *******************)    

(* this function was used in a demonstration of the program
 *   to a non-technical audience.
 *)
fun prove (goal, premises) = let
  val db = cnfToDb (cnf (premises & ~goal));
in
  not (isConsistent db)
end;

val premises = (P <-> S) & (T --> (R v P)) & (R --> (P v S));
val goal     = T --> S;
