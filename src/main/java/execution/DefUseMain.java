package execution;

import org.junit.Test;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;

public class DefUseMain {

	public static void main(String[] args){
		long startTime = System.nanoTime();
		JUnitCore junitCore = new JUnitCore();
		//TextListener listener = new TextListener(System.out);
		//junitCore.addListener(listener);
		junitCore.run(Fibonacci.class);
		long endTime   = System.nanoTime();
		long totalTime = (endTime - startTime) /1000000;
		System.out.println(totalTime);
	}
}
