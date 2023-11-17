package dacite.core.instrumentation;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

public class DaciteAgent {

	static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	static DaciteTransformer transformer;
	static boolean executed = false;
	public static void premain(String agentArgs, Instrumentation inst) {
		if(!executed){
			logger.info("Starting the agent for directory "+ agentArgs);
			executed = true;
			transformer = new DaciteTransformer();
			inst.addTransformer(transformer);
		}
	}

	public static DaciteTransformer getTransformer(){
		return transformer;
	}
}
