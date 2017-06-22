package ar.com.tssa.serena.connectors.utils.mapping;

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
import ar.com.tssa.serena.connectors.utils.ConnectorProperties;

public class Estado {
	
	private Logger logger = Logger.getRootLogger();
	private static Estado instance = null;
	private static String RUTA = ConnectorProperties.getInstance().getRutaArchMapeoEstados();
	private static String STATUS = "status";
	private static String STATUS_MAPPING = "status-mapping";
	private static String STATUS_QC = "statusQC";
	private static String STATUS_DIMENSIONS = "statusDim";
	public static String DIM = "DIM";
	public static String QC= "QC";
	private Map<String,String> statusQc =new HashMap<String, String>();
	private Map<String,String> statusDim =new HashMap<String, String>();
	
	private Estado() {
		try {
			cargarAtributos();
		} catch (Exception e) {
			logger.debug("No se pudo cargar el mapeo de estados " + e.getMessage());
			throw new MapeoException("No se pudo cargar el mapeo de estados", e);
		}
	}

	/**
	 * Obtiene la instancia de {@linkplain Estado}
	 * 
	 * @return una instancia de {@linkplain Estado}
	 */
	public static Estado getInstance() {
		if (instance == null) {
			synchronized (Estado.class){
				if (instance == null) {
					instance = new Estado();
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
		  
		  NodeList listaRequest = doc.getElementsByTagName(STATUS);
		  
		  for (int i= 0; i < listaRequest.getLength(); i++){
			  
			  Node request = listaRequest.item(i);
			  
			  if (request.getNodeType() == Node.ELEMENT_NODE){
				  
				  Element elemento = (Element) request;
				  
					  for (int k=0; k < elemento.getElementsByTagName(STATUS_MAPPING).getLength(); k++) {
						  
						  Node lista = elemento.getElementsByTagName(STATUS_MAPPING).item(k);
						  NamedNodeMap listaAtributosMapping = lista.getAttributes();
						  						  
						  String qc = null;
						  String dim = null;
						
						  
						  for (int j = 0; j < listaAtributosMapping.getLength(); j++){
							  						 						  
							  Node uno = lista.getAttributes().item(j);
							  
							  if (STATUS_QC.equals(uno.getNodeName())){
								  qc = uno.getNodeValue();
							  }
							  else if (STATUS_DIMENSIONS.equals(uno.getNodeName())){
								  dim = uno.getNodeValue();
							  }
							  						 
						  }
						  
						  //Agrego los atributos
						  this.statusDim.put(dim,qc);
						  this.statusQc.put(qc,dim);						  						 
					  }
			  }
		  }
	}
	
	/**
	 * Metodo que obtiene el equivalente del estado especificado
	 * 
	 * @param reqType
	 * 				tipo de atributo this.DIM o this.QC
	 * @param name
	 *            nombre del atributo
	 * @return equivalente del estado
	 * @throws MapeoException
	 */
	public String getEquivalentStatus(String reqType, String status) {
		
		String statusValue = null;
		
		try{
			
			if(DIM.equals(reqType)){
				statusValue = this.statusDim.get(status);
			}
			else if(QC.equals(reqType)){
				statusValue = this.statusQc.get(status);
			}
			
			if (statusValue == null) {
				throw new MapeoException("No se pudo encontrar el equivalente del estado: " + status);
			}
		
		}catch (Exception e) {
			logger.error(e);
		}
		return statusValue;
	}
}
