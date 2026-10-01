package nogrunt;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class PortManager {

    private static final int START_PORT = 9222;
    private static final Set<Integer> assignedPorts = Collections.synchronizedSet(new HashSet<>());
    private static final Queue<Integer> releasedPorts = new LinkedList<>();

    /**
     * Attempts to assign a port.
     * - First, it checks the standard range.
     * - If none are available, it falls back to recycled ports.
     */
    public static synchronized int assignPort(int maxSessions, ExecutionLogger el) {
        // First, try the standard range
        for (int port = START_PORT; port < START_PORT + maxSessions; port++) {
            if (!assignedPorts.contains(port) && isPortAvailable(port)) {
                assignedPorts.add(port);
                el.logExecution("Assigned Port from Range: " + port);
                return port;
            }
        }

        // If no ports are available in the range, check the recycled ones
        while (!releasedPorts.isEmpty()) {
            int recycledPort = releasedPorts.poll();
            if (isPortAvailable(recycledPort)) {
                assignedPorts.add(recycledPort);
                el.logExecution("Assigned Recycled Port: " + recycledPort);
                return recycledPort;
            }
        }

        el.logExecution("No available ports found in the specified range or recycled pool.");
        throw new RuntimeException("No available ports found in the specified range or recycled pool.");
    }

    /**
     * Releases a port and adds it to the recycled pool for future use.
     */
    public static synchronized void releasePort(int port, ExecutionLogger el) {
        if (assignedPorts.remove(port)) {
            releasedPorts.offer(port);
            el.logExecution("Port Released and Added to Recycled Pool: " + port);
        }
    }

    /**
     * Checks if the specified port is available.
     */
    private static boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Getters for monitoring
     */
    public static synchronized Set<Integer> getAssignedPorts() {
        return Collections.unmodifiableSet(assignedPorts);
    }

    public static synchronized Queue<Integer> getRecycledPorts() {
        return new LinkedList<>(releasedPorts);
    }

    public static void main(String[] args) {
        // Example of a simple ExecutionLogger

//    	int maxSessions = 5;

//        int port1 = assignPort(maxSessions, el);
//        int port2 = assignPort(maxSessions, el);

//        el.logExecution("Currently Assigned Ports: " + getAssignedPorts());

        // Simulate test completion
//        releasePort(port1, el);
//        releasePort(port2, el);

//        el.logExecution("Currently Recycled Ports: " + getRecycledPorts());
//        el.logExecution("Currently Assigned Ports: " + getAssignedPorts());

        // Try assigning again; it should use recycled ports first
//        int port3 = assignPort(maxSessions, el);
//        el.logExecution("Re-assigned Port: " + port3);
//        el.logExecution("Currently Assigned Ports: " + getAssignedPorts());
    }
}
