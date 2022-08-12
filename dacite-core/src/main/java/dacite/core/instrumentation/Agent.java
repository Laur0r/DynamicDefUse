package dacite.core.instrumentation;

import java.lang.instrument.Instrumentation;

public class Agent {
	public static void premain(String agentArgs, Instrumentation inst) {
		System.out.println("Starting the agent");
		System.out.println(agentArgs);
		Transformer transformer = new Transformer();
		transformer.setDir(agentArgs);
		inst.addTransformer(transformer);
	}
}
