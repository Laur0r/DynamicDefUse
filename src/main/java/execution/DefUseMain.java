package execution;

import org.objectweb.asm.ClassReader;
import org.junit.Test;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;

import java.util.Properties;

public class DefUseMain {

	public static void main(String[] args){
		long startTime = System.nanoTime();
		Properties get = System.getProperties();
		JUnitCore junitCore = new JUnitCore();
		//TextListener listener = new TextListener(System.out);
		//junitCore.addListener(listener);
		junitCore.run(Increment.class);
		long endTime   = System.nanoTime();
		long totalTime = (endTime - startTime) /1000000;
		System.out.println(totalTime);
		System.out.println("run through main method");
	}
}
