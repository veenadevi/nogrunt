package nogrunt;

import java.net.URLDecoder;
import java.security.SecureRandom;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.json.simple.JSONObject;

public class UserActivities {
	
	public JSONObject authenticate(String jsonstr, MySQlConn msc) {
		try {
			
			JSONParser parser = new JSONParser();
			String decodeStr = URLDecoder.decode(jsonstr, "UTF-8");
			JSONObject authDetails = (JSONObject)parser.parse(decodeStr);
			return authenticate(authDetails, msc);
		} catch (Exception e) {
			e.printStackTrace();
		}
		JSONObject success = new JSONObject();
			success.put("message", "fail");
		return success;
	}
	
	public JSONObject refresh(String jsonstr, MySQlConn msc) {
		
		String uname = "";
		String randomkey = "";
		JSONArray result = null;
		try {			
			JSONParser parser = new JSONParser();
			String decodeStr = URLDecoder.decode(jsonstr, "UTF-8");
			JSONObject authDetails = (JSONObject)parser.parse(decodeStr);
			
//			MySQlConn msc = new MySQlConn();
			uname = (String)authDetails.get("uname");
//			randomkey = (int)authDetails.get("key");
			randomkey = new Long((long)authDetails.get("key")).toString();
			result = msc.refreshUserDetails(uname);
			
//			return authenticate(authDetails);
		} catch (Exception e) {
			e.printStackTrace();
		}
		JSONObject success = new JSONObject();
		success.put("username", uname);
		success.put("randomkey", randomkey);
		success.put("message", "success");
		success.put("products", result);
		return success;
	}

	
	public JSONObject authenticate(JSONObject authDetails, MySQlConn msc) {
		JSONObject success = new JSONObject();
		try {
			String uname = (String)authDetails.get("uname");
			String pwd = (String)authDetails.get("pwd");
            String hashedPwd = PasswordUtils.hashPassword(pwd, AppProperties.hashingsalt);
			JSONObject audit = new JSONObject();
			audit.put("action", "auth");
			audit.put("object", authDetails);
//			MySQlConn msc = new MySQlConn();
			JSONArray result = msc.getUserDetails(uname, hashedPwd);
			if(result == null) {
				success.put("message", "fail");
				audit.put("result", "Fail");
			} 
			JSONObject j1 = (JSONObject) result.get(0);
			int companyId = (int) j1.get("company");			
			String randomkey = "";
			if(result != null && result.size() > 0) {
//				JSONObject j1 = (JSONObject)result.get(0);
				int usertype = (int)j1.get("usertype");
				int userid = (int)j1.get("iduserprofile");
				success.put("usertype", usertype);
				success.put("username", uname);
				if(authDetails.get("key") != null) {
					randomkey = new Long((long)authDetails.get("key")).toString();
				}
				if(randomkey == null || randomkey.equals("")) {
					randomkey = Utilities.randomGen(AppProperties.NUMBER, 8, false);
				}
				success.put("randomkey", randomkey);
					
				audit.put("randomkey", randomkey);						
				audit.put("result", "Success");
				authDetails.put("iduserprofile", userid);
				authDetails.put("companyid", companyId);
				authDetails.put("usertype", usertype);
				authDetails.remove("pwd");
				Utilities.cacheUser(randomkey,authDetails);
			}
			JSONObject Company = msc.getCompany(companyId, randomkey);
			String gentype = (String)Company.get("gentype");
			if(gentype != null && gentype.equalsIgnoreCase("code")) {
				j1.put("gentype",gentype);
			}
			success.put("message", "success");
			success.put("products", result);
			success.put("company", Company);
			
			String configIp = AppProperties.configIp;
			success.put("appiumServerUrl", configIp);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}
	
	
}
