package nogrunt.springboot.Controllers.dtoModel;

import java.time.LocalDateTime;

public class MobileAutomationDTO {

    private String action;
    private Integer extKey;

    private String elementId;

    private String testData;

    private Integer testCaseId;
    private LocalDateTime timestamp;
    private Integer sequenceNumber;
    private String strategyMap;
    private String androidUiautomator;
    private String xpath;
    private String idStrategy;

    private String element;

    // Getters and Setters

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getExtKey() {
        return extKey;
    }

    public void setExtKey(Integer extKey) {
        this.extKey = extKey;
    }


    public Integer getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(Integer testCaseId) {
        this.testCaseId = testCaseId;
    }

    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public LocalDateTime getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getStrategyMap() {
        return strategyMap;
    }

    public void setStrategyMap(String strategyMap) {
        this.strategyMap = strategyMap;
    }

    public String getAndroidUiautomator() {
        return androidUiautomator;
    }

    public void setAndroidUiautomator(String androidUiautomator) {
        this.androidUiautomator = androidUiautomator;
    }

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public String getIdStrategy() {
        return idStrategy;
    }

    public void setIdStrategy(String idStrategy) {
        this.idStrategy = idStrategy;
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }
    public void setTestData(String testData) {
        this.testData = testData;
    }

    public String getTestData() {
        return testData;
    }
}
