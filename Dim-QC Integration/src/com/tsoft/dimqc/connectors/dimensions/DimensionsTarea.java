package com.tsoft.dimqc.connectors.dimensions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.serena.dmclient.api.DimensionsConnection;
import com.serena.dmclient.api.DimensionsConnectionException;
import com.serena.dmclient.api.DimensionsResult;
import com.serena.dmclient.api.DimensionsRuntimeException;
import com.serena.dmclient.api.LoginFailedException;
import com.serena.dmclient.api.Request;
import com.serena.dmclient.api.RequestAttachment;
import com.serena.dmclient.api.RequestAttachmentDetails;
import com.serena.dmclient.api.RequestDetails;
import com.serena.dmclient.api.SystemAttributes;
import com.serena.dmclient.objects.AttributeDefinition;
import com.serena.dmclient.objects.AttributeType;
import com.serena.dmclient.objects.ValidSet;
import com.serena.dmclient.objects.ValidSetRowDetails;
import com.tsoft.dimqc.connectors.alm.Entity;
import com.tsoft.dimqc.connectors.alm.Entity.Fields.Field.Value;
import com.tsoft.dimqc.connectors.alm.QcTarea;
import com.tsoft.dimqc.connectors.exceptions.ServiceException;
import com.tsoft.dimqc.connectors.utils.ConnectorProperties;
import com.tsoft.dimqc.connectors.utils.mapping.CampoMultiple;
import com.tsoft.dimqc.connectors.utils.mapping.Estado;
import com.tsoft.dimqc.connectors.utils.mapping.EstadoExcluido;
import com.tsoft.dimqc.connectors.utils.mapping.Mapeo;
import com.tsoft.dimqc.connectors.utils.mapping.Pair;
import com.tsoft.dimqc.connectors.utils.parser.HtmlElement;
import com.tsoft.dimqc.connectors.utils.parser.HtmlParser;
import com.tsoft.dimqc.connectors.utils.xml.FieldsQc;
import com.tsoft.dimqc.database.ConsultasSql;
import com.tsoft.dimqc.database.TipoConexion;

public class DimensionsTarea {

	private DimensionsConnection connection;
	private Logger logger = Logger.getRootLogger();
	private Map<String, FieldsQc.Field> fieldsQc = new HashMap<String, FieldsQc.Field>();
	private Map<String, AttributeDefinition> definicionCampos = new HashMap<String, AttributeDefinition>();
	private boolean ajustoCantidadCampos = false;
	private static final String ENCODING_BASE = "ISO-8859-1";

	public DimensionsTarea() throws ServiceException {
		connect();
	}

	private void connect() {
		try {
			connection = DimensionsConnectionFactory.getConnection();
			logger.debug("Dimensions Authentication: true");

		} catch (LoginFailedException e) {
			logger.error("Serena: " + e.getMessage());
			throw new ServiceException(e.getMessage());
		} catch (DimensionsConnectionException e) {
			logger.error("Serena: " + e.getMessage());
			throw new ServiceException(e.getMessage());
		} catch (Exception e) {
			logger.error("Ocurrio un error al conectarse con Dimensions");
			logger.error(e);
			throw new ServiceException(e.getMessage());
		}
	}

	public void closeConnection() {
		if (connection != null) {
			connection.close();
			logger.debug("Dimensions LogOut: true");
		}
	}

	/**
	 * Metodo que hace un update de Qc a Dimensions de todos los atributos
	 * definidos en mapeo.xml
	 * 
	 * @param requestDim
	 *          Request de Dimensions
	 * @param requestQc
	 *          Request de Qc
	 * @return void
	 * @throws Exception
	 */
	public boolean updateDimensionsRequest(Request requestDim, Entity requestQc, QcTarea qcTarea, String nombreEsquemaQC, List<String> proyectosEnDim) throws Exception {
		this.fieldsQc = qcTarea.getFieldsQc();
		// Cargo los atributos de Dimensions
		cargarAtributosDimensions(requestDim);
		boolean actualizoOk = true;
		Map<String, Pair<String, String>> mapa = Mapeo.getInstance().getAtributosGenerales();
		try {

			for (Entry<String, Pair<String, String>> entry : mapa.entrySet()) {
				try {
					String attributeQc = Mapeo.getInstance().getAttributeByName(Mapeo.QC, entry.getKey());

					if (Mapeo.getInstance().hayQueCopiarADimensions(entry.getKey())) {
						if (this.fieldsQc.get(attributeQc).isSupportsMultivalue()) {
							updateMultivalueAttribute(requestDim, requestQc, entry.getKey(), nombreEsquemaQC, false, proyectosEnDim);
						} else {
							updateGeneralAttribute(requestDim, requestQc, entry.getKey(), nombreEsquemaQC, false, proyectosEnDim);
						}
					} else {
						logger.debug("No se sincroniza el atributo '" + entry.getKey() + "' de Qc a Dimensions.");
					}
				} catch (Exception e) {
					actualizoOk = false;
					logger.error("Ocurrio un error al settear el campo " + entry.getKey() + " en Dimensions: ");
					logger.error(e);
				}

			}
			// Actualizo la descripcionGeneral (se encuentra en formato html) - Campo
			// de la solapa: General
			updateDescriptionAttribute(requestDim, requestQc, "descripcionGeneral");

			// Actualizo la descripcion (se encuentra en formato html) - Campo de la
			// solapa: Attributes
			updateDescriptionAttribute(requestDim, requestQc, "descripcion");

			// Actualizo los comentarios (se encuentra en formato html)
			updateCommentsAttribute(requestDim, requestQc);

			// Actualizo el "detectado en ciclo"
			logger.debug("Setteando el atributo 'detectadoEnCiclo' de Qc a Dimensions.");
			updateTreeAttribute(requestDim, requestQc, "detectadoEnCiclo");

			// Actualizo el "detectado en release"
			logger.debug("Setteando el atributo 'detectadoEnRelease' de Qc a Dimensions.");
			updateTreeAttribute(requestDim, requestQc, "detectadoEnRelease");

			String attributeName = Mapeo.getInstance().getAttributeByName(Mapeo.QC, "estado");
			com.tsoft.dimqc.connectors.alm.Entity.Fields.Field attributeField = requestQc.getFields().getFieldByName(attributeName);
			String qcStatus = new String();
			if (!attributeField.getValue().isEmpty() && attributeField.getValue().get(0) != null && !"".equals(attributeField.getValue().get(0).getValue())) {
				qcStatus = attributeField.getValue().get(0).getValue();
			}

			/*
			 * Si el estado de Qc no es uno de los estados que se excluyen de ser
			 * replicados a Dimension, se procede a su sincronizacion y de ser
			 * necesario se hace tambien el pasaje de estado.
			 */
			if (qcStatus != null && !"".equals(qcStatus) && !EstadoExcluido.getInstance().isEstadoExcluidoQc(qcStatus)) {
				logger.debug("Setteando el atributo 'estado' de Qc a Dimensions.");
				try {
					String dimStatus = Estado.getInstance().getEquivalentStatus(Estado.QC, qcStatus);
					if (!"".equals(dimStatus)) { // Si esta vacio significa que no existe
						                           // su equivalente
						int numeroDeAtributoEstado = getAttributeNumber("STATUS");

						requestDim.queryAttribute(numeroDeAtributoEstado);

						String estadoDimensions = (String) requestDim.getAttribute(numeroDeAtributoEstado);
						if (!estadoDimensions.trim().equalsIgnoreCase(dimStatus.trim())) {
							requestDim.actionTo(dimStatus);
						} else {
							logger.debug("No se ejecuta el pasaje de estados porque el request de Dimensions ya se encuentra en estado: " + dimStatus);
						}

					} else {
						logger.error("No se pudo encontrar el equivalente en Dimensions del estado: " + qcStatus);
					}
				} catch (DimensionsRuntimeException e) {
					logger.debug(e); // Puede tirar exepcion de que el request ya se
					                 // encuentra en ese estado
				}

				pasajeDeEstadoEnDimensions(requestQc, requestDim);
			} else if (EstadoExcluido.getInstance().isEstadoExcluidoQc(qcStatus)) {
				logger.debug("Estado de Qc excluido de ser replicado a Dimensions:" + qcStatus);
			}

			updateFechaSincronizacionDim(requestDim);

			return actualizoOk;
		} catch (Exception e) {
			logger.error("Ocurrio un error seteando los campos en Dimensions: ");
			throw new Exception(e);
		}
	}

	/**
	 * Realiza el update del campo descripcionGeneral dado que se encuentra en
	 * formato HTML y necesita un tratado especial
	 * 
	 * @param requestDim
	 * @param requestQc
	 * @throws Exception
	 */
	private void updateDescriptionAttribute(Request requestDim, Entity requestQc, String campo) throws Exception {
		logger.debug("Setteando el atributo '" + campo + "' de Qc a Dimensions.");
		String attributeQc = Mapeo.getInstance().getAttributeByName(Mapeo.QC, campo);
		com.tsoft.dimqc.connectors.alm.Entity.Fields.Field attributeField = requestQc.getFields().getFieldByName(attributeQc);
		String description = new String();

		if (!attributeField.getValue().isEmpty() && attributeField.getValue().get(0) != null && !"".equals(attributeField.getValue().get(0).getValue())
		    && !fieldsQc.get(attributeQc).isSupportsMultivalue()) {
			description = attributeField.getValue().get(0).getValue();
		}

		if (!"".equals(description)) {
			description = parsearHtml(description);
		}

		String attributeDim = Mapeo.getInstance().getAttributeByName(Mapeo.DIM, campo);
		int attributeNumber = 0;
		if ("descripcionGeneral".equals(campo)) {
			Field field = SystemAttributes.class.getDeclaredField(attributeDim);
			attributeNumber = field.getInt(null);
		} else if ("descripcion".equals(campo)) {
			attributeNumber = getAttributeNumber(attributeDim);
		}

		description = new String(description.getBytes(ConnectorProperties.getInstance().getEncodingDim()), ConnectorProperties.getInstance().getEncodingQc());
		description = ajustarContenidoACantidadCaracteresDescripcion(attributeDim, description);
		requestDim.setAttribute(attributeNumber, description);
		requestDim.updateAttribute(attributeNumber);
	}

	/**
	 * Realiza el update del campo comentario dado que se encuentra en formato
	 * HTML y necesita un tratado especial
	 * 
	 * @param requestDim
	 * @param requestQc
	 * @throws Exception
	 */
	private void updateCommentsAttribute(Request requestDim, Entity requestQc) throws Exception {
		logger.debug("Setteando el atributo 'comentarios' de Qc a Dimensions.");
		String attributeQc = Mapeo.getInstance().getAttributeByName(Mapeo.QC, "comentarios");
		com.tsoft.dimqc.connectors.alm.Entity.Fields.Field attributeField = requestQc.getFields().getFieldByName(attributeQc);
		String comentario = new String();

		if (!attributeField.getValue().isEmpty() && attributeField.getValue().get(0) != null && !"".equals(attributeField.getValue().get(0).getValue())
		    && !fieldsQc.get(attributeQc).isSupportsMultivalue()) {
			comentario = attributeField.getValue().get(0).getValue();
		}
		if (!"".equals(comentario)) {
			comentario = parsearHtml(comentario);
			String attributeDim = Mapeo.getInstance().getAttributeByName(Mapeo.DIM, "comentarios");
			String attributeDimComentarioAux = Mapeo.getInstance().getAttributeByName(Mapeo.DIM, "comentarios");

			if (CampoMultiple.getInstance().esCampoConCombinacion(attributeDim)) {
				attributeDim = CampoMultiple.getInstance().getAtributoDimPrincipal(attributeDim);
			}

			int attributeNumber = getAttributeNumber(attributeDim);
			comentario = encoding(comentario);

			// Limpio el contenido del campo comentario
			if (esCampoMultiple(attributeDim)) {
				requestDim.setAttribute(attributeNumber, new ArrayList<String>());
			} else {
				requestDim.setAttribute(attributeNumber, "");
			}

			if (!validaCantidadCaracteresCampo(attributeDim, comentario)) {
				generarCargaMaximaOMultiple(requestDim, attributeDim, attributeNumber, comentario);
			} else {
				requestDim.setAttribute(attributeNumber, comentario);
				requestDim.updateAttribute(attributeNumber);
			}

			// Limpio los campos de carga auxiliar de ser necesario.
			if (CampoMultiple.getInstance().esCampoConCombinacion(attributeDimComentarioAux)) {
				String proyectoDim = requestQc.getFields().getFieldByName(ConnectorProperties.getInstance().getCampoProjectQc()).getValue().get(0).getValue();
				String productoDim = DimensionsProducts.getInstance().getProductByName(DimensionsProducts.DIM, proyectoDim);
				List<String> atributosDim = CampoMultiple.getInstance().getAtributosCombinacion(attributeDimComentarioAux);
				limpiarComentarioAuxiliar(atributosDim, requestDim, attributeDimComentarioAux, productoDim);
			}
		}
	}

	/**
	 * Realiza el update del campo tipo tree (Ej: Detectado en release) dado que
	 * se obtiene del attributo del valor y no del valor, y necesita un tratado
	 * especial
	 * 
	 * @param requestDim
	 * @param requestQc
	 * @throws Exception
	 */
	private void updateTreeAttribute(Request requestDim, Entity requestQc, String attribute) throws Exception {
		String attributeQc = Mapeo.getInstance().getAttributeByName(Mapeo.QC, attribute);
		com.tsoft.dimqc.connectors.alm.Entity.Fields.Field attributeField = requestQc.getFields().getFieldByName(attributeQc);
		String value = "";

		if (!attributeField.getValue().isEmpty() && attributeField.getValue().get(0) != null && !"".equals(attributeField.getValue().get(0).getReferenceValue())
		    && !fieldsQc.get(attributeQc).isSupportsMultivalue()) {
			value = attributeField.getValue().get(0).getReferenceValue();
		}
		if (value != null) {
			String attributeDim = Mapeo.getInstance().getAttributeByName(Mapeo.DIM, attribute);
			int attributeNumber = getAttributeNumber(attributeDim);
			value = new String(value.getBytes(ConnectorProperties.getInstance().getEncodingDim()), ConnectorProperties.getInstance().getEncodingQc());
			value = ajustarContenidoACantidadCaracteres(attributeDim, value);
			requestDim.setAttribute(attributeNumber, value);
			requestDim.updateAttribute(attributeNumber);
		}

	}

	/**
	 * Realiza el update de los campos especificados en el mapeo.xml, salvo
	 * aquellos que se copian obligatoriamente
	 * 
	 * @param requestDim
	 * @param requestQc
	 * @param attributeName
	 * @throws Exception
	 */
	private void updateGeneralAttribute(Request requestDim, Entity requestQc, String attributeName, String nombreEsquemaQC, Boolean llamadaRecursiva, List<String> proyectosEnDim)
	    throws Exception {

		String attributeDim = null;
		String attributeQc = null;
		logger.debug("Setteando el atributo '" + attributeName + "' de Qc a Dimensions.");
		attributeDim = Mapeo.getInstance().getAttributeByName(Mapeo.DIM, attributeName);
		int attributeNumber = getAttributeNumber(attributeDim);
		attributeQc = Mapeo.getInstance().getAttributeByName(Mapeo.QC, attributeName);

		com.tsoft.dimqc.connectors.alm.Entity.Fields.Field attributeField = requestQc.getFields().getFieldByName(attributeQc);
		String attribute = new String();
		if (!attributeField.getValue().isEmpty() && attributeField.getValue().get(0) != null && !"".equals(attributeField.getValue().get(0).getValue())
		    && !fieldsQc.get(attributeQc).isSupportsMultivalue()) {
			attribute = attributeField.getValue().get(0).getValue();
			attribute = new String(attribute.getBytes(ConnectorProperties.getInstance().getEncodingDim()), ConnectorProperties.getInstance().getEncodingQc());
		}
		attribute = ajustarContenidoACantidadCaracteres(attributeDim, attribute);

		try {
			requestDim.setAttribute(attributeNumber, attribute);
			requestDim.updateAttribute(attributeNumber);
		} catch (DimensionsRuntimeException excepcionDimensions) {
			if (llamadaRecursiva) {
				throw new DimensionsRuntimeException("No se pudo actualizar el validset para el atributo '" + attributeName + "'.");
			}
			Pattern regex = Pattern.compile(ConnectorProperties.getInstance().getMensajeDeErrorValidSet());
			Matcher regexMatcher = regex.matcher(excepcionDimensions.getMessage());
			if (regexMatcher.find()) {
				logger.info("El valor/es '" + regexMatcher.group(1) + "' para el atributo '" + attributeName + "' no se encuentra dentro del valid set");
				logger.info("Se actualiza el ValidSet");

				// Se obtiene el campo de QC para detectar el id de la lista
				com.tsoft.dimqc.connectors.utils.xml.FieldsQc.Field campo = this.fieldsQc.get(attributeQc);

				// Se obtiene el ID de la lista asociada al campo de QC
				int idDeLista = campo.getListId();

				// Actualizacion de ValidSet
				if (actualizarValidSet(attributeDim, idDeLista, nombreEsquemaQC, proyectosEnDim)) {
					updateGeneralAttribute(requestDim, requestQc, attributeName, nombreEsquemaQC, true, proyectosEnDim);
				} else {
					throw new DimensionsRuntimeException("No se pudo actualizar el validset para el atributo '" + attributeName + ".");
				}
			} else {
				throw new DimensionsRuntimeException(excepcionDimensions.getMessage());
			}
		}
	}

	public Request findRequestById(String requestID) {
		Request requestObj = null;
		try {
			requestObj = connection.getObjectFactory().findRequest(requestID);
			Map<String, Pair<String, String>> mapa = Mapeo.getInstance().getAttributes();
			List<Integer> numeroAtributos = new ArrayList<Integer>();
			for (Entry<String, Pair<String, String>> entry : mapa.entrySet()) {
				String attributeDim = Mapeo.getInstance().getAttributeByName(Mapeo.DIM, entry.getKey());
				int attributeNumber = 0;

				if ("descripcionGeneral".equals(entry.getKey())) {
					java.lang.reflect.Field field = SystemAttributes.class.getDeclaredField(attributeDim);
					attributeNumber = field.getInt(null);
				} else if (CampoMultiple.getInstance().esCampoConCombinacion(attributeDim)) {
					List<String> atributosDim = CampoMultiple.getInstance().getAtributosCombinacion(attributeDim);
					for (String atributo : atributosDim) {
						attributeNumber = getAttributeNumber(atributo);
						numeroAtributos.add(attributeNumber);
					}

					String campoPrincipal = CampoMultiple.getInstance().getAtributoDimPrincipal(attributeDim);
					if (campoPrincipal != null && !"".equals(campoPrincipal)) {
						attributeNumber = getAttributeNumber(campoPrincipal);
						numeroAtributos.add(attributeNumber);
					}

					continue;

				} else {
					attributeNumber = getAttributeNumber(attributeDim);
				}
				numeroAtributos.add(attributeNumber);
			}

			int[] attrNums = new int[numeroAtributos.size()];
			int i = 0;
			for (Integer nro : numeroAtributos) {
				attrNums[i] = nro;
				i++;
			}

			if (requestObj != null) {
				requestObj.queryAttribute(attrNums);
			} else {
				logger.error("No se encontro el request en Dimensions con ID: " + requestID);
			}
		} catch (Exception e) {
			logger.error("Ocurrio un error al obtener el request en Dimensions. ID: " + requestID);
			logger.error(e);
		}
		return requestObj;
	}

	public int getAttributeNumber(String attribute) {
		int number = connection.getObjectFactory().getAttributeNumber(attribute, Request.class);
		if (number == 0) {
			Field field;
			try {
				field = SystemAttributes.class.getDeclaredField(attribute);
				number = field.getInt(null);
			} catch (Exception e) {
				logger.error("Ocurrio un error al obtener el numero de atributo en Dimensions. Atributo: " + attribute);
				logger.error(e);
			}
		}
		return number;
	}

	/**
	 * Metodo que crea un request en Dimensions en base al request de QC
	 * 
	 * @param requestQc
	 * @param productoDim
	 * @param qcTarea
	 * @return el id del request de dimensions creado
	 */
	public String crearRequestEnDim(Entity requestQc, String productoDim, QcTarea qcTarea, String nombreEsquemaQC) {

		try {
			this.fieldsQc = qcTarea.getFieldsQc();
			RequestDetails requestDim = new RequestDetails();
			String attributeDim = null;
			String attributeQc = null;
			String nombreDeBaseline = null;

			// Setteo el id de QC en Dimensions
			attributeQc = Mapeo.getInstance().getAttributeByName(Mapeo.QC, "idQc");
			com.tsoft.dimqc.connectors.alm.Entity.Fields.Field attributeField = requestQc.getFields().getFieldByName(attributeQc);

			String idQc = new String();
			if (!attributeField.getValue().isEmpty() && attributeField.getValue().get(0) != null && !"".equals(attributeField.getValue().get(0).getValue())
			    && !fieldsQc.get(attributeQc).isSupportsMultivalue()) {
				logger.debug("Setteando el atributo 'idQc' en Dimensions.");
				idQc = attributeField.getValue().get(0).getValue();
				idQc = new String(idQc.getBytes(ConnectorProperties.getInstance().getEncodingDim()), ConnectorProperties.getInstance().getEncodingQc());

				attributeDim = Mapeo.getInstance().getAttributeByName(Mapeo.DIM, "idQc");
				int attributeNumber = getAttributeNumber(attributeDim);
				requestDim.setAttribute(attributeNumber, idQc);
			}

			// Setteo el producto
			requestDim.setProductName(productoDim);

			// Se obtiene la baseline afectada
			nombreDeBaseline = obtenerBaselineAfectada(requestQc);

			// Se setea el tipo de stream segun la baseline afectada
			requestDim.setRelatedProject(productoDim + ":" + ConsultasSql.obtenerStreamParaCrearRequestEnDimensions(nombreDeBaseline, productoDim));

			// Setteo el tipo de request
			requestDim.setTypeName("BUG");

			DimensionsResult result = connection.getObjectFactory().createRequest(requestDim);

			return result.getAdmResult().getUserData().toString();

		} catch (Exception e) {
			logger.error("Ocurrio un error al crear el Request en Dimensions.");
			logger.error(e);
		}

		return null;
	}

	public void updateFechaSincronizacionDim(Request dimRequest) throws Exception {

		try {
			logger.debug("Actualizando la fecha de sincronizacion en Dimensions.");
			// Preparo todo para que haya menos diferencia de tiempo
			String attributeIdDim = Mapeo.getInstance().getAttributeByName(Mapeo.DIM, "idDim");
			int attributeIdDimNumber = getAttributeNumber(attributeIdDim);
			String idDim = (String) dimRequest.getAttribute(attributeIdDimNumber);

			String attributeFechaAuditoriaDim = Mapeo.getInstance().getAttributeByName(Mapeo.DIM, "fechaAuditoria");
			int attributeFechaAuditoriaNumber = getAttributeNumber(attributeFechaAuditoriaDim);
			dimRequest.queryAttribute(attributeFechaAuditoriaNumber);
			String fechaAuditoria = (String) dimRequest.getAttribute(attributeFechaAuditoriaNumber);
			String attributeFechaSincronizacionDim = Mapeo.getInstance().getAttributeByName(Mapeo.DIM, "fechaSincronizacion");
			int attributeFechaSincronizacionNumber = getAttributeNumber(attributeFechaSincronizacionDim);
			dimRequest.setAttribute(attributeFechaSincronizacionNumber, fechaAuditoria);
			dimRequest.updateAttribute(attributeFechaSincronizacionNumber);

			Request request = connection.getObjectFactory().findRequest(idDim);

			if (request == null) {
				throw new Exception("Error en la actualizacion de la fecha de sincronizacion en Dimensions.");
			}

			request.queryAttribute(attributeFechaAuditoriaNumber);

			fechaAuditoria = (String) request.getAttribute(attributeFechaAuditoriaNumber); // "dd-MMM-yyyy hh:mm:ss"
			request.setAttribute(attributeFechaSincronizacionNumber, fechaAuditoria);
			request.updateAttribute(attributeFechaSincronizacionNumber);

		} catch (Exception e) {
			logger.error("Error en la actualizacion de la fecha de sincronizacion en Dimensions.");
			logger.error(e);
		}
	}

	public void cargarArchivos(Request requestDim, String path, String nombreBajada) throws Exception {

		try {
			RequestAttachmentDetails archivo = new RequestAttachmentDetails();

			archivo.setName(nombreBajada);
			archivo.setDescription("Archivo migrado de Qc a Dimensions.");
			archivo.setFileName(path);

			RequestAttachmentDetails[] archivos = new RequestAttachmentDetails[] { archivo };

			logger.debug("Setteando el archivo '" + nombreBajada + "' de Qc a Dimensions.");
			requestDim.attach(archivos);

		} catch (Exception e) {
			logger.error("Error en la carga de archivos de Qc a Dimensions.");
			logger.error(e);

		}
	}

	private String parsearHtml(String html) {

		// Si es un html, el mismo se parsea.
		Document doc = Jsoup.parse(html);

		HtmlParser parser = new HtmlParser();
		HtmlElement elementos = parser.parse(doc);

		return (elementos.dibujar());
	}

	public String encoding(String valor) throws UnsupportedEncodingException {

		if (valor == null) {
			valor = "";
		}

		return (new String(valor.getBytes(ConnectorProperties.getInstance().getEncodingDim()), ConnectorProperties.getInstance().getEncodingQc()));
	}

	public String decoding(String valor) throws UnsupportedEncodingException {

		if (valor == null) {
			valor = "";
		}

		return (new String(valor.getBytes(ConnectorProperties.getInstance().getEncodingQc()), ConnectorProperties.getInstance().getEncodingDim()));
	}

	/**
	 * @param requestDim
	 * @param requestQc
	 * @param attributeName
	 * @throws UnsupportedEncodingException
	 */
	private void updateMultivalueAttribute(Request requestDim, Entity requestQc, String attributeName, String nombreEsquemaQC, Boolean llamadaRecursiva, List<String> proyectosEnDim)
	    throws UnsupportedEncodingException {
		String attributeDim = null;
		String attributeQc = null;
		logger.debug("Setteando el atributo '" + attributeName + "' de Qc a Dimensions.");
		attributeDim = Mapeo.getInstance().getAttributeByName(Mapeo.DIM, attributeName);
		int attributeNumber = getAttributeNumber(attributeDim);
		attributeQc = Mapeo.getInstance().getAttributeByName(Mapeo.QC, attributeName);

		com.tsoft.dimqc.connectors.alm.Entity.Fields.Field attributeField = requestQc.getFields().getFieldByName(attributeQc);
		String attribute = new String();

		Vector<String> valores = new Vector<String>();
		if (!attributeField.getValue().isEmpty()) {
			for (Value value : attributeField.getValue()) {
				if (value.getValue() != null && !"".equals(value.getValue())) {
					attribute = value.getValue();
					attribute = new String(attribute.getBytes(ConnectorProperties.getInstance().getEncodingDim()), ConnectorProperties.getInstance().getEncodingQc());
					valores.add(attribute);
				}
			}
		}

		try {
			requestDim.setAttribute(attributeNumber, valores);
			requestDim.updateAttribute(attributeNumber);
		} catch (DimensionsRuntimeException excepcionDimensions) {
			if (llamadaRecursiva) {
				throw new DimensionsRuntimeException("No se pudo actualizar el validset para el atributo '" + attributeName + "'.");
			}
			Pattern regex = Pattern.compile(ConnectorProperties.getInstance().getMensajeDeErrorValidSet());
			Matcher regexMatcher = regex.matcher(excepcionDimensions.getMessage());
			if (regexMatcher.find()) {
				logger.info("El valor/es '" + regexMatcher.group(1) + "' para el atributo '" + attributeName + "' no se encuentra dentro del valid set");
				logger.info("Se actualiza el ValidSet");

				// Se obtiene el campo de QC para detectar el id de la lista
				com.tsoft.dimqc.connectors.utils.xml.FieldsQc.Field campo = this.fieldsQc.get(attributeQc);

				// Se obtiene el ID de la lista asociada al campo de QC
				int idDeLista = campo.getListId();

				// Actualizacion de ValidSet
				if (actualizarValidSet(attributeDim, idDeLista, nombreEsquemaQC, proyectosEnDim)) {
					updateMultivalueAttribute(requestDim, requestQc, attributeName, nombreEsquemaQC, true, proyectosEnDim);
				} else {
					throw new DimensionsRuntimeException("No se pudo actualizar el validset para el atributo '" + attributeName + ".");
				}
			} else {
				throw new DimensionsRuntimeException(excepcionDimensions.getMessage());
			}
		}
	}

	/**
	 * Metodo que carga la definicion de los atributos de Dimensions para un
	 * request en particular.
	 */
	public void cargarAtributosDimensions(Request request) {
		logger.debug("Cargando atributos de Dimensions.");

		// Limpio para cargar los atributos del nuevo request.
		if (!definicionCampos.isEmpty()) {
			definicionCampos.clear();
		}

		@SuppressWarnings("unchecked")
		List<AttributeDefinition> atributos = request.getAttributesForRoleSection((String) request.getAttribute(SystemAttributes.STATUS), "$ALL");

		for (AttributeDefinition atributo : atributos) {
			definicionCampos.put(atributo.getName(), atributo);
		}
	}

	public AttributeDefinition obtenerDefinicionCampoDim(String name) {
		return definicionCampos.get(name);
	}

	/**
	 * Metodo que modifica la cantidad maxima de caracteres permitida que tiene un
	 * campo.
	 */
	public boolean actualizarCantidadCaracteres(String name, int nuevoTamanio) {
		AttributeDefinition atributo = definicionCampos.get(name);

		if (atributo == null) {
			return false;
		}

		atributo.setMaximumLength(nuevoTamanio);
		atributo.flush();
		atributo.save();

		return true;
	}

	/**
	 * Metodo que actualiza la lista de valores permitidos de un campo.
	 * 
	 * @param nombreAtributo
	 *          : El nombre del atributo de dimensions cuyo Validset se desea
	 *          actualizar
	 * @param idDeLista
	 *          : El ID de la lista en QualityCenter para obtener los valores
	 */
	@SuppressWarnings("unchecked")
	public boolean actualizarValidSet(String nombreAtributo, int idDeLista, String nombreEsquemaQC, List<String> proyectosEnDim) {

		// Se obtiene la definicion del atributo
		AttributeDefinition atributo = definicionCampos.get(nombreAtributo);

		// Se define el objeto ValidSet
		ValidSet objValidSet;

		// Se define la lista de valores en Dimensions
		List<ValidSetRowDetails> listaDeValoresDimensions;

		// Se define la lista de valores en QC
		List<String> listaDeValoresQC = null;

		// Si no se pudo obtener el atributo, se devuelve false
		if (atributo == null) {
			return false;
		}

		// Se obtiene el ValidSet del atributo
		objValidSet = atributo.getValidSet();

		// Si no se pudo obtener el ValidSet, se devuelve false
		if (objValidSet == null) {
			return false;
		}

		// Se obtiene la lista de valores actual del ValidSet
		listaDeValoresDimensions = objValidSet.getValues();

		try {
			// Obtengo los datos de la base directamente
			if (TipoConexion.ORACLE.equals(ConnectorProperties.getInstance().getQcDataBaseTipo())) {

				listaDeValoresQC = ConsultasSql.obtenerListaDeValoresParaCampoDesdeQC(String.valueOf(idDeLista), ConnectorProperties.getInstance().getQcDataBaseTipo(), ConnectorProperties
				    .getInstance().getQcDataBaseSDI(), nombreEsquemaQC, proyectosEnDim);

			} else if (TipoConexion.SQL_SERVER.equals(ConnectorProperties.getInstance().getQcDataBaseTipo())) {

				listaDeValoresQC = ConsultasSql.obtenerListaDeValoresParaCampoDesdeQC(String.valueOf(idDeLista), ConnectorProperties.getInstance().getQcDataBaseTipo(), nombreEsquemaQC,
				    "", proyectosEnDim);

			}

			if (!listaDeValoresQC.isEmpty()) {
				listaDeValoresDimensions.clear();

				for (ListIterator<String> iterator = listaDeValoresQC.listIterator(); iterator.hasNext();) {
					String valor = iterator.next();
					ValidSetRowDetails nuevoValorDeValidSet = new ValidSetRowDetails();
					nuevoValorDeValidSet.setColumnValue(0, valor);
					listaDeValoresDimensions.add(nuevoValorDeValidSet);
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			return false;
		}

		objValidSet.save();

		return true;
	}

	/**
	 * Metodo que modifica el tipo de campo que es a multiple en caso de que no lo
	 * sea.
	 */
	public boolean actualizarATipoMultiple(String name) {

		if (esCampoMultiple(name)) {
			return true;
		}

		AttributeDefinition atributo = definicionCampos.get(name);

		if (atributo == null) {
			return false;
		}

		atributo.setType(AttributeType.SFMV);
		atributo.flush();
		atributo.save();

		return true;
	}

	/**
	 * Metodo que modifica el tipo de campo que es a multiple en caso de que no lo
	 * sea.
	 */
	public boolean esCampoMultiple(String name) {
		AttributeDefinition atributo = definicionCampos.get(name);

		if (atributo == null) {
			return false;
		}

		if (atributo.getType() == AttributeType.SFMV) {
			return true;
		}

		return false;
	}

	/**
	 * Metodo que deja los campos que se utilizaron para cargar el campo
	 * Comentario sin contenido. Para hacerlo cambia la configuracion del campo
	 * agrupador de ser necesario para realizar el cambio y luego vuelve a dejarlo
	 * con la configuracion que tenia previamente.
	 * 
	 * @throws Exception
	 */
	public void limpiarComentarioAuxiliar(List<String> atributosDim, Request requestDim, String nombreAgrupador, String productoDim) throws Exception {

		if (!ajustoCantidadCampos) {
			logger.debug("Se limpian los campos de carga auxiliar para 'comentarios' de Dimensions.");
			/*
			 * Solo funciona en una configuracion (Block Update Type) de las dos
			 * posibles: 1) Allow users to add, modify and delete rows. (CON ESTA
			 * CONFIGURACION SI ANDA) 2) Do not allow user to modify or delete rows;
			 * only to append new rows. (CON ESTA CONFIGURACION NO FUNCIONA) En la
			 * base las mismas son representadas: 1) Allow users to add, modify and
			 * delete rows. -> ATTR_1 = 1 2) Do not allow user to modify or delete
			 * rows; only to append new rows. -> ATTR_1 = 2
			 */
			int nro = ConsultasSql.consultarBlockUpdateTypeDimensions(nombreAgrupador, productoDim);
			boolean seModifico = false;
			if (nro != 1) {
				ConsultasSql.updateBlockUpdateTypeDimensions(nombreAgrupador, productoDim, 1);
				seModifico = true;
			}

			for (String attributeDim : atributosDim) {
				int attributeNumber = getAttributeNumber(attributeDim);
				requestDim.setAttribute(attributeNumber, "");
				requestDim.updateAttribute(attributeNumber);
			}

			if (seModifico) {
				ConsultasSql.updateBlockUpdateTypeDimensions(nombreAgrupador, productoDim, nro);
			}
			ajustoCantidadCampos = false;
		}
	}

	/**
	 * Metodo que determina si un campo contiene la cantidad de caracteres maxima
	 * necesaria para cargar el contenido que se especifica.
	 */
	public boolean validaCantidadCaracteresCampo(String attributeName, String contenido) {

		AttributeDefinition attributeDefinition = obtenerDefinicionCampoDim(attributeName);

		if (attributeDefinition == null) {
			// logger.debug("No se pudo obtener la configuración del campo '" +
			// attributeName + "' de Dimensions");
			return false;
		}

		int cantMaxima = 0;
		try {
			cantMaxima = attributeDefinition.getMaximumLength();
		} catch (Exception e) {
			// logger.error("Se produjo un error al obtener la cantidad máxima de caracteres que soporta actualmente el campo '"
			// + attributeName + "' de Dimensions; se actualizará a la máxima.");
			return false;
		}

		if (contenido.length() > cantMaxima) {
			return false;
		}

		return true;
	}

	/**
	 * Método que se utiliza en la actualizacion de un request en Dimensions.
	 * 
	 * @throws UnsupportedEncodingException
	 */
	public void generarCargaMaximaOMultiple(Request requestDim, String attributeName, int attributeNumber, String contenido) throws UnsupportedEncodingException {

		int cantMaximaGeneral = ConnectorProperties.getInstance().getMaximoCaracteresDim();
		actualizarCantidadCaracteres(attributeName, cantMaximaGeneral);

		if (contenido.length() <= cantMaximaGeneral) {
			requestDim.setAttribute(attributeNumber, contenido);
			requestDim.updateAttribute(attributeNumber);
		} else {
			// Tengo q ponerlo multiple
			actualizarATipoMultiple(attributeName);

			// Fracciono el contenido
			Vector<String> valores = parsearParaCampoMultiple(contenido, cantMaximaGeneral);
			Vector<String> valoresFinales = ajustarCantidadCampos(valores);

			requestDim.setAttribute(attributeNumber, valoresFinales);
			requestDim.updateAttribute(attributeNumber);
		}
	}

	private Vector<String> ajustarCantidadCampos(Vector<String> valores) throws UnsupportedEncodingException {

		int cantidadMaximaCampos = ConnectorProperties.getInstance().getCantidadMaximaCamposMultiples();

		if (cantidadMaximaCampos >= valores.size()) {
			this.ajustoCantidadCampos = false;
			return valores;
		}

		Vector<String> nuevosValores = new Vector<String>();
		int i = 1;
		for (String unValor : valores) {
			if (i == cantidadMaximaCampos) {
				String comentario = ConnectorProperties.getInstance().getComentarioFinUltimoCampo();
				// Encoding, y espacio que ocupa en la base.
				comentario = encoding(comentario);
				String unContenido = (new String(comentario.getBytes(ConnectorProperties.getInstance().getEncodingDim()), ENCODING_BASE));
				// Fin Encoding, y espacio que ocupa en la base.
				int posicionFinal = unValor.length() - unContenido.length();
				String aux = unValor.substring(0, posicionFinal);
				aux = aux + comentario;
				nuevosValores.add(aux);
				break;
			}
			nuevosValores.add(unValor);
			i++;
		}
		this.ajustoCantidadCampos = true;
		return nuevosValores;
	}

	private Vector<String> parsearParaCampoMultiple(String contenido, int cantidadMaximaCaracteres) throws UnsupportedEncodingException {

		Vector<String> valores = new Vector<String>();

		contenido = contenido.replaceAll("\r\n", "\n");

		// Fracciono el contenido. Trunco
		int inicial = 0;
		int posicionFinal = cantidadMaximaCaracteres;
		String unContenido = "";

		String parrafo = "";
		while (posicionFinal <= contenido.length()) {
			parrafo = contenido.substring(inicial, posicionFinal);

			unContenido = (new String(parrafo.getBytes(ConnectorProperties.getInstance().getEncodingDim()), ENCODING_BASE));

			if (unContenido.length() > parrafo.length()) {
				posicionFinal = posicionFinal - (unContenido.length() - parrafo.length());
				if (posicionFinal == inicial) {
					posicionFinal = (inicial + (cantidadMaximaCaracteres / 2));
				}
				parrafo = contenido.substring(inicial, posicionFinal);
			}

			if (!"".equals(parrafo)) {
				valores.add(parrafo);
			}

			inicial = posicionFinal;
			posicionFinal = posicionFinal + cantidadMaximaCaracteres;
		}

		parrafo = contenido.substring(inicial);

		unContenido = (new String(parrafo.getBytes(ConnectorProperties.getInstance().getEncodingDim()), ENCODING_BASE));

		while (unContenido.length() > cantidadMaximaCaracteres) {

			if (unContenido.length() > parrafo.length()) {
				posicionFinal = posicionFinal - (unContenido.length() - parrafo.length());
				if (posicionFinal == inicial) {
					posicionFinal = (inicial + (cantidadMaximaCaracteres / 2));
				}
				parrafo = contenido.substring(inicial, posicionFinal);
			}

			if (!"".equals(parrafo)) {
				valores.add(parrafo);
			}

			inicial = posicionFinal;
			parrafo = contenido.substring(inicial);
			unContenido = (new String(parrafo.getBytes(ConnectorProperties.getInstance().getEncodingDim()), ENCODING_BASE));
		}

		if (!"".equals(parrafo)) {
			valores.add(parrafo);
		}

		return valores;
	}

	/**
	 * En caso de que el contenido sea mayor a la cantidad maxima de caracteres
	 * que permite ese campo, se modifica esa cantidad al maximo general. Si
	 * igualmente el contenido es mayor, se trunca el contenido para poder cargar
	 * el campo sin problemas.
	 * 
	 * @throws Exception
	 */
	public String ajustarContenidoACantidadCaracteres(String attributeName, String contenido) throws Exception {

		boolean okCantidad = true;
		if (!validaCantidadCaracteresCampo(attributeName, contenido)) {
			try {
				int cantMaximaGeneral = ConnectorProperties.getInstance().getMaximoCaracteresDim();
				boolean actualiza = actualizarCantidadCaracteres(attributeName, cantMaximaGeneral);
				if (actualiza) {
					if (contenido.length() <= cantMaximaGeneral) {
						okCantidad = true;
					} else {
						okCantidad = false;
					}
				} else {
					okCantidad = false;
				}
			} catch (Exception e) {
				okCantidad = false;
			}
		}

		if (okCantidad) {
			return contenido;
		} else {
			// Se trunca
			if (obtenerDefinicionCampoDim(attributeName) == null) {
				// Se busca el campo en la base
				int cantidad = ConsultasSql.consultarMaximaCantiadadCaracteresDim(attributeName);
				if (cantidad != -1) {
					if (contenido.length() <= cantidad) {
						return contenido;
					} else {
						return contenido.substring(0, cantidad);
					}
				} else {
					return contenido;
				}

			} else {
				return contenido.substring(0, obtenerDefinicionCampoDim(attributeName).getMaximumLength());
			}
		}
	}

	/**
	 * En caso de que el contenido sea mayor a la cantidad maxima de caracteres
	 * que permite ese campo, se modifica esa cantidad al maximo general. Si
	 * igualmente el contenido es mayor, se trunca el contenido para poder cargar
	 * el campo sin problemas. Este metodo se utiliza solo para el atributo
	 * Descripcion (tanto el de la solapa general como atributos). Esto se hace
	 * para lograr que ambos se vean igual.
	 * 
	 * @throws Exception
	 * 
	 */
	public String ajustarContenidoACantidadCaracteresDescripcion(String attributeName, String contenido) throws Exception {
		String contenidoAux = contenido;
		ajustarContenidoACantidadCaracteres(attributeName, contenidoAux);

		int cantidadMaxima = -1;

		if (obtenerDefinicionCampoDim(attributeName) == null) {
			// Se busca el campo en la base
			cantidadMaxima = ConsultasSql.consultarMaximaCantiadadCaracteresDim(attributeName);
		} else {
			cantidadMaxima = obtenerDefinicionCampoDim(attributeName).getMaximumLength();
		}

		if (cantidadMaxima == -1) {
			return contenido;
		}

		// Esto es para ajustar la cantidad de caracteres que se pueden guardar en
		// la base.
		String unContenido = (new String(contenido.getBytes(ConnectorProperties.getInstance().getEncodingDim()), ENCODING_BASE));
		if (unContenido.length() <= cantidadMaxima) {
			return contenido;
		} else {
			contenido = contenido.substring(0, cantidadMaxima);
			unContenido = (new String(contenido.getBytes(ConnectorProperties.getInstance().getEncodingDim()), ENCODING_BASE));
			int posicion = cantidadMaxima;

			if (unContenido.length() > contenido.length()) {
				posicion = posicion - (unContenido.length() - contenido.length());
			}

			return contenido.substring(0, posicion);
		}
	}

	public boolean completarCreacionRequestEnDim(String idDim, Entity requestQc, String productoDim, QcTarea qcTarea, String nombreEsquemaQC, List<String> proyectosEnDim) {

		boolean completoCarga = false;
		try {
			logger.debug("Se va a completar la creacion del Request en Dimensions.");

			Request request = findRequestById(idDim);
			completoCarga = updateDimensionsRequest(request, requestQc, qcTarea, nombreEsquemaQC, proyectosEnDim);

		} catch (Exception e) {
			completoCarga = false;
			logger.error("Ocurrio un error al terminar de crear el Request en Dimensions.");
			logger.error(e);
		}

		return completoCarga;
	}

	public boolean asignarRequestEnDim(String idDim, Entity requestQc, String productoDim, QcTarea qcTarea, String nombreEsquemaQC, List<String> proyectosEnDim) {

		boolean asignacionDeRequest = true;
		String usuarioADelegar = null;
		String rolADelegar = null;
		String capabilityADelegar = null;
		String estadoDeAsignacion = null;
		String baselineEnQC = null;

		try {
			logger.debug("Se va a asignar el Request en Dimensions.");

			// Se obtiene el rol a delegar
			rolADelegar = ConnectorProperties.getInstance().getRolADelegar();

			// Se obtiene el capability para delegar
			capabilityADelegar = ConnectorProperties.getInstance().getCapabilityADelegar();

			// Se obtiene el estado de asignacion
			estadoDeAsignacion = ConnectorProperties.getInstance().getEstadoDeAsignacion();

			// Se obtiene la baseline desde QC
			baselineEnQC = obtenerBaselineAfectada(requestQc);

			// Obtengo el usuario a delegar por Query
			if (TipoConexion.ORACLE.equals(ConnectorProperties.getInstance().getQcDataBaseTipo())) {

				usuarioADelegar = ConsultasSql.obtenerUsuarioAAsignarDesdeDimensions(baselineEnQC, productoDim);

			} else if (TipoConexion.SQL_SERVER.equals(ConnectorProperties.getInstance().getQcDataBaseTipo())) {

				usuarioADelegar = ConsultasSql.obtenerUsuarioAAsignarDesdeDimensions(baselineEnQC, productoDim);

			}

			if (usuarioADelegar != null) {
				List<String> listaDeUsuariosADelegar = new ArrayList<String>();

				listaDeUsuariosADelegar.add(usuarioADelegar);

				Request request = findRequestById(idDim);

				logger.debug("Se delega el Request '" + idDim + "' al usuario '" + usuarioADelegar + "' en Dimensions.");
				request.delegateTo(listaDeUsuariosADelegar, rolADelegar, capabilityADelegar, true);

				logger.debug("Se acciona el Request '" + idDim + "' al estado '" + estadoDeAsignacion + "' en Dimensions.");
				request.actionTo(estadoDeAsignacion);
			} else {
				logger.debug("No se asigna el Request '" + idDim + "' por no encontrar un usuario en Dimensions a quien asignarlo.");
			}

		} catch (Exception e) {
			asignacionDeRequest = false;
			logger.error("Ocurrio un error al asignar el Request en Dimensions.");
			logger.error(e);
		}

		return asignacionDeRequest;
	}

	public String obtenerBaselineAfectada(Entity requestQC) {
		String baselineEnQC = null;
		String nombreDeCampoBaseline = ConnectorProperties.getInstance().getCampoBaselineEnQC();

		try {
			String attributeQc = null;
			logger.debug("Obteniendo el atributo '" + nombreDeCampoBaseline + "' de Qc.");
			attributeQc = Mapeo.getInstance().getAttributeByName(Mapeo.QC, nombreDeCampoBaseline);

			com.tsoft.dimqc.connectors.alm.Entity.Fields.Field attributeField = requestQC.getFields().getFieldByName(attributeQc);

			if (!attributeField.getValue().isEmpty() && attributeField.getValue().get(0) != null && !"".equals(attributeField.getValue().get(0).getValue())
			    && !fieldsQc.get(attributeQc).isSupportsMultivalue()) {
				baselineEnQC = attributeField.getValue().get(0).getValue();
				baselineEnQC = new String(baselineEnQC.getBytes(ConnectorProperties.getInstance().getEncodingDim()), ConnectorProperties.getInstance().getEncodingQc());
			}
		} catch (Exception e) {
			logger.error("Hubo un error obteniendo el atributo '" + nombreDeCampoBaseline + "' de Qc: " + e.getMessage());
		}

		return baselineEnQC;

	}

	/**
	 * Metodo que se utiliza para hacer el paraje de Estado de New a Open en
	 * Dimensions.
	 * 
	 * @throws UnsupportedEncodingException
	 * 
	 */
	private void pasajeDeEstadoEnDimensions(Entity requestQc, Request request) throws UnsupportedEncodingException {

		String attributeName = Mapeo.getInstance().getAttributeByName(Mapeo.QC, "estado");

		com.tsoft.dimqc.connectors.alm.Entity.Fields.Field attributeField = requestQc.getFields().getFieldByName(attributeName);
		String qcStatus = new String();
		if (!attributeField.getValue().isEmpty() && attributeField.getValue().get(0) != null && !"".equals(attributeField.getValue().get(0).getValue())
		    && !fieldsQc.get(attributeName).isSupportsMultivalue()) {
			qcStatus = attributeField.getValue().get(0).getValue();
		}
		try {
			qcStatus = new String(qcStatus.getBytes(ConnectorProperties.getInstance().getEncodingDim()), ConnectorProperties.getInstance().getEncodingQc());

			logger.debug("Verificando pasaje de estado en Dimensions.");
			if (ConnectorProperties.getInstance().isEstadoInicialQc(qcStatus)) {
				String estadoFinalQc = ConnectorProperties.getInstance().getEstadoFinalQc();

				// Se verifica que el estado no sea uno de los "Estados Excluidos" de
				// ser sincronizados.
				if (estadoFinalQc != null && !EstadoExcluido.getInstance().isEstadoExcluidoQc(estadoFinalQc)) {
					String estadoFinalDim = Estado.getInstance().getEquivalentStatus(Estado.QC, estadoFinalQc);
					if (estadoFinalDim != null && !"".equals(estadoFinalDim)) { // Si esta
						                                                          // vacio
						                                                          // significa
						                                                          // que no
						                                                          // existe
						                                                          // su
						                                                          // equivalente
						request.actionTo(estadoFinalDim);
					} else {
						logger.error("No se pudo encontrar el equivalente en Dimensions del estado: " + estadoFinalQc);
					}
				} else if (EstadoExcluido.getInstance().isEstadoExcluidoQc(estadoFinalQc)) {
					logger.debug("Estado de Qc excluido de ser replicado a Dimensions:" + estadoFinalQc);
				}
			}
		} catch (DimensionsRuntimeException e) {
			logger.debug(e);// Puede tirar exepcion de que el request ya se encuentra
			                // en ese estado, o que el estado que se quiere settear no
			                // existe
		}
	}

	public String descargarArchivo(RequestAttachment dimAttachment) {
		String fullPath = null;

		try {
			logger.debug("Bajando archivo desde Dimensions");

			String fileFolder = ConnectorProperties.getInstance().getDimensionsDownloadFolder(); // Carpeta
			                                                                                     // donde
			                                                                                     // esta
			                                                                                     // corriendo
			                                                                                     // el
			                                                                                     // servicio
			                                                                                     // (depende
			                                                                                     // de
			                                                                                     // windows)
			String dimensionsDownloadFileName = ConnectorProperties.getInstance().getDimensionsDownloadFileName(); // nombre
			                                                                                                       // con
			                                                                                                       // el
			                                                                                                       // que
			                                                                                                       // se
			                                                                                                       // va
			                                                                                                       // a
			                                                                                                       // crear
			                                                                                                       // el
			                                                                                                       // archivo
			String tempFolder = ConnectorProperties.getInstance().getRutaArchivosTemporales(); // Carpeta
			                                                                                   // a
			                                                                                   // donde
			                                                                                   // se
			                                                                                   // va
			                                                                                   // a
			                                                                                   // copiar
			                                                                                   // el
			                                                                                   // archivo
			String attachmentName = dimAttachment.getName(); // nombre del attachment,
			                                                 // con el que se va a
			                                                 // crear el archivo
			                                                 // definitivo

			String tempDownloadLocation = fileFolder + "/" + dimensionsDownloadFileName;
			logger.debug("Intentando bajar archivo '" + attachmentName + "' a " + tempDownloadLocation);
			dimAttachment.saveToFile(dimensionsDownloadFileName);
			logger.debug("Archivo bajado desde Dimensions");

			fullPath = tempFolder + "/" + attachmentName;
			copyFiles(tempDownloadLocation, fullPath);

			logger.debug("Archivo copiado a ubicacion: " + fullPath);
		} catch (Exception e) {
			logger.error("Ocurrio un error intentar bajar un attachment de Dimensions");
			logger.error(e);
		}

		return fullPath;
	}

	public void copyFiles(String source, String dest) throws Exception {
		logger.debug("Copiando archivo desde " + source + " a " + dest);

		FileInputStream fileInputStream = null;
		FileOutputStream fileOutputStream = null;

		FileChannel inputChannel = null;
		FileChannel outputChannel = null;

		try {
			fileInputStream = new FileInputStream(source);
			fileOutputStream = new FileOutputStream(dest);

			inputChannel = fileInputStream.getChannel();
			outputChannel = fileOutputStream.getChannel();
			outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
		} finally {
			try {
				if (inputChannel != null)
					inputChannel.close();
			} catch (Exception e) {
			}
			try {
				if (outputChannel != null)
					inputChannel.close();
			} catch (Exception e) {
			}
			try {
				if (fileInputStream != null)
					fileInputStream.close();
			} catch (Exception e) {
			}
			try {
				if (fileOutputStream != null)
					fileOutputStream.close();
				fileOutputStream.close();
			} catch (Exception e) {
			}
		}

		File destFile = new File(dest);
		if (destFile.exists()) {
			logger.debug("File: " + destFile + " existe");
		} else {
			logger.debug("File: " + destFile + " NO existe");
		}

		logger.debug("Archivo copiado a: " + dest);
	}
}