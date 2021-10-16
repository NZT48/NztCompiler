package rs.ac.bg.etf.pp1;


import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.util.Log4JUtils;

public class MJTest {

	static {
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}
	
	public static void main(String[] args) throws IOException {
		Logger log = Logger.getLogger(MJTest.class);
			
		String inputFile = "test/test302.mj";
		String outputFile = "test/program.obj";
		MJCompiler mjCompiler = new MJCompiler();
		List<CompilerError> errorList = mjCompiler.compile(inputFile, outputFile);
		if(!errorList.isEmpty())
			for(CompilerError err: errorList) {
				log.info(err.toString());
			}
		log.info("Compilation finished!");
			
	}
	
}
