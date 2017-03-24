package main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class VCGenerator {
	
	private PhpFile inputFile;
	private String outputFileName;
	
	public VCGenerator(PhpFile newInputFile, String newOutputFileName) {
		
		inputFile = newInputFile;
		outputFileName = newOutputFileName;
		
	}
	
	public PhpFile getInputFile() {
		
		return inputFile;
		
	}
	
	public void setInputFile(PhpFile newInputFile) {
		
		inputFile = newInputFile;
		
	}
	
	public String getOutputFileName() {
		
		return outputFileName;
		
	}
	
	public void setOutputFileName(String newOutputFileName) {
		
		outputFileName = newOutputFileName;
		
	}
	
	public void generate() throws Exception {
		
		// head of the smt file
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));
		writer.write("(set-logic QF_S)\n");
		writer.write("(set-option :strings-exp true)\n");
		writer.write("(set-option :produce-models true)\n");
		
		// generate program
		
		writer.write(inputFile.toSmtLib());
		
		// end of the smt file
		writer.write("(check-sat)\n");
		//writer.write("(get-model)\n");
		
		writer.close();
		
	}

}
