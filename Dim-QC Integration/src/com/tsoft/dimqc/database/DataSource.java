package com.tsoft.dimqc.database;

import java.sql.Connection;

public abstract class DataSource {

	private String nombreBase;
	private String sdi;

	public DataSource() {
		this.nombreBase = "";
		this.sdi = "";
	}

	public DataSource(String nombreBase, String sdi) {
		this.nombreBase = nombreBase;
		this.sdi = sdi;
	}

	/**
	 * Método que inicializa la conexion en la base de datos que corresponda.
	 * 
	 * @return Una instancia de una conexion iniciada en la base de datos. Si se
	 *         produce algun problema iniciando la conexion se muestra el error y
	 *         devuelve null.
	 */
	public abstract Connection getConnection();

	/**
	 * Método con el cual se cargan los drivers.
	 * 
	 * @return void
	 * @throws Exception
	 */
	protected abstract void cargarDriver() throws Exception;

	public String getNombreBase() {
		return nombreBase;
	}

	public void setNombreBase(String nombreBase) {
		this.nombreBase = nombreBase;
	}

	public String getSdi() {
		return sdi;
	}

	public void setSdi(String sdi) {
		this.sdi = sdi;
	}
}
