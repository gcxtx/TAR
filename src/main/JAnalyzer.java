package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import at.ac.tuwien.infosys.www.phpparser.ParseNode;
import at.ac.tuwien.infosys.www.phpparser.ParseTree;
import at.ac.tuwien.infosys.www.pixy.Dumper;
import at.ac.tuwien.infosys.www.pixy.MyOptions;
import funlib.FunctionLibrary;
import utils.DocUtils;



public class JAnalyzer {

	public static int DEBUG_MODE;
	public static FunctionLibrary funlib;

	public static void main(String[] args) throws Exception {

		/*
		DEBUG_MODE = 0;
		funlib = new FunctionLibrary();

		try {
			String phpParserDir = "/home/ruoyu/PROJECTS/TOOLS/PHP-Parser/bin";
			String jaDir = "/home/ruoyu/PROJECTS/JAnalyzer";
			String phpFileName = "sample1.php";
			String phpParserCmd = "php " + phpParserDir + "/php-parse.php --serialize-xml " + jaDir + "/" + phpFileName
					+ " | sed -n '1,2!p' > " + jaDir + "/ast.xml";
			Process p = Runtime.getRuntime().exec(new String[] { "bash", "-c", phpParserCmd });
			Thread.sleep(100);

			// read the xml file
			File xmlFile = new File(jaDir + "/ast.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();

			Element root = doc.getDocumentElement();

			// System.out.println("Root element :" + root.getNodeName());

			// Initialize the PhpFile
			// ASSUMPTION: root always has a direct child "scalar:array" which
			// contains a list of statements
			PhpFile phpFile = new PhpFile(DocUtils.getFirstChildWithName(root, "scalar:array"), null, null);
			phpFile.printPhpFile();

			VCGenerator generator = new VCGenerator(phpFile, "JAnalyzer.smt2");
			generator.generate();
			Thread.sleep(100);

			p = Runtime.getRuntime()
					.exec(new String[] { "bash", "-c",
							"/home/ruoyu/PROJECTS/JAnalyzer/cvc4-2014-04-18-x86_64-linux-opt /home/ruoyu/PROJECTS/JAnalyzer/JAnalyzer.smt2 > "
									+ jaDir + "/result" });
			Thread.sleep(100);

			System.out.println("RESULT>>>>>>>>>>>>>>>>>>>>>");
			BufferedReader br = new BufferedReader(new FileReader(jaDir + "/result"));
			String line = null;
			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}
			

		} catch (Exception e) {

			e.printStackTrace();

		}
		*/
	}

	// return true when the vulnerability is valid
	public static boolean analyze(String filename, ParseTree parseTree, Set<Integer> sinks) throws Exception {
		
		boolean ret = true;
		
		DEBUG_MODE = 0;
		funlib = new FunctionLibrary();
		
	    try {
	    	String jaDir = "/home/ruoyu/PROJECTS/JAnalyzer";
	    	
	    	ParseNode root = parseTree.getRoot();
	    	//Dumper.dumpDot(parseTree, jaDir, "parseTree");
	    	utils.ParseTreeUtils.printParseNode(root, "");
	    	
	    	AST ast = new AST(parseTree.getRoot());
	    	
	    	/*
	    	Preprocess preprs = new Preprocess(ast);
	    	preprs.process();
	    	*/
	    	
	    	PhpFile phpFile = new PhpFile(ast, sinks);
	    	phpFile.printPhpFile();
	    	
	    	
	    	/*
	    	Preprocess preprs = new Preprocess(doc);
	    	preprs.process();
	    	
	    	
	    	// output to a file, for debugging purpose only
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File("/home/ruoyu/PROJECTS/JAnalyzer/preprocess.xml"));
			transformer.transform(source, result);
	    	
	    	Element root = doc.getDocumentElement();
	    	*/
	    	//System.out.println("Root element :" + root.getNodeName());    
	    	 	
	    	// Initialize the PhpFile
	    	// ASSUMPTION: root always has a direct child "scalar:array" which contains a list of statements
	    	
	    	
	    	/*
	    	PhpFile phpFile = new PhpFile(parseTree, sinks);
	    	phpFile.printPhpFile();
	    	
	    	VCGenerator generator = new VCGenerator(phpFile, jaDir + "/JAnalyzer.smt2");
	    	generator.generate();
	    	Thread.sleep(1000);
	    	
	    	
	    	p = Runtime.getRuntime().exec(new String[]{"bash","-c", "/home/ruoyu/PROJECTS/JAnalyzer/cvc4-2014-04-18-x86_64-linux-opt " + jaDir + "/JAnalyzer.smt2 > " + jaDir + "/result"});
	    	Thread.sleep(200);
	    	
	    	System.out.println("RESULT>>>>>>>>>>>>>>>>>>>>>");
	    	 BufferedReader br = new BufferedReader(new FileReader(jaDir + "/result"));
	    	 String line = br.readLine();
	    	 
	    	 if (line.charAt(0) == 'u') ret = false;
	    	 
	    	 while (line != null) {
	    	   System.out.println(line);
	    	   line = br.readLine();
	    	 }
	    	 */
	    	
	    	/*
	    	System.out.println("Output:>>>>>>>>>>>>>");
	    	OutputStream os = p.getOutputStream();
	    	PrintStream prtOs = new PrintStream(os);
	    	prtOs.println();
            
	    	System.out.println("Error:>>>>>>>>>>>>>");
	    	InputStream  es = p.getErrorStream();
	    	PrintStream prtEs = new PrintStream(os);
	    	prtEs.println();
			*/
	    	
	    } catch (Exception e) {
	    	
	    	e.printStackTrace();
	    	return ret;
	        
	    }
	    
	    return ret;
	}

}
