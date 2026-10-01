package nogrunt;

import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v135.network.Network;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.v135.network.model.Response;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.openqa.selenium.devtools.v135.network.model.Request;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.MalformedURLException;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import java.net.ServerSocket;

public class Devtools {
	
	int tcid = -1;
	int tcrid = -1;
	int tsid = -1;
	int tsrid = -1;
	int stepnum = -1;
	int attempt = 1;
	private final int START_PORT = 9222;
	private static MySQlConn devtoolsMSC = new MySQlConn(null);
	
	public void captureApi(String urlString, ExecutionLogger el) {
		try {
			String host = getHost(urlString, el);
			String webSocketDebuggerUrl = fetchWebSocketDebuggerUrl("localhost");
	
			// ✅ Open WebSocket Connection
			WebSocket ws = new WebSocketFactory().createSocket(webSocketDebuggerUrl);
			ws.connect();
	
			// ✅ Enable Network Monitoring
			AtomicInteger id = new AtomicInteger(1);
			JSONObject enableNetwork = new JSONObject();
			enableNetwork.put("id", id.getAndIncrement());
			enableNetwork.put("method", "Network.enable");
			ws.sendText(enableNetwork.toJSONString());
	
			// ✅ Store Request Timestamps
			ConcurrentHashMap<String, Long> requestTimestamps = new ConcurrentHashMap<>();

			ConcurrentHashMap<String, Integer> uniqueId = new ConcurrentHashMap<>();
	
			// ✅ Listen for Events
			ws.addListener(new WebSocketAdapter() {
				@Override
				public void onTextMessage(WebSocket websocket, String message) {	
				try {
					JSONParser parser = new JSONParser();
					JSONObject json = (JSONObject) parser.parse(message);
			
					if ("Network.requestWillBeSent".equals(json.get("method"))) {
					   JSONObject params = (JSONObject) json.get("params");
					   String requestId = (String) params.get("requestId");
					   JSONObject request = (JSONObject) params.get("request");
					   String url = (String) request.get("url");
					   requestTimestamps.put(requestId, System.currentTimeMillis());
					   System.out.println("➡️  Request Sent: " + url);
					    
					    // **Check for Origin Header**
	                    JSONObject headers = (JSONObject) request.get("headers");
	                    if (headers.containsKey("Origin")) {
	                        String origin = (String) headers.get("Origin");
	                        el.logExecution("🌐 Origin Header Present: " + origin);
	                    } else {
	                        el.logExecution("⚠️  Origin Header is **missing** for URL: " + url);

	                        // ✅ Fallback to Initiator URL if available
	                        String injectedOrigin = null;
	                        JSONObject initiator = (JSONObject) params.get("initiator");

	                        if (initiator != null && initiator.containsKey("url")) {
	                            String initiatorUrl = (String) initiator.get("url");
	                            URL parsedUrl = new URL(initiatorUrl);
	                            injectedOrigin = parsedUrl.getProtocol() + "://" + parsedUrl.getHost();
	                            el.logExecution("🔍 Found Initiator URL: " + initiatorUrl);
	                            el.logExecution("🌐 Injecting Origin from Initiator: " + injectedOrigin);
	                        } else {
	                            // Fallback option if initiator is also missing
	                            injectedOrigin = "https://defaultdomain.com";
	                            el.logExecution("🔍 Initiator not found. Using fallback Origin: " + injectedOrigin);
	                        }

	                        // ✅ Inject the Origin header into headers
	                        headers.put("Origin", injectedOrigin);

	                        // ✅ Send an override command to CDP
	                        JSONObject headerOverride = new JSONObject();
	                        headerOverride.put("id", id.getAndIncrement());
	                        headerOverride.put("method", "Network.setExtraHTTPHeaders");

	                        JSONObject headerParams = new JSONObject();
	                        headerParams.put("headers", headers);
	                        headerOverride.put("params", headerParams);

	                        // ✅ Send the override to CDP
	                        ws.sendText(headerOverride.toJSONString());
	                    }
					   
					   int uid = devtoolsMSC.insertRequestToDB(url, tcid, 
							   tcrid, tsid, tsrid, stepnum, attempt);
					   uniqueId.put(requestId, uid);
					   devtoolsMSC.updateApiCallCount(tcrid);
					}
			
					if ("Network.responseReceived".equals(json.get("method"))) {
					   JSONObject params = (JSONObject) json.get("params");
					   String requestId = (String) params.get("requestId");
					   JSONObject response = (JSONObject) params.get("response");
					   String url = (String) response.get("url");
					   long statusCode = (long) response.get("status");
					   Long startTime = requestTimestamps.get(requestId);
					   int uniqueid = (int) uniqueId.get(requestId);
			
					   if (startTime != null) {
						   long endTime = System.currentTimeMillis();
					       long responseTime = endTime - startTime;
					       System.out.println("✅ Response Received: " + url);
					       System.out.println("    ↳ Status Code: " + statusCode);
					       System.out.println("    ↳ Response Time: " + responseTime + " ms");
					       System.out.println("-------------------------------------------------");
					       devtoolsMSC.updateApiDataToDB(responseTime, statusCode, endTime, uniqueid);
					       requestTimestamps.remove(requestId);
					   }
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				}
			});
		} catch (Exception e) {
			el.logExecution(e.getMessage());
			el.logExecution(e);
		}
	}
	
	public String fetchWebSocketDebuggerUrl(String host) {
	    try {
	        // Update the URL to the Node's IP and port (9222), not the Grid Hub
	        URL url = new URL("http://" + host + ":9222/json"); // 9222 is the CDP port we enabled
	        JSONParser parser = new JSONParser();
	        try (InputStreamReader reader = new InputStreamReader(url.openStream())) {
	            JSONArray jsonArray = (JSONArray) parser.parse(reader);
	            JSONObject firstObj = (JSONObject) jsonArray.get(0);
	            return (String) firstObj.get("webSocketDebuggerUrl");
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return null;
	}
	
	public String getHost(String urlString, ExecutionLogger el) {
		String host = "";
		try {
			 URL url = new URL(urlString);
			 host = url.getHost();
	         el.logExecution("Extracted host is: " + host);
		 } catch (MalformedURLException e) {
	         e.printStackTrace();
		 }
		
		return host;
	}

    public int findAvailablePort(int maxSessions) {
        for (int port = START_PORT; port < START_PORT + maxSessions; port++) {
            if (isPortAvailable(port)) {
                System.out.println("Available Port Found: " + port);
                return port;
            }
        }
        throw new RuntimeException("No available ports found in the specified range.");
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false; // Port is busy
        }
    }
}

