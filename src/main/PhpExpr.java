package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import funlib.FunctionSig;
import utils.DocUtils;
import utils.PhpExprUtils;

public class PhpExpr {
	
	private String top;
	private List<PhpExpr> subExprs;
	private int position;

	private PhpExprType exprType;
	private PhpExprKind exprKind;
	
	// When assigning a variable(assignedVar), the position of the variable is its parent statement's startLine
	// When using a variable(!assignedVar), the position is the same with its last assignment
	public Stmt parentStmt;
	public boolean assignedVar;

	// for taint analysis
	private PhpExpr taintCondition;
	
	// constructor for reading php-parser node
	public PhpExpr(Node exprNode, Stmt pt, boolean av) throws Exception {
		
		subExprs = new ArrayList<PhpExpr>();
		setPosition(-1);
		
		parentStmt = pt;
		assignedVar = av;
		
		readExprFromNode(exprNode);
	}
	
	//  constructor for reading phpparser node with the type
	public PhpExpr(Node exprNode, Stmt pt, boolean av, PhpExprType newType) throws Exception {
		
		subExprs = new ArrayList<PhpExpr>();
		setPosition(-1);
		
		exprType = newType;
		
		parentStmt = pt;
		assignedVar = av;
		
		readExprFromNode(exprNode);
		
	}
	
	// constructor for a blank PhpExpr
	public PhpExpr() {
		
		subExprs = new ArrayList<PhpExpr>();
		setPosition(-1);
		
		parentStmt = null;
		assignedVar = false;
		
		taintCondition = null;

	}
	
	// duplicate PhpExpr
	private PhpExpr(PhpExpr expr) throws Exception{
		
		if (expr == null) {
			
			throw new Exception("Can't duplicate a null expression");
			
		}
		
		
		top = expr.top;
		subExprs = new ArrayList<PhpExpr>();
		for (int i = 0; i < expr.subExprs.size(); i++) {
				
			PhpExpr newSubExpr = new PhpExpr(expr.subExprs.get(i));
			subExprs.add(newSubExpr);
				
		}
		setPosition(expr.getPosition());
			
		exprType = expr.exprType;
		exprKind = expr.exprKind;
			
		// when duplicating, the new expression has the same parentStmt and assignedVar
		parentStmt = expr.parentStmt;
		assignedVar = expr.assignedVar;
		
		taintCondition = expr.taintCondition;
	}
	
	public PhpExpr getTaintCondition() {
		
		return taintCondition;
		
	}
	
	public void setTaintCondition(PhpExpr new_tc) {
		
		taintCondition = new_tc;
		
	}
	
	public String getString(boolean withPosition) {
		
		String retString = "";
		
		// Case variable
		if (exprKind == PhpExprKind.VAR) {
			
			if (top == "@") {
				
				if (withPosition) {
					
					retString = subExprs.get(0).top + "@" + subExprs.get(1).top + "*" + String.valueOf(subExprs.get(0).getPosition());
					
				}
				
				else {
					
					retString =  subExprs.get(0).top +  "@" + subExprs.get(1).top;
				}
				
			}
			
			else {
				
				if (withPosition) {
					
					retString = top + "*" + String.valueOf(getPosition());
					
				}
				
				else {
					
					retString = top;
					
				}
					
				
			}
			
		}
		
		// Case constant
		else if (exprKind == PhpExprKind.CONS) {
			
			if (exprType == PhpExprType.STR) {
				retString = "\"" + top + "\"";
			}
			
			else {
				
				retString = top;
				
			}
			
		} 
		
		// Case composite
		else if (exprKind == PhpExprKind.COMP) {
			
			retString = "(";
			retString += top;
			retString += " ";
			
			for(int i = 0; i < subExprs.size(); i++) {
				
				retString += subExprs.get(i).getString(true);
				retString += " ";
				
			}
			
			retString += ")";
			
			
		}
		
		else if (exprKind == PhpExprKind.FUN) {
			
			Set<String> funList = JAnalyzer.funlib.getAllLibSmtNames();
			boolean funInLib = false;
			
			for (String fun : funList) {
				if (fun.compareTo(top) == 0) {
					funInLib = true;
					break;
				}
			}
			if (!funInLib) {
				return PhpExprUtils.funToVar(this).getString(true);
			}
			
			retString = "(";
			retString += top + " ";

			
			for (int i = 0; i < subExprs.size(); i++) {
				
				if (i != 0) {
					
					retString += ",";
					
				}
				
				retString += subExprs.get(i).getString(true);
				
			}
			
			retString += ")";
			
		}
		
		// Case unhandled
		else {
			
			System.out.println("[ERROR]Unknown kind of expression");
			
		}
		
		return retString;
		
	}
	
	// get a relocated expr
	public String getStringReloc(int loc) {
		
		String retString = "";
		
		// Case variable
		if (exprKind == PhpExprKind.VAR) {
			
			if (top == "@") {
					
				retString = subExprs.get(0).top + "@" + subExprs.get(1).top + "*" + loc;
	
			}
			
			else {
					
				retString = top + "*" + loc;
					
			}
			
		}
		
		// Case constant
		else if (exprKind == PhpExprKind.CONS) {
			
			if (exprType == PhpExprType.STR) {
				retString = "\"" + top + "\"";
			}
			
			else {
				
				retString = top;
				
			}
			
		} 
		
		// Case composite
		else if (exprKind == PhpExprKind.COMP) {
			
			retString = "(";
			retString += top;
			retString += " ";
			
			for(int i = 0; i < subExprs.size(); i++) {
				
				retString += subExprs.get(i).getStringReloc(loc);
				retString += " ";
				
			}
			
			retString += ")";
			
			
		}
		
		else if (exprKind == PhpExprKind.FUN) {
			
			retString = "(";
			retString += top + " ";

			
			for (int i = 0; i < subExprs.size(); i++) {
				
				if (i != 0) {
					
					retString += ",";
					
				}
				
				retString += subExprs.get(i).getStringReloc(loc);
				
			}
			
			retString += ")";
			
		}
		
		// Case unhandled
		else {
			
			System.out.println("[ERROR]Unknown kind of expression");
			
		}
		
		return retString;
		
	}
	
	public String getTop() {
		
		return top;
		
	}
	
	public void setTop(String newTop) {
		
		if (newTop == null) {
			
			top = "";
			
		} else {
			
			top = newTop;
			
		}
		
	}
	
	
	public int getPosition() {
		
		return position;
		
	}
	
	public void setPosition(int newPosition) {
		
		position = newPosition;
		
	}
	
	public List<PhpExpr> getSubExprs() {
		
		return subExprs;
		
	}
	
	public PhpExprType getExprType() {
		
		return exprType;
		
	}
	
	
	public PhpExprKind getExprKind() {
		
		return exprKind;
		
	}
	
	
	public void setSubExprs(List<PhpExpr> newSubExprs) {
		
		if (newSubExprs == null) {
			
			subExprs = null;
			
		} else {
			
			subExprs = newSubExprs;
		}
	}
	
	
	public void setExprType(PhpExprType newExprType) {
		
		if (newExprType == null) {
			
			exprType = PhpExprType.UNKOWN;
			
		} else {
			
			exprType = newExprType;
		}
		
	}
	
	
	public void setExprKind(PhpExprKind newExprKind) {
		
		if (newExprKind == null) {
			
			exprKind = PhpExprKind.UNKOWN;
			
		} else {
			
			exprKind = newExprKind;
		}
		
	}
	
	public PhpExpr duplicatePhpExpr(PhpExpr expr) throws Exception {
		
		PhpExpr ret = new PhpExpr(expr);
		return ret;
		
	}
	
	private void readExprFromNode(Node exprNode) throws Exception{
		
		// Case String
		if (exprNode.getNodeName().equals("node:Scalar_String")) {
			
			setExprType(PhpExprType.STR);
			setExprKind(PhpExprKind.CONS);
			
			// Debug information
			if (JAnalyzer.DEBUG_MODE >= 10) {
				NodeList exprChildList = exprNode.getChildNodes();
				
				System.out.println("\n[DEBUG]node:Scalar_String");
				for (int i = 0; i < exprChildList.getLength(); i++) {
					System.out.println("[DEBUG]   " + exprChildList.item(i).getNodeName());
				}			
			}
			
			Node stringValueNode = DocUtils.getFirstChildWithName(
					DocUtils.getFirstChildWithName(exprNode, "subNode:value"), "scalar:string");
			
			if (JAnalyzer.DEBUG_MODE >= 10) {
				NodeList exprChildList = stringValueNode.getChildNodes();
				
				System.out.println("\n[DEBUG]subNode:value");
				for (int i = 0; i < exprChildList.getLength(); i++) {
					System.out.println("[DEBUG]   " + exprChildList.item(i).getNodeName());
				}			
			}
			
			String string = DocUtils.getStringFromNode(stringValueNode);
			setTop(string);
			
			
		}
		
		// Case LNumber. No, I don't know what it means either.
		// [FIXME] Only handling integer at the moment
		else if (exprNode.getNodeName().equals("node:Scalar_LNumber")) {
			
			setExprType(PhpExprType.INT);
			setExprKind(PhpExprKind.CONS);
			
			Node numValueNode = DocUtils.getFirstChildWithName(exprNode, "subNode:value");
			Node valueNode = DocUtils.getFirstChildWithName(numValueNode, "scalar:int");
			
			int num = DocUtils.getIntFromNode(valueNode);
			setTop(String.format("%d", num));
			
		}
		
		// Case Variable
		else if (exprNode.getNodeName().equals("node:Expr_Variable") || exprNode.getNodeName().equals("node:Expr_ArrayDimFetch")) {
			
			setExprType(PhpExprType.UNKOWN);
			setExprKind(PhpExprKind.VAR);
			
			// Debug information
			if (JAnalyzer.DEBUG_MODE >= 10) {
				NodeList exprChildList = exprNode.getChildNodes();
				
				System.out.println("\n[DEBUG]node:Expr_Variable");
				for (int i = 0; i < exprChildList.getLength(); i++) {
					System.out.println("[DEBUG]   " + exprChildList.item(i).getNodeName());
				}			
			}
			
			String varName = "";
			
			if (exprNode.getNodeName().equals("node:Expr_Variable")) {
				
				Node nameStringNode = DocUtils.getFirstChildWithName(
						DocUtils.getFirstChildWithName(exprNode, "subNode:name"), "scalar:string");
				
				// get the string under scalar:string
				varName = DocUtils.getStringFromNode(nameStringNode);
				
			} else if (exprNode.getNodeName().equals("node:Expr_ArrayDimFetch")) {
				
				Node exprVarNode = DocUtils.getFirstChildWithName(DocUtils.getFirstChildWithName(exprNode, "subNode:var"), "node:Expr_Variable");

				Node nameStringNode = DocUtils.getFirstChildWithName(
						DocUtils.getFirstChildWithName(exprVarNode, "subNode:name"), "scalar:string");
				
				// get the string under scalar:string
				varName = DocUtils.getStringFromNode(nameStringNode);
				
				Node exprDimNode = DocUtils.getFirstChildWithName(exprNode, "subNode:dim");

				nameStringNode = DocUtils.getFirstChildWithName(DocUtils.getFirstChildWithName(
						DocUtils.getFirstChildWithName(exprDimNode, "node:Scalar_String"), "subNode:value"), "scalar:string");
				
				varName = varName + "@" + DocUtils.getStringFromNode(nameStringNode);
				
			}
			
			setTop(varName);
			
			if (JAnalyzer.DEBUG_MODE >= 10 ) {
				System.out.print("For variable " + varName + ", parentStmt is " + parentStmt.startLine);
			}
			
			// When the variable is being assigned to a new value;
			if (assignedVar) {
				
				setPosition(parentStmt.startLine);
				if (JAnalyzer.DEBUG_MODE >= 10 ) {
					System.out.println(", it's assigned, and the position is " + getPosition());
				}
				
			}
			
			// When the variable is not being assigned to a new value, but used
			else {
				
				int assignedPos = parentStmt.getPosFromAM(varName);
				if ( assignedPos != 0 ) {
					
					setPosition(assignedPos);
					
				}
				
				else {
					
					setPosition(0);
					
				}
				
				if (top.contains("_POST")) {
					
					//System.out.println("making variable " + this.getString(true) + " tainted.");
					taintCondition = PhpExprUtils.mkTrue();
					
				}
				
				if (JAnalyzer.DEBUG_MODE >= 10 ) {
					System.out.println(", it's used, and the position is " + getPosition());
				}
				
			}
			
		} 
		
		// Case Binary Equation
		else if (exprNode.getNodeName().equals("node:Expr_BinaryOp_Equal")) {
			
			setTop("=");
			setExprType(PhpExprType.BOOL);
			setExprKind(PhpExprKind.COMP);
			
			Node exprLeftNode = DocUtils.getFirstChildWithName(exprNode, "subNode:left");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprLeftNode), parentStmt, assignedVar));
			
			Node exprRightNode = DocUtils.getFirstChildWithName(exprNode, "subNode:right");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprRightNode), parentStmt, assignedVar));
			
		}
		
		// Case Binary Smaller-Than
		else if (exprNode.getNodeName().equals("node:Expr_BinaryOp_Smaller")) {
					
			setTop("<");
			setExprType(PhpExprType.BOOL);
			setExprKind(PhpExprKind.COMP);
					
			Node exprLeftNode = DocUtils.getFirstChildWithName(exprNode, "subNode:left");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprLeftNode), parentStmt, assignedVar));
					
			Node exprRightNode = DocUtils.getFirstChildWithName(exprNode, "subNode:right");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprRightNode), parentStmt, assignedVar));
					
		}
		
		// Case Binary Greater-Than
		else if (exprNode.getNodeName().equals("node:Expr_BinaryOp_Greater")) {
			
			setTop(">");
			setExprType(PhpExprType.BOOL);
			setExprKind(PhpExprKind.COMP);
					
			Node exprLeftNode = DocUtils.getFirstChildWithName(exprNode, "subNode:left");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprLeftNode), parentStmt, assignedVar));
					
			Node exprRightNode = DocUtils.getFirstChildWithName(exprNode, "subNode:right");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprRightNode), parentStmt, assignedVar));
					
		}
		
		// Case boolean or
		else if (exprNode.getNodeName().equals("node:Expr_BinaryOp_BooleanOr")) {
			
			setTop("or");
			setExprType(PhpExprType.BOOL);
			setExprKind(PhpExprKind.COMP);
					
			Node exprLeftNode = DocUtils.getFirstChildWithName(exprNode, "subNode:left");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprLeftNode), parentStmt, assignedVar));
					
			Node exprRightNode = DocUtils.getFirstChildWithName(exprNode, "subNode:right");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprRightNode), parentStmt, assignedVar));
					
		}
		
		// Case boolean and
		else if (exprNode.getNodeName().equals("node:Expr_BinaryOp_BooleanAnd")) {

			setTop("and");
			setExprType(PhpExprType.BOOL);
			setExprKind(PhpExprKind.COMP);

			Node exprLeftNode = DocUtils.getFirstChildWithName(exprNode, "subNode:left");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprLeftNode), parentStmt, assignedVar));

			Node exprRightNode = DocUtils.getFirstChildWithName(exprNode, "subNode:right");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprRightNode), parentStmt, assignedVar));

		}
		
		// Case boolean se
		else if (exprNode.getNodeName().equals("node:Expr_BinaryOp_SmallerOrEqual")) {

			setTop("<=");
			setExprType(PhpExprType.BOOL);
			setExprKind(PhpExprKind.COMP);

			Node exprLeftNode = DocUtils.getFirstChildWithName(exprNode, "subNode:left");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprLeftNode), parentStmt, assignedVar));

			Node exprRightNode = DocUtils.getFirstChildWithName(exprNode, "subNode:right");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprRightNode), parentStmt, assignedVar));

		}
		
		// Case boolean ge
		else if (exprNode.getNodeName().equals("node:Expr_BinaryOp_GreaterOrEqual")) {

			setTop(">=");
			setExprType(PhpExprType.BOOL);
			setExprKind(PhpExprKind.COMP);

			Node exprLeftNode = DocUtils.getFirstChildWithName(exprNode, "subNode:left");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprLeftNode), parentStmt, assignedVar));

			Node exprRightNode = DocUtils.getFirstChildWithName(exprNode, "subNode:right");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprRightNode), parentStmt, assignedVar));

		}
		
		// Case boolean plus
		else if (exprNode.getNodeName().equals("node:Expr_BinaryOp_Plus")) {

			setTop("+");
			setExprType(PhpExprType.INT);
			setExprKind(PhpExprKind.COMP);

			Node exprLeftNode = DocUtils.getFirstChildWithName(exprNode, "subNode:left");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprLeftNode), parentStmt, assignedVar));

			Node exprRightNode = DocUtils.getFirstChildWithName(exprNode, "subNode:right");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprRightNode), parentStmt, assignedVar));

		}

		// Case boolean minus
		else if (exprNode.getNodeName().equals("node:Expr_BinaryOp_Minus")) {

			setTop("-");
			setExprType(PhpExprType.INT);
			setExprKind(PhpExprKind.COMP);

			Node exprLeftNode = DocUtils.getFirstChildWithName(exprNode, "subNode:left");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprLeftNode), parentStmt, assignedVar));

			Node exprRightNode = DocUtils.getFirstChildWithName(exprNode, "subNode:right");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprRightNode), parentStmt, assignedVar));

		}

		// Case boolean multiply
		else if (exprNode.getNodeName().equals("node:Expr_BinaryOp_Mul")) {

			setTop("*");
			setExprType(PhpExprType.INT);
			setExprKind(PhpExprKind.COMP);

			Node exprLeftNode = DocUtils.getFirstChildWithName(exprNode, "subNode:left");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprLeftNode), parentStmt, assignedVar));

			Node exprRightNode = DocUtils.getFirstChildWithName(exprNode, "subNode:right");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprRightNode), parentStmt, assignedVar));

		}

		// Case boolean multiply
		else if (exprNode.getNodeName().equals("node:Expr_BinaryOp_Div")) {

			setTop("/");
			setExprType(PhpExprType.INT);
			setExprKind(PhpExprKind.COMP);

			Node exprLeftNode = DocUtils.getFirstChildWithName(exprNode, "subNode:left");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprLeftNode), parentStmt, assignedVar));

			Node exprRightNode = DocUtils.getFirstChildWithName(exprNode, "subNode:right");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprRightNode), parentStmt, assignedVar));

		}
		
		/*
		// Case array_dim
		else if (exprNode.getNodeName().equals("node:Expr_ArrayDimFetch")) {
			
			// A@B get the item B in array A
			setTop("@");
			setExprType(PhpExprType.UNKOWN);
			setExprKind(PhpExprKind.VAR);
			
			Node exprVarNode = DocUtils.getFirstChildWithName(exprNode, "subNode:var");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprVarNode), parentStmt, assignedVar));
			
			Node exprDimNode = DocUtils.getFirstChildWithName(exprNode, "subNode:dim");
			subExprs.add(new PhpExpr(DocUtils.getFirstExprChild(exprDimNode), parentStmt, assignedVar));	
			
		}
		*/
		
		// Case Function-Call
		// FIXME
		else if (exprNode.getNodeName().equals("node:Expr_FuncCall")) {
			
			setExprType(PhpExprType.UNKOWN);
			setExprKind(PhpExprKind.FUN);
			
			// get the function name
			// [NOTE] For some reason, the name of the function is an array, I'm just looking at the first of the array for the moment
			Node exprNamesArrayNode = DocUtils.getFirstChildWithName(
					DocUtils.getFirstChildWithName(
					DocUtils.getFirstChildWithName(
					DocUtils.getFirstChildWithName(exprNode, "subNode:name"), 
					"node:Name"), "subNode:parts"), "scalar:array");
			
			Node exprNameStrNode =  DocUtils.getFirstChildWithName(exprNamesArrayNode, "scalar:string");		
			setTop(DocUtils.getStringFromNode(exprNameStrNode));
			
			
			// get the list of arguments
			Node exprArgsArrayNode = DocUtils.getFirstChildWithName(
					DocUtils.getFirstChildWithName(exprNode, "subNode:args"), 
					"scalar:array");
			
			List<Node> argsNodeArray = DocUtils.getListofChildrenWithName(exprArgsArrayNode, "node:Arg");
			for (int i = 0; i < argsNodeArray.size(); i++) {
				
				Node argNode = DocUtils.getFirstChildWithName(argsNodeArray.get(i),"subNode:value");
				
				//[ASSUMPTION] subNode:value only have one child
				Node argExprNode = DocUtils.getFirstExprChild(argNode);
				subExprs.add(new PhpExpr(argExprNode, parentStmt, false));
				
			}
			
			FunctionSig funSig = JAnalyzer.funlib.getFunctionSig(top);
			
			if (funSig != null) {
				setExprType(funSig.returnType);
				setTop(funSig.smtName);
			}
			
			parentStmt.phpFile.shadowVars.add(PhpExprUtils.funToVar(this));
			parentStmt.funCalls.add(this);
			
		}
		
		// Unhandled Expressions
		else {
			
			System.out.println("\nUnhandled node: " + exprNode.getNodeName());
			
		}
		
		
	}
	
	public List<PhpExpr> getVarsFromExpr() {
		
		List<PhpExpr> varList = new ArrayList<PhpExpr>();
		
		
		// Add nothing if the expression is a constant
		if (exprKind == PhpExprKind.CONS) {
			
		}
		
		// Add the variable if the expression is a variable
		else if (exprKind == PhpExprKind.VAR) {
			
			varList.add(this);
		}
		
		// Add the variable if the expression is composite
		else if (exprKind == PhpExprKind.COMP) {
			
			for (int i = 0; i < subExprs.size(); i++) {
				
				varList.addAll(subExprs.get(i).getVarsFromExpr());
				
			}
			
		}
		
		return varList;
		
	}
	
	// get the position from a variable
	public int getPositionFromVar(String var) {
		
		String[] elements = var.split("\\*");
		int useVarPosition = Integer.parseInt(elements[1]);
		
		return useVarPosition;
		
	}
}
