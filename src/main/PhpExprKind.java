package main;

/**
 * The kind of a expression. It could only be a variable(VAR), a constant(Cons), a composite(COMP) or unknown.
 * @author ruoyzhang
 *
 */

public enum PhpExprKind {
	VAR, CONS, COMP, FUN, UNKOWN
}
