package com.tsoft.dimqc.database;

import java.sql.Connection;
import java.sql.DriverManager;

import org.apache.log4j.Logger;

import com.tsoft.dimqc.connectors.utils.ConnectorProperties;

public class DataSourceOracleQc extends DataSource {

	private Logger logger = Logger.getRootLogger();

	public DataSourceOracleQc(String nombreBase, String sdi) {
		super(nombreBase, sdi);
		try {
			this.cargarDriver();
		} catch (Exception e) {
			logger.error("Error cargando los drivers de Oracle:" + e.getMessage());
		}
	}

	@Override
	public Connection getConnection() {
		String url = "";
		try {

			if (ConnectorProperties.getInstance().getQcDataBaseHost() == null || "".equals(ConnectorProperties.getInstance().getQcDataBaseHost())) {
				throw new Exception("No se especifico host.");
			}

			if (ConnectorProperties.getInstance().getQcDataBasePort() == -1) {
				throw new Exception("Error con el puerto.");
			}

			if (this.getNombreBase() == null || "".equals(this.getNombreBase())) {
				throw new Exception("No se especifico nombre de la base.");
			}

			if (this.getSdi() == null || "".equals(this.getSdi())) {
				throw new Exception("No se especifico sdi.");
			}

			if (ConnectorProperties.getInstance().getQcDataBaseUser() == null || "".equals(ConnectorProperties.getInstance().getQcDataBaseUser())) {
				throw new Exception("No se especifico usuario.");
			}

			if (ConnectorProperties.getInstance().getQcDataBasePass() == null || "".equals(ConnectorProperties.getInstance().getQcDataBasePass())) {
				throw new Exception("No se especifico password.");
			}

			url = "jdbc:oracle:thin:@" + ConnectorProperties.getInstance().getQcDataBaseHost() + ":" + ConnectorProperties.getInstance().getQcDataBasePort() + ":" + this.getNombreBase();

			Connection conn = DriverManager.getConnection(url, ConnectorProperties.getInstance().getQcDataBaseUser(), ConnectorProperties.getInstance().getQcDataBasePass());
			conn.createStatement().execute("alter session set current_schema=" + this.getSdi());

			return conn;
		} catch (Exception e) {
			logger.error("Error iniciado la conexión a la base de datos (url: " + url + "):" + e.getMessage());
			return null;
		}
	}

	@Override
	protected void cargarDriver() throws Exception {

		try {
			// Cargarmos los drivers para manejar la base de datos.
			Class.forName("oracle.jdbc.driver.OracleDriver").newInstance();
		} catch (ClassNotFoundException e) {
			throw new Exception("Drivers de Oracle no encontrados: " + e.getMessage());
		} catch (IllegalAccessException e) {
			throw new Exception("Acceso no permitido a los drivers de Oracle: " + e.getMessage());
		} catch (InstantiationException e) {
			throw new Exception("No se ha podido instanciar los drivers de Oracle: " + e.getMessage());
		} catch (Exception e) {
			throw new Exception("Error cargando los drivers de oracle: " + e.getMessage());
		}
	}
}
