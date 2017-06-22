package com.tsoft.dimqc.connectors.alm;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "fields" })
@XmlRootElement(name = "Entity")
public class Entity {
	/**
	 *
	 * Java class for anonymous complex type.
	 *
	 * The following schema fragment specifies the expected content contained
	 * within this class.
	 *
	 * 
	 * <complexType> <complexContent> <restriction
	 * base="{http://www.w3.org/2001/XMLSchema}anyType"> <sequence> <element
	 * name="Fields"> <complexType> <complexContent> <restriction
	 * base="{http://www.w3.org/2001/XMLSchema}anyType"> <sequence> <element
	 * name="Field" maxOccurs="unbounded"> <complexType> <complexContent>
	 * <restriction base="{http://www.w3.org/2001/XMLSchema}anyType"> <sequence>
	 * <element name="Value" type="{http://www.w3.org/2001/XMLSchema}string"
	 * maxOccurs="unbounded"/> </sequence> <attribute name="Name" use="required"
	 * type="{http://www.w3.org/2001/XMLSchema}string" /> </restriction>
	 * </complexContent> </complexType> </element> </sequence> </restriction>
	 * </complexContent> </complexType> </element> </sequence> <attribute
	 * name="Type" use="required" type="{http://www.w3.org/2001/XMLSchema}string"
	 * /> </restriction> </complexContent> </complexType>
	 * 
	 * 
	 *
	 *
	 */
	@XmlElement(name = "Fields", required = true)
	protected Entity.Fields fields;
	@XmlAttribute(name = "Type", required = true)
	protected String type;

	/**
	 *
	 */
	public Entity() {
	}

	/**
	 * Gets the value of the fields property.
	 *
	 * @return possible object is {@link Entity.Fields }
	 *
	 */
	public Entity.Fields getFields() {
		return fields;
	}

	/**
	 * Sets the value of the fields property.
	 *
	 * @param value
	 *          allowed object is {@link Entity.Fields }
	 *
	 */
	public void setFields(Entity.Fields value) {
		this.fields = value;
	}

	/**
	 * Gets the value of the type property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the value of the type property.
	 *
	 * @param value
	 *          allowed object is {@link String }
	 *
	 */
	public void setType(String value) {
		this.type = value;
	}

	/**
	 * Java class for anonymous complex type.
	 *
	 * The following schema fragment specifies the expected content contained
	 * within this class.
	 *
	 * 
	 * <complexType> <complexContent> <restriction
	 * base="{http://www.w3.org/2001/XMLSchema}anyType"> <sequence> <element
	 * name="Field" maxOccurs="unbounded"> <complexType> <complexContent>
	 * <restriction base="{http://www.w3.org/2001/XMLSchema}anyType"> <sequence>
	 * <element name="Value" type="{http://www.w3.org/2001/XMLSchema}string"
	 * maxOccurs="unbounded"/> </sequence> <attribute name="Name" use="required"
	 * type="{http://www.w3.org/2001/XMLSchema}string" /> </restriction>
	 * </complexContent> </complexType> </element> </sequence> </restriction>
	 * </complexContent> </complexType>
	 * 
	 * 
	 *
	 *
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = { "field" })
	public static class Fields {
		@XmlElement(name = "Field", required = true)
		protected List<Entity.Fields.Field> field;

		/**
		 *
		 */
		public Fields() {

		}

		/**
		 * Gets the value of the field property.
		 *
		 * This accessor method returns a reference to the live list, not a
		 * snapshot. Therefore any modification you make to the returned list will
		 * be present inside the JAXB object. This is why there is not a set method
		 * for the field property.
		 *
		 *
		 * For example, to add a new item, do as follows:
		 *
		 * 
		 * getField().add(newItem);
		 * 
		 * 
		 *
		 *
		 * Objects of the following type(s) are allowed in the list
		 * {@link Entity.Fields.Field }
		 *
		 *
		 */
		public List<Entity.Fields.Field> getField() {
			if (field == null) {
				field = new ArrayList<Entity.Fields.Field>();
			}
			return this.field;
		}

		/**
		 * @param value
		 *          Field to be added to the fields.
		 */
		public void addField(Entity.Fields.Field value) {
			this.getField().add(value);
		}

		/**
		 * returns a field by its name
		 * 
		 * @param name
		 * @return the field
		 */
		public Entity.Fields.Field getFieldByName(String name) {
			for (Entity.Fields.Field value : this.getField()) {
				if (value.getName().equals(name))
					return value;
			}
			return null;
		}

		/**
		 * Java class for anonymous complex type.
		 *
		 * The following schema fragment specifies the expected content contained
		 * within this class.
		 *
		 * 
		 * <complexType> <complexContent> <restriction
		 * base="{http://www.w3.org/2001/XMLSchema}anyType"> <sequence> <element
		 * name="Value" type="{http://www.w3.org/2001/XMLSchema}string"
		 * maxOccurs="unbounded"/> </sequence> <attribute name="Name" use="required"
		 * type="{http://www.w3.org/2001/XMLSchema}string" /> </restriction>
		 * </complexContent> </complexType>
		 * 
		 * 
		 *
		 *
		 */
		@XmlAccessorType(XmlAccessType.FIELD)
		public static class Field {
			public List<Entity.Fields.Field.Value> Value = new ArrayList<Entity.Fields.Field.Value>();
			@XmlAttribute(name = "Name", required = true)
			protected String name;

			/**
			 * Gets the value of the value property.
			 *
			 * This accessor method returns a reference to the live list, not a
			 * snapshot. Therefore any modification you make to the returned list will
			 * be present inside the JAXB object. This is why there is not a set
			 * method for the value property.
			 *
			 * For example, to add a new item, do as follows:
			 *
			 * 
			 * getValue().add(newItem);
			 * 
			 * 
			 *
			 *
			 * Objects of the following type(s) are allowed in the list {@link String }
			 *
			 *
			 */
			public List<Entity.Fields.Field.Value> getValue() {
				return this.Value;
			}

			public void addValue(Entity.Fields.Field.Value value) {
				this.Value.add(value);
			}

			/**
			 * Devuelve el primer valor
			 * 
			 * @return
			 * 
			 * 
			 *         /** Gets the value of the name property.
			 *
			 * @return possible object is {@link String }
			 *
			 */
			public String getName() {
				return name;
			}

			/**
			 * Sets the value of the name property.
			 *
			 * @param value
			 *          allowed object is {@link String }
			 *
			 */
			public void setName(String value) {
				this.name = value;
			}

			@Override
			public String toString() {
				return getName();
			}

			@XmlAccessorType(XmlAccessType.FIELD)
			@XmlType(name = "ReferenceValue")
			public static class Value {
				@XmlAttribute(name = "ReferenceValue")
				protected String referenceValue;
				@XmlValue
				protected String value;

				public String getReferenceValue() {
					return referenceValue;
				}

				/**
				 * Sets the value of the name property.
				 *
				 * @param value
				 *          allowed object is {@link String }
				 *
				 */
				public void setReferenceValue(String value) {
					this.referenceValue = value;
				}

				public String getValue() {
					if (value == null) {
						value = "";
					}
					return this.value;
				}

				public void setValue(String value) {
					this.value = value;
				}
			}
		}
	}
}
