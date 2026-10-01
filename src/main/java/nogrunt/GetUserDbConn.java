package nogrunt;

import nogrunt.dbconn.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class GetUserDbConn {

	static HashMap issuesMap = new HashMap();

	public Connection testCon;
	
	public String getCustomerDbConn(MySQlConn msc, String sql, int tsid, 
			ExecutionLogger el) throws SQLException {
		
		String result = null;
		try {
			
			int prodid = msc.getProdIdFromTestStepId(tsid);
			JSONObject custDbDetails = msc.getNamedCustProp(prodid, AppProperties.custdbcreds);
        
			String value = (String)custDbDetails.get("value");
			String[] components = value.split(";");
			
			String custType = components[0];			
			String custUrl = components[1];
			String custUsername = components[2];
			String custPwd = components[3]; 
			
			el.logExecution("DB type " + custType);
			el.logExecution("DB URL " + custUrl);
			el.logExecution("DB username " + custUsername);
			el.logExecution("SQL being run " + sql);

			if(custType.equalsIgnoreCase("MySQL")) {
	            Class.forName(AppProperties.MysqldatabaseDriver);
	            testCon = DriverManager.getConnection(custUrl, custUsername, custPwd);
			} else if(custType.equalsIgnoreCase("Oracle")) {
	            Class.forName(AppProperties.OracledatabaseDriver);
	            testCon = DriverManager.getConnection(custUrl, custUsername, custPwd);
			} else if(custType.equalsIgnoreCase("MsSQL")) {
	            Class.forName(AppProperties.MssqldatabaseDriver);
		        testCon = DriverManager.getConnection(custUrl, custUsername, custPwd);
			} else if(custType.equalsIgnoreCase("MongoDB")) {
	            MongoDbConn mdc = new MongoDbConn();
	            result = mdc.readFromMongodb(custUrl, custUsername, custPwd, sql, el);
	            return result;
			}

            
			PreparedStatement stmt_exec = testCon.prepareStatement(sql);
			ResultSet rs2 = stmt_exec.executeQuery();
 		    
            while (rs2.next()) {
            	result  = rs2.getString(1);
            	el.logExecution("1st element received from DB " + result);
	         }
            
	         rs2.close();
	         stmt_exec.close();
	         testCon.close();
        } catch (ClassNotFoundException ex) {
            System.out.println("Database driver not found");
            ex.printStackTrace();
         } catch (SQLException ex) {
            System.out.println("Failed to create Database connection");
            ex.printStackTrace();
            throw ex;
         }
		
		return result;
	}

}
