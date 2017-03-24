package funlib;

import main.PhpExprType;

public class FunctionSig {
	
	public String name;
	public String smtName;
	public boolean isStatic;
	public PhpExprType returnType;
	public String[] paras;
	public PhpExprType[] paraTypes;
	
	// The template of the function contract
	public StringBuilder functionContract;
	
	public FunctionSig(String new_name, String new_smt_name, boolean new_isStatic, PhpExprType new_returnType, 
			String[] new_paras, PhpExprType[] new_paraTypes, StringBuilder new_functionContract) throws Exception{
		
		if (new_name == null || new_returnType == null || new_paras == null || new_paraTypes == null || new_functionContract == null) {
			
			throw new Exception("FunctionSig constructor doesn't take null");
			
		}
		
		name = new String(new_name);
		smtName = new String(new_smt_name);
		isStatic = new_isStatic;
		returnType = new_returnType;
		
		paras = new_paras.clone();
		paraTypes = new_paraTypes.clone();
		
		functionContract = new StringBuilder(new_functionContract);
		
	}
	
	//[FIXME] Under Construction
	public String GenerateConcreteContract(String retVariable, String[] paras) {
		return "";
	}

}
