package dacite.core;

import org.glassfish.jaxb.runtime.v2.runtime.output.XmlOutput;
import org.junit.runner.JUnitCore;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import java.io.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import dacite.core.defuse.DefUseAnalyser;
import dacite.core.defuse.DefUseChain;
import dacite.core.defuse.DefUseField;
import dacite.core.defuse.DefUseVariable;

public class DefUseMain {

	public static void main(String[] args) throws Exception {
		if(args.length > 1){
			throw new IllegalArgumentException("More than one argument for main method detected");
		} else if(args.length == 0){
			throw new IllegalArgumentException("Required to specify analysisJunittest to analyse data-flow");
		}
		long startTime = System.nanoTime();
		JUnitCore junitCore = new JUnitCore();
		String classname = args[0];
		try {
			junitCore.run(Class.forName(classname));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		long endTime   = System.nanoTime();
		long totalTime = (endTime - startTime) /1000000;
		System.out.println(totalTime);
		System.out.println("run through main method");
		DefUseAnalyser.check();
		// write xml file
		XMLOutputFactory xof = XMLOutputFactory.newInstance();
		XMLStreamWriter xsw = null;
		try {
			xsw = xof.createXMLStreamWriter(new BufferedOutputStream(new FileOutputStream("file.xml")));
			xsw.writeStartDocument();
			xsw.writeStartElement("DefUseChains");

			for (DefUseChain chain : DefUseAnalyser.chains.getDefUseChains()) {
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
			format("file.xml");
		}
		catch (Exception e) {
			System.err.println("Unable to write the file: " + e.getMessage());
		}

	}

	private static void parseDefUseVariable(XMLStreamWriter xsw, DefUseVariable var){
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

	private static void format(String file) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
			Document document = builder.parse(new InputSource(new InputStreamReader(new FileInputStream(file))));

	// Gets a new transformer instance
			Transformer xformer = TransformerFactory.newInstance().newTransformer();
	// Sets XML formatting
			xformer.setOutputProperty(OutputKeys.METHOD, "xml");
	// Sets indent
			xformer.setOutputProperty(OutputKeys.INDENT, "yes");
			Source source = new DOMSource(document);
			StringWriter writer = new StringWriter();
			xformer.transform(source, new StreamResult(writer));
			System.out.println(writer.getBuffer().toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
