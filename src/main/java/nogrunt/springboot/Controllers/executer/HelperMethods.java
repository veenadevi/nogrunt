package nogrunt.springboot.Controllers.executer;

import nogrunt.AppProperties;
import nogrunt.ExecutionLogger;
import nogrunt.Utilities;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.net.ConnectException;
import java.util.HashMap;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.json.JSONException;
import org.json.simple.parser.ParseException;

@Component
public class HelperMethods {

	public String saveScreenshot(File screenshot, String ssfilename, int testCaseId, int testCaseResultsId,
			int companyId) {
		try {
			new Thread(() -> {
				try {
					String fsl = Utilities.getScreenShotsDir(companyId, testCaseId, testCaseResultsId) + ssfilename;
					String fullPath = fsl;

					FileUtils.copyFile(screenshot, new File(fullPath));
				} catch (IOException e) {
					System.out.println("Error saving screenshot: " + e.getMessage());
				}
			}).start();

		} catch (Exception e) {
			System.out.println("Error processing screenshot: " + e.getMessage());
		}

		return ssfilename;
	}

	public boolean validateAssertion(String testData, String pageSource, ExecutionLogger el, ExecutionLogger tsel) {
		try {
			if (pageSource.contains(testData)) {
				el.logExecution("Element with description '" + testData + "' found.");
				if (tsel != null)
					tsel.logExecution("Element with description '" + testData + "' found.");
				return true;
			} else {
				el.logExecution("Element with description '" + testData + "' NOT found.");
				if (tsel != null)
					tsel.logExecution("Element with description '" + testData + "' NOT found.");
				return false;
			}
		} catch (Exception e) {
			System.out.println("Error while validating element description: " + e.getMessage());
			return false;
		}
	}

	public boolean validateElement(String elementId, WebElement element, ExecutionLogger el, ExecutionLogger tsel) {
		try {
			el.logExecution("Starting validation for element: '" + elementId + "'");
			if (tsel != null)
				tsel.logExecution("Starting validation for element: '" + elementId + "'");

			if (element == null) {
				String msg = "Cannot validate — element '" + elementId + "' is missing from elementStore.";
				el.logExecution(msg);
				if (tsel != null)
					tsel.logExecution(msg);
				return false;
			} else {
				el.logExecution("Element found in elementStore: '" + elementId + "'");
				if (tsel != null)
					tsel.logExecution("Element found in elementStore: '" + elementId + "'");
			}
			try {
				element.isEnabled();
			} catch (StaleElementReferenceException e) {
				String msg = "Element '" + elementId + "' became stale during validation.";
				el.logExecution(msg);
				if (tsel != null)
					tsel.logExecution(msg);
				return false;
			}
			boolean isDisplayed = element.isDisplayed();
			boolean isEnabled = element.isEnabled();

			String msg = "Element '" + elementId + "' — Displayed: " + isDisplayed + ", Enabled: " + isEnabled;
			el.logExecution(msg);
			if (tsel != null)
				tsel.logExecution(msg);

			return isDisplayed && isEnabled;
		} catch (Exception e) {
			String msg = "Exception while validating element '" + elementId + "': " + e.getClass().getSimpleName()
					+ " - " + e.getMessage();
			el.logExecution(msg);
			if (tsel != null)
				tsel.logExecution(msg);
			return false;
		}
	}

	public String getElementDescription(String selector, WebElement element) {
		try {
			String description = null;

			if (selector.contains(".text(")) {
				description = element.getText();
			} else if (selector.contains(".description(")) {
				description = element.getAttribute("content-desc");
			} else {
				description = element.getText();
				if (description == null || description.isEmpty()) {
					description = element.getAttribute("content-desc");
				}
			}

			return (description != null && !description.isEmpty()) ? description : "N/A";

		} catch (Exception e) {
			System.out.println("Could not get element description: " + e.getMessage());
			return "N/A";
		}
	}

	public void saveRecording(String videoData, String recordingPath, Integer testCaseId, Integer companyId,
			Integer testCaseResultId) {
		try {
			if (videoData == null || videoData.trim().isEmpty()) {
				System.out.println("Error: Empty video data for " + recordingPath);
				return;
			}

			String directoryPath = AppProperties.datadirectory + (companyId != null ? companyId + File.separator : "")
					+ (testCaseId != null ? testCaseId + File.separator : "")
					+ (testCaseResultId != null ? testCaseResultId + File.separator : "");

			String videosDirectoryPath = directoryPath + "videos";
			String fullPath = videosDirectoryPath + File.separator + recordingPath;

			File videosDirectory = new File(videosDirectoryPath);
			if (!videosDirectory.exists()) {
				videosDirectory.mkdirs();
			}

			byte[] decodedVideo;
			try {
				String cleanedVideoData = videoData.trim();
				if (cleanedVideoData.startsWith("\"") && cleanedVideoData.endsWith("\"")) {
					cleanedVideoData = cleanedVideoData.substring(1, cleanedVideoData.length() - 1);
				}

				decodedVideo = Base64.getDecoder().decode(cleanedVideoData);
				if (decodedVideo.length == 0) {
					System.out.println("Error: Decoded video has zero length for " + recordingPath);
					return;
				}
			} catch (IllegalArgumentException e) {
				System.out.println("Error decoding Base64 data: " + e.getMessage() + " for " + recordingPath);
				return;
			}

			try (FileOutputStream stream = new FileOutputStream(fullPath);
					BufferedOutputStream bos = new BufferedOutputStream(stream)) {
				bos.write(decodedVideo);
				bos.flush();
			}

			File videoFile = new File(fullPath);
			if (videoFile.exists() && videoFile.length() > 0) {
				System.out.println("Video saved successfully at: " + fullPath);
			} else {
				System.out.println("Error: Failed to save video or file is empty: " + fullPath);
			}

		} catch (Exception e) {
			System.out.println("Error saving recording: " + e.getMessage() + " for " + recordingPath);
			e.printStackTrace();
		}
	}

	public void checkElementState(WebElement element, ExecutionLogger el, boolean isDisplayed, boolean isEnabled,
			Point location, Dimension size, ExecutionLogger tsel) {
		try {
			String header = "Element State Summary:";
			String status = "Visible: " + isDisplayed + " | Enabled: " + isEnabled;
			String pos = "Location: (x=" + location.getX() + ", y=" + location.getY() + ")";
			String dim = "Size: Width=" + size.getWidth() + ", Height=" + size.getHeight();

			el.logExecution(header);
			el.logExecution(status);
			el.logExecution(pos);
			el.logExecution(dim);
			if (tsel != null) {
				tsel.logExecution(header);
				tsel.logExecution(status);
				tsel.logExecution(pos);
				tsel.logExecution(dim);
			}
		} catch (Exception e) {
			String msg = "Failed to format element state: " + e.getMessage();
			el.logExecution(msg);
			if (tsel != null)
				tsel.logExecution(msg);
		}
	}

	public void checkGestureParameters(int startX, int startY, int endX, int endY, Dimension screenSize,
			ExecutionLogger el, ExecutionLogger tsel) {
		try {
			el.logExecution(" Gesture Parameters Check:");
			el.logExecution(" Screen Size: " + screenSize.width + "x" + screenSize.height);
			el.logExecution(" Start Point: (" + startX + "," + startY + ")");
			el.logExecution(" End Point: (" + endX + "," + endY + ")");
			el.logExecution(" Distance: " + calculateDistance(startX, startY, endX, endY) + "px");
			if (tsel != null)
				tsel.logExecution(" Gesture Parameters Check:");
			if (tsel != null)
				tsel.logExecution(" Screen Size: " + screenSize.width + "x" + screenSize.height);
			if (tsel != null)
				tsel.logExecution(" Start Point: (" + startX + "," + startY + ")");
			if (tsel != null)
				tsel.logExecution(" End Point: (" + endX + "," + endY + ")");
			if (tsel != null)
				tsel.logExecution(" Distance: " + calculateDistance(startX, startY, endX, endY) + "px");
		} catch (Exception e) {
			el.logExecution("Failed to check gesture parameters: " + e.getMessage());
			if (tsel != null)
				tsel.logExecution("Failed to check gesture parameters: " + e.getMessage());
		}
	}

	private double calculateDistance(int x1, int y1, int x2, int y2) {
		return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
	}

	public boolean validatePriceChanges(Map<String, Double> initialPrices, Map<String, Double> updatedPrices,
			String expectedChange, ExecutionLogger el, ExecutionLogger tsel) {
		try {
			if (initialPrices.isEmpty()) {
				el.logExecution("No initial price data found");
				if (tsel != null)
					tsel.logExecution("No initial price data found");
				return false;
			}

			if (updatedPrices.isEmpty()) {
				el.logExecution("No updated price data found");
				if (tsel != null)
					tsel.logExecution("No updated price data found");
				return false;
			}

			el.logExecution("Initial prices captured: " + initialPrices.keySet().size() + " items");
			if (tsel != null)
				tsel.logExecution("Initial prices captured: " + initialPrices.keySet().size() + " items");

			int changesInExpectedDirection = 0;
			int totalComparisons = 0;

			for (String key : initialPrices.keySet()) {
				if (updatedPrices.containsKey(key)) {
					double initial = initialPrices.get(key);
					double updated = updatedPrices.get(key);

					String direction = determinePriceChangeDirection(initial, updated);

					if (direction.equals(expectedChange)) {
						changesInExpectedDirection++;
						el.logExecution(key + " changed from " + initial + " to " + updated + " (" + direction
								+ ") - MATCHES expected direction");
						if (tsel != null)
							tsel.logExecution(key + " changed from " + initial + " to " + updated + " (" + direction
									+ ") - MATCHES expected direction");
					} else {
						el.logExecution(key + " changed from " + initial + " to " + updated + " (" + direction
								+ ") - does NOT match expected direction");
						if (tsel != null)
							tsel.logExecution(key + " changed from " + initial + " to " + updated + " (" + direction
									+ ") - does NOT match expected direction");
					}
					totalComparisons++;
				}
			}

			boolean passed = (changesInExpectedDirection > 0);
			el.logExecution("Price changes in '" + expectedChange + "' direction: " + changesInExpectedDirection
					+ " out of " + totalComparisons);
			if (tsel != null)
				tsel.logExecution("Price changes in '" + expectedChange + "' direction: " + changesInExpectedDirection
						+ " out of " + totalComparisons);

			return passed;

		} catch (Exception e) {
			el.logExecution("Error validating price changes: " + e.getMessage());
			if (tsel != null)
				tsel.logExecution("Error validating price changes: " + e.getMessage());
			return false;
		}
	}

	public String determinePriceChangeDirection(double initial, double updated) {
		if (updated > initial) {
			return "greater";
		} else if (updated < initial) {
			return "lower";
		} else {
			return "equals";
		}
	}

	public double parsePrice(String priceText) {
		if (priceText != null && !priceText.isEmpty() && !priceText.contains("%")) {
			String cleanPrice = priceText.replace(",", "");
			return Double.parseDouble(cleanPrice);
		}
		throw new IllegalArgumentException("Invalid price format");
	}

//	public Map<String, String> getAutomationErrorDetails(Throwable e) {
//		Map<String, String> errorDetails = new HashMap<>();
//		String errorCode = "M0011"; // Default: Unknown Error
//		String errorMessage = "Unknown error occurred during execution.";
//
//		try {
//			if (e instanceof NoSuchElementException) {
//				errorCode = "M0004";
//				errorMessage = "Element could not be located on the page.";
//			} else if (e instanceof StaleElementReferenceException) {
//				errorCode = "M0005";
//				errorMessage = "Element reference became stale.";
//			} else if (e instanceof TimeoutException) {
//				errorCode = "M0006";
//				errorMessage = "Timeout occurred while waiting for element.";
//			} else if (e instanceof AssertionError) {
//				errorCode = "M0007";
//				errorMessage = "Assertion failed during step validation.";
//			} else if (e instanceof ElementClickInterceptedException) {
//				errorCode = "M0008";
//				errorMessage = "Element click was intercepted by another element.";
//			} else if (e instanceof SessionNotCreatedException) {
//				errorCode = "M0009";
//				errorMessage = "Session could not be created. App crash or WDA failure.";
//			} else if (e instanceof JSONException || e instanceof ParseException) {
//				errorCode = "M0010";
//				errorMessage = "Invalid or corrupted test data JSON.";
//			} else if (e.getMessage() != null && e.getMessage().toLowerCase().contains("xpath mismatch")) {
//				errorCode = "M0020";
//				errorMessage = "XPath mismatch and fallback XPath also failed.";
//			} else if (e instanceof ConnectException) {
//				errorCode = "M0003";
//				errorMessage = "Appium server unreachable.";
//			} else if (e instanceof IllegalStateException && e.getMessage() != null
//					&& e.getMessage().toLowerCase().contains("device not found")) {
//				errorCode = "M0001";
//				errorMessage = "Device or session disconnected unexpectedly.";
//			} else if (e instanceof org.openqa.selenium.InvalidSelectorException) {
//				errorCode = "M0012";
//				errorMessage = "Invalid selector used to find the element.";
//			} else if (e instanceof org.openqa.selenium.ElementNotInteractableException) {
//				errorCode = "M0013";
//				errorMessage = "Element found but not interactable.";
//			} else if (e instanceof WebDriverException && e.getMessage() != null
//					&& e.getMessage().toLowerCase().contains("not selectable")) {
//				errorCode = "M0014";
//				errorMessage = "Element found but not selectable.";
//			} else if (e instanceof org.openqa.selenium.WebDriverException) {
//				String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
//				if (msg.contains("unable to start webdriveragent session") || msg.contains("could not proxy command")) {
//					errorCode = "M0002";
//					errorMessage = "Appium session lost! WDA likely crashed.";
//				} else if (msg.contains("connection refused") || msg.contains("invalid server response")) {
//					errorCode = "M0003";
//					errorMessage = "Appium server unreachable.";
//				} else if (msg.contains("session not created")) {
//					errorCode = "M0009";
//					errorMessage = "Session could not be created. App crash or WDA failure.";
//				} else {
//					errorCode = "M0015";
//					errorMessage = "WebDriver internal error.";
//				}
//			} else if (e instanceof org.openqa.selenium.ElementNotInteractableException) {
//				errorCode = "M0013";
//				errorMessage = "Element is present but not interactable (e.g., hidden or disabled).";
//			} else if (e instanceof IllegalArgumentException) {
//				errorCode = "M0015";
//				errorMessage = "Illegal or inappropriate argument passed to a method.";
//			} else if (e instanceof NullPointerException) {
//				errorCode = "M0016";
//				errorMessage = "Null reference encountered during execution.";
//			} else if (e instanceof org.openqa.selenium.InvalidSelectorException) {
//				errorCode = "M0017";
//				errorMessage = "Invalid selector used for locating element.";
//			} else if (e instanceof WebDriverException && e.getMessage() != null
//					&& e.getMessage().toLowerCase().contains("not visible")) {
//				errorCode = "M0018";
//				errorMessage = "Element is not visible on screen.";
//			} else if (e instanceof RuntimeException && e.getMessage() != null
//					&& e.getMessage().toLowerCase().contains("step execution timeout")) {
//				errorCode = "M0019";
//				errorMessage = "Step execution timed out after wait limit.";
//			}
//
//		} catch (
//
//		Exception ex) {
//			errorCode = "M0011";
//			errorMessage = "Unknown error during error code resolution.";
//		}
//
//		errorDetails.put("code", errorCode);
//		errorDetails.put("message", errorMessage);
//		return errorDetails;
//	}
	public Map<String, String> getAutomationErrorDetails(Throwable e) {
		Map<String, String> errorDetails = new HashMap<>();
		String errorCode = "M0011";
		String errorMessage = "Unknown error occurred during execution.";
		try {
			String className = e.getClass().getName();
			String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
			if (e instanceof org.openqa.selenium.NoSuchElementException) {
				errorCode = "M0004";
				errorMessage = "Element could not be located on the page.";
			} else if (e instanceof org.openqa.selenium.StaleElementReferenceException) {
				errorCode = "M0005";
				errorMessage = "Element reference became stale.";
			} else if (e instanceof org.openqa.selenium.TimeoutException || msg.contains("timeout")) {
				errorCode = "M0006";
				errorMessage = "Timeout occurred while waiting for element.";
			} else if (e instanceof AssertionError) {
				errorCode = "M0007";
				errorMessage = "Assertion failed during step validation.";
			} else if (e instanceof org.openqa.selenium.ElementClickInterceptedException) {
				errorCode = "M0008";
				errorMessage = "Element click was intercepted by another element.";
			} else if (e instanceof org.openqa.selenium.SessionNotCreatedException
					|| msg.contains("session not created")) {
				errorCode = "M0009";
				errorMessage = "Session could not be created. App crash or WDA failure.";
			} else if (e instanceof org.json.JSONException || e instanceof org.json.simple.parser.ParseException) {
				errorCode = "M0010";
				errorMessage = "Invalid or corrupted test data JSON.";
			} else if (msg.contains("xpath mismatch")) {
				errorCode = "M0020";
				errorMessage = "XPath mismatch and fallback XPath also failed.";
			} else if (e instanceof java.net.ConnectException || msg.contains("connection refused")
					|| msg.contains("invalid server response")) {
				errorCode = "M0003";
				errorMessage = "Appium server unreachable.";
			} else if (e instanceof IllegalStateException && msg.contains("device not found")) {
				errorCode = "M0001";
				errorMessage = "Device or session disconnected unexpectedly.";
			} else if (e instanceof org.openqa.selenium.InvalidSelectorException) {
				errorCode = "M0012";
				errorMessage = "Invalid selector used to find the element.";
			} else if (e instanceof org.openqa.selenium.ElementNotInteractableException) {
				errorCode = "M0013";
				errorMessage = "Element found but not interactable.";
			} else if (e instanceof org.openqa.selenium.WebDriverException) {
				if (msg.contains("unable to start webdriveragent session") || msg.contains("could not proxy command")) {
					errorCode = "M0002";
					errorMessage = "Appium session lost! WDA likely crashed.";
				} else if (msg.contains("session not created")) {
					errorCode = "M0009";
					errorMessage = "Session could not be created. App crash or WDA failure.";
				} else if (msg.contains("not visible")) {
					errorCode = "M0018";
					errorMessage = "Element is not visible on screen.";
				} else {
					errorCode = "M0015";
					errorMessage = "WebDriver internal error.";
				}
			} else if (e instanceof IllegalArgumentException) {
				errorCode = "M0015";
				errorMessage = "Illegal or inappropriate argument passed to a method.";
			} else if (e instanceof NullPointerException) {
				errorCode = "M0016";
				errorMessage = "Null reference encountered during execution.";
			} else if (e instanceof RuntimeException && msg.contains("step execution timeout")) {
				errorCode = "M0019";
				errorMessage = "Step execution timed out after wait limit.";
			} else {
				System.out.println("Unmapped Exception Class: " + className);
				if (!msg.isEmpty()) {
					System.out.println("Exception Message: " + msg);
				}
			}
		} catch (Exception ex) {
			errorCode = "M0011";
			errorMessage = "Unknown error during error code resolution.";
		}
		errorDetails.put("code", errorCode);
		errorDetails.put("message", errorMessage);
		return errorDetails;
	}

	public Map<String, Object> generateFailureResult(String errorCode, String errorMessage, Throwable e) {
		Map<String, Object> result = new HashMap<>();
		result.put("status", "FAIL");
		result.put("errorCode", errorCode);
		result.put("errorMessage", errorMessage);
		result.put("failureReason", e.getMessage() != null ? e.getMessage() : "Unknown failure");
		return result;
	}

}