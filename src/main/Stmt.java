package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import at.ac.tuwien.infosys.www.phpparser.ParseTree;
import utils.*;

public class Stmt {
	
	private List<PhpExpr> exprs;
	private List<Stmt> body1;
	private List<Stmt> body2;
	
	public StmtType stmtType;
	public PhpFile phpFile;
	
	// parent statement and the immediate previous and next sibling
	public Stmt parentStmt;
	public Stmt preStmt;
	public Stmt nextStmt;
	
	public int startLine;
	public int endLine;
	
	// an assignment map that associate the variables to the positions where it's last defined
	public Map<PhpExpr, Integer> assignMap;
	public Map<String, PhpExprType> typeMap;
	private Map<PhpExpr, PhpExpr> taintVector;
	
	public Set<PhpExpr> funCalls;
	
	public List<Integer> sliceTags;
	
	// constructor when reading a statement from a xml-node
	public Stmt(Node stmtNode, PhpFile file, Stmt newParentStmt, Stmt newPreStmt, ParseTree pTree) throws Exception {
		
		exprs = new ArrayList<PhpExpr>();
		body1 = new ArrayList<Stmt>();
		body1 = new ArrayList<Stmt>();
		
		stmtType = StmtType.SKIP;
		phpFile = file;
		
		startLine = -1;
		endLine = -1;
		
		preStmt = newPreStmt;
		parentStmt = newParentStmt;
		
		assignMap = new HashMap<PhpExpr, Integer>();
		
		// [FIXME] consider more about the scope of the variables
		typeMap = new HashMap<String, PhpExprType>();
		taintVector = new HashMap<PhpExpr, PhpExpr>();
		funCalls = new HashSet<PhpExpr>();
		
		sliceTags = new ArrayList<Integer>();
		readStmtFromNode(stmtNode, pTree);
		
	}
	
	public List<PhpExpr> getExprs() {
		
		return exprs;
		
	}
	
	public void setExprs(List<PhpExpr> newExprs) throws Exception {
		
		if (newExprs == null)
			throw new Exception("Can't set expression to null.");
		
		exprs = newExprs;
		return;
	}
	
	public List<Stmt> getBody1() {
		
		return body1;
		
	}
	
	public void setBody1(List<Stmt> newBody) throws Exception {
		
		if (newBody == null)
			throw new Exception("Can't set body to null.");
		
		body1 = newBody;
		
		for (int i = 0; i < newBody.size(); i++) {
			
			newBody.get(i).parentStmt = this;
			
			if (i == 0) {
				
				newBody.get(i).preStmt = this.preStmt;
				
			} else if (i > 0) {
				
				newBody.get(i).preStmt = newBody.get(i - 1);
				
				if (preStmt != null) {
					
					preStmt.nextStmt = newBody.get(i);
					
				}
				
			}
			
		}
		
	}
	
	public List<Stmt> getBody2() {
		
		return body2;
		
	}
	
	public void setBody2(List<Stmt> newBody) throws Exception {
		
		if (newBody == null)
			throw new Exception("Can't set body to null.");
		
		body2 = newBody;
		
		for (int i = 0; i < newBody.size(); i++) {
			
			newBody.get(i).parentStmt = this;
			
			if (i == 0) {
				
				newBody.get(i).preStmt = this.preStmt;
				
			} else if (i > 0) {
				
				newBody.get(i).preStmt = newBody.get(i - 1);
				
				if (preStmt != null) {
					
					preStmt.nextStmt = newBody.get(i);
					
				}
				
			}
			
		}
		
	}
	
	public void printStmt() throws Exception {
		
		System.out.print(startLine + ":  ");
		switch (stmtType)
		{
		case SKIP:
			System.out.println();
			break;
			
		case ECHO:
			System.out.print("echo ");
			for (PhpExpr expr : getExprs()) {
				System.out.print(expr.getString(true) + ", ");
			}
			System.out.println();
			break;
			
		case ASSIGN:
			System.out.println(exprs.get(0).getString(true) + ":= " + exprs.get(1).getString(true) + ";");
			break;
			
		case FUNC:
			System.out.println("FUNC(" + exprs.get(0).getString(true) + ");");
			break;
			
		case ITE:
			
			System.out.println("if (" + exprs.get(0).getString(true) + ") {");
			System.out.println();
			
			for (int i = 0; i < body1.size(); i++){
				body1.get(i).printStmt();
			}		
			
			System.out.println("} else {");

			for (int i = 0; i < body2.size(); i++){
				body2.get(i).printStmt();
			}
			System.out.println("}");

			break;
			
		case WHILE:
			System.out.println("while (" + exprs.get(0).getString(true) + ") {");
			System.out.println();
			
			for (int i = 0; i < body1.size(); i++){
				body1.get(i).printStmt();
			}
			System.out.println("}");
			break;
		
		default:
		}
		
		// print out the path condition after each statement
		//System.out.println("Path Condition:");
		//System.out.println(getPathCondition().getString(true));
		
		// print out the type map
		//System.out.println("///////////Type Map:");
		//this.printTypeMap();
		//System.out.println("///////////\n");
		
		// print out the taintVector after each statement
		//System.out.println("///////////\nTaint Vector:");
		//this.printTaintVector();
		//System.out.println("///////////\n");
		
		// print out the closest assignments
		/*
		System.out.print("For line" + startLine);
		
		if (preStmt == null) {
			
			System.out.println(" preStmt is null");
			
		}
		
		else {
			
			System.out.println(" preStmt is line" + preStmt.startLine);
			
		}
		
		printAssignmentMap();
		System.out.println();
		*/
		
	}
	
	public void printAssignmentMap() {
		
		Iterator<Entry<PhpExpr, Integer>> itr = assignMap.entrySet().iterator();
		while (itr.hasNext()) {
			
			Entry<PhpExpr, Integer> entry = itr.next();
			System.out.println("(" + entry.getKey().getString(false) + ": \t" + (entry.getKey().getString(false).length() < 15 ? "\t" : "") + entry.getValue() + ")");
		
		}
		
		System.out.println();
		
	}
	
	public void printTypeMap() {
		
		for (String var : typeMap.keySet()) {
			
			System.out.println("(" + var + ": \t" + typeMap.get(var) + ")");
			
		}
		
		System.out.println();
		
	}
	
	public void printTaintVector() {
		
		Iterator<Entry<PhpExpr, PhpExpr>> itr = taintVector.entrySet().iterator();
		while (itr.hasNext()) {
			
			Entry<PhpExpr, PhpExpr> entry = itr.next();
			System.out.println("(" + entry.getKey().getString(true) + ": \t" + (entry.getKey().getString(true).length() < 15 ? "\t" : "") + (entry.getValue() == null ? " false" : entry.getValue().getString(true)) + ")");
			
		}
		
	}
	
	/**
	 * Read an statement from an statement node.
	 * Function declaration is not handled in this function
	 * 
	 * @param stmtNode The node containing the statement information
	 * @throws Exception 
	 */
	private void readStmtFromNode (Node stmtNode, ParseTree pTree) throws Exception {
		
		// Skipping lines
		if (stmtNode.getNodeName().equals("#text")) {
			
			stmtType = StmtType.SKIP;
			return;
		}
		
		Node slNode = DocUtils.getFirstChildWithName(
				DocUtils.getFirstChildWithName(stmtNode, "attribute:startLine"), "scalar:int");
		Node elNode = DocUtils.getFirstChildWithName(
				DocUtils.getFirstChildWithName(stmtNode, "attribute:endLine"), "scalar:int");
		
		startLine = DocUtils.getIntFromNode(slNode);
		endLine = DocUtils.getIntFromNode(elNode);
		
		if (phpFile.inSinkLines(startLine)) {
			phpFile.addSinkStmt(this);
		}
		
		Stmt preStmtInterLayer = phpFile.getPreStmtInterLayer(this);
		
		if (preStmtInterLayer != null) {
			
			GeneralUtils.updateEIHashMap(preStmtInterLayer.assignMap, assignMap);
			GeneralUtils.updateEEHashMap(preStmtInterLayer.taintVector, taintVector);
			GeneralUtils.updateSTHashMap(preStmtInterLayer.typeMap, typeMap);
			
		}
		

		
		// echo statement
		if (stmtNode.getNodeName().equals("node:Stmt_Echo")) {
			
			stmtType = StmtType.ECHO;
			
			Node exprsNode = DocUtils.getFirstChildWithName(
					DocUtils.getFirstChildWithName(stmtNode, "subNode:exprs"),
					"scalar:array");
			
			List<PhpExpr> exprList = readExprsFromArray(exprsNode, this);
			setExprs(exprList);
			
		}
		
		// in case of assignment
		else if (stmtNode.getNodeName().equals("node:Expr_Assign")) {
			
			Node varNode = DocUtils.getFirstChildWithName(
					DocUtils.getFirstChildWithName(stmtNode, "subNode:var"),
					"node:Expr_Variable");
			
			PhpExpr targetVar = new PhpExpr(varNode, this, true);
			
			if (targetVar.getExprKind() != PhpExprKind.VAR) {
				
				throw new Exception("The LHS of an assignment must be a variable.");
				
			}
			
			Node exprNode = DocUtils.getFirstExprChild(
					DocUtils.getFirstChildWithName(stmtNode, "subNode:expr"));
			
			PhpExpr targetExpr = new PhpExpr(exprNode, this, false);
			targetVar.setExprType(targetExpr.getExprType());
		
			// Return an statement with type ASSIGN and two Exprs
			List<PhpExpr> exprList = new ArrayList<PhpExpr>();
			exprList.add(targetVar);
			exprList.add(targetExpr);
			
			stmtType = StmtType.ASSIGN;
			setExprs(exprList);

			// setup the assignment map
			for (Entry<PhpExpr, Integer> entry : assignMap.entrySet()) {
				if (entry.getKey().getString(false).compareTo(targetVar.getString(false)) == 0) {
					assignMap.remove(entry.getKey());
					break;
				}
			}
			
			assignMap.put(targetVar, startLine);
			
			// setup the type map
			if (targetExpr.getExprType() != PhpExprType.UNKOWN) {
				typeMap.put(targetVar.getString(false), targetExpr.getExprType());
			}
			
			/*
			// taint variable list
			for (PhpExpr var : this.getUseVars()) {
			
				if (var.getTaintCondition() != null) {
					PhpExpr tc = var.getTaintCondition();
					// No idea why "put" could change the type of targetVar 
					//taintVector.put(var, tc);
				}
				
			}
			*/
			
			targetVar.setTaintCondition(targetExpr.getTaintCondition());
			putTC(targetVar, targetVar.getTaintCondition());
			
			// add taint sink
			if (targetVar.getString(false).contains("database")) {
				
				phpFile.addSinkStmt(this);
				
			}
			
		}
		
		// in case of function
		// [FIXME]
		else if (stmtNode.getNodeName().equals("node:Stmt_Function")) {
			
			stmtType = StmtType.SKIP;
			
			Node functionNameStringNode = DocUtils.getFirstChildWithName(
					DocUtils.getFirstChildWithName(stmtNode, "subNode:name"), "scalar:string");
			String funName = DocUtils.getStringFromNode(functionNameStringNode);
			phpFile.functionNodesMap.put(funName, stmtNode);
					
		} 
		
		// in case of ITE
		else if (stmtNode.getNodeName().equals("node:Stmt_If")) {
			
			// debug information
			if (JAnalyzer.DEBUG_MODE >= 10) {
				NodeList printList = stmtNode.getChildNodes();
				System.out.println("\n[DEBUG]Stmt_If");
				for (int i = 0; i < printList.getLength(); i++) {
					System.out.println("[DEBUG]   " + printList.item(i).getNodeName());
				}			
			}
			
			// get "if" condition 
			Node ifConditionNode = DocUtils.getFirstChildWithName(stmtNode, "subNode:cond");
			Node ifConditionExprNode = DocUtils.getFirstExprChild(ifConditionNode);
			
			PhpExpr ifConditionExpr = new PhpExpr(ifConditionExprNode, this, false);
			List<PhpExpr> ifConditionExprList = new ArrayList<PhpExpr>();
			ifConditionExprList.add(ifConditionExpr);
			
			// get "then" statements
			Node ifThenNode = DocUtils.getFirstChildWithName(stmtNode, "subNode:stmts");
			Node ifThenStmtsNode = DocUtils.getFirstChildWithName(ifThenNode, "scalar:array");
			List<Stmt> ifThenStmts = readStmtsFromArray(ifThenStmtsNode, phpFile, this, pTree);
			
			// get "elseif" list
			Node ifElseIfNode = DocUtils.getFirstChildWithName(stmtNode, "subNode:elseifs");
			Node ifElseIfStmtsListNode = DocUtils.getFirstChildWithName(ifElseIfNode, "scalar:array");
			
			if (ifElseIfStmtsListNode.getChildNodes().getLength() > 0) {
			
				System.out.println("[ERROR] Unhandled case: non-empty subNode:elseifs");
				
			}
			
			// get "else" statements
			List<Stmt> ifElseStmts = new ArrayList<Stmt>();
			Node elseSubNode = DocUtils.getFirstChildWithName(stmtNode, "subNode:else");
			Node firstChildNode = elseSubNode.getFirstChild();
			while (!(firstChildNode instanceof Element) && firstChildNode != null) {
				firstChildNode = firstChildNode.getNextSibling();
			}
			
			if (firstChildNode.getNodeName().compareTo("node:Stmt_Else") == 0) {
				Node ifElseNode = DocUtils.getFirstChildWithName(elseSubNode, "node:Stmt_Else");
				
				Node ifElseStmtsNode = DocUtils.getFirstChildWithName(ifElseNode, "subNode:stmts");
				Node ifElseStmtsListNode = DocUtils.getFirstChildWithName(ifElseStmtsNode, "scalar:array");
				
				ifElseStmts = readStmtsFromArray(ifElseStmtsListNode, phpFile, this, pTree);
			}
			
			// load into the object
			stmtType = StmtType.ITE;
			setExprs(ifConditionExprList);
			setBody1(ifThenStmts);
			setBody2(ifElseStmts);
			
			// create new variables to store the new values for each variable re-assigned in the ITE, the new variable will be evaluated in "toFormula"
			Set<PhpExpr> defVars = new HashSet<PhpExpr>();
			
			for (int i = 0; i < body1.size(); i++) {
				
				defVars.addAll(body1.get(i).getDefVarsRecur());
				
			}
			
			for (int i = 0; i < body2.size(); i++) {
				
				defVars.addAll(body2.get(i).getDefVarsRecur());
				
			}
			
			// add the shadow variables and their taint conditions
			
			// find the last "if" taint vector and the last "else" taint vector before this ITE
			Stmt lastPreStmt = phpFile.getPreStmtInterLayer(this);
			Stmt lastThenStmt = body1.get(body1.size() - 1);
			Stmt lastElseStmt = body2.size() == 0 ? lastPreStmt : body2.get(body2.size() - 1);
			
			GeneralUtils.updateSTHashMap(lastThenStmt.typeMap, typeMap);
			GeneralUtils.updateSTHashMap(lastElseStmt.typeMap, typeMap);
			
			for (PhpExpr defVar : defVars) {

				for (Entry<PhpExpr, Integer> entry : assignMap.entrySet()) {
					if (entry.getKey().getString(false).compareTo(defVar.getString(false)) == 0) {
						assignMap.remove(entry.getKey());
						break;
					}
				}

				PhpExpr shadowVar = PhpExprUtils.mkVar(defVar.getTop(), (-1) * startLine, defVar.getExprType());
				phpFile.shadowVars.add(shadowVar);
				assignMap.put(shadowVar, (-1) * startLine);
				
				// adding taint condition
				PhpExpr guard = ifConditionExpr;
				
				PhpExpr thenTC = lastThenStmt.getTC(shadowVar, true);
				List<PhpExpr> list = new ArrayList<PhpExpr>();
				list.add(guard);
				list.add(thenTC);
				PhpExpr thenBranch = PhpExprUtils.mkAnd(list);
				
				PhpExpr elseTC = lastElseStmt.getTC(shadowVar, true);
				list = new ArrayList<PhpExpr>();
				list.add(PhpExprUtils.mkNot(guard));
				list.add(elseTC);
				PhpExpr elseBranch = PhpExprUtils.mkAnd(list);
				
				list = new ArrayList<PhpExpr>();
				list.add(thenBranch);
				list.add(elseBranch);
				PhpExpr tc = PhpExprUtils.mkOr(list);
				
				putTC(shadowVar, tc);

			}
		}
		
		// in case of while loop
		else if (stmtNode.getNodeName().equals("node:Stmt_While")) {
			
			// get "if" condition 
			Node whileConditionNode = DocUtils.getFirstChildWithName(stmtNode, "subNode:cond");
			Node whileConditionExprNode = DocUtils.getFirstExprChild(whileConditionNode);
			
			PhpExpr whileConditionExpr = new PhpExpr(whileConditionExprNode, this, false);
			List<PhpExpr> whileConditionExprList = new ArrayList<PhpExpr>();
			whileConditionExprList.add(whileConditionExpr);
			
			// get "then" statements
			Node bodyNode = DocUtils.getFirstChildWithName(stmtNode, "subNode:stmts");
			Node bodyStmtsNode = DocUtils.getFirstChildWithName(bodyNode, "scalar:array");
			List<Stmt> bodyStmts = readStmtsFromArray(bodyStmtsNode, phpFile, this, pTree);
		
			// load into the object
			stmtType = StmtType.WHILE;
			setExprs(whileConditionExprList);
			setBody1(bodyStmts);
			
			
			Stmt lastBodyStmt = body1.get(body1.size() - 1);
			
			// create new variables to store the new values for each variable re-assigned in the while loop, the new variable will be evaluated in "toFormula"
			Set<PhpExpr> defVars = new HashSet<PhpExpr>();
			Set<String> shadowVarStrings = new HashSet<String>();
			
			for (int i = 0; i < body1.size(); i++) {
				
				defVars.addAll(body1.get(i).getDefVarsRecur());
				
			}
			
			processPostShadowVar:
			for (PhpExpr defVar : defVars) {
				
				for (Entry<PhpExpr, Integer> entry : assignMap.entrySet()) {
					if (entry.getKey().getString(false).compareTo(defVar.getString(false)) == 0) {
						assignMap.remove(entry.getKey());
						break;
					}
				}
				
				// check if the shadow variable is already processed
				for (String str : shadowVarStrings) {
					if (defVar.getString(false).compareTo(str) == 0) continue processPostShadowVar;
				}
				
				shadowVarStrings.add(defVar.getString(false));
				
				// add the shadow variables for post-loop
				PhpExpr shadowVar = PhpExprUtils.mkVar(defVar.getTop(), (-1) * startLine, defVar.getExprType());
				phpFile.shadowVars.add(shadowVar);
				assignMap.put(shadowVar, (-1) * startLine);
				
				// add taint condition
				PhpExpr guard = whileConditionExpr;
				
				PhpExpr bodyTC = lastBodyStmt.getTC(shadowVar, true);
				List<PhpExpr> enterList = new ArrayList<PhpExpr>();
				enterList.add(guard);
				enterList.add(bodyTC);
				PhpExpr enterFormula = PhpExprUtils.mkAnd(enterList);
				
				PhpExpr preTC = preStmtInterLayer.getTC(shadowVar, true);
				List<PhpExpr> skipList = new ArrayList<PhpExpr>();
				skipList.add(PhpExprUtils.mkNot(guard));
				skipList.add(preTC);
				PhpExpr skipFormula = PhpExprUtils.mkAnd(skipList);
				
				List<PhpExpr> combineList = new ArrayList<PhpExpr>();
				combineList.add(enterFormula);
				combineList.add(skipFormula);
				
				
				PhpExpr tc = PhpExprUtils.mkOr(combineList);
				
				putTC(shadowVar, tc);
			}
			
			
			// process pre shadow variables
			// re-direct all the variables in the loop to refer to a new variable instead of the ones outside the loop
			Set<PhpExpr> useVars = new HashSet<PhpExpr>();
			
			for (int i = 0; i < body1.size(); i++) {
				
				useVars.addAll(body1.get(i).getUseVarsRecur());
				
			}
			
			for (PhpExpr var : useVars) {
				if (var.getPosition() >= 0 && var.getPosition() < startLine) {
					var.setPosition(startLine);
				}
			}
			
		}
		
		// skipping statements
		else {
			stmtType = StmtType.SKIP;
		}

	}
	
	
	/**
	 * Given an array node of statements, parse it into a list of statements
	 * @param array An array node of statements 
	 * @return
	 * @throws Exception
	 */
	public static List<Stmt> readStmtsFromArray (Node array, PhpFile file, Stmt parent, ParseTree pTree) throws Exception {
		
		if (!array.getNodeName().equals("scalar:array")) {
			
			throw new Exception("ReadStmtsFromArray expcets scalar:array, but getting " + array.getNodeName());
			
		} else {
			
			List <Stmt> retStmts = new ArrayList<Stmt>();
			
			NodeList childList = array.getChildNodes();
			
			// debug information
			if (JAnalyzer.DEBUG_MODE >= 10) {
				
				System.out.println("\n[DEBUG]scalar:array");
				for (int i = 0; i < childList.getLength(); i++) {
					System.out.println("[DEBUG]   " + childList.item(i).getNodeName());
				}
				System.out.println("");
			}
			
			for (int i = 0; i < childList.getLength(); i++) {
				
				// skipping "#text"
				
				if (childList.item(i).getNodeName().equals("#text")) {
					
					continue;
					
				}
				
				Stmt preStmt = null;
				
				if (retStmts.size() >= 1) {
					
					preStmt = retStmts.get(retStmts.size() - 1);
					
				}
				
				Stmt subStmt = new Stmt(childList.item(i), file, parent, preStmt, pTree);
				
				if (retStmts.size() >= 1) {
					
					retStmts.get(retStmts.size() - 1).nextStmt = subStmt;
					
				}
				
				if ( subStmt != null) {
					
					retStmts.add(subStmt);
					
				}
			}
			
			return retStmts;
			
		}
	}
	
	public List<Stmt> instantiateFunStmtFromNode(Node funNode, Stmt callStmt) throws Exception {
		
		Node stmtsArrayNode = DocUtils.getFirstChildWithName(
				DocUtils.getFirstChildWithName(funNode, "subNode:stmts"), "scalar:array");
		
		// [FIXME] Parent is the callStmt? Should it be the function declaration? Or parent of callStmt?
		List<Stmt> retList = readStmtsFromArray(stmtsArrayNode, callStmt.phpFile, callStmt, null);
		return retList;
		
	}
	
	
	public static List<PhpExpr> readExprsFromArray (Node array, Stmt parent) throws Exception {
		
		if (!array.getNodeName().equals("scalar:array")) {
			
			throw new Exception("readExprsFromArray expcets scalar:array, but getting " + array.getNodeName());
			
		} else {
			
			List <PhpExpr> retExprs = new ArrayList<PhpExpr>();
			
			NodeList childList = array.getChildNodes();

			for (int i = 0; i < childList.getLength(); i++) {
				
				// Skipping "#text"
				
				if (childList.item(i).getNodeName().equals("#text")) {
					
					continue;
					
				}
				
				PhpExpr subExpr = new PhpExpr(childList.item(i), parent, false);
				retExprs.add(subExpr);
				
			}
			
			return retExprs;
			
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////
	//
	// Flow Analysis
	//
	
	/**
	 * get the variables defined in this statement, not including the ones defined in sub statements.
	 * @return a list of variables defined in this statement
	 */
	public List<PhpExpr> getDefVars() {
		
		List<PhpExpr> retVars = new ArrayList<PhpExpr>();
		
		// If it's an assignment, add every variable on the left side of the equation
		if (stmtType == StmtType.ASSIGN) {
			
			retVars.addAll(exprs.get(0).getVarsFromExpr());
			
		}
		
		// Else return empty list
		else {
			
			// Add nothing
			
		}
		
		return retVars;
		
	}
	
	/**
	 * get the variables defined in this statement recursively, including the ones defined in sub statements.
	 * @return a list of variables defined in this statement
	 */
	public List<PhpExpr> getDefVarsRecur() {
		
		List<PhpExpr> retVars = new ArrayList<PhpExpr>();
		
		// If it's an assignment, add every variable on the left side of the equation
		if (stmtType == StmtType.ASSIGN) {
			
			retVars = exprs.get(0).getVarsFromExpr();
			
		}
		
		else if (stmtType == StmtType.ITE) {
			
			for (int i = 0; i < body1.size(); i++) {
				
				retVars.addAll(body1.get(i).getDefVarsRecur());
				
			}
			
			for (int i = 0; i < body2.size(); i++) {
				
				retVars.addAll(body2.get(i).getDefVarsRecur());
				
			}
			
			
		}
		
		else if (stmtType == StmtType.WHILE) {
			
			for (int i = 0; i < body1.size(); i++) {
				
				retVars.addAll(body1.get(i).getDefVarsRecur());
				
			}
			
		}
		
		// Else return empty list
		else {
			
			// Add nothing
			
		}
		
		return retVars;
		
	}
	
	/**
	 * get the variables used in this statement, without including sub statements
	 * @return a list of variables used in this statement
	 */
	// FIXME: What happens if variables are defined in statements inside ITE or WHILE
	public List<PhpExpr> getUseVars() {
		
		List<PhpExpr> retVars = new ArrayList<PhpExpr>();
		
		// if it's an echo, add every variable in the sub expressions
		if (stmtType == StmtType.ECHO) {
			
			for (PhpExpr expr : this.getExprs()) {
				retVars.addAll(expr.getVarsFromExpr());
			}
			
		}
		
		// if it's an assignment, add every variable on the right side of the equation
		else if (stmtType == StmtType.ASSIGN) {
			
			retVars.addAll(exprs.get(1).getVarsFromExpr());
			
		}
		
		// if it's an function, add every variable used as parameters
		else if (stmtType == StmtType.FUNC) {
			
			for (int i = 0; i < exprs.size(); i++) {
				
				retVars.addAll(exprs.get(i).getVarsFromExpr());
				
			}
				
		}
		
		// if it's an ITE, add every variable used in the branching condition
		else if (stmtType == StmtType.ITE) {
			
			retVars.addAll(exprs.get(0).getVarsFromExpr());
			
		}
		
		// if it's an ITE, add every variable used in the looping condition
		else if (stmtType == StmtType.WHILE) {
			
			retVars.addAll(exprs.get(0).getVarsFromExpr());
			
		}
		
		// else return empty list
		else {
			
			// Add nothing
			
		}
		
		return retVars;
		
	}
	
	/**
	 * get the variables used in this statement, including sub statements
	 * @return a list of variables used in this statement
	 */
	// FIXME: What happens if variables are defined in statements inside ITE or WHILE
	public List<PhpExpr> getUseVarsRecur() {
		
		List<PhpExpr> retVars = new ArrayList<PhpExpr>();
		
		// If it's an assignment, add every variable on the right side of the equation
		if (stmtType == StmtType.ASSIGN) {
			
			retVars.addAll(exprs.get(1).getVarsFromExpr());
			
		}
		
		// If it's an function, add every variable used as parameters
		else if (stmtType == StmtType.FUNC) {
			
			for (int i = 0; i < exprs.size(); i++) {
				
				retVars.addAll(exprs.get(i).getVarsFromExpr());
				
			}
				
		}
		
		// If it's an ITE, add every variable used in the branching condition
		else if (stmtType == StmtType.ITE) {
			
			retVars.addAll(exprs.get(0).getVarsFromExpr());
			
			// Add every variable inside "then"
			for (int i = 0; i < body1.size(); i++) {
				
				retVars.addAll(body1.get(i).getUseVarsRecur());
				
			}
			
			// Add everything inside "else"
			for (int i = 0; i < body2.size(); i++) {
				
				retVars.addAll(body2.get(i).getUseVarsRecur());
				
			}
			
		}
		
		// If it's an ITE, add every variable used in the looping condition
		else if (stmtType == StmtType.WHILE) {
			
			retVars.addAll(exprs.get(0).getVarsFromExpr());
			
			// Add every variable inside the loop
			for (int i = 0; i < body1.size(); i++) {
				
				retVars.addAll(body1.get(i).getUseVarsRecur());
				
			}
			
		}
		
		// Else return empty list
		else {
			
			// Add nothing
			
		}
		
		return retVars;
		
	}
	
	
	public List<Stmt> getAllAssignStmt() {
		
		List<Stmt> assignStmt = new ArrayList<Stmt>();
		
		// If this is a assignment statement;
		if (stmtType == StmtType.ASSIGN) {
			
			assignStmt.add(this);
			
		} 
		
		else if (stmtType == StmtType.ITE) {
			
			for (int i = 0; i < body1.size(); i++) {
				
				assignStmt.addAll(body1.get(i).getAllAssignStmt());
				
			}
			
			for (int i = 0; i < body2.size(); i++) {
				
				assignStmt.addAll(body2.get(i).getAllAssignStmt());
				
			}
			
			
		}
		
		else if (stmtType == StmtType.WHILE) {
			
			for (int i = 0; i < body1.size(); i++) {
				
				assignStmt.addAll(body1.get(i).getAllAssignStmt());
				
			}
			
		}
		
		return assignStmt;
		
	}
	
	/**
	 * Generate a formula from this statement
	 * @return the formula of this statement
	 */
	public String toFormula(String prefix) {
		String retString = "";
		
		if (stmtType == StmtType.SKIP) {
			
			retString =  prefix + " true ";
			
		}
		
		
		// in case of echo
		else if (stmtType == StmtType.ECHO) {
			
			retString =  prefix + " true ";
			
		}

		// in case of assignment
		else if (stmtType == StmtType.ASSIGN) {
			
			retString = prefix + "(= " + exprs.get(0).getString(true) + "  " + exprs.get(1).getString(true) + " )\n";
			
		}
		
		// in case of function
		// [FIXME] Skipping function for the moment
		else if (stmtType == StmtType.FUNC) {
			
			retString = prefix + " true ";
			
		}
		
		// In case of ITE
		else if (stmtType == StmtType.ITE) {
			
			// Prepare for the shadow variables
			
			// Find all the variables defined in the branches recursively
			Set<PhpExpr> thenDefVars = new HashSet<PhpExpr>();
			
			for (Stmt stmt : body1) {
				
				List<PhpExpr> defVars = stmt.getDefVarsRecur();
				thenDefVars.addAll(defVars);
				
			}
			
			/*
			Set<String> thenDefVarStrings = new HashSet<String>();
			Iterator<PhpExpr> thenDefVarIter = thenDefVars.iterator();
			
			while (thenDefVarIter.hasNext()) {
				
				PhpExpr var = thenDefVarIter.next();
				if (!thenDefVarStrings.contains(var.getString(false))) {
					
					thenDefVarStrings.add(var.getString(false));
					
				} else {
					
					thenDefVarIter.remove();
					
				}
				
			}
			*/
			
			Set<PhpExpr> elseDefVars = new HashSet<PhpExpr>();
			for (Stmt stmt: body2) {
				
				List<PhpExpr> defVars = stmt.getDefVarsRecur();
				elseDefVars.addAll(defVars);
				
			}
			
			/*
			Set<String> elseDefVarStrings = new HashSet<String>();
			Iterator<PhpExpr> elseDefVarIter = elseDefVars.iterator();
			
			while (elseDefVarIter.hasNext()) {
				
				PhpExpr var = elseDefVarIter.next();
				if (!elseDefVarStrings.contains(var.getString(false))) {
					
					elseDefVarStrings.add(var.getString(false));
					
				} else {
					
					elseDefVarIter.remove();
					
				}
				
			}
			*/
			
			Set<PhpExpr> defVars = new HashSet<PhpExpr>();
			defVars.addAll(thenDefVars);
			defVars.addAll(elseDefVars);
			
			/*
			Set<String> defVarStrings = new HashSet<String>();
			Iterator<PhpExpr> defVarIter = defVars.iterator();
			
			while (defVarIter.hasNext()) {
				
				PhpExpr var = defVarIter.next();
				if (!defVarStrings.contains(var.getString(false))) {
					
					defVarStrings.add(var.getString(false));
					
				} else {
					
					defVarIter.remove();
					
				}
				
			}
			*/
			
			// Find the last stmt before this ITE
			Stmt lastPreStmt = phpFile.getPreStmtInterLayer(this);
			Stmt lastThenStmt = body1.get(body1.size() - 1);
			Stmt lastElseStmt = body2.size() == 0 ? lastPreStmt : body2.get(body2.size() - 1);
			
			// Finished preparing for the shadow variables
			// Add the head of ITE
			retString = retString.concat("(ite ");
			
			// Add the guard
			retString = retString.concat(exprs.get(0).getString(true) + "\n");
			
			// Add then
			retString = retString.concat("\t\t");

			retString = retString.concat(" (and ");

			// Add the program itself

			for (int i = 0; i < body1.size(); i++) {

				retString = retString.concat(body1.get(i).toFormula(prefix + "  "));

			}
			
			// Add the evaluation of shadow variables for THEN
			Set<String> processedVars = new HashSet<String>();
			
			processThenVars:
			for (PhpExpr defVar : defVars) {
				
				for (String var : processedVars) {
					
					if (defVar.getString(false).compareTo(var) == 0) break processThenVars;
					
				}
				
				processedVars.add(defVar.getString(false));

				// Give the shadow variable a new name
				String shadowVar = defVar.getString(false) + "*" + (-1) * this.startLine;

				// If "then" branch reassigned the variable, make the connection
				// with the state of the last statement in this branch
				if (GeneralUtils.SetContainsVar(thenDefVars, defVar, false)) {

					String concreteVar = defVar.getString(false) + "*"
							+ lastThenStmt.getPosFromAM(defVar.getString(false));
					String newLine = " (= " + shadowVar + " " + concreteVar + ") ";
					retString = retString.concat(newLine);
					
				}

				// If "then" branch didn't reassign the variable, make the
				// connection with the state of closest previous statement of
				// ITE
				else {

					if (lastPreStmt != null) {

						String concreteVar = defVar.getString(false) + "*"
								+ lastPreStmt.getPosFromAM(defVar.getString(false));
						String newLine = " (= " + shadowVar + " " + concreteVar + ") ";
						retString = retString.concat(newLine);
						
					}

				}
			}

			retString = retString.concat(")\n");

			retString = retString.concat("\n");

			// Add else
			retString = retString.concat("\t\t");
			
			// [FIXME] if the body is empty, should still do something with the shadow variables
			if (body2.size() == 0) {
				
				retString = retString.concat("true");
				
			}
			
			else {
				
				retString = retString.concat(" (and ");
	
				for (int i = 0; i < body2.size(); i++) {
	
					retString = retString.concat(body2.get(i).toFormula(prefix + "  "));
	
				}
	
				// Add the evaluation of shadow variables for ELSE
				
				processedVars = new HashSet<String>();
	
				processElseVars:
				for (PhpExpr defVar : defVars) {
					
					for (String var : processedVars) {
						
						if (defVar.getString(false).compareTo(var) == 0) continue processElseVars;
						
					}
					
					processedVars.add(defVar.getString(false));
	
					// Give the shadow variable a new name
					String shadowVar = defVar.getString(false) + "*" + (-1) * this.startLine;
	
					// If "else" branch reassigned the variable, make the connection
					// with the state of the last statement in this branch
					if (GeneralUtils.SetContainsVar(elseDefVars, defVar, false)) {
	
						String concreteVar = defVar.getString(false) + "*"
								+ lastElseStmt.getPosFromAM(defVar.getString(false));
						String newLine = " (= " + shadowVar + " " + concreteVar + ") ";
						retString = retString.concat(newLine);
	
					}
	
					// If "else" branch didn't reassign the variable, make the
					// connection with the state of closest previous statement of
					// ITE
					else {
	
						if (lastPreStmt != null) {
	
							String concreteVar = defVar.getString(false) + "*"
									+ lastPreStmt.getPosFromAM(defVar.getString(false));
							String newLine = " (= " + shadowVar + " " + concreteVar + ") ";
							retString = retString.concat(newLine);
	
						}
	
					}
	
				}
	
				retString = retString.concat(")\n");
			
			}
			
			retString = retString.concat("\n");
			
			// Add the end of ITE
			retString = retString.concat(")\n");
			
			
			
		}
		
		// In case of loop
		else if (stmtType == StmtType.WHILE) {
			
			retString = prefix + "(and ";
			
			// Add the guard
			retString = retString.concat(exprs.get(0).getStringReloc(startLine) + "\n");

			// Add the program itself

			for (int i = 0; i < body1.size(); i++) {

				retString = retString.concat(body1.get(i).toFormula(prefix + "  "));

			}
			
			retString = retString.concat("(not ");
			retString = retString.concat(exprs.get(0).getStringReloc(-1 * startLine) + "\n");
			retString = retString.concat(")");
			
			retString = retString.concat(")");
			
		}
		
		return retString;
		
	}
	
	public int getPosFromAM(String var) {
		
		for (Entry<PhpExpr, Integer> entry : assignMap.entrySet()) {
			if (entry.getKey().getString(false).compareTo(var) == 0) {
				return entry.getValue();
			}
		}
		
		return 0;
	}
	
	public PhpExprType getVarType(String targetVar) {
		
		for (String var : typeMap.keySet()) {
			if (targetVar.compareTo(var) == 0) {
				return typeMap.get(var);
			}
		}
		
		return PhpExprType.UNKOWN;
		
	}
	
	/**
	 * get taint condition of a variable in the taint vector after the execution of current stmt
	 * @param var is the target variable
	 * @param nameOnly is true when only matching on the name without position
	 * @return
	 */
	public PhpExpr getTC(PhpExpr var, boolean nameOnly) {
		
		for(Entry<PhpExpr, PhpExpr> entry : taintVector.entrySet()) {
			
			if (entry.getKey().getString(false).compareTo(var.getString(false)) == 0) {
				if (nameOnly) {
					PhpExpr tc =  entry.getValue();
					return (tc == null) ? PhpExprUtils.mkFalse() : tc;
				}
				else if (entry.getKey().getPosition() == var.getPosition()) {
					PhpExpr tc =  entry.getValue();
					return (tc == null) ? PhpExprUtils.mkFalse() : tc;
				}
			}
		}
		
		return PhpExprUtils.mkFalse();
		
	}
	/**
	 * Update the taint condition regardless the position
	 * @param var is the variable to be updated
	 * @param tc is the new taint condition
	 */
	public void putTC(PhpExpr var, PhpExpr tc) {
		
		Iterator<Entry<PhpExpr, PhpExpr>> itr = taintVector.entrySet().iterator();
		
		while(itr.hasNext()) {
			
			Entry<PhpExpr, PhpExpr> entry = itr.next();
			if (entry.getKey().getString(false).compareTo(var.getString(false)) == 0) {
				itr.remove();
			}
		}
		
		taintVector.put(var, tc);
		
	}
	
	/* obsolete
	public void putTC(PhpExpr var, PhpExpr tc, boolean Name) {
		
		Iterator<Entry<PhpExpr, PhpExpr>> itr = taintVector.entrySet().iterator();
		
		while(itr.hasNext()) {
			
			Entry<PhpExpr, PhpExpr> entry = itr.next();
			if (entry.getKey().getString(true).compareTo(var.getString(true)) == 0) {
				entry.setValue(tc);
				return;
			}
		}
		
		taintVector.put(var, tc);
		
	}
	*/
	
	/**
	 * get the list of path condition it has to satisfy to reach this statement
	 * @return the path condition list
	 * @throws Exception 
	 */
	public PhpExpr getPathCondition() throws Exception {
		
		if (this.parentStmt == null) {
			
			return PhpExprUtils.mkTrue();
			
		}
		
		else if (parentStmt.stmtType != StmtType.ITE && parentStmt.stmtType != StmtType.WHILE) {
			
			return parentStmt.getPathCondition();
			
		}
		
		// If parent statement is not null and it's an ITE
		else if (parentStmt.stmtType == StmtType.ITE){
			
			if (parentStmt.body1.contains(this)) {
				
				PhpExpr prevPC = parentStmt.getPathCondition();
				List<PhpExpr> retPC = new LinkedList<PhpExpr>();
				retPC.add(prevPC);
				retPC.add(parentStmt.exprs.get(0));
				return PhpExprUtils.mkAnd(retPC);
				
			} else {
				
				PhpExpr prevPC = parentStmt.getPathCondition();
				List<PhpExpr> retPC = new LinkedList<PhpExpr>();
				retPC.add(prevPC);
				retPC.add(PhpExprUtils.mkNot(parentStmt.exprs.get(0)));
				return PhpExprUtils.mkAnd(retPC);
				
			}
		}
		
		// If parent statement is not null and it's a while loop
		else if (parentStmt.stmtType == StmtType.WHILE){
			
			PhpExpr prevPC = parentStmt.getPathCondition();
			List<PhpExpr> retPC = new LinkedList<PhpExpr>();
			retPC.add(prevPC);
			retPC.add(parentStmt.exprs.get(0));
			return PhpExprUtils.mkAnd(retPC);
			
		}
		
		// If parent statement is unhandled stmt
		else {
			
			return PhpExprUtils.mkTrue();
			
		}
	}
	
	/////////////////////////////////////////
	//
	// Program Slicing
	//
	
	/**
	 * Slice the statement by tagging it and propagate backwards
	 * @param targetVars Target variables
	 * @param tag Target slice tag
	 * @throws Exception When targetVars is null
	 */
	
	// [DEBUG]
	// [FIXME] underconstruction
	/*
	public void BackwardSlice(List<String> targetVars, int tag) throws Exception {
		
		if (targetVars == null) {
			
			throw new Exception("Can't slice the program without target variables.");
			
		} 
		
		// If it's an assignment
		if (stmtType == StmtType.ASSIGN) {
			
			String assignedVar = exprs.get(0).getVarsFromExpr(false).get(0);
			// If it assigns a variable in the target variable list
			if (targetVars.contains(assignedVar)) {
				
				// Include this statement
				sliceTags.add(tag);
				
				// Remove this variable
				targetVars.remove(assignedVar);
				
				// Add the variables used in the assignment
				targetVars.addAll(exprs.get(1).getVarsFromExpr(false));
				
			}
			
			// If it has a immediate previous sibling, slice it
			if (preStmt != null) {
				
				preStmt.BackwardSlice(targetVars, tag);
				
			}
			
			// If it doesn't have a immediate previous sibling, but have a parent, slice it
			
			else if (parentStmt != null) {
				
				parentStmt.BackwardSlice(targetVars, tag);
				
			}
			
			// Else it's the beginning of the file
			
		}
		
		// If it's a function
		// FIXME: some functions may change the content of their parameters
		else if (stmtType == StmtType.FUNC) {
			
		}
		
		// If it's an ITE
		else if (stmtType == StmtType.ITE) {
			
			// Find all the target variables defined in the ITE
			List<String> definedVars = this.getDefVarsRecur(false);
			List<String> definedTargeVars = new ArrayList<String>();
			
			
			for (int i = 0; i < definedVars.size(); i++) {
				
				if (targetVars.contains(definedVars.get(i))) {
					
					definedTargeVars.add(definedVars.get(i));
					
				}
				
			}
			
			// If target variables are assigned in the ITE, then slice it, or else skip it
			if (definedTargeVars.size() > 0) {
				
				sliceTags.add(tag);
				
			}
			
		}
		
		// If it's an loop
		// FIXME: skipping loop for the moment
		else if (stmtType == StmtType.WHILE) {
			
		}
		
	}
	*/
	
	
	
}


