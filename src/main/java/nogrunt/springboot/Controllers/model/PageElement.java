package nogrunt.springboot.Controllers.model;

import jakarta.persistence.*;

@Entity
@Table(name = "page_elements")
public class PageElement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int elementid;
    private String elementname;
    private String elementtype;
    private Integer pageid;
    private String recordedxpath;
    private String clickable;
    private Integer coveragecount;
    private String nearestname;
    private String recorded;
    private Integer status;
    private String uniquename;
    
    // Getters and Setters
	public Integer getElementid() {
		return elementid;
	}
	public void setElementid(int elementid) {
		this.elementid = elementid;
	}
	public String getElementname() {
		return elementname;
	}
	public void setElementname(String elementname) {
		this.elementname = elementname;
	}
	public String getElementtype() {
		return elementtype;
	}
	public void setElementtype(String elementtype) {
		this.elementtype = elementtype;
	}
	public Integer getPageid() {
		return pageid;
	}
	public void setPageid(int pageid) {
		this.pageid = pageid;
	}
	public String getRecordedxpath() {
		return recordedxpath;
	}
	public void setRecordedxpath(String recordedxpath) {
		this.recordedxpath = recordedxpath;
	}
	public String isClickable() {
		return clickable;
	}
	public void setClickable(String clickable) {
		this.clickable = clickable;
	}
	public Integer getCoveragecount() {
		return coveragecount;
	}
	public void setCoveragecount(int coveragecount) {
		this.coveragecount = coveragecount;
	}
	public String getNearestname() {
		return nearestname;
	}
	public void setNearestname(String nearestname) {
		this.nearestname = nearestname;
	}
	public String getRecorded() {
		return recorded;
	}
	public void setRecorded(String recorded) {
		this.recorded = recorded;
	}
	public Integer getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String getUniquename() {
		return uniquename;
	}
	public void setUniquename(String uniquename) {
		this.uniquename = uniquename;
	}
}