package ar.com.tssa.serena.connectors.dimensions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ar.com.tssa.serena.connectors.exceptions.MapeoException;
import ar.com.tssa.serena.connectors.utils.ConnectorProperties;

import com.serena.dmclient.api.DimensionsConnection;
import com.serena.dmclient.api.DimensionsRuntimeException;
import com.serena.dmclient.api.Part;
import com.serena.dmclient.collections.AttributeDefinitions;
import com.serena.dmclient.collections.Products;
import com.serena.dmclient.collections.Types;
import com.serena.dmclient.objects.AttributeDefinition;
import com.serena.dmclient.objects.AttributeType;
import com.serena.dmclient.objects.Product;
import com.serena.dmclient.objects.Type;
import com.serena.dmclient.objects.TypeScope;

public class DimensionsProducts {

	private Logger logger = Logger.getRootLogger();	
	private static DimensionsProducts instance = null;
	private Map<String,List<String>> productosDimQc = new HashMap<String, List<String>>();
	private Map<String,String> productosQcDim = new HashMap<String, String>();
	private Map<String,String> productosAsignacion = new HashMap<String, String>();
	private Map<String, List<String>> proyectoQcRelproyectosEnDim = new HashMap<String, List<String>>();
	private Map<String, String> productoDimProyectoQc = new HashMap<String, String>();
	public static String DIM = "DIM";
	public static String QC= "QC";
	public static String PROYECTO_QC_RELACIONADO = "PROYECTO_QC_RELACIONADO";
	public static String QC_USE = "QC_USE";
	public static String QC_PRODUCT = "QC_PRODUCT";	
	
	private DimensionsProducts (){
		this.cargaProductos();
	}
	
	/**
	 * Obtiene la instancia de {@linkplain DimensionsProducts}
	 * 
	 * @return una instancia de {@linkplain DimensionsProducts}
	 */
	public static DimensionsProducts getInstance() {
		if (instance == null) {
			synchronized(DimensionsProducts.class){
				if (instance == null) {
					instance = new DimensionsProducts();
				}
			}
		}
		return instance;
	}
	
	@SuppressWarnings("unchecked")
	private void cargaProductos() {
		
		DimensionsConnection connection = null;
		
		try{
			
			connection = DimensionsConnectionFactory.getConnection();			
						
			// Se conecta a la "base de dimensions" y busca los productos (coleccion)
			Products products = connection.getObjectFactory().getBaseDatabaseAdmin().getProducts();
			
			Iterator<String> productIterator = products.iterator();
			
			String atributoDeAsignacionAutomatica = ConnectorProperties.getInstance().getAtributoDeAsignacionAutomatica();	
			
			while(productIterator.hasNext()){
				String productId= (String)productIterator.next();
				Product product =products.get(productId);
				
				Types types = product.getTypes(TypeScope.DESIGN_PART); // Esto me da el tipo q es cada elemento de la coleccion
			    
			    // get a  type
				Type type = null;
				Iterator<String> it = types.iterator();
				while (it.hasNext()){
					
					String typeName = (String)it.next();
					type = types.get(typeName);
					
					// Tomo todos los atributos que tiene un elemento de la lista segun el tipo que es
					// Cargo los atributos
					AttributeDefinitions attribs = type.getAttributeDefinitions(AttributeType.SFSV);
					AttributeDefinitions attribsMultiple = type.getAttributeDefinitions(AttributeType.SFMV);
					// Selecciono cada atributo que voy a usar
					AttributeDefinition qcUseAttrib = attribs.get(QC_USE);
					AttributeDefinition qcProductAttrib = attribsMultiple.get(QC_PRODUCT);
					AttributeDefinition qcProyQcRelAttrib = attribs.get(PROYECTO_QC_RELACIONADO);
					AttributeDefinition asignacionAutomatica = null;
					
					try{
						asignacionAutomatica = attribs.get(atributoDeAsignacionAutomatica);
					}
					catch (DimensionsRuntimeException e){
						logger.error("El producto " + productId + " no tiene definido el atributo '" + atributoDeAsignacionAutomatica + "'");
					}
					
					//QC_product: Crecer(id 45), Banca(id 30)   50
					if (qcUseAttrib!=null && qcProductAttrib!=null)
					{
						// Aca genero un elemento DimQCItem con el Producto q tome de Dimensions
						Part part = product.getRootPart();
						
						if (asignacionAutomatica != null){
						part.queryAttribute( new int[] { 
								qcUseAttrib.getNumber(), 
								qcProductAttrib.getNumber(),
								qcProyQcRelAttrib.getNumber(),
								asignacionAutomatica.getNumber()
								} );
						}
						else{
							part.queryAttribute( new int[] { 
									qcUseAttrib.getNumber(), 
									qcProductAttrib.getNumber(),
									qcProyQcRelAttrib.getNumber()
									} );							
						}
							
						if ("YES".equalsIgnoreCase((String) part.getAttribute(qcUseAttrib.getNumber()))){
							//Mapeo el id del producto de Dimensions con la lista de proyectos que puede tener los defecto en QC en el campo "Proyecto"
							productosDimQc.put(productId, (List<String>)part.getAttribute(qcProductAttrib.getNumber()));
							for(String item : (List<String>)part.getAttribute(qcProductAttrib.getNumber()) ){
								productosQcDim.put(item, productId);
							}
							
							if (asignacionAutomatica != null){
								// Guardo si el producto asigna tareas automaticamente
								productosAsignacion.put(productId, (String)part.getAttribute(asignacionAutomatica.getNumber()));								
							}
							else{
								productosAsignacion.put(productId, "");
							}

							
							String proyectoQcRel = (String)part.getAttribute(qcProyQcRelAttrib.getNumber());
							List<String> proyectosEnDim = (List<String>)part.getAttribute(qcProductAttrib.getNumber());
							List<String> proyectosEnDimAcum = proyectoQcRelproyectosEnDim.get(proyectoQcRel);
							
							if (proyectosEnDimAcum == null){
								proyectoQcRelproyectosEnDim.put(proyectoQcRel, proyectosEnDim);
							} else {
								//Ya tiene datos cargados. Se acumulan los nuevos.
								proyectosEnDimAcum.addAll(proyectosEnDim);
								proyectoQcRelproyectosEnDim.put(proyectoQcRel, proyectosEnDimAcum);
							}
							
							productoDimProyectoQc.put(product.getName(), proyectoQcRel);							
						}
					}
				}
			}
			
		} catch (IOException e) {
			logger.error("Ocurrio un error al intentar conectarse con Serena: " + e.getMessage());
		}catch (Exception e) {
			logger.error("Error durante la cargar de los productos de Dimensions - proyectos de Qc: " + e.getMessage());
			
		} finally {
			if (connection != null){
				connection.close();
			}
		}
		
	}
	
	public List<String> getProductosDimQc (){
		List<String> listaItems = new ArrayList<String>();
		for(List<String> listaIdProduct : productosDimQc.values()){
			listaItems.addAll(listaIdProduct);
		}
		return listaItems;
	}
	
	public List<String> getQCProducts (String nombreDeProducto){
		return productosDimQc.get(nombreDeProducto);
	}
	
	public String getAsignacionAutomatica (String nombreDeProducto){
		return productosAsignacion.get(nombreDeProducto);
	}
	
	public Map<String,String> getProductosQcDim (){
		return productosQcDim;
	}
	
	/**
	 * Metodo que obtiene un atributo especifico
	 * 
	 * @param type
	 * 				tipo de atributo this.DIM
	 * @param name
	 *            nombre del producto
	 * @return valor del atributo
	 * @throws Exception 
	 * @throws MapeoException
	 */
	public String getProductByName (String type, String name) throws Exception{
		
		String valor = null;
		
		if (DIM.equals(type) && productosQcDim != null){
			name = new String (name.getBytes(ConnectorProperties.getInstance().getEncodingDim()), ConnectorProperties.getInstance().getEncodingQc());
			valor = productosQcDim.get(name);							
		}
		
		if (valor == null){
			logger.error("No se encontro el producto/proyecto:" + name);			
		}
		return valor;
	}
	
	/**
	 * Metodo que obtiene por proyecto de Qc, los proyectos en Dimensions parametrizados
	 * 
	 * @param proyectoQcRelacionado
	 * 				Proyecto de Qc con el cual se relaciona los datos de Dimensions
	 * @return 	
	 */
	public List<String> getProyectoEnDimPorProyQc(String proyectoQcRelacionado){
		return proyectoQcRelproyectosEnDim.get(proyectoQcRelacionado);
	}
	
	/**
	 * Metodo que obtiene por producto de Dimensions, el proyecto que tiene configurado de Qc con el cual se relaciona.
	 * 
	 * @param productoDim
	 * 				Producto de Dimensions
	 * @return el proyecto de Qc relacionado 	
	 */
	public String getProyectoQcRelacionado(String productoDim){
		return productoDimProyectoQc.get(productoDim);
	}	
}
