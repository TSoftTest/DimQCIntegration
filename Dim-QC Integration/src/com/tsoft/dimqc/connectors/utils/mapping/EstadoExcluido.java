package com.tsoft.dimqc.connectors.utils.mapping;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.tsoft.dimqc.connectors.exceptions.MapeoException;
import com.tsoft.dimqc.connectors.utils.ConnectorProperties;

public class EstadoExcluido {

	private Logger logger = Logger.getRootLogger();
	private static EstadoExcluido instance = null;
	private static String RUTA = ConnectorProperties.getInstance().getRutaArchMapeoEstadosExcluidos();
	private static String STATUS_DIM = "status-dim";
	private static String STATUS_QC = "status-qc";
	private static String STATUS_MAPPING = "status-mapping";
	private static String STATUS = "status";
	private Set<String> statusQc = new HashSet<String>();
	private Set<String> statusDim = new HashSet<String>();

	private EstadoExcluido() {
		try {
			cargarAtributos(STATUS_DIM, statusDim); // Cargo los estados excluidos de
																							// Dimensions
			cargarAtributos(STATUS_QC, statusQc); // Cargo los estados excluidos de Qc
		} catch (Exception e) {
			logger.debug("No se pudo cargar el mapeo de estados excluidos " + e.getMessage());
			throw new MapeoException("No se pudo cargar el mapeo de estados excluidos", e);
		}
	}

	/**
	 * Obtiene la instancia de {@linkplain EstadoExcluido}
	 * 
	 * @return una instancia de {@linkplain EstadoExcluido}
	 */
	public static EstadoExcluido getInstance() {
		if (instance == null) {
			synchronized (EstadoExcluido.class) {
				if (instance == null) {
					instance = new EstadoExcluido();
				}
			}
		}
		return instance;
	}

	private void cargarAtributos(String tagName, Set<String> status) throws Exception {

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(new File(RUTA));
		doc.getDocumentElement().normalize();

		NodeList listaRequest = doc.getElementsByTagName(tagName);

		for (int i = 0; i < listaRequest.getLength(); i++) {

			Node request = listaRequest.item(i);

			if (request.getNodeType() == Node.ELEMENT_NODE) {

				Element elemento = (Element) request;

				for (int k = 0; k < elemento.getElementsByTagName(STATUS_MAPPING).getLength(); k++) {

					Node lista = elemento.getElementsByTagName(STATUS_MAPPING).item(k);
					NamedNodeMap listaAtributosMapping = lista.getAttributes();

					String unEstado = null;

					for (int j = 0; j < listaAtributosMapping.getLength(); j++) {

						Node uno = lista.getAttributes().item(j);

						if (STATUS.equals(uno.getNodeName())) {
							unEstado = uno.getNodeValue();
						}
					}

					if (unEstado != null && !"".equals(unEstado)) {
						// Agrego el estado
						status.add(unEstado);
					}

				}
			}
		}
	}

	/**
	 * Metodo que especifica si un estado es excluido de Dimensions.
	 * 
	 * @return si el estado esta excluido o no.
	 */
	public boolean isEstadoExcluidoDim(String status) {
		return statusDim.contains(status);
	}

	/**
	 * Metodo que especifica si un estado es excluido de Qc.
	 * 
	 * @return si el estado esta excluido o no.
	 */
	public boolean isEstadoExcluidoQc(String status) {
		return statusQc.contains(status);
	}

}
