package nogrunt.payments;

import java.util.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

import org.json.simple.JSONObject;

import nogrunt.api.ApiHandler;

import org.json.simple.JSONArray;

public class PhonepeIntegration {
	
	public static void main(String[] args) {
		PhonepeIntegration pi = new PhonepeIntegration();
		pi.callPhonepe();
	}
	
	public void callPhonepe() {
				
		try {
			String env = null;
			JSONObject reqObj = createRequest(env);
			String req = reqObj.toJSONString();
			String encodedString = (String)reqObj.get("request");
			
            // Create a MessageDigest instance for SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            String strForHash = "";
            if(env == null || (env != null && !env.equalsIgnoreCase("prod"))) {
            	strForHash = encodedString + "/pg/v1/pay" +
            		"b88b8050-c868-4a63-a944-644f5a679e60";
            } else {
            	strForHash = encodedString + "/pg/v1/pay" +
                		"4cb7aa4b-8c46-40de-a71a-3ba00e94adff";
            }
            
            // Perform the hashing
            byte[] hashBytes = digest.digest(strForHash.getBytes());
            
            // Convert the byte array to a hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            // Output the hashed string
            System.out.println("SHA-256 Hash: " + hexString.toString());
            
            String forVerify = hexString.toString() + "###" + 1;
            
            JSONObject headers = new JSONObject();
            headers.put("Content-Type", "application/json");
            headers.put("X-VERIFY", forVerify);
            
//            String params = "[" + payload + "]";
            
            
            
            String url = "https://api.phonepe.com/apis/hermes/pg/v1/pay";
            url = "https://api-preprod.phonepe.com/apis/pg-sandbox/pg/v1/pay";
            
            ApiHandler.sendRequest(url, "POST", req, null, headers.toJSONString(), null, 
            		null, 0, 0, null);
            
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (Exception e) {
        	System.err.println("Error: " + e.getMessage());
        }
	}
	
	public JSONObject createRequest(String env) {
		JSONObject payload = new JSONObject();
		
		if(env == null || (env != null && !env.equalsIgnoreCase("prod"))) {
	        payload.put("merchantId", "PGTESTPAYUAT141");
	//      payload.put("merchantId", "PGTESTPAYUAT");
	        payload.put("merchantTransactionId", "MT0000000000000009");
	        payload.put("merchantUserId", "deva@nogrunt.com");
	        payload.put("amount", 101);
	        payload.put("redirectUrl", "https://b513-49-206-3-114.ngrok-free.app");
	        payload.put("redirectMode", "REDIRECT");
	        payload.put("callbackUrl", "https://b513-49-206-3-114.ngrok-free.app");
	        payload.put("mobileNumber", "9845189733");
		} else {
			payload.put("merchantId", "M22ADGU1LPUBV");
//	        payload.put("merchantId", "PGTESTPAYUAT");
	        payload.put("merchantTransactionId", "MT0000000000000008");
	        payload.put("merchantUserId", "deva@nogrunt.com");
	        payload.put("amount", 101);
	        payload.put("redirectUrl", "https://b513-49-206-3-114.ngrok-free.app");
	        payload.put("redirectMode", "REDIRECT");
	        payload.put("callbackUrl", "https://b513-49-206-3-114.ngrok-free.app");
	        payload.put("mobileNumber", "9845189733");
		}
        
        // Create the paymentInstrument object
        JSONObject paymentInstrument = new JSONObject();
        paymentInstrument.put("type", "PAY_PAGE");
        
        // Add paymentInstrument to the payload
        payload.put("paymentInstrument", paymentInstrument);
        
        // Step 2: Convert the JSON payload to a Base64 encoded string
        String jsonString = payload.toString();
        String base64EncodedPayload = Base64.getEncoder().encodeToString(jsonString.getBytes(StandardCharsets.UTF_8));
        
        // Step 3: Create the final JSON request
        JSONObject finalRequest = new JSONObject();
        finalRequest.put("request", base64EncodedPayload);
        
        // Step 4: Print the final request or send it to the API
        System.out.println("Final Request JSON: " + finalRequest.toString());
        return finalRequest;
	}

}
