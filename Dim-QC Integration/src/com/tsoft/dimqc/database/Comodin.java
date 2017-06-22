package com.tsoft.dimqc.database;

public enum Comodin {

	USUARIO_DIM("%usrdim%"), USUARIO_QC("%usrqc%"), PROYECTOS_QC("%proyects%");

	private String value;

	private Comodin(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}
