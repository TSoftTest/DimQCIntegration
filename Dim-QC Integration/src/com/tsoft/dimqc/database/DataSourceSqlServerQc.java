package com.tsoft.dimqc.database;

import java.sql.Connection;
import java.sql.DriverManager;

import org.apache.log4j.Logger;

import com.tsoft.dimqc.connectors.utils.ConnectorProperties;

public class DataSourceSqlServerQc extends DataSource {

	private Logger logger = Logger.getRootLogger();

	public DataSourceSqlServerQc(String name) {
		super(name, "");
		try {
			this.cargarDriver();
		} catch (Exception e) {
			logger.error("Error cargando los drivers de Sql Server:" + e.getMessage());
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
				throw new Exception("No se especifico name.");
			}

			if (ConnectorProperties.getInstance().getQcDataBaseUser() == null || "".equals(ConnectorProperties.getInstance().getQcDataBaseUser())) {
				throw new Exception("No se especifico usuario.");
			}

			if (ConnectorProperties.getInstance().getQcDataBasePass() == null || "".equals(ConnectorProperties.getInstance().getQcDataBasePass())) {
				throw new Exception("No se especifico password.");
			}

			url = "jdbc:sqlserver://" + ConnectorProperties.getInstance().getQcDataBaseHost() + ":" + ConnectorProperties.getInstance().getQcDataBasePort() + ";databaseName="
			    + this.getNombreBase() + ";user=" + ConnectorProperties.getInstance().getQcDataBaseUser() + ";password=" + ConnectorProperties.getInstance().getQcDataBasePass() + ";";

			return DriverManager.getConnection(url, ConnectorProperties.getInstance().getQcDataBaseUser(), ConnectorProperties.getInstance().getQcDataBasePass());

		} catch (Exception e) {
			logger.error("Error iniciado la conexión a la base de datos (url: " + url + "):" + e.getMessage());
			return null;
		}
	}

	@Override
	protected void cargarDriver() throws Exception {
		try {
			// Cargarmos los drivers para manejar la base de datos. Sql Server 2005
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver").newInstance();
		} catch (ClassNotFoundException e) {
			throw new Exception("Drivers de Sql Server no encontrados: " + e.getMessage());
		} catch (IllegalAccessException e) {
			throw new Exception("Acceso no permitido a los drivers de Sql Server: " + e.getMessage());
		} catch (InstantiationException e) {
			throw new Exception("No se ha podido instanciar los drivers de Sql Server: " + e.getMessage());
		} catch (Exception e) {
			throw new Exception("Error cargando los drivers de Sql Server: " + e.getMessage());
		}
	}
}
