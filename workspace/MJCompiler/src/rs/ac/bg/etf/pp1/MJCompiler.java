package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import java_cup.runtime.Symbol;
import rs.ac.bg.etf.pp1.ast.Program;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;

public class MJCompiler implements Compiler {

	@Override
	public List<CompilerError> compile(String sourceFilePath, String outputFilePath) {
		
		Logger log = Logger.getLogger(MJCompiler.class);
		List<CompilerError> errorList = new ArrayList<CompilerError>();
		
		if (sourceFilePath.isEmpty() || sourceFilePath == null
				|| outputFilePath.isEmpty() || outputFilePath == null) {
			log.error("Empty string or null supplied to method!");
			return null;
		}
		
		File sourceCode = new File(sourceFilePath);
		if (!sourceCode.exists()) {
			log.error("Source file [" + sourceCode.getAbsolutePath() + "] not found!");
			return null;
		}
		
		log.info("Compiling source file: " + sourceCode.getAbsolutePath());
		
		try (BufferedReader br = new BufferedReader(new FileReader(sourceCode))) {
			
			Yylex lexer = new Yylex(br);
			MJParser p = new MJParser(lexer);
	        Symbol s;
			s = p.parse(); //pocetak parsiranja
	        
	        Program prog = (Program)(s.value);
			// ispis sintaksnog stabla
			//log.info(prog.toString(""));
	        
			Tab.init(); // Universe scope
			SemanticAnalyzer semanticCheck = new SemanticAnalyzer();
			prog.traverseBottomUp(semanticCheck);
	        tsdump();
	        
	        errorList.addAll(lexer.getLexicalErrors());
	        errorList.addAll(p.getSyntaxErrors());
	        errorList.addAll(semanticCheck.getSemanticErrors());
	        
	        if (!p.errorDetected && semanticCheck.passed()) {
	        	File objFile = new File(outputFilePath);
	        	log.info("Generating bytecode file: " + objFile.getAbsolutePath());
	        	if (objFile.exists())
	        		objFile.delete();
	        	
	        	// Code generation...
	        	CodeGenerator codeGenerator = new CodeGenerator();
	        	prog.traverseBottomUp(codeGenerator);
	        	Code.dataSize = semanticCheck.nVars;
	        	Code.mainPc = codeGenerator.getMainPc();
	        	Code.write(new FileOutputStream(objFile));
	        	log.info("Parsiranje uspesno zavrseno!");
	        }
	        else {
	        	log.error("Parsiranje NIJE uspesno zavrseno!");
	        }
	        

	        return errorList;
	        
		} catch (FileNotFoundException e) {
			log.error("File not found exception: ");
			e.printStackTrace();
		} catch (IOException e) {
			log.error("IO exception: ");
			e.printStackTrace();
		}  catch (Exception e) {
			e.printStackTrace();
		}  
		
		return null;
	}
	
	public void tsdump() {
		Tab.dump();
	}
	
	public static void main(String[] args) throws IOException {
		Logger log = Logger.getLogger(MJTest.class);
			
		String inputFile = args[0];
		String outputFile = args[1];
		MJCompiler mjCompiler = new MJCompiler();
		List<CompilerError> errorList = mjCompiler.compile(inputFile, outputFile);
		if(!errorList.isEmpty())
			for(CompilerError err: errorList) {
				log.info(err.toString());
			}
		log.info("Compilation finished!");
			
	}

}
