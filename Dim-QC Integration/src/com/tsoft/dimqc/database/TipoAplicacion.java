package com.tsoft.dimqc.database;

public enum TipoAplicacion {
	DIM("DIM"), QC("QC");

	private String value;

	private TipoAplicacion(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}
