package nogrunt.springboot.Controllers.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_step_result")
public class TestStepResult {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "idtest_step_result")
	private Integer idtest_step_result;

	@Column(name = "Step_Number")
	private Integer Step_Number;

	@Column(name = "Page_Name")
	private String Page_Name;

	@Column(name = "Page_Description")
	private String Page_Description;

	@Column(name = "Object_Xpath")
	private String Object_Xpath;

	@Column(name = "Keyword")
	private String Keyword;

	@Column(name = "Action")
	private String Action;

	@Column(name = "Flow")
	private String Flow;

	@Column(name = "TestData")
	private String TestData;

	@Column(name = "VarName")
	private String VarName;

	@Column(name = "Status")
	private String Status;

	@Column(name = "Failure_Screenshot_Location")
	private String Failure_Screenshot_Location;

	@Column(name = "Test_Step")
	private Integer Test_Step;

	@Column(name = "Executed_By")
	private String Executed_By;

	@Column(name = "Duration")
	private Double Duration;

	@Column(name = "Test_Case_Results_Id")
	private Integer Test_Case_Results_Id;

	@Column(name = "ValDevice")
	private String ValDevice;

	@Column(name = "Executed_Date")
	private LocalDateTime Executed_Date;

	@Column(name = "issues")
	private String issues;

	@Column(name = "filename")
	private String filename;

	@Column(name = "fileField")
	private String fileField;

	@Column(name = "DBUrl")
	private String DBUrl;

	@Column(name = "DBUsername")
	private String DBUsername;

	@Column(name = "DBPwd")
	private String DBPwd;

	@Column(name = "DBQuery")
	private String DBQuery;

	@Column(name = "DBType")
	private String DBType;

	@Column(name = "testdata_source")
	private String testdata_source;

	@Column(name = "gendataType")
	private String gendataType;

	@Column(name = "gendatalen")
	private Integer gendatalen;

	@Column(name = "DBVersion")
	private String DBVersion;

	@Column(name = "clickable")
	private Integer clickable;

	@Column(name = "regex")
	private String regex;

	@Column(name = "iframexpath")
	private String iframexpath;

	@Column(name = "altpath")
	private String altpath;

	@Column(name = "screenshot1")
	private String screenshot1;

	@Column(name = "ThreshholdStatus")
	private String ThreshholdStatus;

	@Column(name = "issuetype")
	private String issuetype;

	@Column(name = "outerhtml")
	private String outerhtml;

	// GETTER AND SETTER
	public String getOuterhtml() {
		return outerhtml;
	}

	public void setOuterhtml(String outerhtml) {
		this.outerhtml = outerhtml;
	}

	public Integer getIdtest_step_result() {
		return idtest_step_result;
	}

	public void setIdtest_step_result(Integer idtest_step_result) {
		this.idtest_step_result = idtest_step_result;
	}

	public Integer getStep_Number() {
		return Step_Number;
	}

	public void setStep_Number(Integer step_Number) {
		Step_Number = step_Number;
	}

	public String getPage_Name() {
		return Page_Name;
	}

	public void setPage_Name(String page_Name) {
		Page_Name = page_Name;
	}

	public String getPage_Description() {
		return Page_Description;
	}

	public void setPage_Description(String page_Description) {
		Page_Description = page_Description;
	}

	public String getObject_Xpath() {
		return Object_Xpath;
	}

	public void setObject_Xpath(String object_Xpath) {
		Object_Xpath = object_Xpath;
	}

	public String getKeyword() {
		return Keyword;
	}

	public void setKeyword(String keyword) {
		Keyword = keyword;
	}

	public String getAction() {
		return Action;
	}

	public void setAction(String action) {
		Action = action;
	}

	public String getFlow() {
		return Flow;
	}

	public void setFlow(String flow) {
		Flow = flow;
	}

	public String getTestData() {
		return TestData;
	}

	public void setTestData(String testData) {
		TestData = testData;
	}

	public String getVarName() {
		return VarName;
	}

	public void setVarName(String varName) {
		VarName = varName;
	}

	public String getStatus() {
		return Status;
	}

	public void setStatus(String status) {
		Status = status;
	}

	public String getFailure_Screenshot_Location() {
		return Failure_Screenshot_Location;
	}

	public void setFailure_Screenshot_Location(String failure_Screenshot_Location) {
		Failure_Screenshot_Location = failure_Screenshot_Location;
	}

	public Integer getTest_Step() {
		return Test_Step;
	}

	public void setTest_Step(Integer test_Step) {
		Test_Step = test_Step;
	}

	public String getExecuted_By() {
		return Executed_By;
	}

	public void setExecuted_By(String executed_By) {
		Executed_By = executed_By;
	}

	public Double getDuration() {
		return Duration;
	}

	public void setDuration(Double duration) {
		Duration = duration;
	}

	public Integer getTest_Case_Results_Id() {
		return Test_Case_Results_Id;
	}

	public void setTest_Case_Results_Id(Integer test_Case_Results_Id) {
		Test_Case_Results_Id = test_Case_Results_Id;
	}

	public String getValDevice() {
		return ValDevice;
	}

	public void setValDevice(String valDevice) {
		ValDevice = valDevice;
	}

	public LocalDateTime getExecuted_Date() {
		return Executed_Date;
	}

	public void setExecuted_Date(LocalDateTime executed_Date) {
		Executed_Date = executed_Date;
	}

	public String getIssues() {
		return issues;
	}

	public void setIssues(String issues) {
		this.issues = issues;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getFileField() {
		return fileField;
	}

	public void setFileField(String fileField) {
		this.fileField = fileField;
	}

	public String getDBUrl() {
		return DBUrl;
	}

	public void setDBUrl(String DBUrl) {
		this.DBUrl = DBUrl;
	}

	public String getDBUsername() {
		return DBUsername;
	}

	public void setDBUsername(String DBUsername) {
		this.DBUsername = DBUsername;
	}

	public String getDBPwd() {
		return DBPwd;
	}

	public void setDBPwd(String DBPwd) {
		this.DBPwd = DBPwd;
	}

	public String getDBQuery() {
		return DBQuery;
	}

	public void setDBQuery(String DBQuery) {
		this.DBQuery = DBQuery;
	}

	public String getDBType() {
		return DBType;
	}

	public void setDBType(String DBType) {
		this.DBType = DBType;
	}

	public String getTestdata_source() {
		return testdata_source;
	}

	public void setTestdata_source(String testdata_source) {
		this.testdata_source = testdata_source;
	}

	public String getGendataType() {
		return gendataType;
	}

	public void setGendataType(String gendataType) {
		this.gendataType = gendataType;
	}

	public Integer getGendatalen() {
		return gendatalen;
	}

	public void setGendatalen(Integer gendatalen) {
		this.gendatalen = gendatalen;
	}

	public String getDBVersion() {
		return DBVersion;
	}

	public void setDBVersion(String DBVersion) {
		this.DBVersion = DBVersion;
	}

	public Integer getClickable() {
		return clickable;
	}

	public void setClickable(Integer clickable) {
		this.clickable = clickable;
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	public String getIframexpath() {
		return iframexpath;
	}

	public void setIframexpath(String iframexpath) {
		this.iframexpath = iframexpath;
	}

	public String getAltpath() {
		return altpath;
	}

	public void setAltpath(String altpath) {
		this.altpath = altpath;
	}

	public String getScreenshot1() {
		return screenshot1;
	}

	public void setScreenshot1(String screenshot1) {
		this.screenshot1 = screenshot1;
	}

	public String getThreshholdStatus() {
		return ThreshholdStatus;
	}

	public void setThreshholdStatus(String threshholdStatus) {
		ThreshholdStatus = threshholdStatus;
	}

	public String getIssuetype() {
		return issuetype;
	}

	public void setIssuetype(String issuetype) {
		this.issuetype = issuetype;
	}

}
