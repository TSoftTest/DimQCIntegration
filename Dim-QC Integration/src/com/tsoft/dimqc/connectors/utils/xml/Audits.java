package com.tsoft.dimqc.connectors.utils.xml;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "totalResults", "audits" })
@XmlRootElement(name = "Audits")
public class Audits {

	@XmlAttribute(name = "TotalResults", required = true)
	private int totalResults;

	@XmlElement(name = "Audit")
	private List<Audit> audits = new ArrayList<Audits.Audit>();

	public Audits() {
	}

	public int getTotalResults() {
		return totalResults;
	}

	public void setTotalResults(int totalResults) {
		this.totalResults = totalResults;
	}

	public List<Audit> getAudits() {
		return audits;
	}

	public void addAudits(Audit audit) {
		this.audits.add(audit);
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Audit {

		@XmlElement(name = "Id", required = true)
		private int id;

		@XmlElement(name = "Action", required = false)
		private String action;

		@XmlElement(name = "ParentType", required = false)
		private String parentType;

		@XmlElement(name = "Time", required = true)
		private Date time;

		@XmlElement(name = "User", required = true)
		private String user;

		@XmlElement(name = "Properties", required = true)
		private Properties properties;

		public Audit() {
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}

		public String getParentType() {
			return parentType;
		}

		public void setParentType(String parentType) {
			this.parentType = parentType;
		}

		public Date getTime() {
			return time;
		}

		public void setTime(Date time) {
			this.time = time;
		}

		public String getUser() {
			return user;
		}

		public void setUser(String user) {
			this.user = user;
		}

		public Properties getProperties() {
			return properties;
		}

		public void setProperties(Properties properties) {
			this.properties = properties;
		}

		// Properties
		@XmlAccessorType(XmlAccessType.FIELD)
		public static class Properties {

			@XmlElement(name = "Property", required = true)
			private Property property;

			@XmlElement(name = "NewValue", required = true)
			private String newValue;

			@XmlElement(name = "OldValue", required = true)
			private String oldValue;

			public Properties() {
			}

			public String getNewValue() {
				return newValue;
			}

			public void setNewValue(String newValue) {
				this.newValue = newValue;
			}

			public String getOldValue() {
				return oldValue;
			}

			public void setOldValue(String oldValue) {
				this.oldValue = oldValue;
			}

			public Property getProperty() {
				return property;
			}

			public void setProperty(Property property) {
				this.property = property;
			}

			// Property
			@XmlAccessorType(XmlAccessType.FIELD)
			public static class Property {

				@XmlAttribute(name = "Label", required = true)
				private String label;

				@XmlAttribute(name = "Name", required = true)
				private String name;

				public Property() {
				}

				public String getLabel() {
					return label;
				}

				public void setLabel(String label) {
					this.label = label;
				}

				public String getName() {
					return name;
				}

				public void setName(String name) {
					this.name = name;
				}
			}
		}
	}
}
