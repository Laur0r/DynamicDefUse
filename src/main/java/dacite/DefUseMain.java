package dacite;

import defuse.DefUseAnalyser;
import execution.BooleanCounter;
import execution.BooleanCounterTest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import execution.Increment;
import org.apache.maven.doxia.sink.Sink;
import org.objectweb.asm.ClassReader;
import org.junit.Test;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import defuse.DefUseChains;

import java.io.File;
import java.util.Properties;

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
		JAXBContext jaxbContext = null;
		try {

			// Normal JAXB RI
			//jaxbContext = JAXBContext.newInstance(Fruit.class);

			// EclipseLink MOXy needs jaxb.properties at the same package with Fruit.class
			// Alternative, I prefer define this via eclipse JAXBContextFactory manually.
			jaxbContext = JAXBContext.newInstance(defuse.DefUseChains.class);

			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			// output to a xml file
			jaxbMarshaller.marshal(DefUseAnalyser.chains, new File("defuse.xml"));

			// output to console
			// jaxbMarshaller.marshal(o, System.out);

		} catch (JAXBException e) {
			e.printStackTrace();
		}

	}
}
