# Grammar Notes

## Sources and Scope

The primary source is the supplied **Project D.pdf**, titled *Project D:
Dynamic Language*, six pages. The task calls it `Project D(1).pdf`; the supplied
filename differs. Page references below use its printed page numbers (1-6).
It is used locally and is not included in this public repository because
permission to redistribute it has not been confirmed.

Secondary sources are [the team introduction](language-spec/krutaya_komanda%28intro%29.pdf),
[the lexer presentation](language-spec/krutaya_komanda%28lexer%29.pdf), and the
existing Java sources/tests. The teacher specification takes precedence.
These PDFs were read in full; the teacher pages were also visually inspected.

[grammar.ebnf](grammar.ebnf) is the syntax draft implemented by the syntax-only
parser. The following assumptions remain provisional and require teacher
confirmation. Unresolved source rules are preserved rather than silently
expanded. Numeric tuple access now uses the explicit lexer convention below;
other unresolved source rules have not been silently changed.

## Notation and Token Interface

The EBNF uses `rule = expression ;`, commas for sequence, `|` for
alternatives, `(...)` for grouping, `[...]` for optional content, `{...}` for
repetition, and `(* ... *)` for comments. Source commas use `COMMA`.

Lowercase names are defined syntax nonterminals. Uppercase names are lexical
terminals defined by
[TokenType.java](../src/main/java/io/github/alextoday111/projectd/lexer/TokenType.java).
Lexical terminals are intentionally not expanded into character-level syntax
productions. Their spelling/interface is listed in the grammar header:
`:=` is `ASSIGN`, `=` is `EQUAL`, `=>` is `FAT_ARROW`, and `..` is `RANGE`.
`KW_*` names map to lowercase keywords. `INTEGER`, `REAL`, `STRING`, and `IDENT`
carry lexemes/values; `NEWLINE`, `SEMICOLON`, and `EOF` delimit input.

Comments begin with `//` in teacher examples (p. 3). The current lexer discards
them and ordinary horizontal whitespace but emits each newline. It also
accepts a lone carriage return and CRLF. This describes the implementation,
not additional teacher-approved lexical requirements.

## Source Ambiguities and Draft Decisions

| ID | Rule / pages | Issue and treatment |
|---|---|---|
| A1 | `Program`, `Body`, pp. 1-2 | Prose describes semicolon/newline separators; the printed rules make semicolons optional and omit newlines. **Assumption:** require at least one separator between statements; allow blank lines and leading/trailing separators. Header-to-body boundaries remain structural, so `then print x end` is possible. Bodies remain nonempty. |
| N2 | `IfShort`, `Body`, pp. 2-3 | `if Expression => Body` has no closing token and `Body` can contain multiple statements. **Provisional project convention:** `if Expression => Statement` takes exactly one statement, including a complete compound statement, with no layout separator after `=>`. Following separator-delimited statements belong to the enclosing sequence. Short if has no own `else` or `end`; these belong to a surrounding full conditional/block. This narrows the ambiguous source and is not teacher-approved. |
| A3 | `Factor`, p. 3 | The printed repetition makes `+`/`-` optional, admitting adjacent terms such as `1 2`. This conflicts with the infix description and operator table (p. 6). **Assumption:** an explicit additive operator is required. |
| A4 | `Expression`, `Factor`, `Term`, pp. 3, 6 | Repetition defines tiers but not evaluation associativity or short-circuit behavior. **Assumption:** repeated binary operators fold left within each tier. Short-circuit behavior remains unresolved. `or`, `and`, and `xor` have equal precedence in the printed grammar; conventional `and`-before-`or` precedence is not imported. |
| N5 | `Unary`, `Primary`, pp. 3-4 | Prefix `+`, `-`, `not` applies only to `Primary`, which excludes `Reference`. Thus `-x` / `not x` and repeated prefixes do not follow the printed grammar, while `-(x)` / `not (x)` do. `is` applies only to a reference. The p. 6 operator prose does not clearly justify these restrictions. **Preserved pending clarification**, not broadened to arbitrary operands. |
| N6 | `FunctionLiteral`, `Reference`, p. 4 | The parameter group is optional, but if parentheses are present it requires an identifier. Calls require at least one expression. Therefore `func => 1` is described, but `func()` and `f()` are not. No zero-argument-call rule or standalone call statement is supplied. **Preserved; confirm before adding empty lists or expression statements.** |
| N7 | `Literal`, p. 4 | Two conflicting definitions occur on the same page. The first lists only three kinds and references undefined `IntegerLiteral`, `RealLiteral`, `BooleanLiteral`. The later complete definition and pp. 4-5 prose include strings, booleans, arrays, tuples and `none`. **Use the later definition**; positional tuple access uses the existing `INTEGER` token and named elements use `IDENT` (p. 5's `Identifier` is otherwise undefined). |
| N8 | `Array`, `Tuple`, pp. 4-5 | Source brackets/braces and metanotation are not quoted distinctly. Examples resolve the collection delimiters. Arrays explicitly permit `[]`; the tuple production requires an element. `is {}` is a confirmed type test (p. 4), not proof of an empty tuple literal. **Keep tuples nonempty; `{}` as a value needs confirmation.** |
| A8 | Aggregate layout, p. 5 | The collection productions do not explain newline tokens inside a literal. Existing nested lexer examples span multiple lines. **Assumption:** allow newlines immediately inside array/tuple delimiters and before/after their separating commas. Do not infer general newline continuation after infix operators, inside calls, or between a tuple name and `:=`. |
| N9 | `Assignment`, `Reference`, p. 4 | A reference can contain calls and is also admitted as an assignment target. Whether `f(x) := y` is valid, or whether a call result can be indexed/assigned through, is unspecified. Keep the syntax and defer assignability rules to clarification/semantic checks. |
| L1 | Identifiers, pp. 1, 4-5 | No character set, keyword boundary rule, case rule, or length limit is given. The current lexer accepts `_` and Java `Character.isLetter` initially, then `_` / `Character.isLetterOrDigit`, with exact lowercase keyword matching. This is an implementation description, not a formal teacher requirement. |
| L2 | Strings, p. 4 | The source permits arbitrary characters in matching single/double quotes but does not define escapes or multiline strings. The lexer rejects raw line breaks, interprets several backslash escapes, and drops the backslash for unknown escapes. **Potential mismatch; unchanged.** |
| L3 | Numeric and dot tokens, pp. 4-5 | Decimal digit sequences and real fractions are specified, but limits/overflow and conflicting adjacent dots are not. The lexer uses `Long` / `Double`. Numeric access chains now use the explicit convention below: `t.2.3` is `IDENT DOT INTEGER DOT INTEGER`. `1..3` remains a range; `1...3` has no defined source meaning. This convention is a project decision, not a quoted teacher rule. |

## Numeric Tuple Access Convention

The teacher's `Reference` rule (p. 4) permits repeated `.IntegerLiteral`
suffixes, while the real-literal definition (pp. 4-5) permits `digits.digits`.
Previously `scanNumber()` greedily consumed that pattern in every context,
so `a.1.2.3` incorrectly contained `REAL(1.2)`.

**Project convention:** when the last emitted token is `DOT`, the next digit
sequence is an `INTEGER` tuple position. It does not consume a following
decimal point. Thus `a.1.2.3` becomes three positional suffixes. In every other
context the original decimal-literal rule remains unchanged. `RANGE` is still
recognized before `DOT`; `a.1..10` becomes `IDENT DOT INTEGER RANGE INTEGER`.
The same convention works after named fields, indexing, and calls.

Horizontal whitespace does not emit a token, so `a . 1 . 2` follows the same
rule. `NEWLINE` and `SEMICOLON` emit tokens and end this immediate context:
`a.\n1.2` emits `IDENT DOT NEWLINE REAL`, not a continued tuple reference.
Array brackets, commas, assignments, and operators similarly restore ordinary
decimal scanning: `a.1[2.5]` contains `REAL(2.5)`.

This is intentionally a small lexical convention, not recognition of a full
`Reference`. Even a malformed prefix such as `.1.2` is split into positional
tokens; the parser rejects an invalid root. `a.12.345` means two
positions, never a real-valued field. Index positivity, missing tuple fields,
noninteger array indices belong to semantic/runtime checks; nonsensical syntax
such as `1...2` is rejected by Parser. The lexer still emits tokens for
them. Integer overflow and the existing string/identifier limitations remain.

The parser consumes `DOT (IDENT | INTEGER)` as in `tuple_field` and does
not merge these position tokens into reals. A parser-assisted alternative
could retain finer-grained digit/dot tokens and decide decimal grouping in
expression context, but that would change the token contract and move numeric
value construction across stages. It is not needed for this focused fix.

All four [advanced examples](../examples/README.md) were manually compared
with the EBNF rules. They use full conditions, one-based accesses, nonempty
call/parameter lists, and single-line arrow expressions. Their multiline
aggregates use the already documented A8 assumption. The EBNF productions
need no change for numeric tuple access. All four also pass parser integration
tests under this draft. Ascending ranges in Bubble Sort and higher-order processing assume
inclusive endpoints; this runtime assumption remains unconfirmed by the PDF.

## Precedence and Recursive Descent Guidance

From weakest to strongest, the teacher's p. 3 tiers are:

1. `or`, `and`, `xor` at the same level.
2. A single optional comparison (`<`, `<=`, `>`, `>=`, `=`, `/=`).
3. `+`, `-`.
4. `*`, `/`.
5. The restricted prefix/type-test forms and reference suffixes.
6. Literals, function literals, and parenthesized expressions.

Do not admit chained comparisons. Repeated reference suffixes replace the
source's direct left recursion: start with `IDENT`, then consume indexing,
calls, or fields. Literal/call/grouped-expression roots do not gain arbitrary
suffixes. `if` alternatives share a prefix and can be distinguished by `then`
or `=>` after the condition. A named tuple element requires lookahead for
`IDENT ASSIGN`; otherwise its value starts an expression. `for IDENT in`
similarly requires lookahead because a collection expression may start with
`IDENT`. N2 now uses the explicitly provisional one-statement convention.

`NEWLINE` and `SEMICOLON` end statements under A1, including a bare `return`.
`else`, `end`, and `EOF` delimit enclosing bodies; the parser manages
their ownership explicitly. A8 consumes newlines as layout only at
the aggregate positions named above. Semicolons never become aggregate layout.

## Semantic Constraints Outside EBNF

The teacher specification also requires:

- Declaration scope starts at the declaration; duplicate names within a scope
  are disallowed, and nested scopes may shadow outer names (p. 1).
- An uninitialized variable has `none`; operations on that value are restricted
  to type tests (pp. 1-2). `return` leaves a function and `exit` is permitted
  only inside a loop (p. 3).
- Range loops require integer endpoints; a single collection expression must
  be an array or tuple (pp. 2-3). Range inclusivity, direction, and iteration
  order of sparse arrays are not specified.
- Arrays use integer keys with element numbering starting at one; sparse keys
  are explicitly supported. Tuples have fixed structure, unique names and
  one-based positional access; named/unnamed elements may be mixed (p. 5).
- Numeric arithmetic permits integer/real combinations; addition also supports
  string/string, tuple/tuple and array/array concatenation. Comparisons are
  numeric only; logical operations require booleans. Integer division rounds
  down (p. 6). String-plus-number conversion is not specified.

Capture/closure semantics, call arity checks, duplicate parameter names, tuple
element writes, concatenation name collisions, and missing-key behavior need
clarification. They must not be invented by the syntax grammar.

## Team Presentation Conflicts

| Topic | Teacher specification | Team introduction |
|---|---|---|
| Assignment | `:=`; `=` is equality (pp. 1-3) | `=` assignments (slides 2-10) |
| Functions | `func`, optional parameter group, `is ... end` or `=>` (p. 4) | Named `fun` and `lambda` (slides 8-10) |
| Conditions | `then ... [else ...] end` or `=> Body` (p. 2) | Parenthesized conditions and brace blocks (slide 6) |
| Loops | `while ... loop ... end`, `for`, infinite `loop` (pp. 2-3) | Brace-delimited `while` (slide 6) |
| Arrays | One-based sparse integer keys (p. 5) | Zero-based array indices (slide 7) |
| Tuples | Braces, optional names, dot access starting at one (p. 5) | Parenthesized tuples, bracket access starting at zero (slide 7) |
| Conversions | String addition requires two strings (p. 6) | String-plus-value conversion (slides 4-5) |
| Input/calls | No input construct or empty-call production (pp. 2-4) | `input()` (slide 10); `print(...)` examples |

The lexer presentation (slides 2-7) mostly matches teacher spellings and
describes the existing scanner. Neither deck overrides the teacher PDF.
The retained examples demonstrate tokenization; two also pass syntax checks.
They are not a substitute for specification conformance or runtime validation.

## Open Specification Questions

Confirm A1/A3/A4/A8 and the provisional short-if convention (N2) with the teacher;
resolve unary operands (N5),
zero-argument functions/call statements (N6), and empty tuples (N8). Settle
assignability and the lexical gaps above. Obtain redistribution permission
before adding the teacher PDF. The syntax-only parser implements the current
draft; AST, semantic analysis, interpreter and runtime remain planned.

## Syntax Parser Conventions

The syntax-only parser enforces the EBNF draft without building trees or
evaluating expressions. A1/A3/A4/A8 remain provisional. N5 prefixes still
apply only to a primary: `-(x)` is allowed, `-x` is not. N6 keeps nonempty
parentheses for parameters and call arguments; a function may omit its whole
parameter group. N8 keeps value tuples nonempty, while `x is {}` remains a
type test. Calls are expression/reference suffixes, not standalone statements.

Under N2, `if condition => print 1; print 2` has one conditional statement
followed by a separate print. Multiple guarded statements require the full
`if ... then ... end` form. A single compound statement after `=>` owns its
normal closing `end`. An `else` can attach only to the nearest still-open full
conditional, not to a short conditional. Newlines immediately after `=>` are
not accepted, for either arrow functions or short conditionals.

Bodies remain nonempty. No arbitrary newline continuation is introduced in
calls, parameters, indexing, grouped expressions, or infix expressions. A8
permits layout newlines only around aggregate elements and commas. The parser
requires statement separators and a single explicit EOF at the end of
the supplied token stream. Null, empty, truncated, and early-EOF streams
produce `ParseException` rather than index/null failures.

The original `02_calculator.d` has a newline after `=>` (line 13). It remains
unchanged and Lexer-only. `01_simple.d`, `03_nested_values.d`, and the four
advanced inputs are checked as Parser-valid examples. No grammar rule is
expanded to accommodate the original calculator.

Undefined names, duplicate declarations, `return` outside functions, `exit`
outside loops, invalid assignment targets, index types/ranges, and operation
type restrictions are deliberately outside syntax validation. For example,
`f(x) := value` and `array[1.5] := value` are syntactically admitted by the
existing reference grammar; later stages decide their semantics.

## Parser API, Diagnostics and Limits

`new Parser(tokens).parse()` returns `void` on success and throws
`ParseException` on the first error. The constructor snapshots the list and
validates its token/EOF contract. Each `parse()` starts from the beginning.
No lexer calls, token rewrites, trees, symbol tables or evaluation occur inside
Parser. The separate `ParserMain` reads UTF-8 source and invokes Lexer first;
the executable JAR still launches the original lexer CLI.

Exceptions expose the error kind, expected syntax, actual token, line and
column. Syntax locations come directly from the offending lexer token,
including EOF for missing closers. Invalid streams without a usable token
use a synthetic EOF at `1:1` or the preceding valid token for their location.
There is no error recovery or multiple-error collection.

Flat operator, suffix and statement sequences are iterative. Nested blocks,
expressions and collections use Java recursion, so extreme nesting is limited
by the JVM call stack. Parsing is not thread-safe. With no AST or evaluation,
tests verify accepted syntax and tier boundaries, not computed associativity,
short-circuit behavior or runtime results. Lexical gaps L1/L2/L3 are unchanged.
