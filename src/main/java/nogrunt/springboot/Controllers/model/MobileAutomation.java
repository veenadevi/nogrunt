package nogrunt.springboot.Controllers.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mobile_automation")
public class MobileAutomation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "test_case_id")
    private Integer test_case_id;

    @Column(name = "idtest_step")
    private Integer idtest_step;

    @Column(name = "action")
    private String action;

    @Column(name = "element_id")
    private String elementId;

    @Column(name = "device")
    private String device;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "sequence_number")
    private Integer sequenceNumber;

    @Column(columnDefinition = "TEXT", name = "strategy_map")
    private String strategyMap;

    @Column(name = "android_uiautomator", columnDefinition = "TEXT")
    private String androidUiautomator;

    @Column(columnDefinition = "TEXT",name = "xpath")
    private String xpath;

    @Column(name = "id_strategy", columnDefinition = "TEXT")
    private String idStrategy;

    @Column(name = "appium_timestamp")
    private LocalDateTime appiumTimestamp;

    // Getters and Setters
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTest_case_id() {
        return test_case_id;
    }

    public void setTest_case_id(Integer test_case_id) {
        this.test_case_id = test_case_id;
    }

    public Integer getIdtest_step() {
        return idtest_step;
    }

    public void setIdtest_step(Integer idtest_step) {
        this.idtest_step = idtest_step;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
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

    public LocalDateTime getAppiumTimestamp() {
        return appiumTimestamp;
    }

    public void setAppiumTimestamp(LocalDateTime appiumTimestamp) {
        this.appiumTimestamp = appiumTimestamp;
    }

}
