package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Node;

import at.ac.tuwien.infosys.www.phpparser.ParseTree;

public class PhpFile {
	private List<Stmt> stmts;
	public Set<PhpExpr> shadowVars;
	
	private Set<Integer>  sinkLines;
	private Set<Stmt> sinkStmts;
	
	public Map<String,Node> functionNodesMap;
	
	public PhpFile () {
		
		stmts = new ArrayList<Stmt>();
		shadowVars = new HashSet<PhpExpr>();
		sinkLines = new HashSet<Integer>();
		sinkStmts = new HashSet<Stmt>();
		
		functionNodesMap = new HashMap<String, Node>();
		
	}
	
	public PhpFile (List<Stmt> new_stmts) {
		
		stmts = new_stmts;
		shadowVars = new HashSet<PhpExpr>();
		sinkLines = new HashSet<Integer>();
		sinkStmts = new HashSet<Stmt>();
		
		functionNodesMap = new HashMap<String, Node>();
		
	}
	
	public PhpFile (AST ast, Set<Integer> sinks) throws Exception {
		
		stmts = new ArrayList<Stmt>();
		shadowVars = new HashSet<PhpExpr>();
		sinkLines = sinks;
		sinkStmts = new HashSet<Stmt>();
		functionNodesMap = new HashMap<String, Node>();
		readFileFromAST(ast);
		
	}
	
	public PhpFile (ParseTree pTree, Set<Integer> sinks) throws Exception {
		
		stmts = new ArrayList<Stmt>();
		shadowVars = new HashSet<PhpExpr>();
		sinkLines = sinks;
		sinkStmts = new HashSet<Stmt>();
		functionNodesMap = new HashMap<String, Node>();
		readFileFromRoot(pTree);
		
	}
	
	public void addStmt (Stmt newStmt) throws Exception {
		
		if (newStmt == null)
		{
			throw new Exception("Can't add null as statement.");
		}
		
		stmts.add(newStmt);
		
	}
	
	public Set<Stmt> getSinks() {
		
		return sinkStmts;
		
	}
	
	public boolean inSinkLines(int line) {
		return sinkLines.contains(line);
	}
	
	public void addSinkStmt(Stmt sinkStmt) {
		
		sinkStmts.add(sinkStmt);
		
	}
	
	/**
	 * Top level function when you read in a php file and parse it into a list of statements and store them in this PhpFile object.
	 * 
	 * @param root The root of the dom elements containing all the statement information
	 * @throws Exception 
	 */
	private void readFileFromRoot (ParseTree pTree) throws Exception {
		
		//[CONSTRUCTION]
		//stmts = Stmt.readStmtsFromArray(this, null, pTree);
		
	}
	
	private void readFileFromAST (AST ast) throws Exception {
		
		//[CONSTRUCTION]
		//stmts = Stmt.readStmtsFromArray(this, null, ast);
		
	}
	
	/**
	 * Print out the php file
	 * @throws Exception 
	 */
	public void printPhpFile() throws Exception {
		
		if (JAnalyzer.DEBUG_MODE >= 10) {
			System.out.println("size of phpfile: " + stmts.size());
		}
		
		for(int i = 0; i < stmts.size(); i++) {
			
			stmts.get(i).printStmt();
			
		}
		
		/*
		System.out.println("The taint sinks are: \n");
		for (Stmt stmt : sinkStmts) {
			stmt.printStmt();
		}
		*/
	}
	
	public List<Stmt> getAllAssignStmts() {
		
		List<Stmt> assignStmts = new ArrayList<Stmt>();
		
		for (int i = 0; i < stmts.size(); i++) {
			
			assignStmts.addAll(stmts.get(i).getAllAssignStmt());
			
		}
		
		return assignStmts;
		
	}
	
	public Set<PhpExpr> getAllVars() {
		
		Set<PhpExpr> allVars = new HashSet<PhpExpr>();
		
		for (int i = 0; i < stmts.size(); i++) {
			
			allVars.addAll(stmts.get(i).getDefVarsRecur());
			allVars.addAll(stmts.get(i).getUseVarsRecur());
			
		}
		
		Map<String, PhpExpr> allReferredVars = new HashMap<String, PhpExpr>();
		
		for (PhpExpr var : allVars) {
			PhpExpr referredVar = getRefferredVars(allReferredVars, var.getString(true));
			if (referredVar == null) {
				allReferredVars.put(var.getString(true), var);
			} else {
				if (referredVar.getExprType() == PhpExprType.UNKOWN) {
					allReferredVars.put(var.getString(true), var);
				}
			}
		}
		
		allVars = new HashSet<PhpExpr>();
		
		for (String varString : allReferredVars.keySet()) {
			allVars.add(allReferredVars.get(varString));
		}
		
		return allVars;
		
	}
	
	private PhpExpr getRefferredVars (Map<String, PhpExpr> map, String varString) {
		for (String str : map.keySet()) {
			if (str.compareTo(varString) == 0) {
				return map.get(str);
			}
		}
		
		return null;
	}
	
	public List<PhpExpr> getAllDefVars() {
		
		List<PhpExpr> allVars = new ArrayList<PhpExpr>();
		
		for (int i = 0; i < stmts.size(); i++) {
			
			allVars.addAll(stmts.get(i).getDefVarsRecur());
			
		}
		
		return allVars;
		
	}
	
	public List<PhpExpr> getAllUseVars() {
		
		List<PhpExpr> allVars = new ArrayList<PhpExpr>();
		
		for (int i = 0; i < stmts.size(); i++) {
			
			allVars.addAll(stmts.get(i).getUseVarsRecur());
			
		}
		
		return allVars;
		
	}
	
	public Stmt getPreStmtInterLayer(Stmt targetStmt) {
		
		if (targetStmt.preStmt != null ) {
			
			return targetStmt.preStmt;
			
		}
		
		if (targetStmt.parentStmt != null) {
			
			return getPreStmtInterLayer(targetStmt.parentStmt);
			
		}
		
		return null;
		
	}
	
	public String toSmtLib() throws Exception {
		
		String retString = "";
		
		// Declare variables
		
		Set<PhpExpr> vars = this.getAllVars();
		Set<String> varStrings = new HashSet<String>();
		
		// Add the shadow variable
		/*
		System.out.println("Normal Variables");
		for (PhpExpr var : vars) {
			System.out.println(var.getString(true) + ": " + var.getExprType());
		}
		
		System.out.println("\nShadow Variables");
		for (PhpExpr shadowVar : shadowVars) {
			System.out.println(shadowVar.getString(true) + ": " + shadowVar.getExprType());
		}
		*/
		
		vars.addAll(shadowVars);
		
		for (PhpExpr var : vars) {
			if (varStrings.contains(var.getString(true))) continue;
			varStrings.add(var.getString(true));
			retString += "(declare-fun " + var.getString(true) + " () ";
			
			switch (stmts.get(stmts.size() - 1).getVarType(var.getString(false))) {
				case INT:
					retString += "Int";
				break;
				default:
					retString += "String";	
			}
			retString += ")\n";
			
		}
		
		// Add all the undefined variables
		for (PhpExpr var : vars) {
			String newString = var.getString(false) + "*0";
			if(varStrings.contains(newString)) continue;
			varStrings.add(newString);
			retString += "(declare-fun " + newString + " () ";
			switch (var.getExprType()) {
				case INT:
					retString += "Int";
				break;
				default:
					retString += "String";	
			}
			retString += ")\n";
			
		}

		retString = retString.concat("\n");
		
		// Formulate the program
		
		for (int i = 0; i < this.stmts.size(); i++) {
			
			Stmt stmt = stmts.get(i);
			if (stmt.stmtType != StmtType.SKIP) {
				
				retString = retString.concat("(assert ");
				retString = retString.concat(stmt.toFormula(""));
				retString = retString.concat(" )\n");
			
			}		
		}
		
		retString = retString.concat("\n");
		
		// validate taint behavior
		for (Stmt stmt: sinkStmts) {
			retString = retString.concat("(assert (and ");
			retString = retString.concat(stmt.getPathCondition().getString(true));
			
			for (PhpExpr var : stmt.getUseVars()) {
				
				PhpExpr tc = stmt.getTC(var, false);
				retString = retString.concat(tc.getString(true));
				retString = retString.concat(" ");
				
			}
			
			retString = retString.concat(" ))\n");
		}
		
		return retString;
	}
	

}
