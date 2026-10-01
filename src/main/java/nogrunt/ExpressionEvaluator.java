package nogrunt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import groovy.lang.GroovyShell;

public class ExpressionEvaluator {

	private static MySQlConn msc = new MySQlConn(null);
	
	public static boolean evaluate(int tcrid, String expression, ConcurrentHashMap<String, String> varMap,
			ExecutionLogger el) {
		boolean result = true;
		if(expression.contains("stepspassed")){
			el.logExecution("Found stepspassed in the expression");
			Pattern pattern = Pattern.compile("stepspassed\\(([^)]*)\\)");
	        Matcher matcher = pattern.matcher(expression);

	        while (matcher.find()) {
	            String allValues = matcher.group(1);
	            String[] values = allValues.split(",");
	            for (String val : values) {
	            	el.logExecution("Checking if stepnum : " + val + " has passed for current run");
	            	String status = msc.getTestStepStatus(tcrid, Integer.valueOf(val));
	    			if(status.equals("") || status.equalsIgnoreCase("FAIL")) {
	    				el.logExecution("stepnum : " + val + " has failed for current run - so returning false for expression");
	    				return false;
	    			} else {
	    				el.logExecution("stepnum : " + val + " has passed for current run - so continuing to evaluate next expressions");
	    			}
	            }
	        }
	        
	        expression = matcher.replaceAll("true");
		}
		if(expression.contains("stepsfailed")){
			el.logExecution("Found stepsfailed in the expression");
			Pattern pattern = Pattern.compile("stepsfailed\\(([^)]*)\\)");
	        Matcher matcher = pattern.matcher(expression);

	        while (matcher.find()) {
	            String allValues = matcher.group(1);
	            String[] values = allValues.split(",");
	            for (String val : values) {
	            	el.logExecution("Checking if stepnum : " + val + " has failed for current run");
	            	String status = msc.getTestStepStatus(tcrid, Integer.valueOf(val));
	    			if(status.equals("") || status.equalsIgnoreCase("PASS")) {
	    				el.logExecution("stepnum : " + val + " has passed for current run - so returning false for expression");
	    				return false;
	    			} else {
	    				el.logExecution("stepnum : " + val + " has failed for current run - so continuing to evaluate next expressions");
	    			}
	            }
	        }
	        
	        expression = matcher.replaceAll("true");
		}
		
		Pattern pattern = Pattern.compile("~[^~]+~");
        Matcher matcher = pattern.matcher(expression);
        el.logExecution("Checking to see if the expression has any variables whose values need to be replaced at runtime");

        while (matcher.find()) {
        	String var = matcher.group();
        	el.logExecution("found variable : " + var);
            if(varMap.containsKey(var)) {
            	String value = varMap.get(var);
            	el.logExecution("Replacing variable with value : " + value);
            	expression = expression.replaceAll(Pattern.quote(var), value);
            	el.logExecution("The update expression looks like : " + expression);
            }
        }
		
		result = evaluateExpression(expression, el);
		return result;
	}
	
    public static boolean evaluateExpression(String expression, ExecutionLogger el) {
        GroovyShell shell = new GroovyShell();
        el.logExecution("Evaluating the expression : " + expression);
        Object result = shell.evaluate(expression);
        if (result instanceof Boolean) {
        	el.logExecution("The expression evaluated to  : " + (Boolean) result);
            return (Boolean) result;
        } else {
        	el.logExecution("This is an issue - The expression did not return a boolean: " + result);
            return false;
        }
    }
    
}
