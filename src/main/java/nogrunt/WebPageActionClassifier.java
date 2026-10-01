package nogrunt;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Connection;
import org.jsoup.Jsoup;


public class WebPageActionClassifier {
    private static final String MODEL_ENDPOINT = "http://localhost:5000/predict";
    private static final int MAX_FEATURES = 1000;

    public static void main(String[] args) {
        // Load the web page source code from a URL or local file
        String pageSource = loadPageSource("https://example.com");

        // Extract features from the page source code
        List<String> features = extractFeatures(pageSource);

        // Convert the features to a JSON payload
        JSONObject payload = new JSONObject();
        payload.put("features", features);

        // Send the payload to the ML model for prediction
        String prediction = predictAction(payload);

        // Print the predicted action
        System.out.println("The intended action of this page is: " + prediction);
    }

    private static String loadPageSource(String url) {
        try {
            // Load the web page source code from a URL or local file
            InputStream inputStream = new URL(url).openStream();
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static List<String> extractFeatures(String pageSource) {
        // Tokenize the HTML code
        String[] tokens = StringUtils.split(pageSource, "<> \n\r\t");

        // Remove irrelevant tags and attributes
        List<String> features = new ArrayList<>();
        for (String token : tokens) {
            if (token.startsWith("<") && token.endsWith(">")) {
                if (token.startsWith("<form") || token.startsWith("<input") || token.startsWith("<button") || token.startsWith("<a")) {
                    features.add(StringUtils.substringBetween(token, "<", ">"));
                }
            }
        }

        // Limit the number of features to the top MAX_FEATURES most frequent ones
        List<String> topFeatures = new ArrayList<>();
        for (String feature : features) {
            if (!feature.isEmpty() && !topFeatures.contains(feature)) {
                topFeatures.add(feature);
                if (topFeatures.size() >= MAX_FEATURES) {
                    break;
                }
            }
        }

        return topFeatures;
    }

    private static String predictAction(JSONObject payload) {
        try {
            // Send a POST request to the ML model endpoint with the payload
            Connection.Response response = Jsoup.connect(MODEL_ENDPOINT)
                    .ignoreContentType(true)
                    .requestBody(payload.toString())
                    .method(Connection.Method.POST)
                    .execute();

            // Parse the response and extract the predicted action
            JSONObject result = new JSONObject(response.body());
            return result.getString("prediction");
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}

