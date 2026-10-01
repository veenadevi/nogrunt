package nogrunt;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Attributes;

import java.util.HashMap;
import java.util.Map.Entry;

public class HtmlAttributes {
    public void getAttr(String outerHTML, HashMap attr) {
        // Sample outerHTML for an element (this could be any element like <button>, <div>, <svg>, etc.)
//        outerHTML = "<button class=\"MuiButtonBase-root MuiIconButton-root MuiIconButton-sizeMedium css-53g0n7-MuiButtonBase-root-MuiIconButton-root\" tabindex=\"0\" type=\"button\" id=\":r17:\">" +
//                           "<svg class=\"MuiSvgIcon-root MuiSvgIcon-fontSizeMedium css-1g0kpah-MuiSvgIcon-root\" focusable=\"false\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" data-testid=\"PowerSettingsNewIcon\">" +
//                           "<path d=\"M13 3h-2v10h2zm4.83 2.17-1.42 1.42C17.99 7.86 19 9.81 19 12c0 3.87-3.13 7-7 7s-7-3.13-7-7c0-2.19 1.01-4.14 2.58-5.42L6.17 5.17C4.23 6.82 3 9.26 3 12c0 4.97 4.03 9 9 9s9-4.03 9-9c0-2.74-1.23-5.18-3.17-6.83\"></path></svg>" +
//                           "Click me</button>";  // Example with text inside the button

        // Parse the outerHTML string to a Jsoup Document
        Document doc = Jsoup.parse(outerHTML);
        
        // Get the first element (this could be any element based on the outerHTML provided)
        Element element = doc.body().child(0);  // This gets the first child element of the body (can be any element)

        // Extract and print all attributes of the element, including for nested elements
        if (element != null) {
            // Print attributes for the parent element
            System.out.println("Attributes for Parent Element:");
            printElementAttributes(element, attr);

            // Recursively print attributes for nested elements
            System.out.println("\nAttributes for Nested Elements:");
            extractNestedAttributes(element, attr);
        } else {
            System.out.println("No element found!");
        }
    }

    // Method to print the attributes of an element along with the element type (tag name)
    private static void printElementAttributes(Element element, HashMap attr) {
        // Print the element type (tag name) first
        System.out.println("Element Type: " + element.tagName());

        // Print the text content of the element if it exists
        String text = element.text().trim();
        if (!text.isEmpty()) {
        	attr.put("text", text);
            System.out.println("Text: " + text);  // Print the text content
        }

        // Extract and print the attributes of the element
        Attributes attributes = element.attributes();
        for (Entry<String, String> entry : attributes) {
            String attributeName = entry.getKey();
            String attributeValue = entry.getValue();
            attr.put(attributeName, attributeValue);
            System.out.println(attributeName + " = " + attributeValue);
        }
    }

    // Method to recursively extract and print attributes for nested elements
    private static void extractNestedAttributes(Element element, HashMap attr) {
        // Iterate through all child elements of the current element
        for (Element child : element.children()) {
        	HashMap nested = new HashMap();
            // Skip processing the <path> element here
            if (child.tagName().equals("path")) {
                continue; // Don't process <path> in this recursion step
            }

            // Print attributes for the child element
            printElementAttributes(child, nested);
            
            attr.put("nested", nested);
            // Recursively process the child elements (if it has children of its own)
            extractNestedAttributes(child, nested);
        }

        // Special case for SVG elements: only process <path> once (and other SVG elements if needed)
        if (element.tagName().equals("svg")) {
            // Only print <path> elements once for each <svg>
            for (Element svgChild : element.select("path")) {
            	HashMap nested = new HashMap();
                printElementAttributes(svgChild, nested);  // This will print the attributes of <path> elements inside <svg>
                attr.put("nested", nested);
            }
        }
    }
}
