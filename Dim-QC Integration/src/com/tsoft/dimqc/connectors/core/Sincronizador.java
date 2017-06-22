package ar.com.tssa.serena.connectors.core;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ar.com.tssa.serena.connectors.alm.Entities;
import ar.com.tssa.serena.connectors.alm.Entity;
import ar.com.tssa.serena.connectors.alm.Entity.Fields.Field;
import ar.com.tssa.serena.connectors.alm.QcTarea;
import ar.com.tssa.serena.connectors.dimensions.DimensionsProducts;
import ar.com.tssa.serena.connectors.dimensions.DimensionsTarea;
import ar.com.tssa.serena.connectors.utils.ConnectorProperties;
import ar.com.tssa.serena.connectors.utils.mapping.Mapeo;

import com.serena.dmclient.api.DimensionsRelatedObject;
import com.serena.dmclient.api.Part;
import com.serena.dmclient.api.Request;
import com.serena.dmclient.api.RequestAttachment;
import com.serena.dmclient.api.SystemAttributes;

public class Sincronizador {
 
	public Sincronizador (DimensionsTarea dimTarea, QcTarea qcTarea, List<Long> idsQc, String nombreEsquemaBaseQc){
		sincronizar(dimTarea, qcTarea, idsQc, nombreEsquemaBaseQc);
	}
	
	private synchronized void sincronizar(DimensionsTarea dimTarea, QcTarea qcTarea, List<Long> idsQc, String nombreEsquemaBaseQc){

		List<String> proyectosEnDim = null;
		Logger logger = Logger.getRootLogger();		
		//Formato utilizado para parsear las fechas de sincronizacion y auditoria de los request de QC y Dimensions
		SimpleDateFormat qcDateFormat = new SimpleDateFormat(ConnectorProperties.getInstance().getFormatoFechaQC());
		SimpleDateFormat dimDateFormat = new SimpleDateFormat(ConnectorProperties.getInstance().getFormatoFechaDim(),Locale.US);
		
		try {			
			//Listo los atributos para un defecto determinado
			qcTarea.listEntityAttributes();
						
			if(idsQc != null && !idsQc.isEmpty()){					
				for (Long idQc : idsQc) {
					try{						
						Entity requestQc = qcTarea.obtenerBug(idQc);												
						//Obtenemos campos necesarios para la sincronizacion
						String id = requestQc.getFields().getFieldByName(Mapeo.getInstance().getAttributeByName(Mapeo.QC, "idQc")).getValue().get(0).getValue();
						Field nameField  = requestQc.getFields().getFieldByName(Mapeo.getInstance().getAttributeByName(Mapeo.QC, "nombre"));
						String name = null;
						if(!"".equals(nameField.getValue().get(0).getValue())){
							name = nameField.getValue().get(0).getValue();
						}
						Field dimensionsIdField = requestQc.getFields().getFieldByName(Mapeo.getInstance().getAttributeByName(Mapeo.QC, "idDim"));
						String dimensionsId = null;
						if(!"".equals(dimensionsIdField.getValue().get(0).getValue())){
							dimensionsId =  dimensionsIdField.getValue().get(0).getValue(); 
						}
						Field syncTimeField = requestQc.getFields().getFieldByName(Mapeo.getInstance().getAttributeByName(Mapeo.QC, "fechaSincronizacion"));
						String syncTime =  "";
						if(!"".equals(syncTimeField.getValue().get(0).getValue())){
							syncTime =  syncTimeField.getValue().get(0).getValue(); 
						}
						String lastModified =  requestQc.getFields().getFieldByName(Mapeo.getInstance().getAttributeByName(Mapeo.QC, "fechaAuditoria")).getValue().get(0).getValue();
						
						logger.debug("Procesando Request: Id: " + id + ", Name: " + name);
						
						// Voy a buscarlo a Dimensions
						if(dimensionsId != null && !dimensionsId.equals("")){
							logger.debug("El request posee 'dimensionsId', se busca en Dimensions.");
							Request requestDim = dimTarea.findRequestById(dimensionsId);
							//Si el Request que traje de Dimensions es null, es decir alguna vez existio, esto no deberia pasar, loggeo el error
							if(requestDim != null){
								logger.debug("Se encontro el request en Dimensions");
								logger.debug("Request de Dimensions: " + "Nombre: " + requestDim.getName() + " OBJECT_ID: " + requestDim.getAttribute(SystemAttributes.OBJECT_ID) + " TITTLE: " + requestDim.getAttribute(SystemAttributes.TITLE));
								
								@SuppressWarnings({"unchecked" })
								List <DimensionsRelatedObject>listaDePartes = requestDim.getParentParts(null);
								Part parte = (Part) listaDePartes.get(0).getObject();
								
								String nombreDeParte = parte.getName();
								
								Pattern regex = Pattern.compile("^([^:]+)");
								Matcher regexMatcher = regex.matcher(nombreDeParte);
								if (regexMatcher.find()) {
									
									String nombreDeProducto = regexMatcher.group(1);
									logger.debug("El Request '" + requestDim.getAttribute(SystemAttributes.OBJECT_ID) + "' pertenece al producto '" + nombreDeProducto + "'");
									
									proyectosEnDim = DimensionsProducts.getInstance().getQCProducts(nombreDeProducto);

								}								
								//Si el request de Dimensions no es null, pregunto si es el del tipo BUG 
								String attributeDimType = Mapeo.getInstance().getAttributeByName(Mapeo.DIM, "tipo");
								if("BUG".equals(requestDim.getAttribute(dimTarea.getAttributeNumber(attributeDimType)))){
									logger.debug("El request encontrado es del tipo BUG");

									//Comparo fechas de sync y de auditoria de QC
									Date fechaSincQc = null;
									Date fechaAudQc = new Date();
									String syncTimeValueQc = syncTime;
									try{
										fechaSincQc = qcDateFormat.parse(syncTimeValueQc);//Obtengo la fecha de sincronizacion y la convierto a date para compararla con la de auditoria
										logger.debug("El request de QC tiene fecha de sincronizacion: " + fechaSincQc);
										//Obtengo la fecha de auditoria
										String fechaAudQcAux = lastModified;
										try{
											fechaAudQc = qcDateFormat.parse(fechaAudQcAux); //Obtengo la fecha de auditoria  y la convierto a date para compararla con la de sincronizacion
											logger.debug("El request de QC tiene fecha de auditoria: " + fechaAudQc);
											boolean comparacionFechasQc = compareDates(fechaSincQc, fechaAudQc, logger);
											if (comparacionFechasQc){
												//Valido que la ultima modificacion la haya realizado un usuario distinto al de la sincronizacion
												/*
												 * Se agrega el dato sobre la base de Qc porque Qc no esta informando bien la auditoria para todos los bugs. Algunos tiran error
												 * al querer consultarse la misma; por lo tanto, en caso de error, se va a buscar el dato a la base.  
												 */
												boolean esUsuario = qcTarea.esCambioSincronizador(id, nombreEsquemaBaseQc);
												
												if (esUsuario){
													logger.debug("El request de QC NO fue modificado despues de la ultima vez que fue sincronizado.");
													comparacionFechasQc = false;
												} else {
													logger.debug("El request de QC fue modificado despues de la ultima vez que fue sincronizado.");
													comparacionFechasQc = true;
												}
											}
											//Comparo las fechas de Dimensions
											
											Date fechaSincDim = null;
											Date fechaAudDim = new Date();
											String dimSyncTimeAttribute = Mapeo.getInstance().getAttributeByName(Mapeo.DIM, "fechaSincronizacion");
											requestDim.queryAttribute(dimTarea.getAttributeNumber(dimSyncTimeAttribute));
											String fechaSincDimAux = (String) requestDim.getAttribute(dimTarea.getAttributeNumber(dimSyncTimeAttribute));
											try{
												fechaSincDim = dimDateFormat.parse(fechaSincDimAux);
												logger.debug("El request de Dimensions tiene fecha de sincronizacion: "+ fechaSincDim);
												//Tiene fecha de sincronizacion, obtengo la de auditoria
												String dimAudTimeAttribute = Mapeo.getInstance().getAttributeByName(Mapeo.DIM, "fechaAuditoria");
												requestDim.queryAttribute(dimTarea.getAttributeNumber(dimAudTimeAttribute));
												String fechaAudDimAux = (String) requestDim.getAttribute(dimTarea.getAttributeNumber(dimAudTimeAttribute));
												try{
													fechaAudDim = dimDateFormat.parse(fechaAudDimAux);
													logger.debug("El request de Dimensions tiene fecha de auditoria: "+fechaAudDim);
													boolean comparacionFechasDim = compareDates(fechaSincDim,fechaAudDim, logger); // Si la diferencia es de mas de X minutos, sincronizo el request, caso contrario considero que el request esta sincronizado
													if(comparacionFechasQc && comparacionFechasDim){ //Si ambas fechas son mayores a X minutos, paso de QC a Dimensions
														logger.debug("La diferencia de fechas en ambos requests es mayor a " + ConnectorProperties.getInstance().getDifTime() + " minutos. Ambos cambiaron luego de la ultima sincronizacion. Se copia segun paramtro prev");
														copiarSegunPrioridad(requestQc, requestDim, logger, qcTarea, dimTarea, nombreEsquemaBaseQc, proyectosEnDim);
													}
													else if(comparacionFechasQc){
														// Si solo la diferencia de QC es mayor o igual a X minutos, copio de QC a Dimensions
														logger.debug("Se modifico el request de QC.");
														copiarQcADimensions(requestQc, requestDim, logger, qcTarea, dimTarea, nombreEsquemaBaseQc, proyectosEnDim);
													}else if(comparacionFechasDim){
														// Si solo la diferencia de Dim es mayor o igual a X minutos, copio de Dimensions a QC
														logger.debug("Se modifico el request de Dimensions.");
														copiarDimensionsAQc(requestQc, requestDim, logger, qcTarea, dimTarea);
													}else{
														//Ningun request tiene una diferencia mayor o igual a X minutos. No cambiaron
														logger.debug("Los requests se encuentran sincronizados.");
													}

												}catch (ParseException e) {
													//La fecha de auditoria del request de Dimensions no existe, se debe copiar segun prioridad
													logger.debug("El request de Dimensions no tiene fecha de auditoria.");
													copiarSegunPrioridad(requestQc, requestDim, logger, qcTarea, dimTarea, nombreEsquemaBaseQc, proyectosEnDim);
												}
											}
											catch (ParseException e) {
												//La fecha de sincronizacion del request de Dimensions no existe, se debe copiar segun prioridad
												logger.debug("El request de Dimensions no tiene fecha de sincronizacion.");
												copiarSegunPrioridad(requestQc, requestDim, logger, qcTarea, dimTarea, nombreEsquemaBaseQc, proyectosEnDim);
											}
										}
										catch (ParseException e) {
											//La fecha de sincronizacion del request de QC no existe, se debe copiar segun prioridad
											logger.debug("El request de QC no tiene fecha de auditoria.");
											logger.error("El request de QC no tiene fecha de auditoria pero se encuentra en Dimensions.");
											copiarSegunPrioridad(requestQc, requestDim, logger, qcTarea, dimTarea, nombreEsquemaBaseQc,proyectosEnDim);
										}
									}
									catch (ParseException e) {
										//Significa que es la primera vez que se va a sincronizar el elemento, se copia segun prioridad
										logger.debug("El request de QC no tiene fecha de sincronizacion.");
										logger.error("El request de QC no tiene fecha de sincronizacion pero se encuentra en Dimensions.");
										copiarSegunPrioridad(requestQc, requestDim, logger, qcTarea, dimTarea, nombreEsquemaBaseQc, proyectosEnDim);
									}
								}
								else{// Si el dimensionsID que tiene QC es de un request de un tipo que no es BUG, esto no deberia pasar, loggeo el error
									logger.error("El request de dimensions con ID '" + dimensionsId +"' no es del tipo 'BUG', no se realizara ninguna accion.");
								}
							}
						}
						else{
							
							Field attributeFieldEstado = requestQc.getFields().getFieldByName(Mapeo.getInstance().getAttributeByName(Mapeo.QC, "estado"));
							boolean replica = true;
							if (attributeFieldEstado != null && !"".equals(attributeFieldEstado.getValue().get(0).getValue())){
								if (ConnectorProperties.getInstance().getEstadoCerradoQc().equals(attributeFieldEstado.getValue().get(0).getValue())){
									replica = false;
								}
							}
							
							if (!replica){
								logger.debug("El request de QC no tiene un request en Dimensions asociado y en estado " 
												+ attributeFieldEstado.getValue().get(0).getValue() + ", entonces no se replicara.");
							} else {
								logger.debug("El request de QC no tiene un request en Dimensions asociado, el mismo se creara.");
								ar.com.tssa.serena.connectors.alm.Entity.Fields.Field attributeField = requestQc.getFields().getFieldByName(ConnectorProperties.getInstance().getCampoProjectQc());
								String proyectQc = new String();
								if(!"".equals(attributeField.getValue().get(0).getValue())){
									proyectQc =  attributeField.getValue().get(0).getValue();
								}
								
								String productoDim = null;
								if(!"".equals(proyectQc)){
									 productoDim = DimensionsProducts.getInstance().getProductByName(DimensionsProducts.DIM, proyectQc);
								}
								
								if (productoDim != null){
									/*Solo creo el request en Dimensions si la entidad (request) en QC esta desbloqueada
									es decir ningun usuario la esta utilizando, caso contrario el request se replica en la proxima iteracion*/
									boolean locked = qcTarea.verifyObjectStatus(requestQc);
									if(!locked){
										String requestId = dimTarea.crearRequestEnDim(requestQc, productoDim, qcTarea, nombreEsquemaBaseQc);
										
										proyectosEnDim = DimensionsProducts.getInstance().getQCProducts(productoDim);
										
										String asignacionAutomatica = DimensionsProducts.getInstance().getAsignacionAutomatica(productoDim);
										
										if (requestId != null){
											//Setteo el Id de Dimensions en el request de QC
											String attributeIdDim = Mapeo.getInstance().getAttributeByName(Mapeo.QC, "idDim");
											
											Field idDimField  = requestQc.getFields().getFieldByName(attributeIdDim);
											if(!"".equals(idDimField.getValue().get(0).getValue())){
												requestQc.getFields().getFieldByName(attributeIdDim).getValue().get(0).setValue("");
											}
											requestQc.getFields().getFieldByName(attributeIdDim).getValue().get(0).setValue(requestId);
											logger.debug("Request " + requestId + " creado");
											qcTarea.actualiza(requestQc);
											// Con esto se espera que no se creen Request de mas y a su vez se hace porque solo puedo
											// acceder a la configuracion de los atributos de Dimensions mediante un Request
											boolean completoCarga = dimTarea.completarCreacionRequestEnDim(idDimField.getValue().get(0).getValue(), requestQc, productoDim, qcTarea, nombreEsquemaBaseQc, proyectosEnDim);
											//Se hace pasaje de Estado de New a Open
											qcTarea.pasajeDeEstado(requestQc);
											
											if ("SI".equalsIgnoreCase(asignacionAutomatica)){
												// Llamado a la asignacion automatica
												dimTarea.asignarRequestEnDim(idDimField.getValue().get(0).getValue(), requestQc, productoDim, qcTarea, nombreEsquemaBaseQc, proyectosEnDim);
											}
											if (requestId != null){
												logger.debug("Se verifica si se deben sincronizar archivos");
												//Obtengo el archivo recien creado
												Request requestDim = dimTarea.findRequestById(requestId);
												//Verifico si se deben copiar archivos adjuntos
												filesSynchronizationQCToDim(requestQc, requestDim, logger, qcTarea, dimTarea);
												
												if (completoCarga){
													qcTarea.updateFechaSincronizacionQc(requestQc);
												}
											}	
										}	
									}
									else{
										logger.warn("La entidad/objeto se encuentra bloqueado, se sincronizara cuando no lo este.");
									}
								}
							}			
						}
						
					} catch (Exception e) {
						logger.error("Ocurrio un error mientras se actualizaba un bug: " +idQc);
					}
					
				} // FIN FOR
				eliminarCarpetaTemporal(logger);
				
			}									
		}catch (Exception e) {
			logger.error("Ocurrio un error durante la sincronizacion: " );
			logger.error(e);

		}		
	}

	/** Metodo que verifica si se deben o no sincronizar los archivos de QC a Dimensions y los sincroniza
	 * @param requestQc
	 * @param requestId
	 */
	private void filesSynchronizationQCToDim(Entity requestQc, Request requestDim, Logger logger, QcTarea qcTarea, DimensionsTarea dimTarea) {
		Logger filesLogger = Logger.getLogger("filesLogger");
		
		try {		
			ar.com.tssa.serena.connectors.alm.Entity.Fields.Field attributeField = requestQc.getFields().getFieldByName("attachment");
			String attachment = new String();
			if(!attributeField.getValue().isEmpty() && attributeField.getValue().get(0) !=null && !"".equals(attributeField.getValue().get(0).getValue())){
				attachment =  attributeField.getValue().get(0).getValue();// ESTO ES PARA FILTRAR SI LA ENTIDAD TIENE ARCHIVOS O NO
			}
			
			if("Y".equals(attachment)){
				logger.debug("Tiene archivos: " + attachment);
				
				String idEntity = requestQc.getFields().getFieldByName(Mapeo.getInstance().getAttributeByName(Mapeo.QC, "idQc")).getValue().get(0).getValue();
				
				Entities entitiesAttachment = qcTarea.archivos(idEntity);
				
				//Obtengo los attachments que ya tiene el request y verifico si ya existen
				requestDim.queryRequestAttachments();
				@SuppressWarnings("unchecked")
				List<RequestAttachment> attachments = requestDim.getRequestAttachments();
				
				for (Entity entityAttachment : entitiesAttachment.getEntities()) {							
					String nombre = entityAttachment.getFields().getFieldByName("name").getValue().get(0).getValue();											
					String nombreBajada = idEntity +"_" + nombre;
					//Recorro la lista para verificar si ya existe ese archivo
					boolean exists = false;
					for(RequestAttachment attachmentObject : attachments ){
						if((nombreBajada.equalsIgnoreCase(attachmentObject.getName())) || (nombre.equalsIgnoreCase(attachmentObject.getName()))) {
							exists = true;
							break;
						}
					}
					if(!exists){
						String path = qcTarea.descargarArchivo(idEntity,  nombre);	
						dimTarea.cargarArchivos(requestDim, path, nombreBajada);
						filesLogger.info("DimCM: '" + nombreBajada + "' <-- QC: '" + nombre + "'");
					}else{
						logger.debug("El archivo '" + nombre + "' ya se encuentra en el request de Dimensions.");
					}
				}
				dimTarea.updateFechaSincronizacionDim(requestDim);
			}

		} catch (Exception e) {
			logger.error("Ocurrio un error al sincronizar los archivos");
			logger.error(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void filesSynchronizationDimToQC(Entity requestQc, Request requestDim, Logger logger, QcTarea qcTarea, DimensionsTarea dimTarea) {
		Logger filesLogger = Logger.getLogger("filesLogger");

		try {
		  requestDim.queryRequestAttachments();
		  List<RequestAttachment> dimAttachments = requestDim.getRequestAttachments();
		  if ((dimAttachments != null) && (!dimAttachments.isEmpty())) {
			Field dimensionsIdField = requestQc.getFields().getFieldByName(Mapeo.getInstance().getAttributeByName(Mapeo.QC, "idDim"));
			String dimensionsId = null;
			if(!"".equals(dimensionsIdField.getValue().get(0).getValue())) {
				dimensionsId =  dimensionsIdField.getValue().get(0).getValue(); 
			}
			
			logger.debug("El request de Dimensions " + dimensionsId + " tiene archivos. Chequeando si se tienen que sincronizar");
			
			String idEntity = requestQc.getFields().getFieldByName(Mapeo.getInstance().getAttributeByName(Mapeo.QC, "idQc")).getValue().get(0).getValue();
			Entities qcEntitiesAttachment = qcTarea.archivos(idEntity);

			for (RequestAttachment dimAttachment : dimAttachments ) {
				String dimFilename = dimAttachment.getName();
				boolean exists = false;
				for(Entity entityAttachment : qcEntitiesAttachment.getEntities()){
					String qcAttachmentName = entityAttachment.getFields().getFieldByName("name").getValue().get(0).getValue();
					String dimAttachmentName = dimAttachment.getName();
					
					String qcIdPrefix = idEntity + "_";
					if (dimAttachmentName.startsWith(qcIdPrefix)) {
						dimAttachmentName = dimAttachmentName.substring((qcIdPrefix.length()));
					}
					
					logger.debug("Chequeando si el archivo '" + dimAttachmentName + "' de Dimensions ya esta en QC");
					if(qcAttachmentName.equalsIgnoreCase(dimAttachmentName)){
						exists = true;
						break;
					}
				}				
				if(!exists){
					logger.debug("Intentando sincronizar archivo '" + dimFilename + "' desde Dim(" + dimensionsId + ") a QC(" + idEntity + ")");
					String pathToFile = dimTarea.descargarArchivo(dimAttachment);
					logger.debug("Archivo bajado a carpeta temporal: " + pathToFile);
					
					try { Thread.sleep(10000); } catch (Exception e) {}
					logger.debug("Saliendo del sleep");
					
					qcTarea.subirArchivo(idEntity, dimFilename); 
					logger.debug("Archivo subido a QC");
					filesLogger.info("DimCM: '" + dimFilename + "' --> QC: '" + dimFilename + "'");
				} else {
					logger.debug("El archivo '" + dimFilename + "' ya se encuentra en el request de QC.");
				}
			}			
		  }
	    } catch (Exception e) {
		  logger.error("Ocurrio un error al sincronizar los archivos");
		  logger.error(e);
	    }
	}
	
	/** Metodo que copia un request a otro segun la prioridad definida en el archivo properties
	 * @param requestQc
	 * @param requestDim
	 * @throws Exception
	 */
	private void copiarSegunPrioridad(Entity requestQc, Request requestDim, Logger logger, QcTarea qcTarea, DimensionsTarea dimTarea, String nombreEsquemaBaseQc, List<String> proyectosEnDim)
			throws Exception {
		if("QC".equals(ConnectorProperties.getInstance().getPrioridad())){
			//Prevalece QC
			copiarQcADimensions(requestQc, requestDim, logger, qcTarea, dimTarea, nombreEsquemaBaseQc, proyectosEnDim);
		}
		else{ //Prevalece Dimensions
			copiarDimensionsAQc(requestQc, requestDim, logger, qcTarea, dimTarea);
		}
	}

	/** Metodo que copia el request de QC a Dimensions
	 * @param requestQc
	 * @param requestDim
	 * @throws Exception
	 */
	private void copiarQcADimensions(Entity requestQc, Request requestDim, Logger logger, QcTarea qcTarea, DimensionsTarea dimTarea, String nombreEsquemaBaseQc, List<String> proyectosEnDim) {
		try{
			logger.debug("Copio el request de QC a Dimensions");
			if(!qcTarea.verifyObjectStatus(requestQc)){
				dimTarea.updateDimensionsRequest(requestDim, requestQc, qcTarea, nombreEsquemaBaseQc, proyectosEnDim);
				filesSynchronizationQCToDim(requestQc, requestDim, logger, qcTarea, dimTarea);	
				qcTarea.updateFechaSincronizacionQc(requestQc); //Seteo fecha de sincronizacion
			}
			else{
				logger.warn("La entidad/objeto se encuentra bloqueado, se sincronizara cuando no lo este.");
			}

		}catch (Exception e) {
			logger.error("Ocurrio un error al copiar el request de QC a Dimensions:");
			logger.error(e);
		}
	}

	/** Metodo que copia el request de Dimensions a QC
	 * @param requestQc
	 * @param requestDim
	 * @throws Exception
	 */
	private void copiarDimensionsAQc(Entity requestQc, Request requestDim, Logger logger, QcTarea qcTarea, DimensionsTarea dimTarea){
		logger.debug("Copio el request de Dimensions a QC");
		try{
			if(!qcTarea.verifyObjectStatus(requestQc)){
				qcTarea.updateQcRequest(requestQc,requestDim, dimTarea);//Actualizo la entidad
				filesSynchronizationDimToQC(requestQc, requestDim, logger, qcTarea, dimTarea);
				dimTarea.updateFechaSincronizacionDim(requestDim); //Seteo fecha de sincronizacion
			}else{
				logger.warn("La entidad/objeto se encuentra bloqueado, se sincronizara cuando no lo este.");
			}
		}catch (Exception e) {
			logger.error("Ocurrio un error al copiar el request de dimensions a QC: ");
			logger.error(e);
		}
	}

	/** Compara dos fechas y me dice si su diferencia es mayor a la establecida en las propiedades
	 * @param fechaSinc
	 * @param fechaAud
	 * @return boolean indicando si son diferentes o no
	 */
	private boolean compareDates(Date fechaSinc, Date fechaAud, Logger logger) {
		long diff = fechaAud.getTime() - fechaSinc.getTime();
		diff = Math.abs(diff);
		long diffMinutes = diff / (TimeUnit.MINUTES.toSeconds(1) * TimeUnit.SECONDS.toMillis(1));
		logger.debug("La diferencia en minutos es de: "+ diffMinutes);
		// Si la diferencia es de X o mas minutos sincronizo el request, caso contrario considero que el request esta sincronizado
		return diffMinutes>=ConnectorProperties.getInstance().getDifTime();
	}
	
	/**
	 * Metodo que elimina la carpeta temporan utilizada para la descarga de archivos durante la sincronzacion de los mismos
	 */
	private void eliminarCarpetaTemporal(Logger logger){
		String path = ConnectorProperties.getInstance().getRutaArchivosTemporales() + "/";
		File folder = new File(path);		
		
		if (folder.exists()){
			logger.debug("Eliminando carpeta temporal de descarga de archivos.");
			borrarDirectorio(folder);
		}				
	}

	private void borrarDirectorio(File folder) {
		File[] ficheros = folder.listFiles();
		
		if (ficheros != null){
			for (int x=0;x<ficheros.length;x++){
				if (ficheros[x].isDirectory()) {
					borrarDirectorio(ficheros[x]);
				}
				ficheros[x].delete();
			}
		}		
	}
}
