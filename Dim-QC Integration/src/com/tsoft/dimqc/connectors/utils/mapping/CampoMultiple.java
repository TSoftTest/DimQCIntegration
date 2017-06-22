package ar.com.tssa.serena.connectors.utils.mapping;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ar.com.tssa.serena.connectors.dto.CampoMultiplesDTO;
import ar.com.tssa.serena.connectors.exceptions.MapeoException;
import ar.com.tssa.serena.connectors.utils.ConnectorProperties;

public class CampoMultiple {

	private Logger logger = Logger.getRootLogger();
	private static CampoMultiple instance = null;
	private static String RUTA = ConnectorProperties.getInstance().getRutaArchCamposMultiples();
	private static String CAMPO_MULTIPLE = "campoMultiple";
	private static String CAMPO_MULTIPLE_MAPPING = "campoMultiple-mapping";
	private static String ATTRIBUTE_DIMENSIONS = "attributeDim";
	private static String ATTRIBUTE_DIMENSIONS_PRINCIPAL = "attributeDimPrincipal";
	private static String ATTRIBUTES = "attributes";
	private Map<String,CampoMultiplesDTO> atributos = new HashMap<String, CampoMultiplesDTO>();
	
	private CampoMultiple(){
		try {
			cargarAtributos();
		} catch (Exception e) {
			logger.debug("No se pudo cargar el mapeo de campos multiples " + e.getMessage());
			throw new MapeoException("No se pudo cargar el mapeo de campos multiples", e);
		}
	}
	
	public static CampoMultiple getInstance(){
		if (instance == null) {
			synchronized (CampoMultiple.class){
				if (instance == null) {
					instance = new CampoMultiple();
				}
			}
		}
		return instance;
	}
	
	private void cargarAtributos() throws Exception {
		
		  DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		  DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		  Document doc = dBuilder.parse(new File(RUTA));
		  doc.getDocumentElement().normalize();
		  
		  NodeList listaRequest = doc.getElementsByTagName(CAMPO_MULTIPLE);
		  
		  for (int i= 0; i < listaRequest.getLength(); i++){
			  
			  Node request = listaRequest.item(i);
			  
			  if (request.getNodeType() == Node.ELEMENT_NODE){
				  
				  Element elemento = (Element) request;
				  
					  for (int k=0; k < elemento.getElementsByTagName(CAMPO_MULTIPLE_MAPPING).getLength(); k++) {
						  
						  Node lista = elemento.getElementsByTagName(CAMPO_MULTIPLE_MAPPING).item(k);
						  NamedNodeMap listaAtributosMapping = lista.getAttributes();
						  						  
						  String dim = null;
						  String atrib = null;
						  String acumulado = "";
						  
						  for (int j = 0; j < listaAtributosMapping.getLength(); j++){
							  						 						  
							  Node uno = lista.getAttributes().item(j);
							  
							  if (ATTRIBUTE_DIMENSIONS.equals(uno.getNodeName())){
								  if (uno.getNodeValue() == null || "".equals(uno.getNodeValue())){
									  logger.error("El atributo del mapeo '"+ ATTRIBUTE_DIMENSIONS +"' no tiene un valor asociado");
									  throw new MapeoException("El atributo del mapeo '"+ ATTRIBUTE_DIMENSIONS +"' no tiene un valor asociado");
								  }
								  dim = uno.getNodeValue();
								  
							  } else if (ATTRIBUTES.equals(uno.getNodeName())){
								  if (uno.getNodeValue() == null || "".equals(uno.getNodeValue())){
									  logger.error("El atributo del mapeo '"+ ATTRIBUTES +"' no tiene un valor asociado");
									  throw new MapeoException("El atributo del mapeo '"+ ATTRIBUTES +"' no tiene un valor asociado");
								  }
								  atrib = uno.getNodeValue();
							  } else if (ATTRIBUTE_DIMENSIONS_PRINCIPAL.equals(uno.getNodeName())){
								  if (uno.getNodeValue() != null && !"".equals(uno.getNodeValue())){
									  acumulado = uno.getNodeValue();
								  }
							  }				 
						  }
						  
						  String listaAtributos [] = atrib.split(";");
						  List<String> atributosDim = new ArrayList<String>();
						  for (String atributo : listaAtributos){
							  atributosDim.add(atributo.trim());
						  }
						  
						  CampoMultiplesDTO campoMultiplesDTO = new CampoMultiplesDTO(acumulado, atributosDim);
						  
						  //Agrego los atributos
						  atributos.put(dim, campoMultiplesDTO);					  						 
					  }
			  }
		  }
	}
	
	/**
	 * Metodo que obtiene el listado de atributos de Dimensions que hace a la combinación.
	 * 
	 * @param name
	 *            nombre del atributo de Dim
	 * @return listado con los nombres de atributos de Dimensions
	 * @throws MapeoException
	 */
	public List<String> getAtributosCombinacion(String name) {
		CampoMultiplesDTO campoMultiplesDTO = atributos.get(name);
		
		if (campoMultiplesDTO == null) {
			return null;
		}

		return campoMultiplesDTO.getAtributosCombinacion();
	}
	
	/**
	 * Metodo que obtiene el listado de atributos de Dimensions que hace a la combinación.
	 * 
	 * @param name
	 *            nombre del atributo de Dim
	 * @return listado con los nombres de atributos de Dimensions
	 * @throws MapeoException
	 */
	public String getAtributoDimPrincipal(String name) {
		CampoMultiplesDTO campoMultiplesDTO = atributos.get(name);
		
		if (campoMultiplesDTO == null) {
			return null;
		}

		return campoMultiplesDTO.getAtributoDimPrincipal();
	}

	/**
	 * Metodo que valida si un determinado campo que es una agrupacion
	 * (combinacion de campos) en Dimensions. 
	 */
	public boolean esCampoConCombinacion(String attributeDim) {
		List<String> atributosDim=  CampoMultiple.getInstance().getAtributosCombinacion(attributeDim);
		
		if (atributosDim == null || atributosDim.isEmpty()){
			return false;
		}
		
		return true;
	}	
}
