package nogrunt;

import java.util.regex.*;

public class RegexUtil {
	
	public boolean validateRegex(String input, String regex) {
		Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
		
		if (matcher.matches()) {
	        return true;
	    } else {
	    	return false;
	    }
	}

}
