package nogrunt;

import java.util.concurrent.ConcurrentHashMap;

public class LoggerRegistry {
	
	private static final ConcurrentHashMap<String, ExecutionLogger> loggerMap = new ConcurrentHashMap<>();

    public static void registerLogger(String id, ExecutionLogger logger) {
        loggerMap.put(id, logger);
    }

    public static ExecutionLogger getLogger(String id) {
        return loggerMap.get(id);
    }

    public static void removeLogger(String id) {
        loggerMap.remove(id);
    }

    public static boolean containsLogger(String id) {
        return loggerMap.containsKey(id);
    }

}
