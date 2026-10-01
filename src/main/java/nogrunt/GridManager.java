package nogrunt;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class GridManager {
	
	public static void main(String[] args) {
		getNodeCapabilitiessgql();
		
	}
	
	public int getNoOfThreadsgql() {

		int maxSession = -1;
		
		try {
        
	        String query = "{\"query\": \"{ grid { maxSession, sessionCount } }\"}";
	        HttpResponse<String> response = getResponse(query);
	            
	        if (response.statusCode() == 200) {
	        	String responseBody = response.body();
	            JSONParser parser = new JSONParser();
	            JSONObject jsonObject = (JSONObject) parser.parse(responseBody);
	            JSONObject data = (JSONObject)jsonObject.get("data");
	            JSONObject grid = (JSONObject)data.get("grid");
	            Long ms = (Long)grid.get("maxSession");
	            maxSession = ms.intValue();
	        } else {
	        	System.err.println("Request failed with status code: " + response.statusCode());
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
		
		return maxSession;
	}
	
	public JSONObject getSessionInfo(String sessionid) {

		JSONObject json = new JSONObject();
		
		try {
        
	        String query = "{\"query\":\"{ session (id: \\\"" + sessionid + "\\\") { id, capabilities, startTime, uri, nodeId, nodeUri, sessionDurationMillis, slot { id, stereotype, lastStarted } } } \"}";
	        HttpResponse<String> response = getResponse(query);
	            
	        if (response.statusCode() == 200) {
	        	String responseBody = response.body();
	            JSONParser parser = new JSONParser();
	            JSONObject jsonObject = (JSONObject) parser.parse(responseBody);
	            JSONObject data = (JSONObject)jsonObject.get("data");
	            JSONObject session = (JSONObject)data.get("session");
	            String nodeUri = (String)session.get("nodeUri");
	            
	            json.put("nodeUri", nodeUri);
	        } else {
	        	System.err.println("Request failed with status code: " + response.statusCode());
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
		
		return json;
	}
	
	public JSONObject getMaxAndSessionInfo() {

		JSONObject json = new JSONObject();
		
		try {
        
	        String query = "{\"query\":\"{ grid { maxSession, sessionCount } } \"}";
	        HttpResponse<String> response = getResponse(query);
	            
	        if (response.statusCode() == 200) {
	        	String responseBody = response.body();
	            JSONParser parser = new JSONParser();
	            JSONObject jsonObject = (JSONObject) parser.parse(responseBody);
	            JSONObject data = (JSONObject)jsonObject.get("data");
	            json = (JSONObject)data.get("grid");
	        } else {
	        	System.err.println("Request failed with status code: " + response.statusCode());
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
		
		return json;
	}
	
	public HttpResponse<String> getResponse(String query) {
		HttpResponse<String> response = null;
		try {
			HttpClient httpClient = HttpClient.newHttpClient();
	        
	        String graphqlEndpoint = AppProperties.gqlendpoint;
//	        String query = "{\"query\":\"{ sessionsInfo { sessions { id, slot { id, stereotype, lastStarted } } } }\"}";
	        
	        HttpRequest request = HttpRequest.newBuilder()
	                .uri(URI.create(graphqlEndpoint))
	                .header("Content-Type", "application/json")
	                .POST(HttpRequest.BodyPublishers.ofString(query))
	                .build();
	        response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return response;
	}
	
	public int getAllSessionsgql() {

		int maxSession = -1;
		
		try {	        
	        String query = "{\"query\":\"{ sessionsInfo { sessions { id, slot { id, stereotype, lastStarted } } } }\"}";      
	        HttpResponse<String> response = getResponse(query);	        
	            
	        if (response.statusCode() == 200) {
	        	String responseBody = response.body();	            
	            JSONParser parser = new JSONParser();
	            JSONObject jsonObject = (JSONObject) parser.parse(responseBody);
	            JSONObject data = (JSONObject)jsonObject.get("data");
	            JSONObject grid = (JSONObject)data.get("grid");
	            Long ms = (Long)grid.get("maxSession");
	            maxSession = ms.intValue();
	        } else {
	        	System.err.println("Request failed with status code: " + response.statusCode());
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
		
		return maxSession;
	}
	
	public int getcurrentRequestsgql() {

		int maxSession = -1;
		
		try {	        
	        String query = "{\"query\":\"{ sessionsInfo { sessionQueueRequests } }\"}";      
	        HttpResponse<String> response = getResponse(query);	        
	            
	        if (response.statusCode() == 200) {
	        	String responseBody = response.body();	            
	            JSONParser parser = new JSONParser();
	            JSONObject jsonObject = (JSONObject) parser.parse(responseBody);
	            JSONObject data = (JSONObject)jsonObject.get("data");
	            JSONObject grid = (JSONObject)data.get("grid");
	            Long ms = (Long)grid.get("maxSession");
	            maxSession = ms.intValue();
	        } else {
	        	System.err.println("Request failed with status code: " + response.statusCode());
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
		
		return maxSession;
	}
	
	public static int getNodeCapabilitiessgql() {

		int maxSession = -1;
		
		try {	        
	        String query = "{\"query\": \"{ nodesInfo { nodes { stereotypes } } }\"}";      
	        HttpResponse<String> response = getResponseS(query);	        
	            
	        if (response.statusCode() == 200) {
	        	String responseBody = response.body();	            
	            JSONParser parser = new JSONParser();
	            JSONObject jsonObject = (JSONObject) parser.parse(responseBody);
	            JSONObject data = (JSONObject)jsonObject.get("data");
	            JSONObject grid = (JSONObject)data.get("grid");
	            Long ms = (Long)grid.get("maxSession");
	            maxSession = ms.intValue();
	        } else {
	        	System.err.println("Request failed with status code: " + response.statusCode());
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
		
		return maxSession;
	}
	
	public static HttpResponse<String> getResponseS(String query) {
		HttpResponse<String> response = null;
		try {
			HttpClient httpClient = HttpClient.newHttpClient();
	        
	        String graphqlEndpoint = "http://localhost:4444/graphql";
//	        String query = "{\"query\":\"{ sessionsInfo { sessions { id, slot { id, stereotype, lastStarted } } } }\"}";
	        
	        HttpRequest request = HttpRequest.newBuilder()
	                .uri(URI.create(graphqlEndpoint))
	                .header("Content-Type", "application/json")
	                .POST(HttpRequest.BodyPublishers.ofString(query))
	                .build();
	        response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return response;
	}

}
