package nogrunt.springboot.Controllers.model;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "test_step")
public class MobileTestStep {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "idtest_step")
        private Integer idTestStep;

        @Column(name = "Test_Case_Id", nullable = true)
        private Integer testCaseId;

        @Column(name = "Step_Number", nullable = true)
        private Integer stepNumber;

        @Column(name = "Page_Name", length = 45, nullable = true)
        private String pageName;

        @Column(name = "Page_Description", length = 250, nullable = true)
        private String pageDescription;

        @Column(name = "Object_Xpath", length = 1500, nullable = true)
        private String objectXpath;

        @Column(name = "Keyword", length = 45, nullable = true)
        private String keyword;

        @Column(name = "Action", length = 45, nullable = true)
        private String action;

        @Column(name = "subAction", length = 45, nullable = true)
        private String subAction;

        @Column(name = "Flow", length = 45, nullable = true)
        private String flow;

        @Column(name = "TestData", length = 1000, nullable = true)
        private String testData;

        @Column(name = "VarName", length = 45, nullable = true)
        private String varName;

        @Column(name = "ValDevice", length = 45, nullable = true)
        private String valDevice;

        @Column(name = "extKey", nullable = true)
        private Integer extKey;

        @Column(name = "eventTime", nullable = true)
        private Double eventTime;

        @Column(name = "recordeddata", length = 1000, nullable = true)
        private String recordedData;

        @Column(name = "filename", length = 250, nullable = true)
        private String filename;

        @Column(name = "filefield", length = 500, nullable = true)
        private String fileField;

        @Column(name = "DBUrl", length = 250, nullable = true)
        private String dbUrl;

        @Column(name = "apiid", nullable = true)
        private Integer apiId;

        @Column(name = "apiparam", length = 100, nullable = true)
        private String apiParam;

        @Column(name = "DBQuery", length = 1000)
        private String dbQuery;

        @Column(name = "status")
        private Integer status;

        @Column(name = "DBType", length = 45, nullable = true)
        private String dbType;

        @Column(name = "testdata_source", length = 45, nullable = true)
        private String testDataSource;

        @Column(name = "gendataType", length = 45, nullable = true)
        private String genDataType;

        @Column(name = "gendatalen", nullable = true)
        private Integer genDataLen;

        @Column(name = "gendateformat", length = 15, nullable = true)
        private String genDateFormat;

        @Column(name = "gendateoffset", length = 15, nullable = true)
        private String genDateOffset;

        @Column(name = "DBVersion", length = 45, nullable = true)
        private String dbVersion;

        @Column(name = "clickable", nullable = true)
        private Integer clickable;

        @Column(name = "Issues", length = 1000, nullable = true)
        private String issues;

        @Column(name = "screenshot", length = 250, nullable = true)
        private String screenshot;

        @Column(name = "actualtime", nullable = true)
        private Long actualTime;

        @Column(name = "eventname", length = 45, nullable = true)
        private String eventName;

        @Column(name = "regex", length = 150, nullable = true)
        private String regex;

        @Column(name = "iframexpath", length = 1500, nullable = true)
        private String iframeXpath;

        @Column(name = "altpath", length = 500, nullable = true)
        private String altPath;

        @Column(name = "outerhtml", length = 4000, nullable = true)
        private String outerHtml;

        @Column(name = "wait", length = 45, nullable = true)
        private String wait;

        @Column(name = "waittime", nullable = true)
        private Integer waitTime;

        @Column(name = "dependantTC", nullable = true)
        private Integer dependantTC;

        @Column(name = "manuallyAdded", length = 45, nullable = true)
        private String manuallyAdded;

        @Column(name = "recordedXpath", length = 1500, nullable = true)
        private String recordedXpath;

        @Column(name = "samerowtype", length = 45, nullable = true)
        private String sameRowType;

        @Column(name = "samerowindex", nullable = true)
        private Integer sameRowIndex;

        @Column(name = "tabid", nullable = true)
        private Integer tabId;

        @Column(name = "windowid", nullable = true)
        private Integer windowId;

        @Column(name = "teststepthreshold", nullable = true)
        private Integer testStepThreshold;

        @Column(name = "customerEmail", length = 100, nullable = true)
        private String customerEmail;

        @Column(name = "customerPassword", length = 45, nullable = true)
        private String customerPassword;

        @Column(name = "EmailSelectionCriteria", length = 100, nullable = true)
        private String emailSelectionCriteria;

        @Column(name = "EmailFilter", length = 100, nullable = true)
        private String emailFilter;

        @Column(name = "type", length = 45, nullable = true)
        private String type;

        @Column(name = "createddate", nullable = true)
        private Date createdDate;

        @Column(name = "ts_sequence")
        private Integer tsSequence;

    public Integer getIdTestStep() {
        return idTestStep;
    }

    public void setIdTestStep(Integer idTestStep) {
        this.idTestStep = idTestStep;
    }

    public Integer getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(Integer testCaseId) {
        this.testCaseId = testCaseId;
    }

    public Integer getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public String getPageDescription() {
        return pageDescription;
    }

    public void setPageDescription(String pageDescription) {
        this.pageDescription = pageDescription;
    }

    public String getObjectXpath() {
        return objectXpath;
    }

    public void setObjectXpath(String objectXpath) {
        this.objectXpath = objectXpath;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSubAction() {
        return subAction;
    }

    public void setSubAction(String subAction) {
        this.subAction = subAction;
    }

    public String getFlow() {
        return flow;
    }

    public void setFlow(String flow) {
        this.flow = flow;
    }

    public String getTestData() {
        return testData;
    }

    public void setTestData(String testData) {
        this.testData = testData;
    }

    public String getVarName() {
        return varName;
    }

    public void setVarName(String varName) {
        this.varName = varName;
    }

    public String getValDevice() {
        return valDevice;
    }

    public void setValDevice(String valDevice) {
        this.valDevice = valDevice;
    }

    public Integer getExtKey() {
        return extKey;
    }

    public void setExtKey(Integer extKey) {
        this.extKey = extKey;
    }

    public Double getEventTime() {
        return eventTime;
    }

    public void setEventTime(Double eventTime) {
        this.eventTime = eventTime;
    }

    public String getRecordedData() {
        return recordedData;
    }

    public void setRecordedData(String recordedData) {
        this.recordedData = recordedData;
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

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public Integer getApiId() {
        return apiId;
    }

    public void setApiId(Integer apiId) {
        this.apiId = apiId;
    }

    public String getApiParam() {
        return apiParam;
    }

    public void setApiParam(String apiParam) {
        this.apiParam = apiParam;
    }

    public String getDbQuery() {
        return dbQuery;
    }

    public void setDbQuery(String dbQuery) {
        this.dbQuery = dbQuery;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public String getTestDataSource() {
        return testDataSource;
    }

    public void setTestDataSource(String testDataSource) {
        this.testDataSource = testDataSource;
    }

    public String getGenDataType() {
        return genDataType;
    }

    public void setGenDataType(String genDataType) {
        this.genDataType = genDataType;
    }

    public Integer getGenDataLen() {
        return genDataLen;
    }

    public void setGenDataLen(Integer genDataLen) {
        this.genDataLen = genDataLen;
    }

    public String getGenDateFormat() {
        return genDateFormat;
    }

    public void setGenDateFormat(String genDateFormat) {
        this.genDateFormat = genDateFormat;
    }

    public String getGenDateOffset() {
        return genDateOffset;
    }

    public void setGenDateOffset(String genDateOffset) {
        this.genDateOffset = genDateOffset;
    }

    public String getDbVersion() {
        return dbVersion;
    }

    public void setDbVersion(String dbVersion) {
        this.dbVersion = dbVersion;
    }

    public Integer getClickable() {
        return clickable;
    }

    public void setClickable(Integer clickable) {
        this.clickable = clickable;
    }

    public String getIssues() {
        return issues;
    }

    public void setIssues(String issues) {
        this.issues = issues;
    }

    public String getScreenshot() {
        return screenshot;
    }

    public void setScreenshot(String screenshot) {
        this.screenshot = screenshot;
    }

    public Long getActualTime() {
        return actualTime;
    }

    public void setActualTime(Long actualTime) {
        this.actualTime = actualTime;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getIframeXpath() {
        return iframeXpath;
    }

    public void setIframeXpath(String iframeXpath) {
        this.iframeXpath = iframeXpath;
    }

    public String getAltPath() {
        return altPath;
    }

    public void setAltPath(String altPath) {
        this.altPath = altPath;
    }

    public String getOuterHtml() {
        return outerHtml;
    }

    public void setOuterHtml(String outerHtml) {
        this.outerHtml = outerHtml;
    }

    public String getWait() {
        return wait;
    }

    public void setWait(String wait) {
        this.wait = wait;
    }

    public Integer getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(Integer waitTime) {
        this.waitTime = waitTime;
    }

    public Integer getDependantTC() {
        return dependantTC;
    }

    public void setDependantTC(Integer dependantTC) {
        this.dependantTC = dependantTC;
    }

    public String getManuallyAdded() {
        return manuallyAdded;
    }

    public void setManuallyAdded(String manuallyAdded) {
        this.manuallyAdded = manuallyAdded;
    }

    public String getRecordedXpath() {
        return recordedXpath;
    }

    public void setRecordedXpath(String Recorded_Xpath) {
        this.recordedXpath = recordedXpath;
    }

    public String getSameRowType() {
        return sameRowType;
    }

    public void setSameRowType(String sameRowType) {
        this.sameRowType = sameRowType;
    }

    public Integer getSameRowIndex() {
        return sameRowIndex;
    }

    public void setSameRowIndex(Integer sameRowIndex) {
        this.sameRowIndex = sameRowIndex;
    }

    public Integer getTabId() {
        return tabId;
    }

    public void setTabId(Integer tabId) {
        this.tabId = tabId;
    }

    public Integer getWindowId() {
        return windowId;
    }

    public void setWindowId(Integer windowId) {
        this.windowId = windowId;
    }

    public Integer getTestStepThreshold() {
        return testStepThreshold;
    }

    public void setTestStepThreshold(Integer testStepThreshold) {
        this.testStepThreshold = testStepThreshold;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPassword() {
        return customerPassword;
    }

    public void setCustomerPassword(String customerPassword) {
        this.customerPassword = customerPassword;
    }

    public String getEmailSelectionCriteria() {
        return emailSelectionCriteria;
    }

    public void setEmailSelectionCriteria(String emailSelectionCriteria) {
        this.emailSelectionCriteria = emailSelectionCriteria;
    }

    public String getEmailFilter() {
        return emailFilter;
    }

    public void setEmailFilter(String emailFilter) {
        this.emailFilter = emailFilter;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Integer getTsSequence() {
        return tsSequence;
    }

    public void setTsSequence(Integer tsSequence) {
        this.tsSequence = tsSequence;
    }
}
