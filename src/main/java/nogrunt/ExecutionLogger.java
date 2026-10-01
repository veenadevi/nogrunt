package nogrunt;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;
import java.io.*;

import java.sql.PreparedStatement;


public class ExecutionLogger {
	
	private Logger logger;
	private int tcrid;
	private int tcid;
	private MySQlConn msc;
	private String executedBy;
	
	public void setTcrid(int t) {
		tcrid = t;
	}
	
	public int getTcrid() {
		return tcrid;
	}
	
	public void setTcid(int t) {
		tcid = t;
	}
	
	public int getTcid() {
		return tcid;
	}
	
	public void setMsc(MySQlConn m) {
		msc = m;
	}
	
	public MySQlConn getMsc() {
		return msc;
	}
	
	public void setExecutedBy(String s) {
		executedBy = s;
	}
	
	public String getExecutedBy() {
		return executedBy;
	}

    public ExecutionLogger(String loggerName, String logFileName) {
        try {
            logger = Logger.getLogger(loggerName);
            logger.setLevel(Level.INFO);
            FileHandler fh = new FileHandler(logFileName);
            fh.setFormatter(new CustomFormatter());
            logger.addHandler(fh);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logExecution(String msg) {
        logger.info(getNow() + "---" + msg);
        //saveExecutionLog( msg);
    }
    
    public void logExecution(Exception e) {
    	StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();
        logger.info(getNow() + "---" + stackTrace);
        //saveExecutionLog( stackTrace);
    }

    private static class CustomFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return record.getMessage() + System.lineSeparator();
        }
    }
	
	private String getNow() {
		LocalDateTime currentDateTime = LocalDateTime.now();
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
	    String formattedDateTime = currentDateTime.format(formatter);
	    return formattedDateTime;
	}
	
	public void close() {
        for (Handler handler : logger.getHandlers()) {
            handler.close();
        }
    }
	
	public void saveExecutionLog(String msg) {
		if(tcrid == -1 || tcrid == 0) {
			return;
		}
		String sql = "INSERT into testcaseexecutionlog (Test_Case_Results_Id,Executed_By,"
				+ "Executed_Date,message, testcaseid) VALUES (?, ?, ?, ?, ?) ";
		try {
			
			if(msg.length() > 1500) {
				msg = msg.substring(0,1499);
			}
			PreparedStatement stmt = msc.testCon.prepareStatement(sql);
			stmt.setInt(1, tcrid);
			stmt.setString(2, executedBy);
			stmt.setTimestamp(3, Utilities.getCurrentTimestamp());
			stmt.setString(4, msg);
			stmt.setInt(5, tcid);
			stmt.execute();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}
	
	public void deletePreviousPass() {
		String sql = " delete from testcaseexecutionlog where Test_Case_Results_Id < ? and testcaseid  = ?";
		try {
			PreparedStatement stmt = msc.testCon.prepareStatement(sql);
			stmt.setInt(1, tcrid);
			stmt.setInt(2, tcid);
			stmt.execute();
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}

}
