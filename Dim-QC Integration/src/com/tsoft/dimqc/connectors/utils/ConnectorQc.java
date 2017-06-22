package ar.com.tssa.serena.connectors.utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ar.com.tssa.serena.connectors.exceptions.MapeoException;
import ar.com.tssa.serena.connectors.exceptions.PropertiesException;

public class ConnectorQc {
	
	private Logger logger = Logger.getRootLogger();
	private static ConnectorQc instance = null;
	private static String RUTA = ConnectorProperties.getInstance().getRutaArchivoConfiguracionQC();
	private static String CONNECTORQC = "connectorQc";
	private static String CONNECTORQC_MAPPING = "connectorQc-mapping";
	/** 
	 * usuario de conexion de la base de datos de HPALM
	 */
	public static String USER_NAME_ALM = "userNameAlm";
	/**
	 * clave del usuario de conexion de la base de datos de HPALM
	 */
	public static String PASSWORD_ALM = "passwordAlm";
	/**
	 * domain de HPALM
	 */
	public static String DOMAIN_ALM = "domainAlm";
	/**
	 * proyecto de HPALM
	 */
	public static String PROJECT_ALM = "projectAlm";
	/**
	 * server de HPALM
	 */
	public static String SERVER_ALM = "serverAlm";
	/**
	 * puerto de HPALM
	 */
	public static String PORT_ALM = "portAlm";
	/** 
	 * Indica si se deben listar o no los atributos de un defecto de QC.
	 */
	public static String LISTAR_QC_ATTRIBUTES = "listarQCAttributes";
	/** 
	 * Me devuelve cual es el nombre del defecto de QC sobre el que tengo que listar los atributos.
	 */
	public static String DEFECT_SUMMARY = "defectSummary";
	private static String RUTA_ARCHIVO_KEY_ALM ="rutaArchivoKeyAlm";
	private static String RUTA_ARCHIVO_HASH_ALM ="rutaArchivoHashAlm";
	/** 
	 * Nombre o esquema para la base de datos
	 */
	public static String NOMBRE_ESQUEMA_BASE = "nombre-esquema-base";
	
	private Map<Integer,Map<String,String>> coneccionesQc = new HashMap<Integer, Map<String,String>>();
	
	private ConnectorQc() {
		try {
			cargarDatos();
		} catch (Exception e) {
			logger.debug("No se pudo cargar el mapeo de conecciones a Qc " + e.getMessage());
			throw new MapeoException("No se pudo cargar el mapeo de conecciones a Qc", e);
		}
	}

	/**
	 * Obtiene la instancia de {@linkplain ConnectorQc}
	 * 
	 * @return una instancia de {@linkplain ConnectorQc}
	 */
	public static ConnectorQc getInstance() {
		if (instance == null) {
			synchronized (ConnectorQc.class){
				if (instance == null) {
					instance = new ConnectorQc();
				}
			}
		}
		return instance;
	}
	
	private void cargarDatos() throws Exception {
		
		  DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		  DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		  Document doc = dBuilder.parse(new File(RUTA));
		  doc.getDocumentElement().normalize();
		  
		  NodeList listaRequest = doc.getElementsByTagName(CONNECTORQC);
		  
		  for (int i= 0; i < listaRequest.getLength(); i++){
			  
			  Node request = listaRequest.item(i);
			  
			  if (request.getNodeType() == Node.ELEMENT_NODE){
				  
				  Element elemento = (Element) request;
				  
					  for (int k=0; k < elemento.getElementsByTagName(CONNECTORQC_MAPPING).getLength(); k++) {
						  
						  Node lista = elemento.getElementsByTagName(CONNECTORQC_MAPPING).item(k);
						  NamedNodeMap listaAtributosMapping = lista.getAttributes();

						  Map<String,String> datos = new HashMap<String, String>();
						  String rutaKey ="";
						  String rutaHash = "";
						  
						  for (int j = 0; j < listaAtributosMapping.getLength(); j++){
							  						 						  
							  Node uno = lista.getAttributes().item(j);
							  
							  if (USER_NAME_ALM.equals(uno.getNodeName())){
								  datos.put(USER_NAME_ALM, uno.getNodeValue());
							  } else if (RUTA_ARCHIVO_KEY_ALM.equals(uno.getNodeName())){
								  rutaKey = uno.getNodeValue();
							  } else if (RUTA_ARCHIVO_HASH_ALM.equals(uno.getNodeName())){
								  rutaHash = uno.getNodeValue();
							  } else if (DOMAIN_ALM.equals(uno.getNodeName())){
								  datos.put(DOMAIN_ALM, uno.getNodeValue());
							  } else if (PROJECT_ALM.equals(uno.getNodeName())){
								  datos.put(PROJECT_ALM, uno.getNodeValue());
							  } else if (SERVER_ALM.equals(uno.getNodeName())){
								  datos.put(SERVER_ALM, uno.getNodeValue());
							  } else if (PORT_ALM.equals(uno.getNodeName())){
								  
								  // Esto solo es para verificar que sea un valor correcto
								  try{
									  Integer.parseInt(uno.getNodeValue());
								  }
								  catch (NumberFormatException e) {
									  throw new PropertiesException("Ocurrio un error al convertir la propiedad '"+PORT_ALM+"'");
								  }
								  
								  datos.put(PORT_ALM, uno.getNodeValue());
							  } else if (LISTAR_QC_ATTRIBUTES.equals(uno.getNodeName())){
								  if ("TRUE".equals(uno.getNodeValue().toString().toUpperCase())){
									  datos.put(LISTAR_QC_ATTRIBUTES, "true");
								  } else {
									  datos.put(LISTAR_QC_ATTRIBUTES, "false");
								  }
								  
							  } else if (DEFECT_SUMMARY.equals(uno.getNodeName())){
								  datos.put(DEFECT_SUMMARY, uno.getNodeValue());
							  } else if (NOMBRE_ESQUEMA_BASE.equals(uno.getNodeName())){
								  datos.put(NOMBRE_ESQUEMA_BASE, uno.getNodeValue());
							  }
							  						 
						  }
						  String pass = DesencriptadorPassword.obtenerPassword(rutaKey, rutaHash);
						  datos.put(PASSWORD_ALM, pass);

						  //Agrego los atributos
						  this.coneccionesQc.put(i, datos);		  						 
					  }
			  }
		  }
	}
	
	/**
	 * Metodo que retorna la lista de las conecciones a Qc
	 * 
	 * @return conecciones a Qc
	 */
	public Map<Integer,Map<String,String>> getConeccionesQc() {
		return this.coneccionesQc;
	}
	
	/**
	 * Metodo que retorna los datos por proyectos de Qc
	 * 
	 * @return conecciones a Qc por proyecto
	 */
	public Map<String, Map<String,String>> getProyectosDatosQc(){
		
		Map<String, Map<String,String>> proyectosDatos = new HashMap<String, Map<String,String>>();
		
		if (!coneccionesQc.isEmpty()) {

			for (Map<String, String> datos : coneccionesQc.values()) {
				proyectosDatos.put(datos.get(ConnectorQc.PROJECT_ALM), datos);
			}
		}
		
		return proyectosDatos;
	}
}
