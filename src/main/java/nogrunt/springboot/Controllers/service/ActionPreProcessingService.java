package nogrunt.springboot.Controllers.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nogrunt.springboot.Controllers.component.QueueHelper;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ActionPreProcessingService {

	private final ObjectMapper objectMapper;

	@Autowired
	private final QueueHelper queueingHelper;

	public ActionPreProcessingService(ObjectMapper objectMapper, QueueHelper queueingHelper) {
		this.objectMapper = objectMapper;
		this.queueingHelper = queueingHelper;
	}

	public Map<String, Object> preprocessAction(Map<String, Object> actionMap) throws Exception {
		String action = (String) actionMap.get("action");
		Integer extKey = (Integer) actionMap.get("extKey");
		Integer testCaseId = (Integer) actionMap.get("testCaseId");
		Integer sequence = (Integer) actionMap.get("sequenceNumber");
		List<Object> params = (List<Object>) actionMap.get("params");
		List<List<String>> strategyMap = (List<List<String>>) actionMap.get("strategyMap");

		if (action == null) {
			action = detectActionFromParams(params);
		}

		String element = null;
		if (strategyMap != null && !strategyMap.isEmpty() && strategyMap.get(0).size() > 1) {
			String value = strategyMap.get(0).get(1);

			if (value.contains("UiSelector")) {
				Pattern pattern;
				Matcher matcher;

				pattern = Pattern.compile("description\\(\"([^\"]*)\"\\)");
				matcher = pattern.matcher(value);
				if (matcher.find()) {
					element = matcher.group(1);
				}

				pattern = Pattern.compile("text\\(\"([^\"]*)\"\\)");
				matcher = pattern.matcher(value);
				if (matcher.find()) {
					element = matcher.group(1);
				}
			} else {
				element = strategyMap.get(0).get(1);
			}
		}

		Map<String, Object> processedAction;

		switch (action.toLowerCase()) {
		case "swipe":
			processedAction = processSwipeAction(action, params, actionMap);
			break;
		case "findandassign":
			processedAction = processFindAndAssignAction(action, params, strategyMap);
			break;
		case "uploadfile":
			processedAction = processUploadFile(action, params);
			break;
		case "click":
			processedAction = processClickAction(action, params, strategyMap);
			break;
		case "sendkeys":
			processedAction = processSendKeysAction(action, params, strategyMap);
			break;
		case "scroll":
			processedAction = processScrollAction(action, params);
			break;
		case "tap":
			processedAction = processTapAction(action, params, actionMap);
			break;
		default:
			throw new UnsupportedOperationException("Unsupported action: " + action);
		}

//        if (!("swipe").equals(action) && !("tap").equals(action) && ("scroll").equals(action) && ("uploadFile").equals(action)) {
		// Only non-coordinate based actions need the strategy map processed
		if (!("swipe".equals(action) || "tap".equals(action) || "scroll".equals(action)
				|| "uploadFile".equals(action))) {
			if (strategyMap != null) {
				processedAction.put("strategyMap", processStrategyMap(strategyMap));
			}
		}

		processedAction.put("timestamp", LocalDateTime.now());
		processedAction.put("testCaseId", testCaseId);
		processedAction.put("extKey", extKey);
		processedAction.put("sequenceNumber", sequence);

		processedAction.put("element", element);

		JSONObject actionJson = new JSONObject(processedAction);
		queueingHelper.addToQueue(actionJson);

		return processedAction;
	}

	private String detectActionFromParams(List<Object> params) {
		if (params == null || params.isEmpty()) {
			return null;
		}

		for (Object param : params) {
			if (param instanceof Map) {
				Map<String, Object> paramMap = (Map<String, Object>) param;

				if (paramMap.containsKey("actions")) {
					List<Map<String, Object>> actions = (List<Map<String, Object>>) paramMap.get("actions");

					if (actions == null || actions.isEmpty())
						continue;

					boolean hasPointerDown = actions.stream().anyMatch(a -> "pointerDown".equals(a.get("type")));
					boolean hasPointerUp = actions.stream().anyMatch(a -> "pointerUp".equals(a.get("type")));
					boolean hasPointerMove = actions.stream().anyMatch(a -> "pointerMove".equals(a.get("type")));

					if (hasPointerDown && hasPointerUp && hasPointerMove) {
						return "tap";
					}

					boolean hasSwipeMove = actions.stream().filter(a -> "pointerMove".equals(a.get("type")))
							.anyMatch(a -> (int) a.getOrDefault("duration", 0) > 0);

					if (hasSwipeMove && hasPointerDown && hasPointerUp) {
						return "swipe";
					}

					long verticalMoves = actions.stream().filter(a -> "pointerMove".equals(a.get("type")))
							.filter(a -> a.containsKey("y")).count();

					if (verticalMoves >= 2) {
						return "scroll";
					}
				}
			}
		}
		return null;
	}

	private Map<String, Object> processFindAndAssignAction(String action, List<Object> params,
			List<List<String>> strategyMap) throws JsonProcessingException {
		validateParams(params, 3, "findAndAssign");
		String elementId = (String) params.get(2);

		// Process the strategyMap
		String processedStrategyMap = processStrategyMap(strategyMap);
		Map<String, Object> strategyColumns = populateStrategyColumns(strategyMap);

		// Prepare the result
		Map<String, Object> result = new HashMap<>();
		result.put("action", action);
		result.put("elementId", elementId);
		result.put("strategyMap", processedStrategyMap);
		result.putAll(strategyColumns);

		return result;
	}

	private String processStrategyMap(List<List<String>> strategyMap) {
		try {
			List<Map<String, Object>> transformedList = new ArrayList<>();

			for (List<String> strategyPair : strategyMap) {
				if (strategyPair.size() == 2) {
					String strategy = strategyPair.get(0);
					String selector = strategyPair.get(1);

					Map<String, Object> transformedEntry = Map.of("params",
							Map.of("strategy", strategy, "selector", selector));

					transformedList.add(transformedEntry);
				}
			}

			return objectMapper.writeValueAsString(transformedList);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return "[]";
		}
	}

	private Map<String, Object> populateStrategyColumns(List<List<String>> strategyMap) {
		Map<String, Object> strategyColumns = new HashMap<>();
		strategyColumns.put("android_uiautomator", null);
		strategyColumns.put("xpath", null);
		strategyColumns.put("id_strategy", null);

		if (strategyMap != null) {
			for (List<String> strategyItem : strategyMap) {
				if (strategyItem.size() == 2) {
					String strategy = strategyItem.get(0);
					String selector = strategyItem.get(1);

					String columnName = getColumnNameForStrategy(strategy);
					if (columnName != null) {
						Map<String, Object> strategyJson = Map.of("params",
								Map.of("strategy", strategy, "selector", selector));
						try {
							strategyColumns.put(columnName, objectMapper.writeValueAsString(strategyJson));
						} catch (JsonProcessingException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		return strategyColumns;
	}

	private String getColumnNameForStrategy(String strategy) {
		switch (strategy.toLowerCase()) {
		case "-android uiautomator":
			return "android_uiautomator";
		case "xpath":
			return "xpath";
		default:
			return "id_strategy";
		}
	}

//	private Map<String, Object> processSwipeAction(String action, List<Object> params) throws JsonProcessingException {
//
//		Map<String, Object> swipeData = (Map<String, Object>) params.get(2);
//
//		List<Map<String, Object>> finger1 = (List<Map<String, Object>>) swipeData.get("finger1");
//		if (finger1 == null || finger1.isEmpty()) {
//			throw new IllegalArgumentException("Missing or empty 'finger1' data in swipe action payload: " + swipeData);
//		}
//
//		List<Map<String, Object>> simplifiedFinger1 = new ArrayList<>();
//		for (Map<String, Object> fingerAction : finger1) {
//			Map<String, Object> filteredAction = new HashMap<>();
//			filteredAction.put("type", fingerAction.get("type"));
//
//			if (fingerAction.containsKey("x") && fingerAction.containsKey("y")) {
//				filteredAction.put("x", fingerAction.get("x"));
//				filteredAction.put("y", fingerAction.get("y"));
//			}
//
//			if (fingerAction.containsKey("duration")) {
//				filteredAction.put("duration", fingerAction.get("duration"));
//			}
//
//			if (fingerAction.containsKey("button")) {
//				filteredAction.put("button", fingerAction.get("button"));
//			}
//
//			if (fingerAction.containsKey("origin")) {
//				filteredAction.put("origin", fingerAction.get("origin"));
//			}
//
//			simplifiedFinger1.add(filteredAction);
//		}
//
//		Map<String, Object> selectorInfo = (params.size() > 1 && params.get(1) != null)
//				? (Map<String, Object>) params.get(1)
//				: new HashMap<>();
//
//		String strategy = (String) selectorInfo.getOrDefault("strategy", "");
//		String selector = (String) selectorInfo.getOrDefault("selector", "");
//
//		Map<String, Object> finalSwipeData = new HashMap<>();
//
//		Map<String, Object> paramMap = new HashMap<>();
//		paramMap.put("strategy", strategy);
//		paramMap.put("selector", selector);
//		paramMap.put("finger1", simplifiedFinger1);
//
//		finalSwipeData.put("params", paramMap);
//
//		Map<String, Object> result = new HashMap<>();
//		result.put("action", action);
//		result.put("strategyMap", objectMapper.writeValueAsString(finalSwipeData));
//		result.put("elementId", null);
//
//		return result;
//	}
	private Map<String, Object> processSwipeAction(String action, List<Object> params, Map<String, Object> actionMap)
			throws JsonProcessingException {
		if (params.size() < 3 || !(params.get(2) instanceof Map)) {
			throw new IllegalArgumentException("Insufficient or invalid parameters for swipe action");
		}

		Map<String, Object> swipeData = (Map<String, Object>) params.get(2);
		List<Map<String, Object>> finger1 = (List<Map<String, Object>>) swipeData.get("finger1");

		Map<String, Object> windowSizeMap = (Map<String, Object>) actionMap.get("windowSize");
		int screenWidth = 1080;
		int screenHeight = 2340;

		if (windowSizeMap != null) {
			screenWidth = (int) windowSizeMap.getOrDefault("width", 1080);
			screenHeight = (int) windowSizeMap.getOrDefault("height", 2340);
		}

		List<Map<String, Object>> actions = new ArrayList<>();
		for (Map<String, Object> fingerAction : finger1) {
			String type = (String) fingerAction.get("type");
			Map<String, Object> actionItem = new HashMap<>();
			actionItem.put("type", type);

			if ("pointerMove".equals(type)) {
				int x = (int) fingerAction.get("x");
				int y = (int) fingerAction.get("y");
				double xRatio = x / (double) screenWidth;
				double yRatio = y / (double) screenHeight;
				actionItem.put("duration", fingerAction.getOrDefault("duration", 0));
				actionItem.put("xRatio", xRatio);
				actionItem.put("yRatio", yRatio);
			} else if ("pause".equals(type)) {
				actionItem.put("duration", fingerAction.getOrDefault("duration", 100));
			} else if ("pointerDown".equals(type) || "pointerUp".equals(type)) {
				actionItem.put("button", fingerAction.getOrDefault("button", 0));
			}

			actions.add(actionItem);
		}

		Map<String, Object> pointerAction = new HashMap<>();
		pointerAction.put("type", "pointer");
		pointerAction.put("id", "finger1");
		pointerAction.put("parameters", Map.of("pointerType", "touch"));
		pointerAction.put("actions", actions);

		Map<String, Object> finalSwipeData = new HashMap<>();
		finalSwipeData.put("actions", List.of(pointerAction));

		Map<String, Object> finalData = new HashMap<>();
		finalData.put("params", finalSwipeData);

		Map<String, Object> result = new HashMap<>();
		result.put("action", action);
		result.put("strategyMap", objectMapper.writeValueAsString(finalData));
		result.put("elementId", null);

		return result;
	}

//    private Map<String, Object> processTapAction(String action, List<Object> params) throws JsonProcessingException {
//        if (params.size() < 3 || !(params.get(2) instanceof Map)) {
//            throw new IllegalArgumentException("Insufficient or invalid parameters for tap action");
//        }
//
//        Map<String, Object> tapData = (Map<String, Object>) params.get(2);
//        List<Map<String, Object>> finger1 = (List<Map<String, Object>>) tapData.get("finger1");
//
//        List<Map<String, Object>> actions = new ArrayList<>();
//        for (Map<String, Object> fingerAction : finger1) {
//            String type = (String) fingerAction.get("type");
//
//            Map<String, Object> actionItem = new HashMap<>();
//            actionItem.put("type", type);
//
//            if ("pointerMove".equals(type)) {
//                actionItem.put("duration", fingerAction.getOrDefault("duration", 0));
//                actionItem.put("x", fingerAction.get("x"));
//                actionItem.put("y", fingerAction.get("y"));
//            } else if ("pause".equals(type)) {
//                actionItem.put("duration", fingerAction.getOrDefault("duration", 100));
//            } else if ("pointerDown".equals(type) || "pointerUp".equals(type)) {
//                actionItem.put("button", fingerAction.getOrDefault("button", 0));
//            }
//
//            actions.add(actionItem);
//        }
//
//        Map<String, Object> pointerAction = new HashMap<>();
//        pointerAction.put("type", "pointer");
//        pointerAction.put("id", "finger1");
//        pointerAction.put("parameters", Map.of("pointerType", "touch"));
//        pointerAction.put("actions", actions);
//
//        Map<String, Object> finalTapData = new HashMap<>();
//        finalTapData.put("actions", List.of(pointerAction));
//
//        Map<String, Object> finalData = new HashMap<>();
//        finalData.put("params", finalTapData);
//
//        Map<String, Object> result = new HashMap<>();
//        result.put("action", action);
//        result.put("strategyMap", objectMapper.writeValueAsString(finalData));
//        result.put("elementId", null);
//
//        return result;
//    }

	private Map<String, Object> processTapAction(String action, List<Object> params, Map<String, Object> actionMap)
			throws JsonProcessingException {
		if (params.size() < 3 || !(params.get(2) instanceof Map)) {
			throw new IllegalArgumentException("Insufficient or invalid parameters for tap action");
		}

		Map<String, Object> tapData = (Map<String, Object>) params.get(2);
		List<Map<String, Object>> finger1 = (List<Map<String, Object>>) tapData.get("finger1");

		Map<String, Object> windowSizeMap = (Map<String, Object>) actionMap.get("windowSize");
		int screenWidth = 1080;
		int screenHeight = 2340;

		if (windowSizeMap != null) {
			screenWidth = (int) windowSizeMap.getOrDefault("width", 1080);
			screenHeight = (int) windowSizeMap.getOrDefault("height", 2340);
		}

		List<Map<String, Object>> actions = new ArrayList<>();
		for (Map<String, Object> fingerAction : finger1) {
			String type = (String) fingerAction.get("type");
			Map<String, Object> actionItem = new HashMap<>();
			actionItem.put("type", type);

			if ("pointerMove".equals(type)) {
				int x = (int) fingerAction.get("x");
				int y = (int) fingerAction.get("y");
				double xRatio = x / (double) screenWidth;
				double yRatio = y / (double) screenHeight;
				actionItem.put("duration", fingerAction.getOrDefault("duration", 0));
				actionItem.put("xRatio", xRatio);
				actionItem.put("yRatio", yRatio);
			} else if ("pause".equals(type)) {
				actionItem.put("duration", fingerAction.getOrDefault("duration", 100));
			} else if ("pointerDown".equals(type) || "pointerUp".equals(type)) {
				actionItem.put("button", fingerAction.getOrDefault("button", 0));
			}

			actions.add(actionItem);
		}

		Map<String, Object> pointerAction = new HashMap<>();
		pointerAction.put("type", "pointer");
		pointerAction.put("id", "finger1");
		pointerAction.put("parameters", Map.of("pointerType", "touch"));
		pointerAction.put("actions", actions);

		Map<String, Object> finalTapData = new HashMap<>();
		finalTapData.put("actions", List.of(pointerAction));

		Map<String, Object> finalData = new HashMap<>();
		finalData.put("params", finalTapData);

		Map<String, Object> result = new HashMap<>();
		result.put("action", action);
		result.put("strategyMap", objectMapper.writeValueAsString(finalData));
		result.put("elementId", null);

		return result;
	}

	private Map<String, Object> processClickAction(String action, List<Object> params, List<List<String>> strategyMap) {
		validateParams(params, 1, "click");

		String elementId = params.get(0) != null ? params.get(0).toString() : null;
		String processedStrategyMap = strategyMap != null ? processStrategyMap(strategyMap) : "[]";

		Map<String, Object> result = new HashMap<>();
		result.put("action", action);
//        result.put("testData", testData); // Explicitly add null if required
		result.put("testData", null);
		result.put("elementId", elementId);
		result.put("strategyMap", processedStrategyMap);

		return result;
	}

	private Map<String, Object> processSendKeysAction(String action, List<Object> params,
			List<List<String>> strategyMap) throws JsonProcessingException {
		validateParams(params, 3, "sendKeys");

		String elementId = params.get(0) != null ? params.get(0).toString() : null;
		String inputText = params.get(2) != null ? params.get(2).toString() : null;
		String processedStrategyMap = strategyMap != null ? processStrategyMap(strategyMap) : "[]";

		Map<String, Object> sendKeysData = new HashMap<>();
		sendKeysData.put("value", inputText);
		sendKeysData.put("elementId", elementId);

		Map<String, Object> result = new HashMap<>();
		result.put("action", action);
		result.put("testData", convertToJson(Map.of("params", sendKeysData)));
		result.put("elementId", elementId);
		result.put("strategyMap", processedStrategyMap);

		return result;
	}

	private Map<String, Object> processScrollAction(String action, List<Object> params) throws JsonProcessingException {
		validateParams(params, 2, "scroll");

		Map<String, Object> scrollData = (Map<String, Object>) params.get(2);
		String targetText = (String) scrollData.get("targetText");

		Map<String, Object> finalScrollData = new HashMap<>();
		finalScrollData.put("params", Map.of("targetText", targetText));

		Map<String, Object> result = new HashMap<>();
		result.put("action", action);
		result.put("strategyMap", objectMapper.writeValueAsString(finalScrollData));
		result.put("elementId", null);
		return result;
	}

	private Map<String, Object> processUploadFile(String action, List<Object> params) throws JsonProcessingException {

		Map<String, Object> fileUploadData = (Map<String, Object>) params.get(2);

		String fileName = (String) fileUploadData.get("fileName");

		String filePath = (String) fileUploadData.get("filePath");

		Map<String, Object> paramsMap = new HashMap<>();
		paramsMap.put("filePath", filePath);

		Map<String, Object> finalData = new HashMap<>();
		finalData.put("params", paramsMap);

		Map<String, Object> result = new HashMap<>();
		result.put("action", action);
		result.put("elementId", fileName);
		result.put("strategyMap", objectMapper.writeValueAsString(finalData));

		return result;
	}

	private Map<String, Object> processCameraAction(String action, List<Object> params, List<List<String>> strategyMap)
			throws JsonProcessingException {
		validateParams(params, 3, "camera");

		Map<String, Object> cameraData = (Map<String, Object>) params.get(2);
		String cameraAction = (String) cameraData.get("cameraAction");
		String elementId = (String) cameraData.get("elementId");

		Map<String, Object> cameraParams = new HashMap<>();
		cameraParams.put("cameraAction", cameraAction);

		if (cameraData.containsKey("quality")) {
			cameraParams.put("quality", cameraData.get("quality"));
		}
		if (cameraData.containsKey("flash")) {
			cameraParams.put("flash", cameraData.get("flash"));
		}
		if (cameraData.containsKey("frontCamera")) {
			cameraParams.put("frontCamera", cameraData.get("frontCamera"));
		}

		String processedStrategyMap = strategyMap != null ? processStrategyMap(strategyMap) : "[]";

		Map<String, Object> result = new HashMap<>();
		result.put("action", action);
		result.put("testData", objectMapper.writeValueAsString(Map.of("params", cameraParams)));
		result.put("elementId", elementId);
		result.put("strategyMap", processedStrategyMap);

		return result;
	}

	private List<Map<String, Object>> simplifyFingerActions(List<Map<String, Object>> fingerActions) {
		List<Map<String, Object>> simplifiedActions = new ArrayList<>();
		for (Map<String, Object> action : fingerActions) {
			Map<String, Object> filteredAction = new HashMap<>();
			if (action.containsKey("x"))
				filteredAction.put("x", action.get("x"));
			if (action.containsKey("y"))
				filteredAction.put("y", action.get("y"));
			filteredAction.put("duration", action.getOrDefault("duration", 100));
			simplifiedActions.add(filteredAction);
		}
		return simplifiedActions;
	}

	private void validateParams(List<Object> params, int requiredSize, String action) {
		if (params.size() < requiredSize) {
			throw new IllegalArgumentException("Insufficient parameters for " + action + " action");
		}
	}

	private String convertToJson(Object data) {
		try {
			return objectMapper.writeValueAsString(data);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return "{}";
		}
	}

}
