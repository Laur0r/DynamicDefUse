package dacite.core;

import org.junit.runner.JUnitCore;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import dacite.core.analysis.DaciteAnalyzer;
import dacite.core.analysis.DefUseChain;
import dacite.core.analysis.DefUseField;
import dacite.core.analysis.DefUseVariable;

public class DaciteDynamicExecutor {

	static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	public static void exec(String projectdir, String packagename, String classname) throws Exception {
		long startTime = System.nanoTime();
		JUnitCore junitCore = new JUnitCore();
		packagename = packagename.replace(".","/");
		File file = new File(projectdir+packagename+classname+".java");

		// compile files in path to have all local changes for the execution
		URL url = ClassLoader.getSystemResource(packagename+classname+".class");
		String sourcePath = url.getPath().substring(0,url.getPath().indexOf(packagename));
		List<File> sourceFileList = new ArrayList<File>();
		sourceFileList.add(file);
		File packagedir = new File(projectdir+packagename);
		for(File f: packagedir.listFiles()){
			if(!f.isDirectory() && f.getName().contains(".java")){
				sourceFileList.add(f);
			}
		}
		compileFiles(sourceFileList,sourcePath);

		// run test cases
		try {
			junitCore.run(Class.forName(packagename.replace("/",".")+classname));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		long endTime   = System.nanoTime();
		long totalTime = (endTime - startTime) /1000000;
		logger.info(String.valueOf(totalTime));
		logger.info("run through main method");
		DaciteAnalyzer.check();

		// write xml file of identified DUC
		XMLOutputFactory xof = XMLOutputFactory.newInstance();
		XMLStreamWriter xsw = null;
		try {
			xsw = xof.createXMLStreamWriter(new BufferedOutputStream(new FileOutputStream("coveredDUCs.xml")));
			xsw.writeStartDocument();
			xsw.writeStartElement("DefUseChains");

			for (DefUseChain chain : DaciteAnalyzer.chains.getDefUseChains()) {
				xsw.writeStartElement("DefUseChain");
				xsw.writeStartElement("def");
				parseDefUseVariable(xsw, chain.getDef());
				xsw.writeEndElement();
				xsw.writeStartElement("use");
				parseDefUseVariable(xsw, chain.getUse());
				xsw.writeEndElement();
				xsw.writeEndElement();
			}
			xsw.writeEndElement();
			xsw.writeEndDocument();
			xsw.flush();
			xsw.close();
		}
		catch (Exception e) {
			logger.info("Unable to write the file: " + e.getMessage());
		}

	}

	/**
	 * Parse a DUC as xml
	 * @param xsw XMLStreamWriter
	 * @param var DefUseVariable to parse
	 */
	protected static void parseDefUseVariable(XMLStreamWriter xsw, DefUseVariable var){
		try {
			xsw.writeStartElement("linenumber");
			xsw.writeCharacters(String.valueOf(var.getLinenumber()));
			xsw.writeEndElement();
			xsw.writeStartElement("method");
			xsw.writeCharacters(String.valueOf(var.getMethod()));
			xsw.writeEndElement();
			xsw.writeStartElement("variableIndex");
			xsw.writeCharacters(String.valueOf(var.getVariableIndex()));
			xsw.writeEndElement();
			xsw.writeStartElement("instruction");
			xsw.writeCharacters(String.valueOf(var.getInstruction()));
			xsw.writeEndElement();
			xsw.writeStartElement("variableName");
			xsw.writeCharacters(String.valueOf(var.getVariableName()));
			xsw.writeEndElement();
			if(var instanceof DefUseField){
				xsw.writeStartElement("objectName");
				xsw.writeCharacters(String.valueOf(((DefUseField)var).getInstanceName()));
				xsw.writeEndElement();
			}
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Compile the given list of java files to the given path
	 * @param files list of java files to-be-compiled
	 * @param sourcePath path where the compiled files are saved
	 */
	protected static void compileFiles(List<File> files, String sourcePath){
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager( null, null, null );
		Iterable<? extends JavaFileObject> javaSource = fileManager.getJavaFileObjectsFromFiles( files );
		Iterable<String> options = Arrays.asList("-d", sourcePath, "--release", "11", "-g");
		JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null, javaSource);
		task.call();
	}
}
