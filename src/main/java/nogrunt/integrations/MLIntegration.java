package nogrunt.integrations;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import nogrunt.*;
import java.util.*;

public class MLIntegration {
	
	public JSONArray getImpactedTC(int prodid, MySqlConn2 msc2, JSONArray comments) {
		
		JSONObject prodIntDets = msc2.getIntegrationdetail(prodid, "Impact Analysis");
		String  conn = (String)prodIntDets.get("adminprop");
		if(conn == null) {
			return new JSONArray();
		}
		JSONArray jsonArray = new JSONArray();
		String[] connDetails = conn.split(",");
		
		for(int i=0; i<comments.size(); i++) {
			String comment = (String)comments.get(i);
			comment = comment.substring(comment.indexOf(":") + 3, comment.length());
			comment = "Give me all scenarios that are impacted by this comment --> " + comment;
            Object tcList = ApiAccess.postToApi(connDetails[0], null, null,"{\"query\":\"" + comment + "}", 
            		null, null, null);
//            for(int j=0; j<tcList.size(); j++) {
//            	String tc = tcList.get(i);            	
//            }
		}
		return jsonArray;
	}
	
}
