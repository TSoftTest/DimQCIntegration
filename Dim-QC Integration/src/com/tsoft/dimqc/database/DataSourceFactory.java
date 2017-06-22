package com.tsoft.dimqc.database;

public class DataSourceFactory {

	public static DataSource build(TipoConexion tipoConexion, String nombreBase, String sdi, TipoAplicacion tipoAplicacion) throws Exception {

		if (TipoAplicacion.DIM.equals(tipoAplicacion)) {
			return (new DataSourceOracleDim(nombreBase, sdi));
		} else if (TipoAplicacion.QC.equals(tipoAplicacion)) {
			if (TipoConexion.ORACLE.equals(tipoConexion)) {
				return (new DataSourceOracleQc(nombreBase, sdi));
			} else if (TipoConexion.SQL_SERVER.equals(tipoConexion)) {
				return (new DataSourceSqlServerQc(nombreBase));
			} else {
				throw new Exception("No es un tipo de conexión válida para Qc.");
			}
		} else {
			throw new Exception("No es un tipo de aplicación válida.");
		}
	}
}
