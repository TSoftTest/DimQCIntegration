package com.tsoft.dimqc.connectors.utils.xml;

public enum TypeFieldsXml {

	Number("Number"), UsersList("UsersList"), String("String"), LookupList("LookupList"), Reference("Reference"), Memo("Memo"), Date("Date"), DateTime("DateTime");

	private String value;

	private TypeFieldsXml(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
