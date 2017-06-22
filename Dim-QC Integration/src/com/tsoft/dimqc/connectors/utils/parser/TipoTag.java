package com.tsoft.dimqc.connectors.utils.parser;

public enum TipoTag {

	HTML("html"), HEAD("head"), BODY("body"), DIV("div"), FONT("font"), SPAN("span"), OL("ol"), LI("li"), UL("ul"), TBODY("tbody"), TABLE("table"), TR("tr"), TD("td"), COLGROUP(
	    "colgroup"), COL("col");

	private String value;

	private TipoTag(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}
