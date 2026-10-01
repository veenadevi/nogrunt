package nogrunt;
import java.io.File;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PDFTextExtractor {
	
	
    public static void main(String[] args) {
        
    }
    
    public static String getpdfText(String filename) {
    	String text = "";
    	try {
            // Load the PDF document
            PDDocument document = PDDocument.load(new File(filename));
            
            text = getpdfText(document);
            
            // Create a PDFTextStripper object to extract text from the document
//            PDFTextStripper pdfStripper = new PDFTextStripper();
//            
//            // Get the text content of the document
//            text = pdfStripper.getText(document);
//            
//            // Print the text content to the console
//            System.out.println(text);
//            
//            // Close the document
//            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    	
    	return text;
    }
    
    public static String getpdfText(PDDocument document) {
    	String text = "";
    	try {
            // Load the PDF document
//            PDDocument document = PDDocument.load(new File(filename));
            
            // Create a PDFTextStripper object to extract text from the document
            PDFTextStripper pdfStripper = new PDFTextStripper();
            
            // Get the text content of the document
            text = pdfStripper.getText(document);
            
            // Print the text content to the console
            System.out.println(text);
            
            // Close the document
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    	
    	return text;
    }
}

