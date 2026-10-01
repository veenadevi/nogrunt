package nogrunt.springboot.Controllers.model;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_case_results")
public class TestCaseResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idtest_case_results")
    private Integer idtest_case_results;

    @Column(name = "Test_Case_Id")
    private Integer Test_Case_Id;

    @Column(name = "Status")
    private String Status;

    @Column(name = "Executed_By")
    private String Executed_By;

    @Column(name = "Executed_Date")
    private LocalDateTime Executed_Date;

    @Column(name = "Duration")
    private Double Duration;

    @Column(name = "Browser")
    private String Browser;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "envurl")
    private String envurl;

    @Column(name = "test_case_name")
    private String test_case_name;

    @Column(name = "ThresholdStatus")
    private String ThresholdStatus;

    @Column(name = "sessionid")
    private String sessionid;

    @Column(name = "nodeUri")
    private String nodeUri;


    // Getters and Setters
    public Integer getIdtest_case_results() {
        return idtest_case_results;
    }

    public void setIdtest_case_results(Integer idtest_case_results) {
        this.idtest_case_results = idtest_case_results;
    }

    public Integer getTest_Case_Id() {
        return Test_Case_Id;
    }

    public void setTest_Case_Id(Integer test_Case_Id) {
        Test_Case_Id = test_Case_Id;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }

    public String getExecuted_By() {
        return Executed_By;
    }

    public void setExecuted_By(String executed_By) {
        Executed_By = executed_By;
    }

    public LocalDateTime getExecuted_Date() {
        return Executed_Date;
    }

    public void setExecuted_Date(LocalDateTime executed_Date) {
        Executed_Date = executed_Date;
    }

    public Double getDuration() {
        return Duration;
    }

    public void setDuration(Double duration) {
        Duration = duration;
    }

    public String getBrowser() {
        return Browser;
    }

    public void setBrowser(String browser) {
        Browser = browser;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getEnvurl() {
        return envurl;
    }

    public void setEnvurl(String envurl) {
        this.envurl = envurl;
    }

    public String getTest_case_name() {
        return test_case_name;
    }

    public void setTest_case_name(String test_case_name) {
        this.test_case_name = test_case_name;
    }

    public String getThresholdStatus() {
        return ThresholdStatus;
    }

    public void setThresholdStatus(String thresholdStatus) {
        ThresholdStatus = thresholdStatus;
    }

    public String getSessionid() {
        return sessionid;
    }

    public void setSessionid(String sessionid) {
        this.sessionid = sessionid;
    }

    public String getNodeUri() {
        return nodeUri;
    }

    public void setNodeUri(String nodeUri) {
        this.nodeUri = nodeUri;
    }
}
