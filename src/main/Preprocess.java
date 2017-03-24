package main;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import utils.*;

public class Preprocess {
	
	//[CONSTRUCTION]
	private ASTreeNode astRoot;
	private Document doc;
	private NodeList funList;
	private int lineLength;
	private int fileLength;
	private int lineOffset;
	private int filePosOffset;
	
	
	public Preprocess(Document document) {
		
		doc = document;
		funList = doc.getElementsByTagName("node:Stmt_Function");
		try {
			Node lastStmtNode = DocUtils.getLastChildSkipText(DocUtils.getFirstChildWithName(doc.getDocumentElement(), "scalar:array"));
			lineLength = DocUtils.getIntFromNode(DocUtils.getFirstChildWithName(DocUtils.getFirstChildWithName(lastStmtNode, "attribute:endLine"), "scalar:int"));
			fileLength = DocUtils.getIntFromNode(DocUtils.getFirstChildWithName(DocUtils.getFirstChildWithName(lastStmtNode, "attribute:endFilePos"), "scalar:int"));
			lineOffset = lineLength;
			filePosOffset = fileLength;
		} catch (Exception e) {
			System.out.println("[ERROR]" + e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	public Preprocess(ASTreeNode ast) {
		astRoot = ast;
		
		//[construction]
	}
	

	public void process() throws Exception {
		
		// process the continues and breaks in loops
		processCntBrk();
		
		// process functions by inlining
		processFun();

	}
	
	/**
	 * Pre-process the continue and break statements. Adding a flag variable to signal if
	 * continue/break is encountered.
	 * 
	 * [NOTE]The only difference between continue and break is constrain the loop condition.
	 * Because of the way we handle loop, break is eventually the same as continue.
	 * 
	 * @throws Exception
	 */
	private void processCntBrk() throws Exception {
		
		NodeList ctns = doc.getElementsByTagName("node:Stmt_Continue");
		NodeList brks = doc.getElementsByTagName("node:Stmt_Break");
		
		for (int i = 0; i < ctns.getLength(); i++) {
			processOneCtn(ctns.item(i));
		}
		
		for (int i = 0; i < brks.getLength(); i++) {
			processOneCtn(brks.item(i));
		}
		
	}
	
	private void processOneCtn(Node ctnNode) throws Exception {
		
		// get the node of the corresponding while loop statement
		Node whileNode = ctnNode.getParentNode();
		while (whileNode.getNodeName().compareTo("node:Stmt_While") != 0) {
			whileNode = whileNode.getParentNode();
		}

		// get the nodes of the start/end line information of the continue stmt 
		Node startLineNode = DocUtils.getFirstChildWithName(ctnNode, "attribute:startLine");
		//Node endLineNode = DocUtils.getFirstChildWithName(ctn, "attribute:endLine");
		
		Node slValNode = DocUtils.getFirstChildWithName(startLineNode, "scalar:int");
		//Node elValNode = DocUtils.getFirstChildWithName(endLineNode, "scalar:int");
		
		int startLine = DocUtils.getIntFromNode(slValNode);
		//int endLine = DocUtils.getIntFromNode(elValNode);
		
		// get the new flag variable name continue_flag_k
		String flagName = "continue_flag_" + startLine;
		
		// wrap all the "later" stmts with condition "continue_flag_k == 0"
		wrapLaterStmtsInterLayer(flagName, ctnNode);
		
		// replace the "continue;" stmt with an assignment stmt "continue_flag_k = 1;"
		Node parentNode = ctnNode.getParentNode();
		insertAssignmentStmtBefore(flagName, "1", ctnNode);
		parentNode.removeChild(ctnNode);
		
		// insert a new stmt "continue_flag_k = 0;" before the while stmt;
		parentNode = whileNode.getParentNode();
		insertAssignmentStmtBefore(flagName, "0", whileNode);

	}
	
	/**
	 * Insert an assignment statement "$varName = $value;" before the target stmt node assuming the new assignment statement
	 * 
	 * @param varName is the name of the variable assigned
	 * @param value is the value being assigned
	 * @param targetStmtNode is the target stmt
	 * @throws Exception
	 */
	private void insertAssignmentStmtBefore(String varName, String value, Node targetStmtNode) throws Exception {

		// get the nodes of the start/end line information of the target stmt 
		Node startLineNode = DocUtils.getFirstChildWithName(targetStmtNode, "attribute:startLine");
		Node endLineNode = DocUtils.getFirstChildWithName(targetStmtNode, "attribute:endLine");

		// create a new integer variable with name $varName
		Node varNameSubNode = DocUtils.mkSubNodeNameNode(doc, varName);
		
		Element varExprNode = doc.createElement("node:Expr_Variable");
		varExprNode.appendChild(startLineNode.cloneNode(true));
		varExprNode.appendChild(endLineNode.cloneNode(true));
		varExprNode.appendChild(varNameSubNode);
		
		Element varSubNode = doc.createElement("subNode:var");
		varSubNode.appendChild(varExprNode);
		
		// create a new integer constant expression 1
		Node numValSubNode= DocUtils.mkIntValNode(doc, Integer.valueOf(value));
		
		Element numberNode = doc.createElement("node:Scalar_LNumber");
		numberNode.appendChild(startLineNode.cloneNode(true));
		numberNode.appendChild(endLineNode.cloneNode(true));
		numberNode.appendChild(numValSubNode);
		
		Element exprSubNode = doc.createElement("subNode:expr");
		exprSubNode.appendChild(numberNode);
		
		// node:Expr_Assign node
		Element asmNode = doc.createElement("node:Expr_Assign");
		asmNode.appendChild(startLineNode.cloneNode(true));
		asmNode.appendChild(endLineNode.cloneNode(true));
		asmNode.appendChild(varSubNode);
		asmNode.appendChild(exprSubNode);
		
		Node parentNode = targetStmtNode.getParentNode();
		parentNode.insertBefore(asmNode, targetStmtNode);
		
	}
	
	private Node wrapStmtsWithBranch(Node cond, Node targetStmts) throws Exception {
		
		if (cond.getNodeName().compareTo("subNode:cond") != 0 ||
				targetStmts.getNodeName().compareTo("subNode:stmts") != 0) {
			System.out.println("[ERROR] wrapStmtWithBranch expect subNode:cond and subNode:stmts,"
					+ "but getting " + cond.getNodeName() + " and " + targetStmts.getNodeName());
		}
		
		// get the first and last of the later stmts
		Node firstStmtNode = targetStmts.getFirstChild().getFirstChild();
		while (!(firstStmtNode instanceof Element) && firstStmtNode != null) {
			firstStmtNode = firstStmtNode.getNextSibling();
		}
		
		Node lastStmtNode = targetStmts.getFirstChild().getLastChild();
		while (!(lastStmtNode instanceof Element) && lastStmtNode != null) {
			lastStmtNode = lastStmtNode.getPreviousSibling();
		}
		
		Node startLineNode = DocUtils.getFirstChildWithName(firstStmtNode, "attribute:startLine");
		Node endLineNode = DocUtils.getFirstChildWithName(lastStmtNode, "attribute:endLine");

		// create the ITE stmt
		Element ITEStmt = doc.createElement("node:Stmt_If");
		ITEStmt.appendChild(startLineNode.cloneNode(true));
		ITEStmt.appendChild(endLineNode.cloneNode(true));
		ITEStmt.appendChild(cond);
		ITEStmt.appendChild(targetStmts);
		
		Element elseIfsNode = doc.createElement("subNode:elseifs");
		elseIfsNode.appendChild(doc.createElement("scalar:array"));
		ITEStmt.appendChild(elseIfsNode);
		
		Element elseNode = doc.createElement("subNode:else");
		elseNode.appendChild(doc.createElement("scalar:array"));
		ITEStmt.appendChild(elseNode);
		
		return ITEStmt;
	}
	
	private void wrapLaterStmtsOneLayer(String varName, Node targetNode) throws Exception {
		//System.out.println("targetNode is a " + targetNode.getNodeName());
		
		// get the first and last of the later nodes
		Node firstSiblingStmtNode = targetNode.getNextSibling();
		while (!(firstSiblingStmtNode instanceof Element) && firstSiblingStmtNode != null) {
			firstSiblingStmtNode = firstSiblingStmtNode.getNextSibling();
		}
		if (firstSiblingStmtNode == null) return;
		
		//System.out.println("Before firstSiblingStmtNode is a " + firstSiblingStmtNode.getNodeName());
		
		Node lastSiblingStmtNode = firstSiblingStmtNode;
		while (lastSiblingStmtNode.getNextSibling() != null) {
			lastSiblingStmtNode = lastSiblingStmtNode.getNextSibling();
		}
		
		while (!(lastSiblingStmtNode instanceof Element) && lastSiblingStmtNode != null) {
			lastSiblingStmtNode = lastSiblingStmtNode.getPreviousSibling();
		}
		
		//// make the node for condition "$varName == 0"
		
		// get the nodes of the start/end line information of the later stmts
		Node startLineNode = DocUtils.getFirstChildWithName(firstSiblingStmtNode, "attribute:startLine");
		Node endLineNode = DocUtils.getFirstChildWithName(lastSiblingStmtNode, "attribute:endLine");

		// create the left side $varName
		Node subNodeNameNode = DocUtils.mkSubNodeNameNode(doc, varName);
		Element exprVarNode = doc.createElement("node:Expr_Variable");
		exprVarNode.appendChild(startLineNode.cloneNode(true));
		exprVarNode.appendChild(endLineNode.cloneNode(true));
		exprVarNode.appendChild(subNodeNameNode);
		
		Element leftNode = doc.createElement("subNode:left");
		leftNode.appendChild(exprVarNode);
		
		// create the right side "0"
		Node intValNode = DocUtils.mkIntValNode(doc, 0);
		
		Element numberNode = doc.createElement("node:Scalar_LNumber");
		numberNode.appendChild(startLineNode.cloneNode(true));
		numberNode.appendChild(endLineNode.cloneNode(true));
		numberNode.appendChild(intValNode);
		
		Element rightNode = doc.createElement("subNode:right");
		rightNode.appendChild(numberNode);
		
		Element eqExprNode = doc.createElement("node:Expr_BinaryOp_Equal");
		eqExprNode.appendChild(startLineNode.cloneNode(true));
		eqExprNode.appendChild(endLineNode.cloneNode(true));
		eqExprNode.appendChild(leftNode);
		eqExprNode.appendChild(rightNode);
		
		Element condNode = doc.createElement("subNode:cond");
		condNode.appendChild(eqExprNode);
		
		//// make the node for the list of all the "later stmts"
		Element stmtArrayNode = doc.createElement("scalar:array");
		
		Node parentNode = targetNode.getParentNode();
		Node currentStmtNode = firstSiblingStmtNode;
		while (currentStmtNode != null) {
			Node nextSiblingNode = currentStmtNode.getNextSibling();
			stmtArrayNode.appendChild(currentStmtNode);
			currentStmtNode = nextSiblingNode;
		}
		
		Element stmtsNode = doc.createElement("subNode:stmts");
		stmtsNode.appendChild(stmtArrayNode);
		
		Node ITENode = wrapStmtsWithBranch(condNode, stmtsNode);
		parentNode.appendChild(ITENode);
	}
	
	private void wrapLaterStmtsInterLayer(String varName, Node targetNode) throws Exception {
		
		if (varName == null || targetNode == null) {
			System.out.println("[ERROR] wrapLaterStmtsInterLayer doesn't take null as input, but "
					+ ((varName == null) ? "varName" : "targetNode") + " is null");
		}
		
		wrapLaterStmtsOneLayer(varName, targetNode);
		
		Node parentNode = DocUtils.getParentStmtNode(targetNode);
		if (parentNode.getNodeName().compareTo("node:Stmt_While") == 0) {
			return;
		} else  {
			wrapLaterStmtsInterLayer(varName, parentNode);
		}
		
	}
	
	/**
	 * Process the functions by inlining
	 */
	private void processFun() {
		
		NodeList funCallNodes = doc.getElementsByTagName("node:Expr_FuncCall");
		for (int i = 0; i < funCallNodes.getLength(); i++) {
			
			try {
				processOneFunCall(funCallNodes.item(i));
			} catch (Exception e) {
				System.out.println("[ERROR]" + e.getMessage());
				e.printStackTrace();
			}
		}
		
	}
	
	private void processOneFunCall(Node funCallNode) throws Exception {
		
		if (funCallNode.getNodeName().compareTo("node:Expr_FuncCall") != 0) {
			throw new Exception("Expecting node:Expr_FuncCall in processOneFunCall, get " + funCallNode.getNodeName() + " instead.");
		}
		
		// get the function called
		Node funNamePartsNode =  
				DocUtils.getFirstChildWithName(
				DocUtils.getFirstChildWithName(
				DocUtils.getFirstChildWithName(funCallNode, 
						"subNode:name"), 
						"node:Name"), 
						"subNode:parts");
		
		
		String funName = getFullName(funNamePartsNode);
		Node funNode = getFunNodeByName(funName);
		// skip undefined/library function call
		if (funNode == null) return;
		Node newFunNode = funNode.cloneNode(true);
		
		String retVarName = "TAR_RET_" + funName;
		
		// get the parameters of the function
		Node paraArrayNode = DocUtils.getFirstChildWithName(
				DocUtils.getFirstChildWithName(funNode, "subNode:params"), "scalar:array");
		List<Node> paraList = DocUtils.getListofChildrenWithName(paraArrayNode, "node:Param");
		
		// get the arguments of the call
		Node argArrayNode = DocUtils.getFirstChildWithName(
				DocUtils.getFirstChildWithName(funCallNode, "subNode:args"), "scalar:array");
		List<Node> argList = DocUtils.getListofChildrenWithName(argArrayNode, "node:Arg");
		
		// re-instantiate the functions with new start/end lines and start/end file position
		relocateBlock(newFunNode);
		lineOffset = lineOffset + lineLength;
		filePosOffset = filePosOffset + fileLength;
		
		// rename the variables in the function with <variable_name>__<caller_position>
		Node funCallStartFilePosNumNode = DocUtils.getFirstChildWithName(DocUtils.getFirstChildWithName(funCallNode, "attribute:startFilePos"), "scalar:int");	
		int funCallStartFilePos = DocUtils.getIntFromNode(funCallStartFilePosNumNode);
		
		Node funCallStartLineNode = DocUtils.getFirstChildWithName(DocUtils.getFirstChildWithName(funCallNode, "attribute:startLine"), "scalar:int");
		int funCallStartLine = DocUtils.getIntFromNode(funCallStartLineNode);
		addPosInVarNames(newFunNode, funCallStartFilePos);
		
		// replace the return statements with assignment
		List<Node> returnStmtNodes = DocUtils.getListofChildrenWithName(newFunNode, "node:Stmt_Return");
		for (Node returnStmtNode : returnStmtNodes) {
			
			Node returnStmtStartFilePosNumNode = DocUtils.getFirstChildWithName(DocUtils.getFirstChildWithName(returnStmtNode, "attribute:startFilePos"), "scalar:int");	
			int returnStmtStartFilePos = DocUtils.getIntFromNode(returnStmtStartFilePosNumNode);
			
			Node returnStmtEndFilePosNumNode = DocUtils.getFirstChildWithName(DocUtils.getFirstChildWithName(returnStmtNode, "attribute:endFilePos"), "scalar:int");	
			int returnStmtEndFilePos = DocUtils.getIntFromNode(returnStmtEndFilePosNumNode);
			
			Node returnStmtStartLineNode = DocUtils.getFirstChildWithName(DocUtils.getFirstChildWithName(returnStmtNode, "attribute:startLine"), "scalar:int");
			int returnStmtStartLine = DocUtils.getIntFromNode(returnStmtStartLineNode);
			
			Node returnStmtEndLineNode = DocUtils.getFirstChildWithName(DocUtils.getFirstChildWithName(returnStmtNode, "attribute:endLine"), "scalar:int");
			int returnStmtEndLine = DocUtils.getIntFromNode(returnStmtEndLineNode);
			
			// [ASSUME] the program is normalized to a equivalent one with returning only variable
			Node returnVarNameStringNode = DocUtils.getFirstChildWithName(
					DocUtils.getFirstChildWithName(
					DocUtils.getFirstChildWithName(
					DocUtils.getFirstChildWithName(
					DocUtils.getFirstChildWithName(
					DocUtils.getFirstChildWithName(
					DocUtils.getFirstChildWithName(returnStmtNode, 
							"subNode:expr"), 
					"node:Expr_ConstFetch"), 
					"subNode:name"), 
					"node:Name"),
					"subNode:parts"),
					"scalar:array"), 
					"scalar:string");
			
			String returnedVarName = DocUtils.getStringFromNode(returnVarNameStringNode);
			
			Node returnedVarNode = DocUtils.mkVarNode(doc, returnStmtStartFilePos, returnStmtStartLine, returnStmtEndLine, returnStmtEndFilePos, returnedVarName);
			Node retVarNode = DocUtils.mkVarNode(doc, returnStmtStartFilePos, returnStmtStartLine, returnStmtEndLine, returnStmtEndFilePos, retVarName);
			
			Node returnAssignNode = DocUtils.mkAssignNode(doc, returnStmtStartFilePos, returnStmtStartLine, returnStmtEndLine, returnStmtEndFilePos, retVarNode, returnedVarNode);
			Node parentNode = returnStmtNode.getParentNode();
			parentNode.insertBefore(returnAssignNode, returnStmtNode);
			parentNode.removeChild(returnStmtNode);
		}
		
		// Rename the call-by-reference parameter
		// [FIXME] handle call-by-reference later
		/*
		for (int i = 0; i < paraList.size(); i++) {
			Node para = paraList.get(i);
			Node arg = argList.get(i);
			
			Node byRefNode = DocUtils.getFirstChildWithName(para, "subNode:byRef");
			boolean byRef = DocUtils.containsChildNodeWithName(byRefNode, "scalar:true");
			
			
			if (byRef) {
			}
		}
		*/
		
		// connect the variables and parameters by assigning the variables to parameters
		
		if (paraList.size() != argList.size()) {
			System.out.println("[ERROR][processOneFunCall] The number of parameters and arguments doesn't match.");
			System.out.println("[ERROR][processOneFunCall] The function call is " + funName);
		}
		
		// get the stmt containing this function call
		Node stmtNode = DocUtils.getStmtOfExpr(funCallNode);
		if (stmtNode == null) {
			System.out.println("[ERROR][processOneFunCall] Can't find parent statement node.");
		}
		Node parentNode = stmtNode.getParentNode();
		
		for (int i = 0; i < paraList.size(); i++) {
			Node para = paraList.get(i);
			Node arg = argList.get(i);
			
			// [FIXME] handle call-by-reference later
			//Node byRefNode = DocUtils.getFirstChildWithName(para, "subNode:byRef");
			//boolean byRef = DocUtils.containsChildNodeWithName(byRefNode, "scalar:true");	
			//if (byRef) continue;
			
			// make the variable node for the parameter
			Node paraStringNode = DocUtils.getFirstChildWithName(DocUtils.getFirstChildWithName(para, "subNode:name"), "scalar:string");
			String paraString = DocUtils.getStringFromNode(paraStringNode);
			Node paraVarNode = DocUtils.mkVarNode(doc, 
					funCallStartFilePos + i * 2, 
					funCallStartLine, 
					funCallStartLine, 
					funCallStartFilePos + i * 2, 
					paraString + "__" + String.valueOf(funCallStartFilePos));
			
			// make the node for the expression of the argument
			Node argNode = DocUtils.getFirstChildSkipText(DocUtils.getFirstChildWithName(arg, "subNode:value")).cloneNode(true);
			
			// make the assignment node, and insert it
			Node assignNode = DocUtils.mkAssignNode(doc, funCallStartFilePos + i * 2, funCallStartLine, funCallStartLine, funCallStartFilePos + i * 2, paraVarNode, argNode);
			
			parentNode.insertBefore(assignNode, stmtNode);
			
		}
		
		// Insert the body of the function call
		NodeList funBodyArrayNodes = DocUtils.getFirstChildWithName(DocUtils.getFirstChildWithName(newFunNode, "subNode:stmts"), "scalar:array").getChildNodes();
		for (int i = 0; i < funBodyArrayNodes.getLength(); i++) {
			parentNode.insertBefore(funBodyArrayNodes.item(i).cloneNode(true), stmtNode);
		}
		
		// substitute the function call node with the return variable which takes form of TAR_RET_<FUNCTION_NAME>
		Node retVarNode = DocUtils.mkVarNode(doc, funCallStartFilePos, funCallStartLine, funCallStartLine, funCallStartFilePos, retVarName);
		Node parentOfCallNode = funCallNode.getParentNode();
		parentOfCallNode.insertBefore(retVarNode, funCallNode);
		parentOfCallNode.removeChild(funCallNode);
		
	}
	
	/**
	 * get the function node by its name
	 * @param targetName is the name of the target function
	 * @return the function node
	 * @throws Exception
	 */
	private Node getFunNodeByName(String targetName) throws Exception {
		
		for (int i = 0; i < funList.getLength(); i++) {
			Node funNode = funList.item(i);
			Node funNameStringNode =  DocUtils.getFirstChildWithName(
					DocUtils.getFirstChildWithName(funNode, "subNode:name"), "scalar:string");
			String funName = DocUtils.getStringFromNode(funNameStringNode);
			if (funName.compareTo(targetName) == 0) return funNode;
		}
		
		System.out.println("[WARNING]Calling undefined/library function: " + targetName);
		return null;
	}
	
	private String getFullName(Node partsNode) throws Exception {
		
		if (partsNode.getNodeName().compareTo("subNode:parts") != 0) {
			throw new Exception("Expecting subNode:parts in getFullName, but getting " + partsNode.getNodeName());
		}
		
		StringBuilder sb = new StringBuilder();
		
		NodeList stringNodes = DocUtils.getFirstChildWithName(partsNode, "scalar:array").getChildNodes();
		for (int i = 0; i < stringNodes.getLength(); i++) {
			
			Node stringNode = stringNodes.item(i);
			
			// if the string node is not scalar:string, skip it
			if (stringNode.getNodeName().compareTo("#text") == 0) continue;
			if (stringNode.getNodeName().compareTo("scalar:string") != 0) {
				System.out.println("[WARNING] The array of parts of function name in function call contains something "
						+ "other than scalar:string, it's " + stringNode.getNodeName());
				continue;
			}
			
			String partName = DocUtils.getStringFromNode(stringNode);
			if(sb.length() != 0) sb.append("-");
			sb.append(partName);
		}
		
		return sb.toString();
		
	}
	
	/**
	 * Relocate the block by changing the start/end line and start/end file position.
	 * The new position will be the old ones + global offset
	 * @param blockNode is the target block node
	 * @throws Exception 
	 */
	private void relocateBlock(Node blockNode) throws Exception {
		
		if (blockNode == null) {
			System.out.println("[ERROR] relocateBlock doesn't take null as input, but "
					+ "blockNode is null");
		}
		// get all the start/end line and start/end file position nodes
		List<Node> startLineNodes = DocUtils.getDecendentNodeByName(blockNode, "attribute:startLine");
		List<Node> endLineNodes = DocUtils.getDecendentNodeByName(blockNode, "attribute:endLine");
		List<Node> startFilePosNodes = DocUtils.getDecendentNodeByName(blockNode, "attribute:startFilePos");
		List<Node> endFilePosNodes = DocUtils.getDecendentNodeByName(blockNode, "attribute:endFilePos");
		
		// relocate all the start/end line and start/end file position nodes
		for (Node startLineNode : startLineNodes) {
			relocateItem(startLineNode, lineOffset);
		}
		
		for (Node endLineNode : endLineNodes) {
			relocateItem(endLineNode, lineOffset);
		}
		
		for (Node startFilePosNode : startFilePosNodes) {
			relocateItem(startFilePosNode, filePosOffset);
		}
		
		for (Node endFilePosNode : endFilePosNodes) {
			relocateItem(endFilePosNode, filePosOffset);
		}
	}
	
	/**
	 * relocate a start/end line and start/end file position node
	 * @param item is a start/end line and start/end file position node to be relocated
	 * @return the new position
	 * @throws Exception 
	 */
	private int relocateItem(Node item, int offset) throws Exception {
		
		if (item == null) {
			System.out.println("[ERROR] relocateItem doesn't take null as input, but "
					+ "item is null");
		}
		
		String itemNodeName = item.getNodeName();
		if (itemNodeName.compareTo("attribute:startLine") != 0 && 
				itemNodeName.compareTo("attribute:endLine") != 0 &&
				itemNodeName.compareTo("attribute:startFilePos") != 0 &&
				itemNodeName.compareTo("attribute:endFilePos") != 0
				) {
			System.out.println("[ERROR] relocateItem only take start/end line and start/end "
					+ "file position node, but item is " + itemNodeName);
		}
		
		int pos = DocUtils.getIntFromNode(DocUtils.getFirstChildWithName(item, "scalar:int"));
		int newPos = pos + offset;
		return newPos;
	}
	
	private void addPosInVarNames(Node blockNode, int position) {
		
		List<Node> varNodes = DocUtils.getDecendentNodeByName(blockNode, "node:Expr_Variable");
		for (Node varNode : varNodes) {
			try {
			Node varNameNode = DocUtils.getFirstChildWithName(varNode, "subNode:name");
			Node stringNode = DocUtils.getFirstChildWithName(varNameNode, "scalar:string");
			String varName = DocUtils.getStringFromNode(stringNode);
			varName = varName + "__" + String.valueOf(position);
			stringNode.setTextContent(varName);
			} catch (Exception e) {
				System.out.println("[ERROR][addPosInVarNames] Bad format in node:Expr_Variable");
				System.out.println(e.getMessage());
				e.printStackTrace();
				
			}
		}
		
	}
	
	
	private void renameVars(Node blockNode, String oldName, String newName) {
		List<Node> varNodes = DocUtils.getDecendentNodeByName(blockNode, "node:Expr_Variable");
		for (Node varNode : varNodes) {
			
			try {
				Node varNameNode = DocUtils.getFirstChildWithName(varNode, "subNode:name");
				Node stringNode = DocUtils.getFirstChildWithName(varNameNode, "scalar:string");
				String varName = DocUtils.getStringFromNode(stringNode);
				if (varName.compareTo(oldName) == 0) {
					stringNode.setTextContent(newName);
				}

			} catch (Exception e) {
				System.out.println("[ERROR][addPosInVarNames] Bad format in node:Expr_Variable");
				System.out.println(e.getMessage());
				e.printStackTrace();

			}
		}
	}
}
