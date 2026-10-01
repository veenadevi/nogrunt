//package 

import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindAll;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import com.qt.utils.FileReaderManager;

public class runLogic extends BasePage {
	
	public WebDriver driver;
	
    public runLogic(WebDriver driver) {
        this.driver = driver;
        super.driver = driver;
        PageFactory.initElements(driver, this);
    }
    
    
	//Xpath Element
	
	//Decleation
}