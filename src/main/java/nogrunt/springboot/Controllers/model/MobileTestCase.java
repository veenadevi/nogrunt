package nogrunt.springboot.Controllers.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_case")
public class MobileTestCase {

    @Id
    @Column(name = "idtest_case")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idtest_case;

    @Column(name = "Test_Case")
    private String testCase;

    @Column(name = "testcasedescription")
    private String testCaseDescription;

    @Column(name = "XLS_Location")
    private String xlsLocation;

    @Column(name = "version")
    private String version;

    @Column(name = "Created_By")
    private String createdBy;

    @Column(name = "Created_Date")
    private LocalDateTime createdDate;

    @Column(name = "SaveType")
    private String SaveType;

    @Column(name = "status")
    private String status;

    @Column(name = "extKey")
    private Integer extKey;

    @Column(name = "module")
    private String module;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "browser")
    private String browser;

    @Column(name = "envname")
    private String envname;

    @Column(name = "envurl")
    private String envurl;

    @Column(name = "copiedfrom")
    private String copiedfrom;

    @Column(name = "testcasethreshold")
    private Integer testcasethreshold;

    @Column(name = "videofilename")
    private String videofilename;

    @Column(name = "ss_status")
    private String ss_status;

    @Column(name = "laststeprecordedtime")
    private String laststeprecordedtime;

    @Column(name = "continuetest")
    private Boolean continuetest;

    @Column(name = "multirun")
    private Integer multirun;

    @Column(name = "coverageflag")
    private String coverageflag;

    @Column(name = "proxyurl")
    private String proxyurl;

    @Column(name = "beenanalysed")
    private Boolean beenanalysed;

    @Column(name = "analysisdate")
    private String analysisdate;

    @Column(name = "imagesbaselined")
    private Boolean imagesbaselined;

    @Column(name = "baselinedate")
    private String baselinedate;

    @Column(name = "enableaudio")
    private Boolean enableaudio;

    @Column(name = "enablevideo")
    private Boolean enablevideo;

    @Column(name = "forcenewsession")
    private Boolean forcenewsession;

    @Column(name = "case_Type")
    private String caseType;

    // Getters and Setters
    public int getIdtest_case() {
        return idtest_case;
    }

    public void setIdtest_case(int idtest_case) {
        this.idtest_case = idtest_case;
    }

    public String getTestCase() {
        return testCase;
    }

    public void setTestCase(String testCase) {
        this.testCase = testCase;
    }

    public String getTestCaseDescription() {
        return testCaseDescription;
    }

    public void setTestCaseDescription(String testCaseDescription) {
        this.testCaseDescription = testCaseDescription;
    }

    public String getXlsLocation() {
        return xlsLocation;
    }

    public void setXlsLocation(String xlsLocation) {
        this.xlsLocation = xlsLocation;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getSaveType() {
        return SaveType;
    }

    public void setSaveType(String saveType) {
        SaveType = saveType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getExtKey() {
        return extKey;
    }

    public void setExtKey(Integer extKey) {
        this.extKey = extKey;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
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

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getEnvname() {
        return envname;
    }

    public void setEnvname(String envname) {
        this.envname = envname;
    }

    public String getEnvurl() {
        return envurl;
    }

    public void setEnvurl(String envurl) {
        this.envurl = envurl;
    }

    public String getCopiedfrom() {
        return copiedfrom;
    }

    public void setCopiedfrom(String copiedfrom) {
        this.copiedfrom = copiedfrom;
    }

    public Integer getTestcasethreshold() {
        return testcasethreshold;
    }

    public void setTestcasethreshold(Integer testcasethreshold) {
        this.testcasethreshold = testcasethreshold;
    }

    public String getVideofilename() {
        return videofilename;
    }

    public void setVideofilename(String videofilename) {
        this.videofilename = videofilename;
    }

    public String getSs_status() {
        return ss_status;
    }

    public void setSs_status(String ss_status) {
        this.ss_status = ss_status;
    }

    public String getLaststeprecordedtime() {
        return laststeprecordedtime;
    }

    public void setLaststeprecordedtime(String laststeprecordedtime) {
        this.laststeprecordedtime = laststeprecordedtime;
    }

    public Boolean getContinuetest() {
        return continuetest;
    }

    public void setContinuetest(Boolean continuetest) {
        this.continuetest = continuetest;
    }

    public Integer getMultirun() {
        return multirun;
    }

    public void setMultirun(Integer multirun) {
        this.multirun = multirun;
    }

    public String getCoverageflag() {
        return coverageflag;
    }

    public void setCoverageflag(String coverageflag) {
        this.coverageflag = coverageflag;
    }

    public String getProxyurl() {
        return proxyurl;
    }

    public void setProxyurl(String proxyurl) {
        this.proxyurl = proxyurl;
    }

    public Boolean getBeenanalysed() {
        return beenanalysed;
    }

    public void setBeenanalysed(Boolean beenanalysed) {
        this.beenanalysed = beenanalysed;
    }

    public String getAnalysisdate() {
        return analysisdate;
    }

    public void setAnalysisdate(String analysisdate) {
        this.analysisdate = analysisdate;
    }

    public Boolean getImagesbaselined() {
        return imagesbaselined;
    }

    public void setImagesbaselined(Boolean imagesbaselined) {
        this.imagesbaselined = imagesbaselined;
    }

    public String getBaselinedate() {
        return baselinedate;
    }

    public void setBaselinedate(String baselinedate) {
        this.baselinedate = baselinedate;
    }

    public Boolean getEnableaudio() {
        return enableaudio;
    }

    public void setEnableaudio(Boolean enableaudio) {
        this.enableaudio = enableaudio;
    }

    public Boolean getEnablevideo() {
        return enablevideo;
    }

    public void setEnablevideo(Boolean enablevideo) {
        this.enablevideo = enablevideo;
    }

    public Boolean getForcenewsession() {
        return forcenewsession;
    }

    public void setForcenewsession(Boolean forcenewsession) {
        this.forcenewsession = forcenewsession;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }
}
