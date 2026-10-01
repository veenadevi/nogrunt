package nogrunt;

import java.sql.SQLException;

public class CleanUpQueueReader extends Thread {

    private final CleanUpQueueHelper queueHelper;
    private MySQlConn msc;

    public CleanUpQueueReader(CleanUpQueueHelper helper) {
        this.queueHelper = helper;
        msc = new MySQlConn(null);
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                Long testcaseResultId = queueHelper.readFromQueue();
                if (testcaseResultId != null && AppProperties.isCleanUpOn.equalsIgnoreCase("true")) {
                	try {
					    // Attempt to use the connection for some operation
					    msc.testCon.prepareStatement("SELECT 1").executeQuery();
					} catch (SQLException e) {
				        System.out.println("clean up reader Connection OPENING NEW ONE");
				        msc = new MySQlConn(null);
					}
                    System.out.println("Cleaning testcaseResultId: " + testcaseResultId);
                    
                    CleanUpService cleanUpService = new CleanUpService(msc);
                    cleanUpService.cleanTestcaseByResultId(testcaseResultId);
                }
            } catch (Exception e) {
                System.err.println("Error in CleanupReader: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
