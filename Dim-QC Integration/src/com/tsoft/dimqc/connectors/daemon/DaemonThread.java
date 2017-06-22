package com.tsoft.dimqc.connectors.daemon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.tsoft.dimqc.connectors.alm.QcTarea;
import com.tsoft.dimqc.connectors.core.Sincronizador;
import com.tsoft.dimqc.connectors.dimensions.DimensionsProducts;
import com.tsoft.dimqc.connectors.dimensions.DimensionsTarea;
import com.tsoft.dimqc.connectors.utils.ConnectorProperties;
import com.tsoft.dimqc.connectors.utils.ConnectorQc;
import com.tsoft.dimqc.database.Comodin;
import com.tsoft.dimqc.database.ConsultasSql;
import com.tsoft.dimqc.database.TipoAplicacion;
import com.tsoft.dimqc.database.TipoConexion;

/**
 * Thread que se dispara para verificar la existencia de archivos nuevos.
 * 
 * @author gmartin
 * 
 */
public class DaemonThread implements Runnable {
	private Logger logger = null;

	/**
	 * Constructor por defecto
	 */
	public DaemonThread() {
		logger = Logger.getRootLogger();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		logger.debug("Comienza una nueva instancia del demonio " + Thread.currentThread().getName() + " (" + Thread.currentThread().getId() + ")");

		try {

			// Ejecuto segun prioridad
			// Primer vuelta:
			if (TipoAplicacion.DIM.getValue().equalsIgnoreCase(ConnectorProperties.getInstance().getPrioridad())) {
				// Como la prioridad la tiene Dimensions y estoy en la primer vuelta,
				// uso query de Dimensions.
				sincronizarConDimensions();
			} else if (TipoAplicacion.QC.getValue().equalsIgnoreCase(ConnectorProperties.getInstance().getPrioridad())) {
				// Como la prioridad la tiene Qc y estoy en la primer vuelta, uso query
				// de Qc.
				sincronizarConQc();
			}

			// Segunda vuelta
			if (TipoAplicacion.DIM.getValue().equalsIgnoreCase(ConnectorProperties.getInstance().getPrioridad())) {
				// Como la prioridad la tiene Dimensions y estoy en la segunda vuelta,
				// uso query de Qc.
				sincronizarConQc();
			} else if (TipoAplicacion.QC.getValue().equalsIgnoreCase(ConnectorProperties.getInstance().getPrioridad())) {
				// Como la prioridad la tiene Qc y estoy en la segunda vuelta, uso query
				// de Dimensions.
				sincronizarConDimensions();
			}

		} catch (Exception e) {
			logger.error("Error no esperado en el thread", e);
		} finally {
			logger.debug("Finalizo la ejecucion del demonio " + Thread.currentThread().getName() + " (" + Thread.currentThread().getId() + ")");
		}
	}

	private void sincronizarConDimensions() throws Exception {

		DimensionsTarea dimTarea = null;
		QcTarea qcTarea = null;

		try {

			String query = obtenerQueryDimensions();
			Map<String, List<Long>> productosDimIdsQc = ConsultasSql.obtenerIdBugsAActualizarDimensions(TipoConexion.ORACLE, ConnectorProperties.getInstance().getDataBaseDimName(),
			    ConnectorProperties.getInstance().getDataBaseDimSdi(), query);

			Map<String, Map<String, String>> proyectosQcDesdeQc = ConnectorQc.getInstance().getProyectosDatosQc();

			/*
			 * Obtengo los ids de los Bugs desde Dimensions, separados por Producto de
			 * Dim. Por cada producto de Dimensions, busco el proyecto de Qc
			 * relacionado. Le mando los ids de los Bugs relacionados con ese producto
			 * y los datos del proyecto de Qc relacionado para sincronizar.
			 */
			if (productosDimIdsQc != null && !productosDimIdsQc.isEmpty()) {
				String proyectoQc = "";
				// List<String> proyectosEnDim = null;
				for (String productoDim : productosDimIdsQc.keySet()) {
					try {
						String proyectoQcRelacionado = DimensionsProducts.getInstance().getProyectoQcRelacionado(productoDim);
						if (proyectoQcRelacionado == null || "".equals(proyectoQcRelacionado)) {
							logger.error("El Producto de Dimensions '" + productoDim + "' no tiene Proyecto de Qc relacionado. No se parametrizo en Dimensions.");
						}

						Map<String, String> datosQc = proyectosQcDesdeQc.get(proyectoQcRelacionado);

						List<Long> idsBugs = productosDimIdsQc.get(productoDim);
						if (idsBugs != null && !idsBugs.isEmpty()) {
							// Conexion a Dimensions
							if (dimTarea == null) {
								dimTarea = new DimensionsTarea();
							}

							if (!proyectoQc.equals(datosQc.get(ConnectorQc.PROJECT_ALM))) {
								// Me desconecto del anterior proyecto
								if (qcTarea != null) {
									logger.debug("Finalizo la ejecucion de la instancia para el proyecto '" + qcTarea.getProject() + "' de Qc.");
									qcTarea.logOut();
								}

								// Me conecto al nuevo proyecto
								qcTarea = new QcTarea(datosQc);
								logger.debug("Comienza una nueva instancia para el proyecto '" + qcTarea.getProject() + "' de Qc.");
							}

							logger.debug("Se han encontrado en Dimensions " + idsBugs.size() + " request para sincronizar del proyecto '" + datosQc.get(ConnectorQc.PROJECT_ALM) + "' de Qc.");

							new Sincronizador(dimTarea, qcTarea, idsBugs, datosQc.get(ConnectorQc.NOMBRE_ESQUEMA_BASE));

							proyectoQc = datosQc.get(ConnectorQc.PROJECT_ALM);

						} else {
							logger.debug("Se han encontrado en Dimensions 0 request para sincronizar para el proyecto '" + datosQc.get(ConnectorQc.PROJECT_ALM) + "' de Qc.");
						}
					} catch (Exception e) {
						logger.error("Se produjo un error:" + e);
					}
				}
			} else {
				logger.debug("No se encontraron request en Dimensions para sincronizar.");
			}

		} catch (Exception e) {
			logger.error("Se produjo un error sincronizando desde Dimensions: " + e);
		} finally {
			// Deberia de quedar con la ultima conexion
			if (qcTarea != null) {
				logger.debug("Finalizo la ejecucion de la instancia para el proyecto '" + qcTarea.getProject() + "' de Qc.");
				qcTarea.logOut();
			}

			if (dimTarea != null) {
				dimTarea.closeConnection();
			}
		}
	}

	private String obtenerQueryDimensions() {
		String query = ConnectorProperties.getInstance().getQueryDimensions();

		if (query != null && !"".equals(query)) {
			query = query.replaceAll(Comodin.USUARIO_DIM.getValue(), ConnectorProperties.getInstance().getDbUserDim());
		}

		return query;
	}

	private void sincronizarConQc() throws Exception {

		/*
		 * Por cada proyecto de Qc: Obtener los proyectos (campo) parametrizados en
		 * Dimensions para el proyecto en particular. Ejecuto query de Qc con el
		 * filtro de solo los proyectos parametrizados en Dimensions. Sincronizo
		 */
		DimensionsTarea dimTarea = null;
		try {

			Map<Integer, Map<String, String>> coneccionesQc = ConnectorQc.getInstance().getConeccionesQc();

			if (!coneccionesQc.isEmpty()) {
				dimTarea = new DimensionsTarea();
				QcTarea qcTarea = null;
				boolean reconecto = false;
				for (Map<String, String> datos : coneccionesQc.values()) {

					try {
						List<String> proyectosEnDim = DimensionsProducts.getInstance().getProyectoEnDimPorProyQc(datos.get(ConnectorQc.PROJECT_ALM));
						String query = obtenerQueryQc(proyectosEnDim, datos.get(ConnectorQc.USER_NAME_ALM));
						List<Long> idsQc = new ArrayList<Long>();

						// Obtengo los datos de la base directamente
						if (TipoConexion.ORACLE.equals(ConnectorProperties.getInstance().getQcDataBaseTipo())) {
							idsQc = ConsultasSql.obtenerIdBugsAActualizarQc(ConnectorProperties.getInstance().getQcDataBaseTipo(), ConnectorProperties.getInstance().getQcDataBaseSDI(),
							    datos.get(ConnectorQc.NOMBRE_ESQUEMA_BASE), query);
						} else if (TipoConexion.SQL_SERVER.equals(ConnectorProperties.getInstance().getQcDataBaseTipo())) {
							idsQc = ConsultasSql.obtenerIdBugsAActualizarQc(ConnectorProperties.getInstance().getQcDataBaseTipo(), datos.get(ConnectorQc.NOMBRE_ESQUEMA_BASE), "", query);
						}

						// Si obtuve datos de la base (bugs), sincronizo
						if (idsQc != null && !idsQc.isEmpty()) {
							logger.debug("Se han encontrado en Qc " + idsQc.size() + " bugs para sincronizar del proyecto '" + datos.get(ConnectorQc.PROJECT_ALM) + "' de Qc.");
							qcTarea = new QcTarea(datos);
							reconecto = true;

							logger.debug("Comienza una nueva instancia para el proyecto '" + qcTarea.getProject() + "' de Qc.");
							new Sincronizador(dimTarea, qcTarea, idsQc, datos.get(ConnectorQc.NOMBRE_ESQUEMA_BASE));

						} else {
							logger.debug("Se han encontrado en Qc 0 bugs para sincronizar del proyecto '" + datos.get(ConnectorQc.PROJECT_ALM) + "' de Qc.");
							reconecto = false;
						}
					} catch (Exception e) {
						logger.error("Se produjo un error:" + e);
					} finally {
						if (qcTarea != null && reconecto) {
							logger.debug("Finalizo la ejecucion de la instancia para el proyecto '" + qcTarea.getProject() + "' de Qc.");
							qcTarea.logOut();
						}
					}
				}
			}

		} catch (Exception e) {
			logger.error("Se produjo un error sincronizando desde Qc: " + e);
		} finally {
			if (dimTarea != null) {
				dimTarea.closeConnection();
			}
		}
	}

	private String obtenerQueryQc(List<String> proyectosEnDim, String user) {

		String query = ConnectorProperties.getInstance().getQueryQuality();
		// Reemplazo usuario
		query = query.replaceAll(Comodin.USUARIO_QC.getValue(), user);

		// Reemplazo proyectos
		String listadoProyectos = "";
		for (String proyecto : proyectosEnDim) {
			listadoProyectos += " '" + proyecto + "' , ";
		}

		int posicion = listadoProyectos.lastIndexOf(",");
		if (posicion != -1) {
			listadoProyectos = listadoProyectos.substring(0, posicion);
		}
		query = query.replaceAll(Comodin.PROYECTOS_QC.getValue(), listadoProyectos);

		return query;
	}
}
