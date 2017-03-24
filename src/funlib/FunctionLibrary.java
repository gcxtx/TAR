package funlib;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import main.PhpExprType;

public class FunctionLibrary {
	private List<FunctionSig> funList;
	
	public FunctionLibrary() throws Exception {
		
		funList = new ArrayList<FunctionSig>();
		
		StringBuilder strlen_sb = new StringBuilder("(= ret (str.len para1))");
		funList.add(new FunctionSig
				("strlen", "str.len", true, PhpExprType.INT, 
						new String[] {"para1"}, new PhpExprType[]{PhpExprType.STR}, strlen_sb));
		
		StringBuilder strconcat_sb = new StringBuilder("(= ret (str.++ para1, para2))");
		funList.add(new FunctionSig
				("concat", "str.++", true, PhpExprType.STR, 
						new String[] {"para1", "para2"}, new PhpExprType[]{PhpExprType.STR, PhpExprType.STR}, strconcat_sb));
		
		
		
	}
	
	public List<FunctionSig> getFunctionList() {
		
		return funList;
		
	}
	
	public FunctionSig getFunctionSig(String name) {
		for (FunctionSig functionSig : funList) {
			if (functionSig.name.compareTo(name) == 0) {
				return functionSig;
			}
		}
		
		return null;
	}
	
	public Set<String> getAllLibSmtNames() {
		
		Set<String> retList = new HashSet<String>();
		for (FunctionSig funSig : funList) {
			retList.add(funSig.smtName);
		}
		
		return retList;
		
	}

}
