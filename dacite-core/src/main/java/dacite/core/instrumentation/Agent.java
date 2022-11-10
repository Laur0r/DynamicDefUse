package dacite.core.instrumentation;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

public class Agent {

	static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	static boolean executed = false;
	public static void premain(String agentArgs, Instrumentation inst) {
		if(!executed){
			logger.info("Starting the agent for directory "+ agentArgs);
			executed = true;
			Transformer transformer = new Transformer();
			transformer.setDir(agentArgs);
			inst.addTransformer(transformer);

		}
	}
}
