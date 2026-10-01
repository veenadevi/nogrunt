package nogrunt.springboot.Controllers.model;

import jakarta.persistence.*;

@Entity
@Table(name = "test_step_attr")
public class MobileTestStepAttr {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "idtest_step_attr", nullable = false)
        private Integer id;

        @Column(name = "test_step")
        private Integer testStep;

        @Column(name = "imgpath", length = 500)
        private String imgPath;

        @Column(name = "bgcolor", length = 45)
        private String bgColor;

        @Column(name = "color", length = 45)
        private String color;

        @Column(name = "pageurl", length = 1000)
        private String pageUrl;

        @Column(name = "uniquetext", length = 100)
        private String uniqueText;

        @Column(name = "uniquetoparent", length = 1000)
        private String uniqueToParent;

        @Column(name = "parenttotarget", length = 1000)
        private String parentToTarget;

        @Column(name = "isDynamic", columnDefinition = "int default 0")
        private Integer isDynamic;

        @Column(name = "createsAlert", columnDefinition = "int default 0")
        private Integer createsAlert;

        @Column(name = "dynamicProcessed", columnDefinition = "int default 0")
        private Integer dynamicProcessed;

        @Column(name = "endrange", length = 45)
        private String endRange;

        @Column(name = "scope", length = 45)
        private String scope;

        @Column(name = "pagename", length = 45)
        private String pageName;

        @Column(name = "pagenumber", columnDefinition = "int default 0")
        private Integer pageNumber;

        @Column(name = "locationstrategy", length = 45)
        private String locationStrategy;

        @Column(name = "searchpageXpath", length = 1500)
        private String searchPageXpath;

        @Column(name = "shadowdom")
        private Integer shadowDom;

        @Column(name = "shadowindex")
        private Integer shadowIndex;

        @Column(name = "shadowelement", length = 45)
        private String shadowElement;

        @Column(name = "shadowpath", length = 500)
        private String shadowPath;

        @Column(name = "toaddress", length = 100)
        private String toAddress;

        @Column(name = "emailsubject", length = 100)
        private String emailSubject;

        @Column(name = "emailcontent", length = 500)
        private String emailContent;

        @Column(name = "uniqueisbackup", columnDefinition = "int default 0")
        private Integer uniqueIsBackup;

        @Column(name = "elementId")
        private Integer elementId;

        @Column(name = "scriptfile", length = 250)
        private String scriptFile;

        @Column(name = "script", length = 45)
        private String script;

        @Column(name = "cmdfile", length = 250)
        private String cmdFile;

        @Column(name = "nearestname", length = 100)
        private String nearestName;

        @Column(name = "parenttotargetcvrg", length = 1000)
        private String parentToTargetCvrg;

        @Column(name = "uniquetoparentcvrg", length = 1000)
        private String uniqueToParentCvrg;

        @Column(name = "indexcvrg")
        private Integer indexCvrg;

        @Column(name = "getattribute", length = 45)
        private String getAttribute;

        @Column(name = "xpos", columnDefinition = "int default 0")
        private Integer xPos;

        @Column(name = "ypos", columnDefinition = "int default 0")
        private Integer yPos;

        @Column(name = "elementplaceholder", length = 100)
        private String elementPlaceholder;

        @Column(name = "elementplaceholderindex", columnDefinition = "int default -1")
        private Integer elementPlaceholderIndex;

        @Column(name = "classname", length = 250)
        private String className;

        @Column(name = "classnameindex")
        private Integer classNameIndex;

        @Column(name = "href", length = 250)
        private String href;

        @Column(name = "hrefindex")
        private Integer hrefIndex;

        @Column(name = "typeindex")
        private Integer typeIndex;

        //Getter and Setter


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTestStep() {
        return testStep;
    }

    public void setTestStep(Integer testStep) {
        this.testStep = testStep;
    }

    public String getImgPath() {
        return imgPath;
    }

    public void setImgPath(String imgPath) {
        this.imgPath = imgPath;
    }

    public String getBgColor() {
        return bgColor;
    }

    public void setBgColor(String bgColor) {
        this.bgColor = bgColor;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getUniqueText() {
        return uniqueText;
    }

    public void setUniqueText(String uniqueText) {
        this.uniqueText = uniqueText;
    }

    public String getUniqueToParent() {
        return uniqueToParent;
    }

    public void setUniqueToParent(String uniqueToParent) {
        this.uniqueToParent = uniqueToParent;
    }

    public String getParentToTarget() {
        return parentToTarget;
    }

    public void setParentToTarget(String parentToTarget) {
        this.parentToTarget = parentToTarget;
    }

    public Integer getIsDynamic() {
        return isDynamic;
    }

    public void setIsDynamic(Integer isDynamic) {
        this.isDynamic = isDynamic;
    }

    public Integer getCreatesAlert() {
        return createsAlert;
    }

    public void setCreatesAlert(Integer createsAlert) {
        this.createsAlert = createsAlert;
    }

    public Integer getDynamicProcessed() {
        return dynamicProcessed;
    }

    public void setDynamicProcessed(Integer dynamicProcessed) {
        this.dynamicProcessed = dynamicProcessed;
    }

    public String getEndRange() {
        return endRange;
    }

    public void setEndRange(String endRange) {
        this.endRange = endRange;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getLocationStrategy() {
        return locationStrategy;
    }

    public void setLocationStrategy(String locationStrategy) {
        this.locationStrategy = locationStrategy;
    }

    public String getSearchPageXpath() {
        return searchPageXpath;
    }

    public void setSearchPageXpath(String searchPageXpath) {
        this.searchPageXpath = searchPageXpath;
    }

    public Integer getShadowDom() {
        return shadowDom;
    }

    public void setShadowDom(Integer shadowDom) {
        this.shadowDom = shadowDom;
    }

    public Integer getShadowIndex() {
        return shadowIndex;
    }

    public void setShadowIndex(Integer shadowIndex) {
        this.shadowIndex = shadowIndex;
    }

    public String getShadowElement() {
        return shadowElement;
    }

    public void setShadowElement(String shadowElement) {
        this.shadowElement = shadowElement;
    }

    public String getShadowPath() {
        return shadowPath;
    }

    public void setShadowPath(String shadowPath) {
        this.shadowPath = shadowPath;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    public String getEmailContent() {
        return emailContent;
    }

    public void setEmailContent(String emailContent) {
        this.emailContent = emailContent;
    }

    public Integer getUniqueIsBackup() {
        return uniqueIsBackup;
    }

    public void setUniqueIsBackup(Integer uniqueIsBackup) {
        this.uniqueIsBackup = uniqueIsBackup;
    }

    public Integer getElementId() {
        return elementId;
    }

    public void setElementId(Integer elementId) {
        this.elementId = elementId;
    }

    public String getScriptFile() {
        return scriptFile;
    }

    public void setScriptFile(String scriptFile) {
        this.scriptFile = scriptFile;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getCmdFile() {
        return cmdFile;
    }

    public void setCmdFile(String cmdFile) {
        this.cmdFile = cmdFile;
    }

    public String getNearestName() {
        return nearestName;
    }

    public void setNearestName(String nearestName) {
        this.nearestName = nearestName;
    }

    public String getParentToTargetCvrg() {
        return parentToTargetCvrg;
    }

    public void setParentToTargetCvrg(String parentToTargetCvrg) {
        this.parentToTargetCvrg = parentToTargetCvrg;
    }

    public String getUniqueToParentCvrg() {
        return uniqueToParentCvrg;
    }

    public void setUniqueToParentCvrg(String uniqueToParentCvrg) {
        this.uniqueToParentCvrg = uniqueToParentCvrg;
    }

    public Integer getIndexCvrg() {
        return indexCvrg;
    }

    public void setIndexCvrg(Integer indexCvrg) {
        this.indexCvrg = indexCvrg;
    }

    public String getGetAttribute() {
        return getAttribute;
    }

    public void setGetAttribute(String getAttribute) {
        this.getAttribute = getAttribute;
    }

    public Integer getxPos() {
        return xPos;
    }

    public void setxPos(Integer xPos) {
        this.xPos = xPos;
    }

    public Integer getyPos() {
        return yPos;
    }

    public void setyPos(Integer yPos) {
        this.yPos = yPos;
    }

    public String getElementPlaceholder() {
        return elementPlaceholder;
    }

    public void setElementPlaceholder(String elementPlaceholder) {
        this.elementPlaceholder = elementPlaceholder;
    }

    public Integer getElementPlaceholderIndex() {
        return elementPlaceholderIndex;
    }

    public void setElementPlaceholderIndex(Integer elementPlaceholderIndex) {
        this.elementPlaceholderIndex = elementPlaceholderIndex;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Integer getClassNameIndex() {
        return classNameIndex;
    }

    public void setClassNameIndex(Integer classNameIndex) {
        this.classNameIndex = classNameIndex;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public Integer getHrefIndex() {
        return hrefIndex;
    }

    public void setHrefIndex(Integer hrefIndex) {
        this.hrefIndex = hrefIndex;
    }

    public Integer getTypeIndex() {
        return typeIndex;
    }

    public void setTypeIndex(Integer typeIndex) {
        this.typeIndex = typeIndex;
    }
}
