package nogrunt;

import java.sql.*;
import java.time.LocalDateTime;

public class CleanUpService {

    private MySQlConn msc;
    
    int n = AppProperties.cleanupRetentionCount;
    int d = AppProperties.cleanupRetentionDays;

    public CleanUpService(MySQlConn msc) {
        this.msc = msc;
    }

    public void cleanTestcaseByResultId(Long testcaseResultId) {
        Connection conn = null;
        
        Timestamp cleanupStart = Timestamp.valueOf(LocalDateTime.now());
        int lastDeletedResultId = -1;
        String cleanUpComments = "";
        int testCaseId = -1;

        try {
            conn = msc.testCon;
            
            // Step 1: Get Test_Case_Id and company id
            PreparedStatement ps1 = conn.prepareStatement(
                "SELECT Test_Case_Id FROM cta.test_case_results WHERE idtest_case_results = ?"
            );
            ps1.setLong(1, testcaseResultId);
            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) {
                testCaseId = rs1.getInt("Test_Case_Id");
            } else {
                System.out.println("No Test_Case_Id found for idtest_case_results: " + testcaseResultId);
                return;
            }
            ps1.close();
            rs1.close();
            
            int companyId = -1;
            PreparedStatement psCompanyId = conn.prepareStatement(
                "SELECT comp.idcompany " +
                "FROM cta.test_case tc " +
                "JOIN cta.modules mods ON tc.Module = mods.idmodules " +
                "JOIN cta.products prod ON mods.product = prod.idproducts " +
                "JOIN cta.company comp ON prod.company = comp.idcompany " +
                "WHERE tc.idtest_case = ?"
            );
            psCompanyId.setLong(1, testCaseId);
            ResultSet rsCompanyId = psCompanyId.executeQuery();
            if (rsCompanyId.next()) {
                companyId = rsCompanyId.getInt("idcompany");
            }
            rsCompanyId.close();
            psCompanyId.close();

            // Step 2: Find latest PASS result to preserve
            Long lastPassResultId = null;
            PreparedStatement ps2 = conn.prepareStatement(
                "SELECT idtest_case_results FROM cta.test_case_results " +
                "WHERE Test_Case_Id = ? AND Status = 'PASS' " +
                "ORDER BY Executed_Date DESC LIMIT 1"
            );
            ps2.setLong(1, testCaseId);
            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) {
                lastPassResultId = rs2.getLong("idtest_case_results");
            }
            ps2.close();
            rs2.close();
            
            cleanUpComments += "last passed result Id: " + lastPassResultId;

            // Step 3: Compute threshold date
            Timestamp thresholdDate = Timestamp.valueOf(LocalDateTime.now().minusDays(d));

            // Step 4: Determine cutoff result ID
            Long cutoffResultId = null;
            PreparedStatement ps3 = conn.prepareStatement(
                "(" +
                    "SELECT idtest_case_results, Executed_Date FROM cta.test_case_results " +
                    "WHERE Test_Case_Id = ? AND Executed_Date < ? " +
                    "ORDER BY Executed_Date DESC LIMIT 1" +
                ") " +
                "UNION ALL " +
                "(" +
                    "SELECT idtest_case_results, Executed_Date FROM cta.test_case_results " +
                    "WHERE Test_Case_Id = ? AND Status IN ('PASS', 'FAIL')" +
                    "ORDER BY Executed_Date DESC LIMIT 1 OFFSET ?" +
                ") " +
                "ORDER BY Executed_Date ASC LIMIT 1"
            );
            ps3.setLong(1, testCaseId);
            ps3.setTimestamp(2, thresholdDate);
            ps3.setLong(3, testCaseId);
            ps3.setInt(4, n);

            ResultSet rs3 = ps3.executeQuery();
            if (rs3.next()) {
                cutoffResultId = rs3.getLong("idtest_case_results");
            }
            ps3.close();
            rs3.close();

            if (cutoffResultId == null) {
                System.out.println("No eligible rows found for deletion.");
                return;
            }
            
            cleanUpComments += "; ";
            cleanUpComments += "cutoff result Id: " + cutoffResultId;

            // Step 5: Iterate over rows to delete based on result ID
            PreparedStatement ps4 = conn.prepareStatement(
                "SELECT idtest_case_results FROM cta.test_case_results " +
                "WHERE Test_Case_Id = ? AND idtest_case_results < ? " +
                (lastPassResultId != null ? "AND idtest_case_results <> ? " : "") +
                "ORDER BY idtest_case_results ASC"
            );
            ps4.setLong(1, testCaseId);
            ps4.setLong(2, cutoffResultId);
            if (lastPassResultId != null) {
                ps4.setLong(3, lastPassResultId);
            }

            ResultSet rs4 = ps4.executeQuery();
            
            int deletedCount = 0;

            while (rs4.next()) {
                int resultId = rs4.getInt("idtest_case_results");
                lastDeletedResultId = resultId;
                PreparedStatement delStmt;
                
                // 1. delete folder based on resultId
    			String TCRDir = Utilities.getTCRDir(companyId, testCaseId, resultId);
    			Utilities.deleteFolderContents(TCRDir);
    			System.out.println(Utilities.getCurrentTimestamp() + "Folder deleted for - test case result : " +  resultId);

                // 2. Delete related test_step_results entries
                delStmt = conn.prepareStatement(
                    "DELETE FROM cta.test_step_result WHERE Test_Case_Results_Id = ?"
                );
                delStmt.setLong(1, resultId);
                int rowsDeleted = delStmt.executeUpdate();
                System.out.println(Utilities.getCurrentTimestamp() + "test_step_result rows deleted for - test case result : " +  resultId + 
                		" | Rows deleted: " + rowsDeleted);
                
                // 3. delete row from test_suite_case_results as foreign key
                delStmt = conn.prepareStatement(
                	    "DELETE FROM cta.test_suite_case_results where test_case_results_id = ?"
                	);
                delStmt.setLong(1, resultId);
                rowsDeleted = delStmt.executeUpdate();
                System.out.println(Utilities.getCurrentTimestamp() + "test_suite_case_results row deleted for - test case result : " +  resultId + 
                	    " | Rows deleted: " + rowsDeleted);
                
                // 4. delete rows from testcaseexecutionlog as foreign key
                delStmt = conn.prepareStatement(
                	    "DELETE FROM cta.testcaseexecutionlog WHERE Test_Case_Results_Id = ?"
                	);
                delStmt.setLong(1, resultId);
                rowsDeleted = delStmt.executeUpdate();
                System.out.println(Utilities.getCurrentTimestamp() + "testcaseexecutionlog rows deleted for - test case result : " +  resultId + 
                	    " | Rows deleted: " + rowsDeleted);
                
                // 5. Delete from test_case_results
                delStmt = conn.prepareStatement(
                    "DELETE FROM cta.test_case_results WHERE idtest_case_results = ?"
                );
                delStmt.setLong(1, resultId);
                delStmt.executeUpdate();
                delStmt.close();
                System.out.println(Utilities.getCurrentTimestamp() + "test_case_results row deleted for - test case result : " +  resultId);
                
                deletedCount++;
            }

            rs4.close();
            ps4.close();
            
            cleanUpComments += "; ";
            cleanUpComments += "total deleted test case result rows : " + deletedCount;

            System.out.println("Cleanup complete. Total result id rows deleted: " + deletedCount);

        } catch (Exception e) {
            System.err.println("Error in CleanUpService: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (lastDeletedResultId != -1) {
                try {
                    PreparedStatement psFinalLog = msc.testCon.prepareStatement(
                        "UPDATE cta.test_case SET lastCleanUpTry = ?, lastDeletedResultId = ?, cleanUpComments = ? WHERE idtest_case = ?"
                    );
                    psFinalLog.setTimestamp(1, cleanupStart);
                    psFinalLog.setInt(2, lastDeletedResultId);
                    psFinalLog.setString(3, cleanUpComments);
                    psFinalLog.setInt(4, testCaseId);
                    psFinalLog.executeUpdate();
                    psFinalLog.close();
                } catch (Exception e2) {
                    System.err.println("Error while logging cleanup info: " + e2.getMessage());
                }
            }
        }
    }
}
