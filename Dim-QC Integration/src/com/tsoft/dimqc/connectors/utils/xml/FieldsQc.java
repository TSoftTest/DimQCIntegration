package com.tsoft.dimqc.connectors.utils.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "fields" })
@XmlRootElement(name = "Fields")
public class FieldsQc {

	@XmlElement(name = "Field", required = true)
	private List<FieldsQc.Field> fields = new ArrayList<FieldsQc.Field>();

	public FieldsQc() {
	}

	public void addFields(FieldsQc.Field field) {
		fields.add(field);
	}

	public List<FieldsQc.Field> getFields() {
		return fields;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Field {

		@XmlAttribute(name = "PhysicalName", required = true)
		private String physicalName;

		@XmlAttribute(name = "Name", required = true)
		private String name;

		@XmlAttribute(name = "Label", required = true)
		private String label;

		@XmlElement(name = "Size", required = true)
		private int size;

		@XmlElement(name = "History", required = true)
		private boolean history;

		@XmlElement(name = "Required", required = true)
		private boolean required;

		@XmlElement(name = "System", required = true)
		private boolean system;

		@XmlElement(name = "Type", required = true)
		private TypeFieldsXml type;

		@XmlElement(name = "Verify", required = true)
		private boolean verify;

		@XmlElement(name = "Virtual", required = true)
		private boolean virtual;

		@XmlElement(name = "Active", required = true)
		private boolean active;

		@XmlElement(name = "Visible", required = true)
		private boolean visible;

		@XmlElement(name = "Editable", required = true)
		private boolean editable;

		@XmlElement(name = "Filterable", required = true)
		private boolean filterable;

		@XmlElement(name = "Groupable", required = true)
		private boolean groupable;

		@XmlElement(name = "SupportsMultivalue", required = true)
		private boolean supportsMultivalue;

		@XmlElement(name = "List-Id", required = false)
		private int listId;

		public Field() {
		}

		public String getPhysicalName() {
			return physicalName;
		}

		public void setPhysicalName(String physicalName) {
			this.physicalName = physicalName;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public int getSize() {
			return size;
		}

		public void setSize(int size) {
			this.size = size;
		}

		public boolean isHistory() {
			return history;
		}

		public void setHistory(boolean history) {
			this.history = history;
		}

		public boolean isRequired() {
			return required;
		}

		public void setRequired(boolean required) {
			this.required = required;
		}

		public boolean isSystem() {
			return system;
		}

		public void setSystem(boolean system) {
			this.system = system;
		}

		public TypeFieldsXml getType() {
			return type;
		}

		public void setType(TypeFieldsXml type) {
			this.type = type;
		}

		public boolean isVerify() {
			return verify;
		}

		public void setVerify(boolean verify) {
			this.verify = verify;
		}

		public boolean isVirtual() {
			return virtual;
		}

		public void setVirtual(boolean virtual) {
			this.virtual = virtual;
		}

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}

		public boolean isVisible() {
			return visible;
		}

		public void setVisible(boolean visible) {
			this.visible = visible;
		}

		public boolean isEditable() {
			return editable;
		}

		public void setEditable(boolean editable) {
			this.editable = editable;
		}

		public boolean isFilterable() {
			return filterable;
		}

		public void setFilterable(boolean filterable) {
			this.filterable = filterable;
		}

		public boolean isGroupable() {
			return groupable;
		}

		public void setGroupable(boolean groupable) {
			this.groupable = groupable;
		}

		public boolean isSupportsMultivalue() {
			return supportsMultivalue;
		}

		public void setSupportsMultivalue(boolean supportsMultivalue) {
			this.supportsMultivalue = supportsMultivalue;
		}

		public int getListId() {
			return listId;
		}

		public void setListId(int listId) {
			this.listId = listId;
		}
	}
}
