package com.tsoft.dimqc.database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.tsoft.dimqc.connectors.utils.ConnectorProperties;

public class ConsultasSql {

	private static Logger logger = Logger.getRootLogger();

	public static int consultarBlockUpdateTypeDimensions(String campoDimObjId, String productoDimensionsId) throws Exception {

		/*
		 * BlockUpdateType:
		 * 
		 * ATTR_1 = 1 => Allow users to add, modify and delete rows. ATTR_1 = 2 =>
		 * Do not allow user to modify or delete rows; only to append new rows.
		 */
		StringBuilder query = new StringBuilder("");

		// SELECT cplAttr.ATTR_1
		// FROM CPL_ATTRIBUTES cplAttr
		// inner join CPL_CATALOGUE cplCata
		// on cplAttr.OBJ_UID = cplCata.OBJ_UID
		// where cplAttr.ATTR_2 = 'COMENTARIO_US'
		// and cplCata.PRODUCT_ID = 'T_CRECER21'
		// and cplCata.OBJ_ID = 'COMENTARIO_US';

		String sdi = "";
		if (ConnectorProperties.getInstance().getDataBaseDimSdi() != null && !"".equals(ConnectorProperties.getInstance().getDataBaseDimSdi())) {
			sdi = ConnectorProperties.getInstance().getDataBaseDimSdi() + ".";
		}

		query.append("  SELECT cplAttr.ATTR_1  ");
		query.append("  FROM " + sdi + "CPL_ATTRIBUTES cplAttr  ");
		query.append("  inner join " + sdi + "CPL_CATALOGUE cplCata   ");
		query.append("  on cplAttr.OBJ_UID = cplCata.OBJ_UID ");
		query.append("  where cplAttr.ATTR_2 = '" + campoDimObjId + "'  ");
		query.append("  and cplCata.PRODUCT_ID = '" + productoDimensionsId + "'  ");
		query.append("  and cplCata.OBJ_ID = '" + campoDimObjId + "'  ");

		Connection conn = null;

		try {

			DataSource dataSourceQc = DataSourceFactory.build(TipoConexion.ORACLE, ConnectorProperties.getInstance().getDataBaseDimName(), ConnectorProperties.getInstance()
			    .getDataBaseDimSdi(), TipoAplicacion.DIM);
			conn = dataSourceQc.getConnection();

			List<Map<String, Object>> listado = EjecucionSql.executeQuery(query.toString(), conn);
			return Integer.parseInt((String) listado.get(0).get("ATTR_1"));

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			throw new Exception("Ocurrio un error al obtener Block Update Type de Dimensions:" + e.getMessage());
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (Exception _ignore) {
			}
		}

		return -1;
	}

	public static void updateBlockUpdateTypeDimensions(String campoDimObjId, String productoDimensionsId, int nuevoValor) throws Exception {

		/*
		 * BlockUpdateType:
		 * 
		 * ATTR_1 = 1 => Allow users to add, modify and delete rows. ATTR_1 = 2 =>
		 * Do not allow user to modify or delete rows; only to append new rows.
		 */
		Connection conn = null;
		if (nuevoValor == 1 || nuevoValor == 2) {
			StringBuilder query = new StringBuilder("");

			String sdi = "";
			if (ConnectorProperties.getInstance().getDataBaseDimSdi() != null && !"".equals(ConnectorProperties.getInstance().getDataBaseDimSdi())) {
				sdi = ConnectorProperties.getInstance().getDataBaseDimSdi() + ".";
			}

			query.append("  UPDATE  " + sdi + "CPL_ATTRIBUTES cplAttr ");
			query.append("  SET  cplAttr.ATTR_1 = '" + nuevoValor + "'");
			query.append("  where cplAttr.ATTR_2 = '" + campoDimObjId + "'  ");
			query.append("  and cplAttr.OBJ_UID = ( select cplCata.OBJ_UID from  " + sdi + "CPL_CATALOGUE cplCata where ");
			query.append("   cplCata.PRODUCT_ID = '" + productoDimensionsId + "'  ");
			query.append("  and cplCata.OBJ_ID = '" + campoDimObjId + "'  ");
			query.append("  ) ");

			try {
				DataSource dataSourceQc = DataSourceFactory.build(TipoConexion.ORACLE, ConnectorProperties.getInstance().getDataBaseDimName(), ConnectorProperties.getInstance()
				    .getDataBaseDimSdi(), TipoAplicacion.DIM);
				conn = dataSourceQc.getConnection();

				EjecucionSql.executeUpdate(query.toString(), conn);
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (Exception e) {
				throw new Exception("Ocurrio un error al actualizar Block Update Type de Dimensions:" + e.getMessage());
			} finally {
				try {
					if (conn != null) {
						conn.close();
					}
				} catch (Exception _ignore) {
				}
			}
		}
	}

	public static int consultarMaximaCantiadadCaracteresDim(String campoNombreDim) throws Exception {
		StringBuilder query = new StringBuilder("");

		String sdi = "";
		if (ConnectorProperties.getInstance().getDataBaseDimSdi() != null && !"".equals(ConnectorProperties.getInstance().getDataBaseDimSdi())) {
			sdi = ConnectorProperties.getInstance().getDataBaseDimSdi() + ".";
		}

		query.append("  select MAX_LEN ");
		query.append("  from " + sdi + "GLOBAL_ATTRIBUTES  ");
		query.append("  where VARIABLE_NAME = '" + campoNombreDim + "' ");

		Connection conn = null;

		try {
			DataSource dataSourceQc = DataSourceFactory.build(TipoConexion.ORACLE, ConnectorProperties.getInstance().getDataBaseDimName(), ConnectorProperties.getInstance()
			    .getDataBaseDimSdi(), TipoAplicacion.DIM);
			conn = dataSourceQc.getConnection();

			List<Map<String, Object>> listado = EjecucionSql.executeQuery(query.toString(), conn);
			return ((BigDecimal) listado.get(0).get("MAX_LEN")).intValue();

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			throw new Exception("Ocurrio un error al obtener MAX_LEN de Dimensions:" + e.getMessage());
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (Exception _ignore) {
			}
		}

		return -1;
	}

	/**
	 * @Precondicion Debe de tener el valor de la lista para el campo (LS_NAME) de
	 *               qc en primer lugar como minimo. Solo se va a usar ese dato
	 *               del query.
	 * @return lista con valores validos para un campo en QC.
	 */
	public static List<String> obtenerListaDeValoresParaCampoDesdeQC(String idLista, TipoConexion base, String nombreBase, String sdi, List<String> proyectosEnDim) throws Exception {

		Connection conn = null;

		try {

			DataSource dataSourceQc = DataSourceFactory.build(base, nombreBase, sdi, TipoAplicacion.QC);
			conn = dataSourceQc.getConnection();

			String queryMultiple = ConnectorProperties.getInstance().getQueryValidSetMultiple().replaceAll("%ID_LISTA%", idLista);

			String listadoProyectos = "";

			// Reemplazo proyectos
			if (!(proyectosEnDim == null)) {

				for (String proyecto : proyectosEnDim) {
					listadoProyectos += " '" + proyecto + "' , ";
				}

				int posicion = listadoProyectos.lastIndexOf(",");
				if (posicion != -1) {
					listadoProyectos = listadoProyectos.substring(0, posicion);
				}
			}

			queryMultiple = queryMultiple.replaceAll(Comodin.PROYECTOS_QC.getValue(), listadoProyectos);

			logger.debug("QUERY Multiple:" + queryMultiple);

			String querySimple = ConnectorProperties.getInstance().getQueryValidSetSimple().replaceAll("%ID_LISTA%", idLista);
			logger.debug("QUERY Simple:" + querySimple);

			List<Map<String, Object>> listado = EjecucionSql.executeQuery(queryMultiple.toString(), conn);
			if (listado.isEmpty()) {
				logger.debug("No se encontraron valores con la query multiple. Se ejecuta la Simple");
				dataSourceQc = DataSourceFactory.build(base, nombreBase, sdi, TipoAplicacion.QC);
				conn = dataSourceQc.getConnection();
				listado = EjecucionSql.executeQuery(querySimple.toString(), conn);
			}

			List<String> valores = new ArrayList<String>();

			if (listado != null && !listado.isEmpty()) {
				for (Map<String, Object> linea : listado) {
					for (String key : linea.keySet()) {

						valores.add(((String) linea.get(key)));

						// Solo tomo el primer elemento, que se supone es el id
						break;
					}
				}
			} else {
				throw new Exception("No se pudieron encontrar valores para el validset");
			}

			return valores;
		} catch (SQLException e) {
			logger.error("Ocurrio un error al obtener la lista de valores para el validset por query: " + e);
		} catch (Exception e) {
			logger.error("Ocurrio un error al obtener la lista de valores para el validset por query: " + e);
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (Exception _ignore) {
			}
		}

		return null;
	}

	public static String obtenerUsuarioAAsignarDesdeDimensions(String nombreDeBaseline, String nombreDeProducto) throws Exception {
		String aliasDelCampoUsuario = ConnectorProperties.getInstance().getAliasSQLAsignacion();

		String query = ConnectorProperties.getInstance().getQueryAsignacionAutomatica(nombreDeProducto).replaceAll("%NOMBRE_BASELINE%", nombreDeBaseline);

		query = query.replaceAll(Comodin.PROYECTOS_QC.getValue(), nombreDeProducto);

		logger.debug("QUERY para obtener el usuario a asignar desde Dimensions: " + query);

		Connection conn = null;

		try {
			DataSource dataSourceQc = DataSourceFactory.build(TipoConexion.ORACLE, ConnectorProperties.getInstance().getDataBaseDimName(), ConnectorProperties.getInstance()
			    .getDataBaseDimSdi(), TipoAplicacion.DIM);
			conn = dataSourceQc.getConnection();

			List<Map<String, Object>> listado = EjecucionSql.executeQuery(query, conn);

			if (!listado.isEmpty()) {
				return ((String) listado.get(0).get(aliasDelCampoUsuario).toString());
			} else {
				return null;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			throw new Exception("Ocurrio un error al obtener el usuario a asignar desde Dimensions:" + e.getMessage());
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (Exception _ignore) {
			}
		}

		return null;
	}

	public static String obtenerStreamParaCrearRequestEnDimensions(String nombreDeBaseline, String nombreDeProducto) throws Exception {
		String aliasDelCampoStream = ConnectorProperties.getInstance().getAliasStreamDeCreacion();

		String query = ConnectorProperties.getInstance().getQueryStreamDeCreacion().replaceAll("%NOMBRE_BASELINE%", nombreDeBaseline);

		query = query.replaceAll(Comodin.PROYECTOS_QC.getValue(), nombreDeProducto);

		logger.debug("QUERY para obtener el Stream donde crear Requests en Dimensions: " + query);

		Connection conn = null;

		try {
			DataSource dataSourceQc = DataSourceFactory.build(TipoConexion.ORACLE, ConnectorProperties.getInstance().getDataBaseDimName(), ConnectorProperties.getInstance()
			    .getDataBaseDimSdi(), TipoAplicacion.DIM);
			conn = dataSourceQc.getConnection();

			List<Map<String, Object>> listado = EjecucionSql.executeQuery(query, conn);

			if (!listado.isEmpty()) {
				return ((String) listado.get(0).get(aliasDelCampoStream).toString());
			} else {
				return ConnectorProperties.getInstance().getProjectDim();
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			throw new Exception("Ocurrio un error al obtener el Stream donde crear Requests en Dimensions:" + e.getMessage());
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (Exception _ignore) {
			}
		}

		return null;
	}

	/**
	 * @Precondicion Debe de tener el id de qc en primer lugar como minimo. Solo
	 *               se va a usar ese dato del query.
	 * @return lista con los ids de los bugs de Qc.
	 */
	public static List<Long> obtenerIdBugsAActualizarQc(TipoConexion base, String nombreBase, String sdi, String query) throws Exception {

		Connection conn = null;
		try {

			DataSource dataSourceQc = DataSourceFactory.build(base, nombreBase, sdi, TipoAplicacion.QC);
			conn = dataSourceQc.getConnection();

			if (ConnectorProperties.getInstance().getMostrarQuery()) {
				logger.debug(query);
			}

			List<Map<String, Object>> listado = EjecucionSql.executeQuery(query, conn);

			List<Long> idsQc = new ArrayList<Long>();

			// El query deberia de devolver solo el id de qc; o sea, una lista con los
			// ids de los bugs de qc.
			if (listado != null && !listado.isEmpty()) {

				for (Map<String, Object> linea : listado) {
					for (String key : linea.keySet()) {
						// Tecno:
						// idsQc.add(((BigDecimal) linea.get(key)).longValue());

						// Banco:
						idsQc.add(((Integer) linea.get(key)).longValue());

						// Solo tomo el primer elemento, que se supone es el id
						break;
					}
				}
			}

			return idsQc;

		} catch (SQLException e) {
			logger.error("Ocurrio un error al obtener los ids de los bugs de Qc desde Qc mediante query: " + e);
		} catch (Exception e) {
			logger.error("Ocurrio un error al obtener los ids de los bugs de Qc desde Qc: " + e);
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (Exception _ignore) {
			}
		}

		return null;
	}

	/**
	 * @Precondicion Debe de tener el id de qc en primer lugar, y en segundo lugar
	 *               el campo que contiene el producto de Dimensions. Solo se van
	 *               a usar esos datos del query.
	 * @return por cada producto de Dimensions la lista con los ids de los bugs de
	 *         Qc.
	 */
	public static Map<String, List<Long>> obtenerIdBugsAActualizarDimensions(TipoConexion base, String nombreBase, String esquema, String query) throws Exception {

		Connection conn = null;
		try {

			DataSource dataSourceQc = DataSourceFactory.build(base, nombreBase, esquema, TipoAplicacion.DIM);
			conn = dataSourceQc.getConnection();

			if (ConnectorProperties.getInstance().getMostrarQuery()) {
				logger.debug(query);
			}

			EjecucionSql.execute("ALTER SESSION SET NLS_LANGUAGE = ENGLISH", conn);

			List<Map<String, Object>> listado = EjecucionSql.executeQuery(query, conn);

			Map<String, List<Long>> datos = new HashMap<String, List<Long>>();

			// El query deberia de devolver solo el id de qc; o sea, una lista con los
			// ids de los bugs de qc.
			if (listado != null && !listado.isEmpty()) {
				Long idQc = -1L;
				String productoDim = "";

				// Recorro la lista
				for (Map<String, Object> linea : listado) {
					Iterator<String> it = linea.keySet().iterator();
					int i = 0; // Con esto me voy a asegurar de tomar solo los dos
										 // primeros campos de los que haya traido el query.
					idQc = -1L;
					productoDim = "";
					while (i < 2 && it.hasNext()) {
						/*
						 * Como precondicion en primer lugar deberia de venir el id del bug
						 * en Qc; y en segundo lugar el nombre del proyecto (campo, no de
						 * logueo).
						 */
						String key = it.next();
						if (linea.get(key) == null || "".equals(linea.get(key)) || "null".equalsIgnoreCase((String) linea.get(key))) {
							// Continuo con la siguiente fila
							break;
						}

						if (i == 0) {
							// idQc = (Long) linea.get(it.next());
							try {
								idQc = Long.parseLong((String) linea.get(key));
							} catch (NumberFormatException e) {
								throw new Exception("Ocurrio un error al obteniendo los ids de los bugs desde Dimensions: " + e);
							}
						} else if (i == 1) {
							productoDim = (String) linea.get(key);
						}

						i++;
					}

					if (productoDim != null && !"".equals(productoDim) && idQc != -1L) {
						List<Long> acumulado = datos.get(productoDim);
						if (acumulado == null || acumulado.isEmpty()) {
							acumulado = new ArrayList<Long>();
						}

						acumulado.add(idQc);
						datos.put(productoDim, acumulado);
					}
				}
			}

			return datos;

		} catch (SQLException e) {
			logger.error("Ocurrio un error al obtener los ids de los bugs de Qc desde Dimensions mediante query: " + e);
		} catch (Exception e) {
			logger.error("Ocurrio un error al obtener los ids de los bugs de Qc desde Dimensions: " + e);
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (Exception _ignore) {
			}
		}

		return null;
	}

	public static String obtenerUltimaAuditoriaQc(String idBug, TipoConexion base, String nombreBase, String sdi) throws Exception {
		Connection conn = null;
		try {

			DataSource dataSourceQc = DataSourceFactory.build(base, nombreBase, sdi, TipoAplicacion.QC);
			conn = dataSourceQc.getConnection();

			// SELECT AU_USER, AU_TIME
			// FROM AUDIT_LOG (NOLOCK) AU_L
			// WHERE AU_ENTITY_TYPE = 'BUG' AND
			// AU_ENTITY_ID = ID_DEL_BUG AND
			// AU_TIME = (SELECT MAX(AU_TIME)
			// FROM AUDIT_LOG (NOLOCK)
			// WHERE AU_ENTITY_TYPE = 'BUG' AND
			// AU_ENTITY_ID = AU_L.AU_ENTITY_ID)

			StringBuilder query = new StringBuilder("");
			query.append("  SELECT AU_USER  ");
			query.append("  FROM  AUDIT_LOG (NOLOCK) AU_L   ");
			query.append("  WHERE AU_ENTITY_TYPE = 'BUG' AND  ");
			query.append("        AU_ENTITY_ID = " + idBug);
			query.append("        AND AU_TIME = (SELECT MAX(AU_TIME)  ");
			query.append("                   FROM AUDIT_LOG (NOLOCK)     ");
			query.append("                   WHERE AU_ENTITY_TYPE = 'BUG' AND   ");
			query.append("                          AU_ENTITY_ID = AU_L.AU_ENTITY_ID)   ");

			logger.debug("QUERY AUDITORIA:" + query.toString());

			List<Map<String, Object>> listado = EjecucionSql.executeQuery(query.toString(), conn);
			return ((String) listado.get(0).get("AU_USER"));
		} catch (SQLException e) {
			logger.error("Ocurrio un error al obtener datos de auditoria por query: " + e);
		} catch (Exception e) {
			logger.error("Ocurrio un error al obtener datos de auditoria por query: " + e);
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (Exception _ignore) {
			}
		}

		return null;
	}
}