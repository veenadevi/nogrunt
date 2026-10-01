package nogrunt.payments;

import java.util.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.owasp.encoder.Encode;

import nogrunt.AppProperties;
import nogrunt.Utilities;
import nogrunt.api.ApiHandler;
import nogrunt.*;

import org.json.simple.JSONArray;

public class CashfreeIntegration {
	
	public static void main(String[] args) {
		CashfreeIntegration pi = new CashfreeIntegration();
		pi.callCashfree(null, -1,null, null);
	}
	
	public String callCashfree( JSONObject userdetails, int companyId, 
			JSONObject planDetails, MySQlConn msc) {
				
		try {			
			JSONObject header = createHeader();
			JSONObject body = createBody(userdetails, companyId,planDetails);
			System.err.println(Utilities.getNow());
			System.err.println(body.toJSONString());
			int orderid = createOrder(userdetails, companyId,planDetails, msc, 
					(String)body.get("order_id"));
			            
            String result = ApiHandler.sendRequest(AppProperties.cfurl, "POST", body.toJSONString(), 
            		null, header.toJSONString(), null,null, 0, 0, null);
            
            JSONParser parser = new JSONParser();
            JSONObject cfResult = (JSONObject) parser.parse(result);
            String psid = (String)cfResult.get("payment_session_id");
            activateOrder(orderid, msc, cfResult);
            return psid;
        } catch (Exception e) {
        	System.err.println("Error: " + e.getMessage());        	
        }
		
		return null;
	}
	
	private JSONObject createHeader() {
		JSONObject header = new JSONObject();
		header.put("accept", "application/json");
		header.put("content-type", "application/json");
		header.put("x-api-version", "2023-08-01");
		header.put("x-client-id", AppProperties.cfappid);
		header.put("x-client-secret", AppProperties.cfsecretkey);
		
		return header;
	}
	
	public JSONObject createBody(JSONObject userdetails, int companyId,
			JSONObject planDetails) {
		JSONObject body = new JSONObject();
		
		JSONObject customer = new JSONObject();
		
		String email = (String) userdetails.get("email");
        String emailEncoded = Encode.forHtml(email);
        
        String phoneNumber = (String) userdetails.get("phonenumber");
		String phoneNumberEncoded = Encode.forHtml(phoneNumber);
        
		customer.put("customer_id", String.valueOf(companyId));
		customer.put("customer_email", emailEncoded);
		customer.put("customer_phone", phoneNumberEncoded);		
		body.put("customer_details", customer);
		
		body.put("order_amount", planDetails.get("price"));
		body.put("order_currency", "INR");
		String ngorderid = "Nogrunt" + 
				Utilities.randomGen(AppProperties.NUMBER, 10, false);
		body.put("order_id", ngorderid);

		JSONObject returnurl = new JSONObject();
		String notify_url = "";
		String returl = "";
		if(AppProperties.environment.equalsIgnoreCase("production")) {
			returl = AppProperties.javareacturl + "/payment/verify?order_id=" + ngorderid;
			notify_url = AppProperties.javareacturl + "?action=confirmorder";
		} else {
			returl = AppProperties.ngrokurlFE + "/payment/verify?order_id=" + ngorderid;
			notify_url = AppProperties.ngrokurlBE + "?action=confirmorder";
		}
		returnurl.put("return_url", returl);
		returnurl.put("notify_url", notify_url);
		
		body.put("order_meta", returnurl);
		
		return body;
	}
	
	public int createOrder(JSONObject userdetails, int companyId,
			JSONObject planDetails, MySQlConn msc, String ngoredrid) {
		
		String sql = "INSERT into orders (companyid, userid, nogruntorderid, amount, status,"
				+ "createddate, planid) VALUES (?,?, ?, ?, ?, ?, ?)";
		int pk = -1;
		try {
			PreparedStatement stmt = msc.testCon.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			stmt.setInt(1, companyId);
			stmt.setInt(2, (int)userdetails.get("iduserprofile"));
			stmt.setString(3, ngoredrid);
			stmt.setBigDecimal(4, (BigDecimal)planDetails.get("price"));
			stmt.setString(5, "INITIATED");
			stmt.setTimestamp(6, Utilities.getCurrentTimestamp());
			stmt.setInt(7, (int)planDetails.get("idpricingplans"));
			stmt.execute();
			ResultSet generatedKeys = stmt.getGeneratedKeys();
			
			if(generatedKeys.next()) {
				pk = (generatedKeys.getBigDecimal(1)).intValue();
			} else {
				System.err.println(Utilities.getNow());
				System.err.println("The oredr was not created " + userdetails.toJSONString());
			}
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
		
		return pk;
	}
	
	public void activateOrder(int orderid, MySQlConn msc, JSONObject cfresponse) {
		
		String sql = "UPDATE orders SET pgorderid = ?, activateddate = ?, status = ? where "
				+ "idorders = ?";
		try {
//			String isoDateString = (String)cfresponse.get("created_at");
//			ZonedDateTime zonedDateTime = ZonedDateTime.parse(isoDateString);
//			Timestamp timestamp = Timestamp.from(zonedDateTime.toInstant());
			
			PreparedStatement stmt = msc.testCon.prepareStatement(sql);
			stmt.setString(1, (String)cfresponse.get("cf_order_id"));			
			stmt.setTimestamp(2, Utilities.getCurrentTimestamp());
			stmt.setString(3, (String)cfresponse.get("order_status"));
			stmt.setInt(4, orderid);
			stmt.execute();				
			stmt.close();
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			e.printStackTrace();
		}
	}
	
	public void confirmCFOrder(JSONObject cfresponse, MySQlConn msc, MySqlConn2 msc2) {
		
		JSONObject data = (JSONObject)cfresponse.get("data");
		
		JSONObject order = (JSONObject)data.get("order");
		String ngorderid = (String)order.get("order_id");
		
		completeOrder(ngorderid,data,cfresponse, msc);
		generateUsageLic(ngorderid, data, msc, msc2);
	}
	
	public void generateUsageLic(String ngorderid, JSONObject data, MySQlConn msc, MySqlConn2 msc2) {
		JSONObject customerDetails = (JSONObject)data.get("customer_details");
		String custids = (String)customerDetails.get("customer_id");
		int compId = Integer.valueOf(custids);
		
		JSONObject orderDetails = msc.getOrderByNgorderid(ngorderid, true);
		int planId = (int)orderDetails.get("planid");
		JSONObject planDetails = msc.getPlan(planId);
		int quantity = (int)planDetails.get("quantity");
		
		String usagelic = msc.generateUsageLicense("codegen", compId, "deva@nogrunt.com",
		msc2, quantity);
		
		msc.createLicenseKeyDuringRegistration(compId, usagelic);
	}
	
	public void completeOrder(String ngorderid, JSONObject data, JSONObject cfresponse, 
			MySQlConn msc) {
		System.out.println(cfresponse.toJSONString());		
		
		JSONObject payment = (JSONObject)data.get("payment");
		long cfpaymentidl = (long)payment.get("cf_payment_id");
		String cfpaymentid = String.valueOf(cfpaymentidl);
		String paymentStatus = (String)payment.get("payment_status");
		
		String sql = "UPDATE orders SET pgpaymentid = ?, completionjson = ?, "
				+ "completeddate = ?, status = ? where nogruntorderid = ? "
				+ "and status = 'ACTIVE'";
		
		try {
			PreparedStatement stmt = msc.testCon.prepareStatement(sql);
			stmt.setString(1, cfpaymentid);
			stmt.setString(2, cfresponse.toJSONString());
			stmt.setTimestamp(3, Utilities.getCurrentTimestamp());
			stmt.setString(4, paymentStatus);
			stmt.setString(5, ngorderid);
			stmt.execute();
			stmt.close();			
		} catch (Exception e) {
			System.err.println(Utilities.getNow());
			System.err.println(cfresponse.toJSONString());
			e.printStackTrace();
		}
	}

}
