package main;

public class UDChain {
	
	private PhpFile file;
	private Stmt targetStmt;
	
	public UDChain(PhpFile newFile, Stmt newTarget) throws Exception {
		
		SetPhpFile(newFile);
		SetTargetStmt(newTarget);
		
	}
	
	public void SetPhpFile(PhpFile newFile) throws Exception {
		
		if (newFile == null) {
			
			throw new Exception("Can't set file of UDChain to null");
		}
		
		else {
			
			file = newFile;
			
		}
		
	}
	
	public PhpFile getPhpFile() {
		
		return file;
		
	}
	
	public void SetTargetStmt(Stmt newTarget) throws Exception {
		
		if (newTarget == null) {
			
			throw new Exception("Can't set file of UDChain to null");
		}
		
		else {
			
			targetStmt = newTarget;
			
		}
		
	}
	
	public Stmt getTargetStmt() {
		
		return targetStmt;
		
	}

}
