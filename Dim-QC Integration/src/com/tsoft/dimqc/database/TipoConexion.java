package com.tsoft.dimqc.database;

public enum TipoConexion {

	ORACLE("oracle"), SQL_SERVER("sql_server");

	private String value;

	private TipoConexion(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}
