package com.tsoft.dimqc.connectors.alm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.serena.dmclient.api.Request;
import com.tsoft.dimqc.connectors.alm.Entity.Fields;
import com.tsoft.dimqc.connectors.alm.Entity.Fields.Field;
import com.tsoft.dimqc.connectors.alm.Entity.Fields.Field.Value;
import com.tsoft.dimqc.connectors.dimensions.DimensionsProducts;
import com.tsoft.dimqc.connectors.dimensions.DimensionsTarea;
import com.tsoft.dimqc.connectors.exceptions.ServiceException;
import com.tsoft.dimqc.connectors.utils.ConnectorProperties;
import com.tsoft.dimqc.connectors.utils.ConnectorQc;
import com.tsoft.dimqc.connectors.utils.mapping.CampoMultiple;
import com.tsoft.dimqc.connectors.utils.mapping.Estado;
import com.tsoft.dimqc.connectors.utils.mapping.EstadoExcluido;
import com.tsoft.dimqc.connectors.utils.mapping.Mapeo;
import com.tsoft.dimqc.connectors.utils.mapping.Pair;
import com.tsoft.dimqc.connectors.utils.parser.ArmadoHtml;
import com.tsoft.dimqc.connectors.utils.xml.Audits.Audit;
import com.tsoft.dimqc.connectors.utils.xml.FieldsQc;

public class QcTarea {

	private ALMConnection qcConnection;
	private Logger logger = Logger.getRootLogger();
	private boolean listar = false;
	private String project = "";
	private String summary = "";
	private Map<String, FieldsQc.Field> fieldsQc = new HashMap<String, FieldsQc.Field>();

	public QcTarea(Map<String, String> datos) throws Exception {
		this.listar = Boolean.valueOf(datos.get(ConnectorQc.LISTAR_QC_ATTRIBUTES));
		this.project = datos.get(ConnectorQc.PROJECT_ALM);
		this.summary = datos.get(ConnectorQc.DEFECT_SUMMARY);
		connection(datos);
		this.fieldsQc = getFieldsQc();
	}

	private void connection(Map<String, String> datos) {
		try {
			qcConnection = QCConnectionFactory.getConnection(datos);
			boolean result = qcConnection.AuthenticateAndStartSession();
			logger.debug("Qc Authentication en el proyecto '" + datos.get(ConnectorQc.PROJECT_ALM) + "' de Qc: " + result);
		} catch (Exception e) {
			logger.error("Ocurrio un error al conectarse con QC en el proyecto '" + datos.get(ConnectorQc.PROJECT_ALM) + "'.");
			logger.error(e);
			throw new ServiceException(e.getMessage());
		}

	}

	/**
	 * Metodo que hace un update de Dimensions a Qc de todos los atributos
	 * definidos en mapeo.xml y los obligatorios
	 * 
	 * @param requestDim
	 *          Request de Qc
	 * @param requestQc
	 *          Request de Dimension
	 * @return void
	 * @throws Exception
	 */
	public void updateQcRequest(Entity requestQc, Request requestDim, DimensionsTarea dimTarea) throws Exception {

		Map<String, Pair<String, String>> mapa = Mapeo.getInstance().getAtributosGenerales();
		dimTarea.cargarAtributosDimensions(requestDim);
		boolean actualizo = false;
		try {

			for (Entry<String, Pair<String, String>> entry : mapa.entrySet()) {
				try {
					String attributeQc = Mapeo.getInstance().getAttributeByName(Mapeo.QC, entry.getKey());
					if (Mapeo.getInstance().hayQueCopiarAQC(entry.getKey())) {

						if (this.fieldsQc.get(attributeQc).isSupportsMultivalue()) {
							updateMultivalueAttribute(requestQc, requestDim, entry.getKey(), dimTarea);
						} else {
							updateGeneralAttribute(requestQc, requestDim, entry.getKey(), dimTarea);
						}
					} else {
						logger.debug("No se sincroniza el atributo '" + entry.getKey() + "' de Dimensions a Qc .");
					}
				} catch (Exception e) {
					logger.error("Ocurrio un error al settear el campo " + entry.getKey() + " en Qc:");
					logger.error(e);
				}
			}

			// Actualizo el comentario
			updateGeneralAttribute(requestQc, requestDim, "comentarios", dimTarea);

			// Estado
			String dimStatus = (String) requestDim.getAttribute(dimTarea.getAttributeNumber(Mapeo.getInstance().getAttributeByName(Mapeo.DIM, "estado")));

			/*
			 * Si el estado de Dimensions no es uno de los estados que se excluyen de
			 * ser replicados a Qc, se procede a su sincronizacion.
			 */
			if (dimStatus != null && !"".equals(dimStatus) && !EstadoExcluido.getInstance().isEstadoExcluidoDim(dimStatus)) {
				logger.debug("Setteando el atributo 'estado' de Dimensions a Qc .");

				String attributeQc = Mapeo.getInstance().getAttributeByName(Mapeo.QC, "estado");
				Field attributeField = requestQc.getFields().getFieldByName(attributeQc);
				if (!attributeField.getValue().isEmpty() && attributeField.getValue().get(0) != null && !"".equals(attributeField.getValue().get(0).getValue())
				    && !fieldsQc.get(attributeQc).isSupportsMultivalue()) {
					requestQc.getFields().getFieldByName(attributeQc).getValue().get(0).setValue("");
				}
				String equivalentStatus = Estado.getInstance().getEquivalentStatus(Estado.DIM, dimStatus);
				equivalentStatus = new String(equivalentStatus.getBytes(ConnectorProperties.getInstance().getEncodingQc()), ConnectorProperties.getInstance().getEncodingDim());
				if (!"".equals(equivalentStatus)) {
					requestQc.getFields().getFieldByName(attributeQc).getValue().get(0).setValue(equivalentStatus);
				} else {
					logger.error("No se encontro el equivalente en QC al estado " + dimStatus);
				}
			} else if (EstadoExcluido.getInstance().isEstadoExcluidoDim(dimStatus)) {
				logger.debug("Estado de Dimensions excluido de ser replicado a Qc:" + dimStatus);
			}

			actualizo = qcConnection.actualizarEntidad(requestQc);
			// Actualizo la fecha de sincronizacion
			updateFechaSincronizacionQc(requestQc);

			if (!actualizo) {
				logger.error("Error en la copia de Dimensions a QC.");
			}
		} catch (Exception e) {
			logger.error("Ocurrio un error al settear los atributos en Qc");
			throw new Exception(e);
		}
	}

	/**
	 * @param requestQc
	 * @param requestDim
	 * @param attributeName
	 * @param dimTarea
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private void updateGeneralAttribute(Entity requestQc, Request requestDim, String attributeName, DimensionsTarea dimTarea) throws Exception {
		logger.debug("Setteando el atributo '" + attributeName + "' de Dimensions a Qc .");
		String attributeQc = Mapeo.getInstance().getAttributeByName(Mapeo.QC, attributeName);
		String attributeDim = Mapeo.getInstance().getAttributeByName(Mapeo.DIM, attributeName);
		Vector<String> vectorAtributoDimensions = null;
		Object objAtributoDimensions = null;
		Field attributeField = requestQc.getFields().getFieldByName(attributeQc);
		if (!attributeField.getValue().isEmpty() && attributeField.getValue().get(0) != null && !"".equals(attributeField.getValue().get(0).getValue())) {
			requestQc.getFields().getFieldByName(attributeQc).getValue().get(0).setValue("");
		}

		String atributoDim = "";

		// Se valida si es un atributo compuesto (multiples campos) o no
		if (CampoMultiple.getInstance().esCampoConCombinacion(attributeDim)) {
			String proyectoDim = requestQc.getFields().getFieldByName(ConnectorProperties.getInstance().getCampoProjectQc()).getValue().get(0).getValue();
			String productoDim = DimensionsProducts.getInstance().getProductByName(DimensionsProducts.DIM, proyectoDim);
			atributoDim = obtenerContenidoCampoMultiple(attributeName, attributeDim, requestDim, dimTarea, productoDim);
		} else {
			int attributeNumber = dimTarea.getAttributeNumber(attributeDim);
			objAtributoDimensions = requestDim.getAttribute(attributeNumber);
			if (objAtributoDimensions instanceof java.util.Vector) {
				vectorAtributoDimensions = (Vector<String>) objAtributoDimensions;
				if (Mapeo.getInstance().isMemo(attributeName)) {
					for (ListIterator<String> iterator = vectorAtributoDimensions.listIterator(); iterator.hasNext();) {
						String valor = iterator.next();
						if ("".equals(atributoDim)) {
							atributoDim = valor;
						} else {
							atributoDim += "\n" + valor;
						}
					}
				} else {
					if (!vectorAtributoDimensions.isEmpty()) {
						atributoDim = vectorAtributoDimensions.lastElement();
					} else {
						atributoDim = "";
					}

				}

			} else {
				atributoDim = objAtributoDimensions.toString();
			}
		}

		if (atributoDim != null && !"".equals(atributoDim) && Mapeo.getInstance().isMemo(attributeName)) {
			atributoDim = armarHtml(atributoDim);
		}

		atributoDim = encoding(atributoDim);

		if (!attributeField.getValue().isEmpty()) {
			requestQc.getFields().getFieldByName(attributeQc).getValue().get(0).setValue(atributoDim);
		} else {
			Entity.Fields.Field.Value value = new Value();
			value.setValue(atributoDim);
			requestQc.getFields().getFieldByName(attributeQc).getValue().add(value);
		}
	}

	public void actualiza(Entity entity) throws Exception {

		boolean result;

		result = qcConnection.actualizarEntidad(entity);
		logger.debug("Actualizacion de request QC: " + result);
	}

	public void logOut() {
		try {
			boolean result = qcConnection.logout();
			logger.debug("QC LogOut: " + result);
		} catch (Exception e) {
			logger.debug("Error al desloggearse: " + e.getMessage());
		}
	}

	public void updateFechaSincronizacionQc(Entity requestQc) throws Exception {

		/*
		 * Vuelvo a buscar el objeto para asegurarme que tiene los valores
		 * actualizados.
		 */

		boolean actualizo = false;

		try {
			logger.debug("Actualizando la fecha de sincronizacion en QC.");
			String idEntity = requestQc.getFields().getFieldByName(Mapeo.getInstance().getAttributeByName(Mapeo.QC, "idQc")).getValue().get(0).getValue();
			// Se actualiza la entidad para que se modifique la fecha de auditoria y
			// sea lo mas parecida a la de sincronizacion
			String attributeFechaSincQc = Mapeo.getInstance().getAttributeByName(Mapeo.QC, "fechaSincronizacion");
			Field lastModified = requestQc.getFields().getFieldByName(Mapeo.getInstance().getAttributeByName(Mapeo.QC, "fechaAuditoria"));

			String fechaSincAux = lastModified.getValue().get(0).getValue();

			Field syncTimeField = requestQc.getFields().getFieldByName(attributeFechaSincQc);
			if (!syncTimeField.getValue().isEmpty() && syncTimeField.getValue().get(0) != null && !"".equals(syncTimeField.getValue().get(0).getValue())
			    && !fieldsQc.get(attributeFechaSincQc).isSupportsMultivalue()) {
				requestQc.getFields().getFieldByName(attributeFechaSincQc).getValue().get(0).setValue("");
			}
			requestQc.getFields().getFieldByName(attributeFechaSincQc).getValue().get(0).setValue(fechaSincAux);
			qcConnection.actualizarEntidad(requestQc);
			Entities entities = qcConnection.listEntities("?query={id[" + idEntity + "]}");

			if (entities.getEntities().size() != 1) {
				logger.error("Error en la actualizacion de la fecha de sincronizacion en QC: ");
				throw new Exception("Error en la actualizacion de la fecha de sincronizacion en QC.");
			}

			Entity entity = entities.getEntities().get(0);

			lastModified = entity.getFields().getFieldByName(Mapeo.getInstance().getAttributeByName(Mapeo.QC, "fechaAuditoria"));

			String fechaSincAux2 = lastModified.getValue().get(0).getValue();
			if (!fechaSincAux2.equals(fechaSincAux)) {
				syncTimeField = entity.getFields().getFieldByName(attributeFechaSincQc);
				if (!syncTimeField.getValue().isEmpty() && syncTimeField.getValue().get(0) != null && !"".equals(syncTimeField.getValue().get(0).getValue())
				    && !fieldsQc.get(attributeFechaSincQc).isSupportsMultivalue()) {
					entity.getFields().getFieldByName(attributeFechaSincQc).getValue().get(0).setValue("");
				}
				entity.getFields().getFieldByName(attributeFechaSincQc).getValue().get(0).setValue(fechaSincAux);
				actualizo = qcConnection.actualizarEntidad(entity);
			}

			if (!actualizo) {
				logger.error("Error en la actualizacion de la fecha de sincronizacion en QC. Request Id: " + idEntity);
			}

		} catch (Exception e) {
			logger.error("Error en fecha de sincronizacion en QC.");
			logger.error(e);
		}

	}

	public Entities archivos(String idEntidad) {

		logger.debug("Se obtiene el listado de archivos de QC.");

		try {

			return qcConnection.archivos(idEntidad);

		} catch (Exception e) {
			logger.error("Error al obtener el listado de archivos de QC: ");
			logger.error(e);
		}

		return null;
	}

	public String descargarArchivo(String idEntidad, String nombreArchivo) {

		logger.debug("Descargando archivo de QC.");

		try {

			return qcConnection.descargarArchivo(idEntidad, nombreArchivo);

		} catch (Exception e) {
			logger.error("Error al descargar el archivo de QC: ");
			logger.error(e);
		}

		return null;

	}

	public boolean subirArchivo(String idEntidad, String nombreArchivo) {
		logger.debug("Subiendo archivo de QC.");

		try {

			return qcConnection.subirArchivo(idEntidad, nombreArchivo);

		} catch (Exception e) {
			logger.error("Error al subir el archivo de QC: ");
			logger.error(e);
		}

		return false;
	}

	/**
	 * Verifica si la entidad (request) de QC esta desbloqueada, es decir ningun
	 * usuario la esta utilizando.
	 * 
	 * @param requestQc
	 * @return boolean locked or not
	 */
	public boolean verifyObjectStatus(Entity requestQc) {
		boolean locked = false;
		try {
			locked = qcConnection.verifyObjectStatus(requestQc);
		} catch (Exception e) {
			logger.error("Error verificando si la entidad se encuentra bloqueada");
			logger.error(e);
		}
		return locked;
	}

	public void listEntityAttributes() {

		Entity entity = new Entity();
		String rutaArchivo = ConnectorProperties.getInstance().getRutaAttrQC();
		File qcAttr = null;
		FileWriter writer = null;
		if (this.listar) {
			rutaArchivo = rutaArchivo + "/QCAttr_" + this.project + ".txt";
			logger.debug("Listando atributos del defecto: \'" + summary + "\' en " + rutaArchivo);
			try {
				String query = URLEncoder.encode(summary, ConnectorProperties.getInstance().getEncodingQc());
				query = "'" + query + "'";
				query = query.replace("+", "%20");

				entity = qcConnection.getEntityBySummary("?query={Summary[" + query + "]}");
				if (entity != null) {
					qcAttr = new File(rutaArchivo);
					writer = new FileWriter(qcAttr);
					Fields fields = entity.getFields();
					writer.write("Atributos del defecto: " + summary + "\n");
					writer.flush();
					writer.write("<Nombre Atributo> = <VALOR> \n");
					writer.flush();
					writer.write("\n");
					writer.flush();
					for (int i = 0; i < fields.getField().size(); i++) {
						Field field = fields.getField().get(i);

						if (field != null && !field.getValue().isEmpty()) {
							String valorCampo = field.name + ": ";

							for (Value valor : field.getValue()) {
								valorCampo += "Value = " + valor.getValue() + " | ReferenceValue = " + (valor.getReferenceValue() == null ? "" : valor.getReferenceValue());
								if (field.getValue().size() > 1) {
									valorCampo += " || ";
								}
							}
							int j = valorCampo.lastIndexOf(" || ");
							if (j != -1) {
								valorCampo = valorCampo.substring(0, j);
							}
							writer.write(valorCampo + "\n");
							writer.flush();
						}
					}
				} else {
					logger.debug("No se encontro el defecto con Summary: \'" + summary + "\'. No se pudieron listar los atributos.");
				}

			} catch (IOException e) {
				logger.error("No se pudo abrir el archivo, verifique que no este siendo usado.");
				logger.error(e);
			} catch (Exception e) {
				logger.error("Ocurrio un error al listar los atributos del defecto: " + summary);
				logger.error(e);
			} finally {
				try {
					if (writer != null) {
						writer.close();
					}
				} catch (IOException _ignore) {
				}
			}
		}
	}

	public void pasajeDeEstado(Entity requestQc) throws Exception {

		/*
		 * No se verifica si se trata de un Estado Excluido ya que el dato
		 * "se toma directamente de Qc" (se lee de la configuracion pero se supone
		 * que es el nombre que tiene en la aplicacion realmente). Esto no pasa en
		 * el pasaje de estado de Dimensions, ya que ahi se parte de Qc para ir a
		 * Dimensions y en ese caso se necesita que si o si este el estado.
		 */
		try {
			// Actualizo el estado
			logger.debug("Verificando pasaje de estado en Qc.");
			String attributeQc = Mapeo.getInstance().getAttributeByName(Mapeo.QC, "estado");
			Field attributeField = requestQc.getFields().getFieldByName(attributeQc);

			if (!attributeField.getValue().isEmpty() && attributeField.getValue().get(0) != null && !"".equals(attributeField.getValue().get(0).getValue())
			    && !fieldsQc.get(attributeQc).isSupportsMultivalue()) {
				String estadoActual = attributeField.getValue().get(0).getValue();
				if (ConnectorProperties.getInstance().isEstadoInicialQc(estadoActual)) {
					requestQc.getFields().getFieldByName(attributeQc).getValue().get(0).setValue("");
					requestQc.getFields().getFieldByName(attributeQc).getValue().get(0).setValue(ConnectorProperties.getInstance().getEstadoFinalQc());
					boolean actualizo = qcConnection.actualizarEntidad(requestQc);

					if (!actualizo) {
						logger.error("Error en el pasaje de estado en QC.");
					}
				}
			}
		} catch (Exception e) {
			logger.error("Ocurrio un error durante el pasaje de estado en Qc.");
			throw new Exception(e);
		}
	}

	private String armarHtml(String texto) {
		logger.debug("Armando Html.");
		ArmadoHtml armadoHtml = new ArmadoHtml();
		return armadoHtml.armar(texto);
	}

	public Map<String, FieldsQc.Field> getFieldsQc() throws Exception {

		logger.debug("Obteniendo configuracion de Qc.");
		Map<String, FieldsQc.Field> fields = new HashMap<String, FieldsQc.Field>();

		try {
			FieldsQc fieldsQc = qcConnection.getFieldsQc();

			for (FieldsQc.Field field : fieldsQc.getFields()) {
				fields.put(field.getName(), field);
			}

		} catch (Exception e) {
			logger.error("Ocurrio un error obteniendo configuracion de Qc.");
			throw new Exception(e);
		}

		return fields;
	}

	private String encoding(String valor) throws UnsupportedEncodingException {

		if (valor == null) {
			valor = "";
		}

		return (new String(valor.getBytes(ConnectorProperties.getInstance().getEncodingQc()), ConnectorProperties.getInstance().getEncodingDim()));
	}

	/**
	 * @param requestQc
	 * @param requestDim
	 * @param attributeName
	 * @param dimTarea
	 * @throws UnsupportedEncodingException
	 */
	private void updateMultivalueAttribute(Entity requestQc, Request requestDim, String attributeName, DimensionsTarea dimTarea) throws UnsupportedEncodingException {

		logger.debug("Setteando el atributo '" + attributeName + "' de Dimensions a Qc .");
		String attributeQc = Mapeo.getInstance().getAttributeByName(Mapeo.QC, attributeName);
		String attributeDim = Mapeo.getInstance().getAttributeByName(Mapeo.DIM, attributeName);

		Field attributeField = requestQc.getFields().getFieldByName(attributeQc);
		if (!attributeField.getValue().isEmpty()) {
			attributeField.getValue().clear();
		}

		int attributeNumber = dimTarea.getAttributeNumber(attributeDim);

		@SuppressWarnings("unchecked")
		List<String> atributos = (List<String>) requestDim.getAttribute(attributeNumber);
		/*
		 * Esto se hace porque en Dimensions se puede ingresar valores repetidos,
		 * mientras que si en Qc no se encuentran repetidos en la lista a
		 * seleccionar se produce una excepcion.
		 */
		Map<String, String> valoresDim = new HashMap<String, String>();
		for (String valor : atributos) {
			valoresDim.put(valor, valor);
		}
		List<String> atributoDim = new ArrayList<String>();
		for (String valor : valoresDim.keySet()) {
			atributoDim.add(valor);
		}

		for (String valor : atributoDim) {
			if (valor != null && !"".equals(valor)) {
				Entity.Fields.Field.Value value = new Value();
				value.setValue(encoding(valor));
				requestQc.getFields().getFieldByName(attributeQc).addValue(value);
			}
		}
	}

	public boolean esCambioSincronizador(String id, String nombreEsquemaBaseQc) throws Exception {
		logger.debug("Obteniendo informacion sobre el ultimo cambio efectuado en Qc (auditoria) para el bug con id: " + id);
		Audit audit = qcConnection.getUltimaAuditoria(id, nombreEsquemaBaseQc);

		if (audit != null && audit.getUser() != null) {
			logger.debug("Usuario que efectuo la ultima modificacion en Qc: " + audit.getUser());
			return (audit.getUser().equals(qcConnection.getAlmUser()));
		} else {
			logger.error("No se pudo obtener registro sobre el ultimo cambio efectuado en Qc (auditoria).");
			throw new Exception("No se pudo obtener registro sobre el ultimo cambio efectuado en Qc (auditoria).");
		}
	}

	/**
	 * Metodo que se retorna el contenido del campo que es una agrupacion
	 * (combinacion de campos) en Dimensions en un solo string. Solo sirve para el
	 * campo 'comentarios' y bajo la estructura que pidio el cliente, para el
	 * resto de los campos devuelve una cadena vacia. Esto es un caso excepcional.
	 * 
	 * @throws Exception
	 */
	private String obtenerContenidoCampoMultiple(String attributeName, String attributeDim, Request requestDim, DimensionsTarea dimTarea, String productoDim) throws Exception {

		String contenido = "";
		if (!"comentarios".equals(attributeName)) {
			return contenido;
		}

		List<String> atributosDim = CampoMultiple.getInstance().getAtributosCombinacion(attributeDim);

		if (atributosDim == null || atributosDim.isEmpty()) {
			return contenido;
		}

		if (atributosDim.isEmpty()) {
			return contenido;
		}

		String campoAAcumular = CampoMultiple.getInstance().getAtributoDimPrincipal(attributeDim);

		if (campoAAcumular != null && !"".equals(campoAAcumular)) {
			// Cargo contenido acumulado
			int attributeNumber = dimTarea.getAttributeNumber(campoAAcumular);

			if (dimTarea.esCampoMultiple(campoAAcumular)) {
				@SuppressWarnings("unchecked")
				List<String> contenidoInicial = (List<String>) requestDim.getAttribute(attributeNumber);

				for (String aux : contenidoInicial) {
					if (!"".equals(aux)) {
						contenido = contenido + aux;
					}
				}

			} else {
				contenido = (String) requestDim.getAttribute(attributeNumber);
			}
		}

		String contenidoAux = obtenerComentarioAuxiliarMultiple(atributosDim, requestDim, dimTarea);

		if (!contenido.isEmpty() && !"".equals(contenidoAux)) {
			contenido = contenido + "\n";
		}

		contenido = contenido + contenidoAux;

		String contenidoFinal = contenido;

		contenido = dimTarea.encoding(contenido);
		// Se actualiza el campo comentario en Dimensions previo a enviarse a Qc
		if (campoAAcumular != null && !"".equals(campoAAcumular)) {
			int attributeNumber = dimTarea.getAttributeNumber(campoAAcumular);
			if (!dimTarea.validaCantidadCaracteresCampo(campoAAcumular, contenido)) {
				dimTarea.generarCargaMaximaOMultiple(requestDim, campoAAcumular, attributeNumber, contenido);
			} else {
				requestDim.setAttribute(attributeNumber, contenido);
				requestDim.updateAttribute(attributeNumber);
			}
		}

		// Limpio contenido del campo auxiliar de carga de los comentarios en
		// Dimensions
		dimTarea.limpiarComentarioAuxiliar(atributosDim, requestDim, attributeDim, productoDim);

		return contenidoFinal;
	}

	/**
	 * Metodo auxiliar para terminar de obtener el contenido del campo que es una
	 * agrupacion (combinacion de campos) en Dimensions en un solo string. Solo se
	 * utiliza para el campo 'comentarios'.
	 */
	private String obtenerComentarioAuxiliarMultiple(List<String> atributosDim, Request requestDim, DimensionsTarea dimTarea) {

		if (atributosDim.size() != 3) {
			logger.error("No se especifico de manera correcta los campos que conforman al campo 'comentarios' de Dimensions.");
			return "";
		}

		String campoUsuario = atributosDim.get(0);
		String campoFechaHora = atributosDim.get(1);
		String campoComentario = atributosDim.get(2);

		Map<String, List<String>> contenidos = obtenerCamposMultiple(atributosDim, requestDim, dimTarea);
		// Valido que todas las listas tengan la misma cantidad de registros
		if (!validaCantidadRegistros(contenidos)) {
			return "";
		}

		// Estructura: <Usuario>, <Fecha y Hora>: \n <Nuevo Comentario>
		int cantidadRegistros = contenidos.get(campoUsuario).size();
		List<String> contenidoUsuarios = contenidos.get(campoUsuario);
		List<String> contenidoFechaHora = contenidos.get(campoFechaHora);
		List<String> contenidoComentario = contenidos.get(campoComentario);

		// Esta parte se hace porque en Dimensions se pueden agregar campos arriba o
		// debajo del actual.
		String contenidoAux = "";
		Map<String, String> contenidoAOrdenar = new HashMap<String, String>();

		int i = 0;
		for (; i < cantidadRegistros; i++) {

			if ("".equals(contenidoUsuarios.get(i)) && "".equals(contenidoFechaHora.get(i)) && "".equals(contenidoComentario.get(i))) {
				continue;
			}

			contenidoAux = contenidoUsuarios.get(i) + ", " + contenidoFechaHora.get(i) + ":\n" + contenidoComentario.get(i) + "\n";

			String unaKey = contenidoFechaHora.get(i) + "|" + contenidoUsuarios.get(i);
			if (contenidoAOrdenar.get(unaKey) != null) {
				contenidoAux = contenidoAOrdenar.get(unaKey) + "\n" + contenidoAux;
				contenidoAOrdenar.put(unaKey, contenidoAux);
			} else {
				contenidoAOrdenar.put(unaKey, contenidoAux);
			}
		}

		Map<String, String> contenidoOrdenado = new TreeMap<String, String>(contenidoAOrdenar);
		contenidoAux = "";
		i = 0;
		cantidadRegistros = contenidoAOrdenar.size();
		for (String key : contenidoOrdenado.keySet()) {

			contenidoAux = contenidoAux + contenidoOrdenado.get(key);
			if (i < (cantidadRegistros - 1)) {
				contenidoAux = contenidoAux + "\n\n";
			}
			i++;
		}

		return contenidoAux;
	}

	/**
	 * Metodo retorna el contenido de cada campo que forma el bloque o el campo
	 * que es una agrupacion (combinacion de campos) en Dimensions.
	 */
	private Map<String, List<String>> obtenerCamposMultiple(List<String> atributosDim, Request requestDim, DimensionsTarea dimTarea) {

		Map<String, List<String>> contenidoAtributos = new HashMap<String, List<String>>();

		for (String attributeDim : atributosDim) {
			int attributeNumber = dimTarea.getAttributeNumber(attributeDim);
			@SuppressWarnings("unchecked")
			List<String> contenido = (List<String>) requestDim.getAttribute(attributeNumber);
			contenidoAtributos.put(attributeDim, contenido);
		}

		return contenidoAtributos;
	}

	/**
	 * Metodo que valida que los campos que forma el bloque o el campo que es una
	 * agrupacion (combinacion de campos) en Dimensions contenga la misma cantidad
	 * de registros.
	 */
	private boolean validaCantidadRegistros(Map<String, List<String>> contenido) {
		boolean esPrimero = true;
		int cantidad = 0;

		for (String key : contenido.keySet()) {
			if (esPrimero) {
				cantidad = contenido.get(key).size();
				esPrimero = false;
			} else {
				if (cantidad != contenido.get(key).size()) {
					return false;
				}
			}
		}

		return true;
	}

	public Entity obtenerBug(Long idQc) throws Exception {

		try {
			Entities entities = qcConnection.listEntities("?query={id[" + idQc + "]}");

			if (entities.getEntities().size() != 1) {
				logger.error("No se encontro un bug en Qc que tenga el id:" + idQc);
			}

			return entities.getEntities().get(0);
		} catch (Exception e) {
			logger.error("Ocurrio un error intentando obtener el Bug de Qc.");
			throw new Exception(e);
		}
	}

	public String getProject() {
		return project;
	}
}
