package utils;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DocUtils {
	
	/**
	 * get the string from scalar:string
	 * @param stringNode
	 * @return
	 * @throws Exception 
	 */
	static public String getStringFromNode(Node stringNode) throws Exception {
		
		if (!stringNode.getNodeName().equals("scalar:string")) {
			
			throw new Exception("DocUtils.getStringFromNode doesn't take " + stringNode.getNodeName());
			
		} 
		
		else {

			return stringNode.getTextContent();
			
		}
		
	}
	
	static public int getIntFromNode(Node intNode) throws Exception {
		
		if (!intNode.getNodeName().equals("scalar:int")) {
			
			throw new Exception("DocUtils.getIntFromNode doesn't take " + intNode.getNodeName());
			
		}
		
		else {
			
			return Integer.parseInt(intNode.getTextContent());
			
		}
		
	}

	static public Node getFirstChildWithName(Node targetNode, String targetString) throws Exception {
		
		//System.out.println("Looking for " + targetString + " in " + targetNode.getNodeName());
		
		NodeList ChildNodeList = targetNode.getChildNodes();
		
		for (int i = 0; i < ChildNodeList.getLength(); i++) {
			
			//System.out.println("The child node is " + ChildNodeList.item(i).getNodeName());
			
			if (ChildNodeList.item(i).getNodeName().equals(targetString)) {
				
				return ChildNodeList.item(i);
				
			}
			
		}
		
		throw new Exception("Target node \"" +  targetNode.getNodeName() + "\" doesn't contain a child \"" + targetString + "\"" );
	}
	
	static public List<Node> getListofChildrenWithName(Node targetNode, String targetString) {
		
		List<Node> retNodeList = new ArrayList<Node>();
		NodeList ChildNodeList = targetNode.getChildNodes();
		
		for (int i = 0; i < ChildNodeList.getLength(); i++) {
			
			if (ChildNodeList.item(i).getNodeName().equals(targetString)) {
				
				retNodeList.add(ChildNodeList.item(i));
				
			}
			
		}
		
		return retNodeList;
		
	}
	
	static public boolean isStmt(Node node) {
		
		String nodeName = node.getNodeName();
		if (nodeName.equals("node:Stmt_Echo") ||
				nodeName.equals("node:Expr_Assign") ||
				// a function declaration is considered as a statement
				nodeName.equals("node:Stmt_Function") ||
				nodeName.equals("node:Stmt_If") ||
				nodeName.equals("node:Stmt_While")) {
			return true;
		}
		
		return false;
				
	}
	
	/**
	 * Given an expression node, find the statement node it's in
	 * @param exprNode is the given expression node
	 * @return the statement node the exprNode is in
	 */
	static public Node getStmtOfExpr(Node exprNode) {
		
		Node parentNode = exprNode.getParentNode();
		while ((parentNode != null)) {
			if (isStmt(parentNode)) return parentNode;
			parentNode = parentNode.getParentNode();
		}
		
		return null;
	}
	
	static public boolean isExpr(Node node) {
		
		String nodeName = node.getNodeName();
		if (nodeName.equals("node:Scalar_String") ||
				nodeName.equals("node:Expr_Variable") ||
				nodeName.equals("node:Expr_BinaryOp_BooleanOr") ||
				nodeName.equals("node:Expr_BinaryOp_BooleanAnd") ||
				nodeName.equals("node:Expr_BinaryOp_SmallerOrEqual") ||
				nodeName.equals("node:Expr_BinaryOp_GreaterOrEqual") ||
				nodeName.equals("node:Expr_BinaryOp_Equal") ||
				nodeName.equals("node:Expr_BinaryOp_Smaller") ||
				nodeName.equals("node:Expr_BinaryOp_Greater") ||
				nodeName.equals("node:Expr_BinaryOp_Plus") ||
				nodeName.equals("node:Expr_BinaryOp_Minus") ||
				nodeName.equals("node:Expr_BinaryOp_Mul") ||
				nodeName.equals("node:Expr_BinaryOp_Div") ||
				nodeName.equals("node:Expr_ArrayDimFetch") ||
				nodeName.equals("node:Expr_FuncCall") ||
				nodeName.equals("node:Scalar_LNumber")) {
			
			return true;
			
		}
		
		return false;
	}
	
	static public Node getFirstExprChild(Node targetNode) throws Exception {
		
		NodeList ChildNodeList = targetNode.getChildNodes();
		
		for (int i = 0; i < ChildNodeList.getLength(); i++) {
			
			Node currentNode = ChildNodeList.item(i);
			
			// Search for expression node
			if (isExpr(currentNode)) {
				
				return currentNode;
				
			}
			
		}
		
		throw new Exception("Target node doesn't contain a handled expr");
		
	}
	
	/**
	 * get the parent Stmt of the given stmt, for now the parent Stmt can only be "node:Stmt_While" or "node:Stmt_If"
	 * If the parent Stmt is anything other than these two, a [WARNING] is generated.
	 * @param targetStmt
	 * @return the parent Stmt of the given stmt
	 */
	public static Node getParentStmtNode(Node targetStmt) {
		
		if (targetStmt == null) {
			System.out.println("[ERROR] getParentStmtNode doesn't take null as input, but "
					+ "targetStmt is null");
		}
		
		Node parentNode = targetStmt.getParentNode();
		String parentNodeName = parentNode.getNodeName();
		if (parentNodeName.contains("node:Stmt")) {
			if (parentNodeName.compareTo("node:Stmt_While") != 0 &&
					parentNodeName.compareTo("node:Stmt_If") != 0) {
				System.out.println("[WARNING] getParentStmtNode only expects parent node to be node:Stmt_While or node:Stmt_If, "
						+ "but gets " + parentNodeName + " instead.");
			}
			
			return parentNode;
		}
		
		return getParentStmtNode(parentNode);
	}
	
	public static Node getFirstChildSkipText(Node targetNode) {
		
		if (targetNode == null) {
			System.out.println("[ERROR] getFirstChildSkipText doesn't take null as input, but "
					+ "targetNode is null");
		}
		
		NodeList childNodes = targetNode.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeName().compareTo("#text") != 0) return childNode;
		}
		
		// the targetNode doesn't contain non-text child nodes
		return null;
	}
	
	public static Node getLastChildSkipText(Node targetNode) {
		
		if (targetNode == null) {
			System.out.println("[ERROR] getLastChildSkipText doesn't take null as input, but "
					+ "targetNode is null");
		}
		
		NodeList childNodes = targetNode.getChildNodes();
		for (int i = childNodes.getLength() - 1; i >= 0; i--) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeName().compareTo("#text") != 0) return childNode;
		}
		
		// the targetNode doesn't contain non-text child nodes
		return null;
	}
	
	/**
	 * get the exclusive descendants nodes with the given name
	 * @param targetNode is the target node we are searching at
	 * @param targetName is the target name we are searching for
	 * @return a list of nodes strictly under the target node and are with the target name
	 */
	public static List<Node> getDecendentNodeByName(Node targetNode, String targetName) {
		
		if (targetNode == null) {
			System.out.println("[ERROR] getDecendentNodeByName doesn't take null as input, but "
					+ "targetNode is null");
		}
		
		if (targetName == null) {
			System.out.println("[ERROR] getDecendentNodeByName doesn't take null as input, but "
					+ "targetName is null");
		}
		
		List<Node> list = new ArrayList<Node>();
		NodeList childList = targetNode.getChildNodes();
		for (int i = 0; i < childList.getLength(); i++) {
			Node childNode = childList.item(i);
			if (childNode.getNodeName().compareTo(targetName) == 0) list.add(childNode);
			List<Node> subList = getDecendentNodeByName(childNode, targetName);
			list.addAll(subList);
		}
		
		return list;
	}
	
	public static int getStartFilePos(Node node) throws Exception{
		
		if (node == null) {
			throw new Exception("[ERROR] getStartLine can't take null.");
		}
		
		Node spIntNode = getFirstChildWithName(
				getFirstChildWithName(node, "attribute:startFilePos"), "scalar:int");
		
		return getIntFromNode(spIntNode);
	}
	
	public static int getEndFilePos(Node node) throws Exception{
		
		if (node == null) {
			throw new Exception("[ERROR] getEndFilePos can't take null.");
		}
		
		Node epIntNode = getFirstChildWithName(
				getFirstChildWithName(node, "attribute:endFilePos"), "scalar:int");
		
		return getIntFromNode(epIntNode);
	}
	
	public static int getStartLine(Node node) throws Exception{
		
		if (node == null) {
			throw new Exception("[ERROR] getStartLine can't take null.");
		}
		
		Node slIntNode = getFirstChildWithName(
				getFirstChildWithName(node, "attribute:startLine"), "scalar:int");
		
		return getIntFromNode(slIntNode);
	}
	
	public static int getEndLine(Node node) throws Exception{
		
		if (node == null) {
			throw new Exception("[ERROR] getEndLine can't take null.");
		}
		
		Node elIntNode = getFirstChildWithName(
				getFirstChildWithName(node, "attribute:endLine"), "scalar:int");
		
		return getIntFromNode(elIntNode);
	}
	
	public static boolean containsChildNodeWithName(Node node, String name) throws Exception{
		
		if (node == null) {
			throw new Exception("[ERROR] containsChildNodeWithName can't take null.");
		}
		
		NodeList childNodes = node.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++ ) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeName().compareTo(name) == 0) return true;
		}
		
		return false;
	}
	
	/**
	 * Make a name node with the given name
	 * @param doc is the root document
	 * @param name is the name of the subNode:name
	 * @return a node with "subNode:name"
	 */
	public static Node mkSubNodeNameNode (Document doc, String name) {
		
		if (name == null) {
			System.out.println("[ERROR] createSubNodeNameNode can't take null.");
			return null;
		}
		
		Element varNameSubNode = doc.createElement("subNode:name");
		Element varStringNode = doc.createElement("scalar:string");
		varStringNode.setTextContent(name);
		varNameSubNode.appendChild(varStringNode);
		
		return varNameSubNode;
	}
	
	/**
	 * Make a node for integer value
	 * @param num is the integer number
	 * @return a node with node name "subNode:value"
	 */
	
	public static Node mkIntValNode (Document doc, int num) {
		
		Element numValSubNode = doc.createElement("subNode:value");
		Element numValStringNode = doc.createElement("scalar:int");
		numValStringNode.setTextContent(String.valueOf(num));
		numValSubNode.appendChild(numValStringNode);
		
		return numValSubNode;
	}
	
	public static Node mkVarNode (Document doc, int sp, int sl, int el, int ep, String name) {
		Element spNode = doc.createElement("attribute:startFilePos");
		Element spIntNode = doc.createElement("scalar:int");
		spIntNode.setTextContent(String.valueOf(sp));
		spNode.appendChild(spIntNode);
		
		Element slNode = doc.createElement("attribute:startLine");
		Element slIntNode = doc.createElement("scalar:int");
		slIntNode.setTextContent(String.valueOf(sl));
		slNode.appendChild(slIntNode);
		
		Element elNode = doc.createElement("attribute:endLine");
		Element elIntNode = doc.createElement("scalar:int");
		elIntNode.setTextContent(String.valueOf(el));
		elNode.appendChild(elIntNode);
		
		Element epNode = doc.createElement("attribute:endFilePos");
		Element epIntNode = doc.createElement("scalar:int");
		epIntNode.setTextContent(String.valueOf(ep));
		epNode.appendChild(epIntNode);
		
		Node varNameNode = mkSubNodeNameNode(doc, name);
		
		Element varNode = doc.createElement("node:Expr_Variable");
		varNode.appendChild(spNode);
		varNode.appendChild(slNode);
		varNode.appendChild(elNode);
		varNode.appendChild(epNode);
		varNode.appendChild(varNameNode);
		
		return varNode;
	}
	
	/**
	 * make an assignment node
	 * @param doc is the root document
	 * @param sp is the startFilePos
	 * @param sl is the startLine
	 * @param el is the endLine
	 * @param ep is the endFilePos
	 * @param varNode is the variable node being assigned to
	 * @param exprNode is the expression node being assigned
	 * @return the assignment node
	 */
	public static Node mkAssignNode (Document doc, int sp, int sl, int el, int ep, Node varNode, Node exprNode) {
		Element spNode = doc.createElement("attribute:startFilePos");
		Element spIntNode = doc.createElement("scalar:int");
		spIntNode.setTextContent(String.valueOf(sp));
		spNode.appendChild(spIntNode);
		
		Element slNode = doc.createElement("attribute:startLine");
		Element slIntNode = doc.createElement("scalar:int");
		slIntNode.setTextContent(String.valueOf(sl));
		slNode.appendChild(slIntNode);
		
		Element elNode = doc.createElement("attribute:endLine");
		Element elIntNode = doc.createElement("scalar:int");
		elIntNode.setTextContent(String.valueOf(el));
		elNode.appendChild(elIntNode);
		
		Element epNode = doc.createElement("attribute:endFilePos");
		Element epIntNode = doc.createElement("scalar:int");
		epIntNode.setTextContent(String.valueOf(ep));
		epNode.appendChild(epIntNode);
		
		Element varSubNode = doc.createElement("subNode:var");
		varSubNode.appendChild(varNode);
		
		Element exprSubNode = doc.createElement("subNode:expr");
		exprSubNode.appendChild(exprNode);
		
		Element assignNode = doc.createElement("node:Expr_Assign");
		assignNode.appendChild(spNode);
		assignNode.appendChild(slNode);
		assignNode.appendChild(elNode);
		assignNode.appendChild(epNode);
		assignNode.appendChild(varSubNode);
		assignNode.appendChild(exprSubNode);
		
		return assignNode;
	}

}
