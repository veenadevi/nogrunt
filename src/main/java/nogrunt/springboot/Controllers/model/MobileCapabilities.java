package nogrunt.springboot.Controllers.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "mobile_capabilities")
public class MobileCapabilities {

	@Id
	@Column(name = "capablities_Id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int capabilitiesId;

	@Column(name = "platformName")
	private String platformName;

	@Column(name = "platformVersion")
	private String platformVersion;

	@Column(name = "deviceName")
	private String deviceName;

	@Column(name = "automationName")
	private String automationName;

	@Column(name = "app")
	private String app;

	@Column(name = "label")
	private String label;

	@Column(name = "companyId")
	private Integer companyId;

	@Column(name = "Created_Date")
	private LocalDateTime createdDate;

	@Column(name = "capName")
	private String Name;

	public int getCapabilitiesId() {
		return capabilitiesId;
	}

	public void setCapabilitiesId(int capabilitiesId) {
		this.capabilitiesId = capabilitiesId;
	}

	public String getName() {
		return Name;
	}

	public void setName(String name) {
		Name = name;
	}

	public Integer getCompanyId() {
		return companyId;
	}

	public void setCompanyId(Integer companyId) {
		this.companyId = companyId;
	}

	public int getCapablities_Id() {
		return capabilitiesId;
	}

	public void setCapablities_Id(int capablities_Id) {
		this.capabilitiesId = capablities_Id;
	}

	public String getPlatformName() {
		return platformName;
	}

	public void setPlatformName(String platformName) {
		this.platformName = platformName;
	}

	public String getPlatformVersion() {
		return platformVersion;
	}

	public void setPlatformVersion(String platformVersion) {
		this.platformVersion = platformVersion;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getAutomationName() {
		return automationName;
	}

	public void setAutomationName(String automationName) {
		this.automationName = automationName;
	}

	public String getApp() {
		return app;
	}

	public void setApp(String app) {
		this.app = app;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public LocalDateTime getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(LocalDateTime createdDate) {
		this.createdDate = createdDate;
	}

}
