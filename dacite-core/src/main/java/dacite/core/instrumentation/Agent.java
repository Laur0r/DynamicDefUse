package dacite.core.instrumentation;

import java.lang.instrument.Instrumentation;
import java.nio.file.FileSystems;
import java.nio.file.Paths;

public class Agent {

	static boolean executed = false;
	public static void premain(String agentArgs, Instrumentation inst) {
		if(!executed){
			System.out.println("Starting the agent for directory "+ agentArgs);
			executed = true;
			Transformer transformer = new Transformer();
			transformer.setDir(agentArgs);
			inst.addTransformer(transformer);

		}
	}
}
