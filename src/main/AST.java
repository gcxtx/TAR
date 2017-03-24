package main;

import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.infosys.www.phpparser.ParseNode;

public class AST {
	public String val;
	public List<AST> children;
	public AST parent;
	
	/**
	 * Constructor for a dummy AST
	 */
	public AST() {
		val = "NULL";
		children = new ArrayList<AST>();
		parent = null;
	}
	
	/**
	 * Constructor for create a new AST
	 * @param newVal
	 * @param newChildren
	 * @param newParent
	 */
	public AST(String newVal, List<AST> newChildren, AST newParent) {
		val = newVal;
		children = newChildren;
		parent = newParent;
	}
	
	/**
	 * Construct an AST from a ParseTree
	 * @param pTree
	 */
	public AST(ParseNode pNode) {
		AST ast = pTreeToAST(pNode);
		
		val = ast.val;
		children = ast.children;
		parent = ast.parent;
	}
	
	/**
	 * Convert a parse tree node to an AST
	 * @param pTree
	 * @return
	 */
	private AST pTreeToAST(ParseNode pNode){
		
		String name = pNode.getName();
		AST ast = new AST();
		
		// If the node is the root
		if (name.compareTo("S") == 0) {
			ast.val = "root";
		}
		
		// If the node is a statement list
		else if (name.compareTo("top_statement_list") == 0) {
			ast.val = "StatementList";
		}
		
		else if (name.compareTo("") == 0) {
			
		}
		
		else {
			System.out.println("[ERROR] Unhandled parse node:" + name);
		}
		
		for (int i = 0; i < pNode.getChildren().size(); i++) {
			ast.children.add(new AST(pNode.getChild(i)));
		}
		
		return ast;
	}
	
	/**
	 * Clone this ASTreeNode to a new one
	 * @param recur is true when the clone is recursive
	 * @return the cloned new node
	 */
	public AST clone(boolean recur) {
		
		AST newNode = new AST(this.val, new ArrayList<AST>(), this.parent);
		if (!recur) {
			for (AST child : this.children) {
				newNode.children.add(child);
			}
		} else {
			for (AST child : this.children) {
				newNode.children.add(child.clone(true));
			}
		}
		return newNode;
	}
	
	public void print(String prefix) {
		
		System.out.println(prefix + this.val);
		String newPrefix = ">>" + prefix;
		for (int i = 0; i < this.children.size(); i++) {
			this.children.get(i).print(newPrefix);
		}
	}

}
