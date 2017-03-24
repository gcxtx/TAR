package utils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import main.PhpExpr;
import main.PhpExprKind;
import main.PhpExprType;

public class PhpExprUtils {
	
	// make variable
	public static PhpExpr mkVar(String newName, int newPos, PhpExprType newType) {
		
		PhpExpr ret = new PhpExpr();
		ret.setTop(newName);
		ret.setPosition(newPos);
		
		ret.setExprType(newType);
		ret.setExprKind(PhpExprKind.VAR);
		
		return ret;
	}
	
	// make conjunction
	// IMPORTANT: new expression doesn't inherent taint condition
	public static PhpExpr mkAnd(List<PhpExpr> newSubExprs) throws Exception {
		
		Iterator<PhpExpr> itr = newSubExprs.iterator();
		while (itr.hasNext()) {
			
			PhpExpr expr = itr.next();
			if (expr == null || expr.getTop() == "false") {
				return mkFalse();
			}
			
			if (expr.getTop() == "true") {
				itr.remove();
			}
			
		}
		
		if (newSubExprs.size() == 0) {
			
			return mkTrue();
			
		} else if (newSubExprs.size() == 1) {
			
			return newSubExprs.get(0);
			
		}
		
		PhpExpr ret = new PhpExpr();
		ret.setTop("and");
		ret.setSubExprs(newSubExprs);
		
		ret.setExprType(PhpExprType.BOOL);
		ret.setExprKind(PhpExprKind.COMP);

		return ret;
		
	}
	
	// make disjunction
	// IMPORTANT: new expression doesn't inherent taint condition
	public static PhpExpr mkOr(List<PhpExpr> newSubExprs) throws Exception {
		
		Iterator<PhpExpr> itr = newSubExprs.iterator();
		while (itr.hasNext()) {
			
			PhpExpr expr = itr.next();
			if (expr == null || expr.getTop() == "false") {
				itr.remove();
			}
			
			if (expr.getTop() == "true") {
				return mkTrue();
			}
			
		}
		
		if (newSubExprs.size() == 0) {
			
			return mkFalse();
			
		} else if (newSubExprs.size() == 1) {
			
			return newSubExprs.get(0);
			
		}
		
		PhpExpr ret = new PhpExpr();
		ret.setTop("or");
		ret.setSubExprs(newSubExprs);
		
		ret.setExprType(PhpExprType.BOOL);
		ret.setExprKind(PhpExprKind.COMP);

		return ret;
		
	}
	
	public static PhpExpr mkNot(PhpExpr newSubExpr) throws Exception {
		
		PhpExpr ret = new PhpExpr();
		ret.setTop("not");
		ret.setSubExprs(Arrays.asList(newSubExpr));
		
		ret.setExprType(PhpExprType.BOOL);
		ret.setExprKind(PhpExprKind.COMP);
		
		ret.setTaintCondition(newSubExpr.getTaintCondition());

		return ret;
		
	}
	
	// make boolean constant true
	public static PhpExpr mkTrue() {
		
		PhpExpr ret = new PhpExpr();
		ret.setTop("true");
		
		ret.setExprType(PhpExprType.BOOL);
		ret.setExprKind(PhpExprKind.CONS);

		return ret;
	}
	
	// make boolean constant false
	public static PhpExpr mkFalse() {
		
		PhpExpr ret = new PhpExpr();
		ret.setTop("false");
		
		ret.setExprType(PhpExprType.BOOL);
		ret.setExprKind(PhpExprKind.CONS);

		return ret;
	}
	
	public static PhpExpr funToVar(PhpExpr funCall) {
		if (funCall.getExprKind() != PhpExprKind.FUN) {
			System.out.println("[ERROR/funToVar@PhpExprUtils] Can not generate variable from a non function call");
			return null;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(funCall.getTop());
		
		for (PhpExpr subExpr : funCall.getSubExprs()) {
			
			sb.append("_" + subExpr.getString(true));
			
		}
		
		PhpExpr retVar = mkVar(sb.toString(), funCall.getPosition(), funCall.getExprType());
		retVar.parentStmt = funCall.parentStmt;
		retVar.assignedVar = false;
		return retVar;
	}

}
