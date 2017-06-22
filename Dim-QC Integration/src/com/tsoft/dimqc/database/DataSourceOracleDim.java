package com.tsoft.dimqc.database;

import java.sql.Connection;
import java.sql.DriverManager;

import org.apache.log4j.Logger;

import com.tsoft.dimqc.connectors.utils.ConnectorProperties;

public class DataSourceOracleDim extends DataSource {

	private Logger logger = Logger.getRootLogger();

	public DataSourceOracleDim(String nombreBase, String sdi) {
		super(nombreBase, sdi);
		try {
			cargarDriver();
		} catch (Exception e) {
			logger.error("Error cargando los drivers de Oracle:" + e.getMessage());
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

	@Override
	public Connection getConnection() {

		String url = "";
		try {
			// logger.debug("Iniciando conexión a la base de datos de Dimensions - queries de la consulta.");

			if (ConnectorProperties.getInstance().getDataBaseDimHost() == null || "".equals(ConnectorProperties.getInstance().getDataBaseDimHost())) {
				throw new Exception("No se especifico host.");
			}

			if (ConnectorProperties.getInstance().getDataBaseDimPort() == -1) {
				throw new Exception("Error con el puerto.");
			}

			if (this.getNombreBase() == null || "".equals(this.getNombreBase())) {
				throw new Exception("No se especifico nombre de la base en el archivo de configuración para las consultas.");
			}

			if (this.getSdi() == null || "".equals(this.getSdi())) {
				throw new Exception("No se especifico sdi en el archivo de configuración para las consultas.");
			}

			if (ConnectorProperties.getInstance().getDataBaseDimUser() == null || "".equals(ConnectorProperties.getInstance().getDataBaseDimUser())) {
				throw new Exception("No se especifico usuario.");
			}

			if (ConnectorProperties.getInstance().getDataBaseDimPass() == null || "".equals(ConnectorProperties.getInstance().getDataBaseDimPass())) {
				throw new Exception("No se especifico password.");
			}

			url = "jdbc:oracle:thin:@" + ConnectorProperties.getInstance().getDataBaseDimHost() + ":" + ConnectorProperties.getInstance().getDataBaseDimPort() + "/"
			    + this.getNombreBase();

			Connection conn = DriverManager.getConnection(url, ConnectorProperties.getInstance().getDataBaseDimUser(), ConnectorProperties.getInstance().getDataBaseDimPass());
			conn.createStatement().execute("alter session set current_schema=" + this.getSdi());

			return conn;
		} catch (Exception e) {
			logger.error("Error iniciado la conexión a la base de datos (url: " + url + "):" + e.getMessage());
			return null;
		}
	}
}