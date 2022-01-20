package dacite;

import execution.BooleanCounter;
import execution.BooleanCounterTest;
import execution.Increment;
import org.objectweb.asm.ClassReader;
import org.junit.Test;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;

import java.util.Properties;

public class DefUseMain {

	public static void main(String[] args){
		if(args.length > 1){
			System.err.println("More than one argument for main method detected");
		} else if(args.length == 0){
			System.err.println("Required to specify analysisJunittest to analyse data-flow");
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
	}
}
