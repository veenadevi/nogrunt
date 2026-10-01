package nogrunt;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;  // Add this import statement
import java.util.List;       // Add this import statement

import javax.imageio.ImageIO;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.FileWriter;
import java.io.IOException;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;


public class ImageCompare {
//	boolean checker = true;
//    public void testCompareImage(boolean checker) {
//        // Load the OpenCV native library
//    	try {    	
//    			System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//    	} catch(Exception e) {
//    		e.printStackTrace();
//    	}
//
//	
//	        // Paths of the images to compare
//	        String imagePath1 = "D:\\Capture1.JPG";
//	        String imagePath2 = "D:\\Screenshot1.PNG";
//	        String imagePath3 = "D:\\Capture.JPG";
//	
//	        // Read the images
//	        Mat image1 = Imgcodecs.imread(imagePath1);
//	        Mat image3 = Imgcodecs.imread(imagePath3);
//	        
//	        testConn(image1,image3);
//    }
//    
//    public void testConn(Mat image1, Mat image2) {
//    	  // Resize images to ensure size match
//        if (image1.size().width != image2.size().width || image1.size().height != image2.size().height) {
//            Imgproc.resize(image1, image1, image2.size());
//        }
//
//        // Convert images to ensure channel match
//        if (image1.channels() != image2.channels()) {
//            Imgproc.cvtColor(image1, image1, Imgproc.COLOR_BGR2GRAY);
//            Imgproc.cvtColor(image2, image2, Imgproc.COLOR_BGR2GRAY);
//        }
//
//        // Compute the absolute difference between the two images
//        Mat diffImage = new Mat();
//        Core.absdiff(image1, image2, diffImage);
//
//        // Calculate the average pixel difference
//        Scalar scalar = Core.mean(diffImage);
//        double averageDiff = scalar.val[0];
//
//        // Print the average pixel difference
//        System.out.println("Average pixel difference: " + averageDiff);
//
//        // Save the difference image
//        String diffImagePath = "D:\\DiffImage.PNG";
//        Imgcodecs.imwrite(diffImagePath, diffImage);
//
//        // Release the allocated resources
//        image1.release();
//        image2.release();
//        diffImage.release();
//    }
	 
	
	// 2nd concept.
//	public void testCompareImage(boolean checker) {
//		// Paths of the images to compare
//	        String imagePath1 = "D:\\Capture1.JPG";
//	        String imagePath2 = "D:\\Capture.JPG";
//	        
//	        try {
//	            // Read the images
//	            BufferedImage image1 = ImageIO.read(new File(imagePath1));
//	            BufferedImage image2 = ImageIO.read(new File(imagePath2));
//
//	            // Create ImageComparison object
//	            ImageComparison imageComparison = new ImageComparison(image1, image2);
//	            
//
//	            // Compare the images
//	            ImageComparisonResult comparisonResult = imageComparison.compareImages();
//
//	            // Get the result
//	            ImageComparisonState comparisonState = comparisonResult.getImageComparisonState();
//	            double differencePercentage = comparisonResult.getDifferencePercent();
//	            BufferedImage resultImage = comparisonResult.getResult();
//	            String diffImagePath = "D:\\DiffImage.JPG";
//	            ImageIO.write(resultImage, "jpg", new File(diffImagePath));
//	            
//	            BufferedImage expectedImage = comparisonResult.getExpected();
//	            String expecImagePath = "D:\\expectImage.JPG";
//	            ImageIO.write(expectedImage, "jpg", new File(expecImagePath));
//	            
//
//	            // Highlight differences with rectangular boxes
//	            Graphics2D g2d = resultImage.createGraphics();
//	            g2d.setColor(Color.GREEN);
//
//	            for (Rectangle diffArea : comparisonResult.getRectangles()) {
//	                int x = (int) diffArea.getMaxPoint().x;
//	                int y = (int) diffArea.getMaxPoint().y;
//	                int width = (int) diffArea.getWidth();
//	                int height = (int) diffArea.getHeight();
//
//	                g2d.drawRect(x, y, width, height);
//	            }
//
//	            // Print the result
//	            System.out.println("Comparison state: " + comparisonState);
//	            System.out.println("Difference percentage: " + differencePercentage);
//
//	            // Save the result image highlighting the differences
////	            String diffImagePath = "D:\\DiffImage.JPG";
////	            ImageIO.write(resultImage, "jpg", new File(diffImagePath));
//	        } catch (IOException e) {
//	            e.printStackTrace();
//	        }
//	 }
	
	
	//3rd Concept
//	public void testCompareImage(boolean checker) {
//		// Paths of the images to compare
//	        String imagePath1 = "D:\\Capture1.JPG";
//	        String imagePath2 = "D:\\Capture.JPG";
//	        
//	        Comparer comparer = new Comparer(imagePath1);
//	        comparer.add(imagePath2);
//	        comparer.compare("result-Image.jpg");
//	}
	
	//4Th Concept
  public String[] testCompareImage(boolean checker, String imagePath1, String imagePath2, int tcrid, int tsid, int tcid, int cid, int stepNumber) {
  // Load the OpenCV native library
	try {
		if(checker) System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	} catch(Exception e) {
		e.printStackTrace();
	}
	String[] str = new String[3];
//      // Paths of the images to compare
//      String imagePath1 = "C:\\Nogrunt\\1\\520\\814\\screenshots\\_1_520_51136_814_Step2.png";
//      String imagePath2 = "C:\\Nogrunt\\1\\520\\813\\screenshots\\_1_520_51136_813_Step2.png";
      File screenshotFail = new File(imagePath1);
      File screenshotPass = new File(imagePath2);
      String passString = ocrImplementation(screenshotPass);
      String failString = ocrImplementation(screenshotFail);
      
       //Read the images
      Mat image1 = Imgcodecs.imread(imagePath1);
      Mat image2 = Imgcodecs.imread(imagePath2);
      
      String fileName = testConn(image1,image2, tcrid, tsid, tcid, cid, stepNumber);
      str[0]=passString;
      str[1]=failString;
      str[2]=fileName;
      
     return str;
  }

	public String testConn(Mat image1, Mat image2, int tcrid, int tsid, int tcid, int cid, int stepNumber) {
		  // Resize images to ensure size match
	  if (image1.size().width != image2.size().width || image1.size().height != image2.size().height) {
	      Imgproc.resize(image1, image1, image2.size());
	  }
	
	  // Convert images to ensure channel match
	  Mat grayImage1 = new Mat();
	  Mat grayImage2 = new Mat();
	  Imgproc.cvtColor(image1, grayImage1, Imgproc.COLOR_RGB2GRAY);
	  Imgproc.cvtColor(image2, grayImage2, Imgproc.COLOR_RGB2GRAY);
	
	  // Compute the absolute difference between the two images
	  Mat diffImage = new Mat();
	  Core.absdiff(grayImage1, grayImage2, diffImage);
	
	  //Apply a threshold to the difference image
	  Mat thresholdImage = new Mat();
	  Imgproc.threshold(diffImage, thresholdImage, 10, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
	  
	  //Find contours in the threshold image
	  List<MatOfPoint> contours = new ArrayList<>();  // Use List<> instead of ArrayList<>
	  Mat hierarchy = new Mat();
	  Imgproc.findContours(thresholdImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
	  
	  // Draw rectangles around the contour areas
	  Mat outputImage = image2.clone();
	  for (MatOfPoint contour : contours) {
	      Rect rect = Imgproc.boundingRect(contour);
	      Imgproc.rectangle(outputImage, rect.tl(), rect.br(), new Scalar(0, 255, 0), 1);
	  }
	  
	  // Calculate the average pixel difference
	  Scalar scalar = Core.mean(outputImage);
	  double averageDiff = scalar.val[0];
	  
	  // Print the average pixel difference
	  System.out.println("Average pixel difference: " + averageDiff);
	
	  // Save the difference image
	  SelGrid sg = new SelGrid();
	  String ssfilename = "_"  + cid + "_" + tcid + "_" + tsid + "_" + tcrid + "_Step" + stepNumber + "Diff.png";
	  String diffImagePath = sg.getSSFullLoc(ssfilename, tcid ,tcrid , cid);
	  Imgcodecs.imwrite(diffImagePath, outputImage);
	  return ssfilename;
	}
	
	public String ocrImplementation (File screenshot) {
//	    Tesseract tesseract = new Tesseract();
		String result = "";
	    ITesseract tesseract = new Tesseract();
	    tesseract.setDatapath("C:\\tesseract-5.3.1\\tesseract-5.3.1\\tessdata");
		try {
			tesseract.setTessVariable("user_defined_dpi", "300");
			result  = tesseract.doOCR(screenshot);
//			String outputPath = "D:\\sanjeev2.txt";
//            // Write the OCR result to the output file
//            try (FileWriter writer = new FileWriter(outputPath)) {
//                writer.write(resultF);
//            }
//
//            System.out.println("OCR result saved to: " + outputPath);
		} catch (TesseractException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public String getLatestPassScreenshotUsingTestStepId(int tcid, int companyid, int teststepid, MySQlConn msc) {
		String str = "";
		try {
//			MySQlConn msc = new MySQlConn();
			JSONArray json = msc.getLatestPass(tcid, companyid);
			for(int i=0; i<json.size(); i++) {
				JSONObject data= (JSONObject) json.get(i);
				JSONObject testData = (JSONObject) data.get("data");
                JSONObject jsonObj = findObjectByTestStepId(testData, teststepid);
                str = (String) jsonObj.get("Failure_Screenshot_Location");
			}
		} catch (Exception e) {
            e.printStackTrace();
        }
		return str;
	}
	
    // Function to find the object by Test_Step ID in the JSON object
    private static JSONObject findObjectByTestStepId(JSONObject testData, int testStepId) {
        for (Object value : testData.values()) {
            JSONObject obj = (JSONObject) value;
            int objTestStepId = (int) obj.get("Test_Step");
            if (objTestStepId == testStepId) {
                return obj;
            }
        }
        return null; // Object not found
    }
	
}
	
	
	// 5th Concept
//	public void testCompareImage(boolean checker) {
//        ITesseract tesseract = new Tesseract();
//        tesseract.setDatapath("C:\\tesseract-5.3.1\\tesseract-5.3.1\\tessdata");
//        tesseract.setTessVariable("user_defined_dpi", "300");
//        try {
//	          String imagePath1 = "D:\\Capture.PNG";
//	          String imagePath2 = "D:\\Capture1.PNG";
//            File image1 = new File(imagePath1);
//            File image2 = new File(imagePath2);
//
//            // Load the images
//            BufferedImage img1 = ImageIO.read(image1);
//            BufferedImage img2 = ImageIO.read(image2);
//
//            // Perform image comparison and extract differing regions
//            BufferedImage diffImage = compareImages(img1, img2);
//
//            // Apply OCR on the differing regions and obtain the text content
//            String textDifferences = performOCR(diffImage, tesseract);
//
//            // Present the text differences
//            System.out.println("Text Differences:");
//            System.out.println(textDifferences);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//	  }
//
//    private static BufferedImage compareImages(BufferedImage img1, BufferedImage img2) {
//        // Ensure both images have the same dimensions
//        int width = Math.min(img1.getWidth(), img2.getWidth());
//        int height = Math.min(img1.getHeight(), img2.getHeight());
//
//        // Create a diff image with the same dimensions as the input images
//        BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
//
//        // Iterate over each pixel in the images and compare their RGB values
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                int rgb1 = img1.getRGB(x, y);
//                int rgb2 = img2.getRGB(x, y);
//
//                // Compare the RGB values of the two pixels
//                if (rgb1 != rgb2) {
//                    // If the pixels differ, mark the corresponding pixel in the diff image
//                    diffImage.setRGB(x, y, Color.RED.getRGB());
//                } else {
//                    // If the pixels are the same, set the pixel in the diff image to white
//                    diffImage.setRGB(x, y, Color.WHITE.getRGB());
//                }
//            }
//        }
//        
////  	  // Save the difference image
////  	  String diffImagePath = "D:\\DiffCapture.PNG";
////  	  Imgcodecs.imwrite(diffImagePath, diffImage);
//
//        return diffImage;
//    }
//
//    private static String performOCR(BufferedImage image, ITesseract tesseract) {
//        try {
//            // Apply OCR on the image and obtain the text content
//            return tesseract.doOCR(image);
//        } catch (TesseractException e) {
//            e.printStackTrace();
//            return "";
//        }
//    }
//}