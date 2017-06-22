package ar.com.tssa.serena.connectors.alm;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "lockStatus" })
@XmlRootElement(name = "LockStatusEntity")
public class LockStatusEntity {
	
	@XmlElement(name = "LockStatus", required = true)
	protected String lockStatus;
	
	/**
	 *
	 */
	public LockStatusEntity() {}
	/**
	 * Gets the value of the fields property.
	 *
	 * @return possible object is {@link LockStatusEntity.Fields }
	 *
	 */
	public String getLockStatus() {
		return lockStatus;
	}
	/**
	 * Sets the value of the fields property.
	 *
	 * @param value
	 * allowed object is {@link LockStatusEntity.Fields }
	 *
	 */
	public void setLockStatus(String value) {
		this.lockStatus = value;
	}
	public boolean isLocked(){
		return !this.lockStatus.equalsIgnoreCase("UNLOCKED");
	}

}
