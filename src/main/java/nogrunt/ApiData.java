package nogrunt;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ApiData {
	
	public static boolean areApiCallsEquivalent(String url1, String url2) {
        String base1 = getBasePath(url1);
        String base2 = getBasePath(url2);

        if (!base1.equals(base2)) return false;

        Map<String, String> params1 = getQueryParams(url1);
        Map<String, String> params2 = getQueryParams(url2);
        
        for(String key:params1.keySet()) {
        	String val1 = params1.get(key);
        	if(params2.containsKey(key)) {
        		String val2 = params2.get(key);
        		if(!val2.equals("*") && !val2.equals(val1)) {
        			return false;
        		}
        	} else {
        		return false;
        	}
        }

        return true;
    }
	
	private static String getBasePath(String url) {
		try {
			URI uri = new URI(url);
	        String host = uri.getHost();
	        int port = uri.getPort();
	        String path = uri.getPath();
	
	        // Print host and port
	        System.out.println("Host: " + host);
	        System.out.println("Port: " + (port == -1 ? "default" : port));
	
	        return path;
		} catch (Exception e) {
			
		}
        int index = url.indexOf('?');
        return index == -1 ? url : url.substring(0, index);
    }
	
	private static Map<String, String> getQueryParams(String url) {
        Map<String, String> queryPairs = new HashMap<>();
        int questionMarkIndex = url.indexOf('?');
        if (questionMarkIndex == -1) return queryPairs;

        String query = url.substring(questionMarkIndex + 1);
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            System.out.println("Key: " + key);
            System.out.println("Value: " + value);
            queryPairs.put(key, value);
        }
        return queryPairs;
    }

}
