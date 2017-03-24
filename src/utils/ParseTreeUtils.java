package utils;

import at.ac.tuwien.infosys.www.phpparser.ParseNode;

public class ParseTreeUtils {
	public static void printParseNode(ParseNode node, String prefix) {
		
		if (node == null) {
			System.out.println(prefix + " null");
			return;
		}
		
		System.out.println(prefix + " " + node.getName());
		String newPrefix = ">>" + prefix;
		if (node.getNumChildren() == 0) {
			System.out.println(prefix + "### "  + node.getLexeme());
		}
		for (int i = 0; i < node.getNumChildren(); i++) {
			printParseNode(node.getChild(i), newPrefix);
		}
	}
}
