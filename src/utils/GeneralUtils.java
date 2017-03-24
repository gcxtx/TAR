package utils;

import java.util.Map;
import java.util.Set;

import main.PhpExpr;
import main.PhpExprType;

public class GeneralUtils {
	
	public static void updateEIHashMap(Map<PhpExpr, Integer> src, Map<PhpExpr, Integer> dst) throws Exception {
		
		if (src == null || dst == null) {
			
			throw new Exception("The maps can't be null");
			
		}
		
		for (PhpExpr var : src.keySet()) {
			
			dst.put(var, src.get(var));
			
		}
		
		return;
		
	}
	
	public static void updateEEHashMap(Map<PhpExpr, PhpExpr> src, Map<PhpExpr, PhpExpr> dst) throws Exception {
		
		if (src == null || dst == null) {
			
			throw new Exception("The maps can't be null");
			
		}
		
		for (PhpExpr var : src.keySet()) {
			
			dst.put(var, src.get(var));
			
		}
		
		return;
		
	}
	
public static void updateSTHashMap(Map<String, PhpExprType> src, Map<String, PhpExprType> dst) throws Exception {
		
		if (src == null || dst == null) {
			
			throw new Exception("The maps can't be null");
			
		}
		
		for (String var : src.keySet()) {
			
			dst.put(var, src.get(var));
			
		}
		
		return;
		
	}
	
	public static boolean SetContainsVar(Set<PhpExpr> set, PhpExpr targetVar, boolean nameOnly) {
		
		if (set == null || set.size() == 0 || targetVar == null) return false;
		
		for (PhpExpr var : set) {
			if (var.getString(nameOnly).compareTo(targetVar.getString(nameOnly)) == 0) return true;
		}
		
		return false;
		
	}

}
