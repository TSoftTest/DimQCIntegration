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

import ar.com.tssa.serena.connectors.exceptions.MapeoException;
import ar.com.tssa.serena.connectors.exceptions.PropertiesException;
import ar.com.tssa.serena.connectors.utils.ConnectorProperties;

public class Mapeo {
	private Logger logger = Logger.getRootLogger();
	private static Mapeo instance = null;
	private static String RUTA = ConnectorProperties.getInstance().getRutaArchivoMapeo();
	private static String ATTRIBUTE = "attribute";
	private static String ATTRIBUTE_MAPPING = "attribute-mapping";
	private static String ATTRIBUTE_NAME = "attributeName";
	private static String ATTRIBUTE_QC = "attributeQC";
	private static String ATTRIBUTE_DIMENSIONS = "attributeDim";
	private static String ATTRIBUTE_MEMO = "memo";
	private static String ATTRIBUTE_COPIARAQC = "copiarAQC";
	private static String ATTRIBUTE_COPIARADIMENSIONS = "copiarADimensions";
	public static String DIM = "DIM";
	public static String QC= "QC";
	private Map<String,Pair<String,String>> atributos =new HashMap<String, Pair<String,String>>();
	private List<String> listaDimensions = new ArrayList<String>();
	private List<String> listaQc = new ArrayList<String>();
	private Map<String,Pair<String,String>> atributosGenerales = new HashMap<String, Pair<String,String>>();
	private Map<String,Boolean> atributosMemo = new HashMap<String,Boolean>();
	private Map<String,Boolean> atributosCopiarAQC = new HashMap<String,Boolean>();
	private Map<String,Boolean> atributosCopiarADimensions = new HashMap<String,Boolean>();
	
	private Mapeo() {
		try {
			cargarAtributos();
			cargarAtributosGenerales();
		} catch (Exception e) {
			logger.debug("No se pudo cargar el mapeo de atributos " + e.getMessage());
			throw new MapeoException("No se pudo cargar el mapeo de atributos", e);
		}
	}
	
		
	/**
	 * Obtiene la instancia de {@linkplain Mapeo}
	 * 
	 * @return una instancia de {@linkplain Mapeo}
	 */
	public static Mapeo getInstance() {
		if (instance == null) {
			synchronized(Mapeo.class){
				if (instance == null) {
					instance = new Mapeo();
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
		  
		  NodeList listaRequest = doc.getElementsByTagName(ATTRIBUTE);
		  
		  for (int i= 0; i < listaRequest.getLength(); i++){
			  
			  Node request = listaRequest.item(i);
			  
			  if (request.getNodeType() == Node.ELEMENT_NODE){
				  
				  Element elemento = (Element) request;
				  
					  for (int k=0; k < elemento.getElementsByTagName(ATTRIBUTE_MAPPING).getLength(); k++) {
						  
						  Node lista = elemento.getElementsByTagName(ATTRIBUTE_MAPPING).item(k);
						  NamedNodeMap listaAtributosMapping = lista.getAttributes();
						  
						  String name = null;
						  String qc = null;
						  String dim = null;
						  Boolean memo = Boolean.FALSE;
						  Boolean copiarAQC = Boolean.TRUE;
						  Boolean copiarADimensions = Boolean.TRUE;
						  
						  for (int j = 0; j < listaAtributosMapping.getLength(); j++){
							  						 						  
							  Node uno = lista.getAttributes().item(j);
							  if (ATTRIBUTE_NAME.equals( uno.getNodeName())){
								   name= uno.getNodeValue();
							  }							
							  else if (ATTRIBUTE_QC.equals(uno.getNodeName())){
								  qc = uno.getNodeValue();
							  }
							  else if (ATTRIBUTE_DIMENSIONS.equals(uno.getNodeName())){
								  dim = uno.getNodeValue();
							  }
							  else if (ATTRIBUTE_MEMO.equals(uno.getNodeName())){
								  if (uno.getNodeName() != null && ("false".equals(uno.getNodeValue().toLowerCase()) ||
										  "true".equals(uno.getNodeValue().toLowerCase()))){
									  memo = new Boolean(uno.getNodeValue());
								  }								  
							  }
							  else if (ATTRIBUTE_COPIARAQC.equals(uno.getNodeName())){
								  if (uno.getNodeName() != null && ("false".equals(uno.getNodeValue().toLowerCase()) ||
										  "true".equals(uno.getNodeValue().toLowerCase()))){
									  copiarAQC = new Boolean(uno.getNodeValue());
								  }								  
							  }
							  
							  else if (ATTRIBUTE_COPIARADIMENSIONS.equals(uno.getNodeName())){
								  if (uno.getNodeName() != null && ("false".equals(uno.getNodeValue().toLowerCase()) ||
										  "true".equals(uno.getNodeValue().toLowerCase()))){
									  copiarADimensions = new Boolean(uno.getNodeValue());
								  }								  
							  }							  
						  }
						  Pair<String,String> par = new Pair<String,String>(qc,dim);
						  //Agrego los atributos a la instancia
						  atributos.put(name,par);
						  listaDimensions.add(dim);
						  listaQc.add(qc);
						  atributosMemo.put(name, memo);
						  atributosCopiarAQC.put(name, copiarAQC);
						  atributosCopiarADimensions.put(name, copiarADimensions);
					  }
			  }
		  }
	}
	
	/**
	 * Metodo que obtiene un atributo especifico
	 * 
	 * @param reqType
	 * 				tipo de atributo this.DIM o this.QC
	 * @param name
	 *            nombre del atributo
	 * @return valor del atributo
	 * @throws MapeoException
	 */
	public String getAttributeByName(String reqType, String name) throws PropertiesException {
		Pair<String,String> par= atributos.get(name); //Me devuelve el par de values para un atributo en particular
		String attributeValue = null;
		if(reqType.equals(DIM)){
			attributeValue = par.getDimAttribute();
		}
		else if(reqType.equals(QC)){
			attributeValue = par.getQCAttribute();
		}
		if (name == null || attributeValue == null || ("").equals(attributeValue)) {
			logger.debug("No se encontro el atributo: " + name);
			throw new MapeoException("No se encontro el atributo: " + name);
		}
		return attributeValue;
	}
	
	
	/**
	 * Metodo que obtiene el par de un atributo especifico
	 * 
	 * @param name
	 *            nombre del atributo
	 * @return valor del atributo
	 * @throws MapeoException
	 */
	
	public Map<String,Pair<String,String>>  getAttributes(){
		return atributos;
	}
	
	private void cargarAtributosGenerales() {
		
		if (atributos != null){
			atributosGenerales.putAll(atributos);
			// Se remueven los que no son necesarios sincronizar o se sincronizan obligatoriamente
			atributosGenerales.remove("idDim");
			atributosGenerales.remove("idQc");
			atributosGenerales.remove("tipo");
			atributosGenerales.remove("estado");
			atributosGenerales.remove("descripcionGeneral");
			atributosGenerales.remove("descripcion");
			atributosGenerales.remove("fechaSincronizacion");
			atributosGenerales.remove("fechaAuditoria");
			atributosGenerales.remove("comentarios");
			atributosGenerales.remove("detectadoEnCiclo");
			atributosGenerales.remove("detectadoEnRelease");
		}
	}
	
	public Map<String,Pair<String,String>> getAtributosGenerales(){				
		return this.atributosGenerales;		
	}
	
	/**
	 * Metodo para saber si un campo es de tipo memo
	 * 
	 * @param name
	 *            nombre del atributo
	 * @return si es memo o no
	 */
	public boolean isMemo(String name){
		
		Boolean esMemo = this.atributosMemo.get(name);
		if (esMemo == null){
			return false;
		}
		
		return esMemo.booleanValue();
	}
	/**
	 * Metodo para saber si hay que copiar un campo a QC
	 * 
	 * @param name
	 *            nombre del atributo
	 * @return si hay que copiar o no
	 */
	public boolean hayQueCopiarAQC(String name){
		
		Boolean copiarAQC = this.atributosCopiarAQC.get(name);
		if (copiarAQC == null){
			return false;
		}
		
		return copiarAQC.booleanValue();
	}
	/**
	 * Metodo para saber si hay que copiar un campo a DIMENSIONS
	 * 
	 * @param name
	 *            nombre del atributo
	 * @return si hay que copiar o no
	 */
	public boolean hayQueCopiarADimensions(String name){
		
		Boolean copiarADimensions = this.atributosCopiarADimensions.get(name);
		if (copiarADimensions == null){
			return false;
		}
		
		return copiarADimensions.booleanValue();
	}
}
