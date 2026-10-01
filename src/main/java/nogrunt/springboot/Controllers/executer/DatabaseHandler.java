package nogrunt.springboot.Controllers.executer;

import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;

import java.sql.*;

@Component
public class DatabaseHandler {

	private static final String URL = "jdbc:mysql://localhost:3306/cta";
	private static final String USER = "deva";
	private static final String PASSWORD = "#Deva12deva";
	private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

	static {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			throw new ExceptionInInitializerError("MySQL JDBC driver not found: " + e.getMessage());
		}
	}

	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(URL, USER, PASSWORD);
	}

	public static JSONArray getActionsForMobileDevice(JSONObject actionsObject) {

		JSONArray actionsList = new JSONArray();
		for (Object key : actionsObject.keySet()) {
			JSONObject actionObj = (JSONObject) actionsObject.get(key);

			if (actionObj != null) {
				JSONObject entryJson = new JSONObject();

				JSONObject dataStep = new JSONObject(actionObj);

				String action = (String) actionObj.get("Action");
				String Keyword = (String) actionObj.get("Keyword");
				String elementId = (String) actionObj.get("element_id");
				Integer Test_Step = (Integer) actionObj.get("idtest_step");
				Integer Step_Number = (Integer) actionObj.get("Step_Number");
				Integer test_case_id = (Integer) actionObj.get("test_case_id");
				String filename = (String) actionObj.get("filename");
				String testdata_source = (String) actionObj.get("testdata_source");
				String fileField = (String) actionObj.get("fileField");
				String page_Description = (String) actionObj.get("Page_Description");
				String TestData = (String) actionObj.get("TestData");
				String regex = (String) actionObj.get("regex");
				String customerEmail = (String) actionObj.get("customerEmail");
				String customerPassword = (String) actionObj.get("customerPassword");
				String EmailSelectionCriteria = (String) actionObj.get("EmailSelectionCriteria");
				String EmailFilter = (String) actionObj.get("EmailFilter");

				if (action != null) {
					entryJson.put("action", action);
				}
				if (Keyword != null) {
					entryJson.put("Keyword", Keyword);
				}
				if (elementId != null) {
					entryJson.put("element_id", elementId);
				}
				if (Test_Step != null) {
					entryJson.put("Test_Step", Test_Step);
				}
				if (Step_Number != null) {
					entryJson.put("Step_Number", Step_Number);
				}
				if (test_case_id != null) {
					entryJson.put("test_case_id", test_case_id);
				}
				if (filename != null) {
					entryJson.put("filename", filename);
				}
				if (testdata_source != null) {
					entryJson.put("testdata_source", testdata_source);
				}
				if (fileField != null) {
					entryJson.put("fileField", fileField);
				}
				if (page_Description != null) {
					entryJson.put("page_Description", page_Description);
				}
				if (TestData != null) {
					entryJson.put("TestData", TestData);
				}
				if (regex != null) {
					entryJson.put("regex", regex);
				}
				if (customerEmail != null) {
					entryJson.put("customerEmail", customerEmail);
				}
				if (customerPassword != null) {
					entryJson.put("customerPassword", customerPassword);
				}
				if (EmailSelectionCriteria != null) {
					entryJson.put("EmailSelectionCriteria", EmailSelectionCriteria);
				}
				if (EmailFilter != null) {
					entryJson.put("EmailFilter", EmailFilter);
				}
				entryJson.put("dataStep", dataStep);

				if (action != null && !action.isEmpty()) {
					if ("findAndAssign".equals(action)) {
						String uiautomator = (String) actionObj.get("android_uiautomator");
						String idStrategy = (String) actionObj.get("id_strategy");
						String xpath = (String) actionObj.get("xpath");

						String testData = null;

						if (uiautomator != null && !uiautomator.isEmpty()) {
							testData = uiautomator;
							System.out.println("Using android_uiautomator: " + testData);
						} else if (idStrategy != null && !idStrategy.isEmpty()) {
							testData = idStrategy;
							System.out.println("Using id_strategy: " + testData);
						} else if (xpath != null && !xpath.isEmpty()) {
							testData = xpath;
							System.out.println("Using xpath: " + testData);
						} else {
							System.out.println("All strategies are null or empty. Skipping action.");
						}
						if (testData != null && !testData.isEmpty()) {
							if (!testData.trim().startsWith("{")) {
								testData = "{\"params\":{\"strategy\":\"fallback\",\"selector\":\""
										+ testData.replace("\"", "\\\"") + "\"}}";
							}
						}
						entryJson.put("testdata", testData);
					} else if ("swipe".equals(action)) {
						String strategy_map = (String) actionObj.get("strategy_map");
						String testdata = null;

						if (strategy_map != null && !strategy_map.isEmpty()) {
							if (!strategy_map.trim().startsWith("{")) {
								testdata = "{\"params\":{\"strategy\":\"default\",\"data\":\""
										+ strategy_map.replace("\"", "\\\"") + "\"}}";
							} else {
								testdata = strategy_map;
							}
							System.out.println("Using strategy_map: " + testdata);
						}
						entryJson.put("testdata", testdata);
					} else if ("tap".equals(action)) {
						String strategy_map = (String) actionObj.get("strategy_map");
						String testdata = null;

						if (strategy_map != null && !strategy_map.isEmpty()) {
							if (!strategy_map.trim().startsWith("{")) {
								testdata = "{\"params\":{\"strategy\":\"default\",\"data\":\""
										+ strategy_map.replace("\"", "\\\"") + "\"}}";
							} else {
								testdata = strategy_map;
							}
							System.out.println("Using strategy_map: " + testdata);
						}
						entryJson.put("testdata", testdata);
					} else if ("tapAndHold".equals(action)) {
						String strategy_map = (String) actionObj.get("strategy_map");
						String testdata = null;

						if (strategy_map != null && !strategy_map.isEmpty()) {
							if (!strategy_map.trim().startsWith("{")) {
								testdata = "{\"params\":{\"strategy\":\"default\",\"data\":\""
										+ strategy_map.replace("\"", "\\\"") + "\"}}";
							} else {
								testdata = strategy_map;
							}
							System.out.println("Using strategy_map: " + testdata);
						}
						entryJson.put("testdata", testdata);
					} else if ("scroll".equals(action)) {
						String strategy_map = (String) actionObj.get("strategy_map");
						String testdata = null;

						if (strategy_map != null && !strategy_map.isEmpty()) {
							if (!strategy_map.trim().startsWith("{")) {
								testdata = "{\"params\":{\"strategy\":\"default\",\"data\":\""
										+ strategy_map.replace("\"", "\\\"") + "\"}}";
							} else {
								testdata = strategy_map;
							}
							System.out.println("Using strategy_map: " + testdata);
						}
						entryJson.put("testdata", testdata);
					} else if ("uploadfile".equals(action)) {
						String strategy_map = (String) actionObj.get("strategy_map");
						String testdata = null;

						if (strategy_map != null && !strategy_map.isEmpty()) {
							if (!strategy_map.trim().startsWith("{")) {
								testdata = "{\"params\":{\"strategy\":\"default\",\"data\":\""
										+ strategy_map.replace("\"", "\\\"") + "\"}}";
							} else {
								testdata = strategy_map;
							}
							System.out.println("Using strategy_map: " + testdata);
						}
						entryJson.put("testdata", testdata);
					} else {
						Object test_data = actionObj.get("test_data");
						if (test_data != null) {
							String testDataStr = test_data.toString();
							if (!testDataStr.trim().startsWith("{")) {
								testDataStr = "{\"params\":{\"value\":\"" + testDataStr.replace("\"", "\\\"") + "\"}}";
							}
							entryJson.put("testdata", testDataStr);
						} else {
							entryJson.put("testdata", null);
						}
					}
					actionsList.put(entryJson);
				}
			}
		}
		return actionsList;
	}

}