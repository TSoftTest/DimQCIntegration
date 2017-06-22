package com.tsoft.dimqc.connectors.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.tsoft.dimqc.connectors.exceptions.PropertiesException;
import com.tsoft.dimqc.database.TipoConexion;

/**
 * @author nrusz
 * 
 */
public class ConnectorProperties {

	private Logger logger = Logger.getRootLogger();
	private static String propertiesPath = "C:/qcdim/connector.properties";
	private static ConnectorProperties instance = null;
	private Properties properties = new Properties();

	private ConnectorProperties() {
		/*
		 * Se verifica si existe variable de entorno para buscar el archivo de
		 * configuracion.
		 */
		boolean existe = false;
		String nuevoPath = "";
		try {
			Process p = Runtime.getRuntime().exec("cmd.exe /c echo %QCDIM_CONF%");
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

			nuevoPath = br.readLine(); // SI NO RECONOCIO LA VARIABLE O NO ESTA, LA
																 // LINEA DEVUELVE: %QCDIM_CONF%
			if (nuevoPath != null && !"%QCDIM_CONF%".equals(nuevoPath)) {
				File fichero = new File(nuevoPath);
				if (fichero.exists()) {
					existe = true;
				}
			}
		} catch (Exception e) {
			existe = false;
		}

		if (existe) {
			propertiesPath = nuevoPath;
		}

		loadProperties(propertiesPath);
	}

	/**
	 * Obtiene la instancia de {@linkplain ConnectorProperties}
	 * 
	 * @return una instancia de {@linkplain ConnectorProperties}
	 */
	public static ConnectorProperties getInstance() {
		if (instance == null) {
			synchronized (ConnectorProperties.class) {
				if (instance == null) {
					instance = new ConnectorProperties();
				}
			}
		}
		return instance;
	}

	/**
	 * Carga el archivo de propiedades
	 * 
	 * @param propertiesPath
	 * @throws ConfigurationException
	 */
	private void loadProperties(String propertiesPath) throws PropertiesException {
		try {
			InputStream fileInput = new FileInputStream(propertiesPath);
			properties.load(fileInput);
		} catch (Exception e) {
			logger.error("Error al cargar las propiedades " + e.getMessage());
			throw new PropertiesException("No se pudieron cargar las propiedades", e);
		}
	}

	/**
	 * @return el tiempo en minutos entre cada ejecucion del daemon
	 */
	public int getSleepTime() {
		int sleepTime = 0;
		try {
			sleepTime = Integer.parseInt(instance.getProperty("sleepTime"));
		} catch (NumberFormatException e) {
			throw new PropertiesException("Ocurrio un error al convertir la propiedad 'sleepTime'");
		}
		return sleepTime;
	}

	/**
	 * Metodo que lee una propiedad especifica
	 * 
	 * @param name
	 *          nombre de la propiedad
	 * @return valor de la propiedad
	 * @throws PropertiesException
	 */
	private String getProperty(String name) throws PropertiesException {

		String propertyValue = properties.getProperty(name);

		if (name == null || propertyValue == null || ("").equals(propertyValue)) {
			logger.debug("No se encontro la propiedad: " + name);
			throw new PropertiesException("No se encontro la propiedad: " + name);
		}

		return propertyValue;
	}

	// DIMENSIONS
	/**
	 * 
	 * @return el usuario de conexion de la base de datos de Dimensions
	 */
	public String getDbUserDim() {
		return instance.getProperty("userNameDim");
	}

	/**
	 * 
	 * @return la clave del usuario de conexion de la base de datos de Dimensions
	 */
	public String getDbPasswordDim() {
		return DesencriptadorPassword.obtenerPassword(instance.getProperty("rutaArchivoKeyDim"), instance.getProperty("rutaArchivoHashDim"));
	}

	/**
	 * 
	 * @return el nombre de la base de datos de Dimensions
	 */
	public String getDbNameDim() {
		return instance.getProperty("dbNameDim");
	}

	/**
	 * 
	 * @return la conexion de la base de datos de Dimensions
	 */
	public String getDbConnDim() {
		return instance.getProperty("dbConnDim");
	}

	/**
	 * 
	 * @return la conexion de la base de datos de Dimensions
	 */
	public String getDbServerDim() {
		return instance.getProperty("serverDim");
	}

	/**
	 * 
	 * @return la ruta del archivo para los campos multiples
	 */
	public String getRutaArchCamposMultiples() {
		return instance.getProperty("rutaArchivoMapeoCamposMultiples");
	}

	// Mapping
	/**
	 * 
	 * @return la ruta del archivo de mapeo
	 */
	public String getRutaArchivoMapeo() {
		return instance.getProperty("rutaArchivoMapeo");
	}

	public String getRutaArchMapeoEstados() {
		return instance.getProperty("rutaArchivoMapeoEstados");
	}

	// Logging
	/**
	 * 
	 * @return La ruta del archivo de configuracion de log4j
	 */
	public String getRutaLog4j() {
		return instance.getProperty("rutaLog4j");
	}

	// Descarga de archivos
	/**
	 * 
	 * @return La ruta del archivo para descarga de archivos
	 */
	public String getRutaArchivosTemporales() {
		return instance.getProperty("rutaArchivoTemporal");
	}

	// Sincronizador
	/**
	 * 
	 * @return La diferencia en minutos entre la fecha de auditoria y la de
	 *         sincronizacion para considerar desactualizado un request
	 */
	public int getDifTime() {
		int difTime = 0;
		try {
			difTime = Integer.parseInt(instance.getProperty("difTime"));
		} catch (NumberFormatException e) {
			throw new PropertiesException("Ocurrio un error al convertir la propiedad 'difTime'");
		}
		return difTime;
	}

	// Descarga de archivos
	/**
	 * 
	 * @return La propiedad que me indica si QC prevalece sobre Dimensions o al
	 *         reves.
	 */
	public String getPrioridad() {
		String prev = instance.getProperty("prioridad");
		if ("QC".equals(prev) || "DIM".equals(prev)) {

		} else {
			throw new PropertiesException("Ocurrio un error al convertir la propiedad 'prioridad'. Asegurese que sea 'QC' o 'DIM'");
		}
		return prev;
	}

	// Proyecto en Dimensions
	/**
	 * 
	 * @return El proyecto donde se van a crear los request en Dimensions.
	 */
	public String getProjectDim() {
		return instance.getProperty("project");
	}

	// Campo de Proyect en cada defect en Qc
	/**
	 * 
	 * @return El campo a utilizar para tomar proyecto en cada defect en QC.
	 */
	public String getCampoProjectQc() {
		return instance.getProperty("campoProjectQc");
	}

	/**
	 * Devuelve el formato de la fecha para DIMENSIONS en los campos DATE
	 * 
	 * @return
	 */
	public String getFormatoFechaDim() {
		return instance.getProperty("dimDateFormat");
	}

	/**
	 * Devuelve el formato de la fecha para QC en los campos DATE
	 * 
	 * @return
	 */
	public String getFormatoFechaQC() {
		return instance.getProperty("qcDateFormat");
	}

	/**
	 * 
	 * @return El campo a utilizar para tomar el encoding referente a Dimensions.
	 */
	public String getEncodingDim() {
		return instance.getProperty("encodingDim");
	}

	/**
	 * 
	 * @return El patron para detectar el mensaje de error del validset.
	 */
	public String getMensajeDeErrorValidSet() {
		return instance.getProperty("mensajeDeErrorValidSet");
	}

	/**
	 * 
	 * @return La query para obtener ValidSets Multiples.
	 */
	public String getQueryValidSetMultiple() {
		return instance.getProperty("queryValidSetMultiple");
	}

	/**
	 * 
	 * @return La query para obtener ValidSets Simples.
	 */
	public String getQueryValidSetSimple() {
		return instance.getProperty("queryValidSetSimple");
	}

	/**
	 * 
	 * @return El campo a utilizar para tomar el encoding referente a Qc.
	 */
	public String getEncodingQc() {
		return instance.getProperty("encodingQc");
	}

	public String getRutaAttrQC() {
		return instance.getProperty("rutaAttrQC");
	}

	public String getEstadoInicialQc() {
		return instance.getProperty("estadoInicialQc");
	}

	public String getEstadoFinalQc() {
		return instance.getProperty("estadoFinalQc");
	}

	public boolean isEstadoInicialQc(String estado) {
		if (getEstadoInicialQc().equals(estado)) {
			return true;
		}
		return false;
	}

	public String getEstadoCerradoQc() {
		return instance.getProperty("estadoCerradoQc");
	}

	public String getRutaArchivoConfiguracionQC() {
		return instance.getProperty("rutaArchivoConfiguracionQC");
	}

	/**
	 * 
	 * @return La cantidad maxima de caracteres que puede contener un campo en
	 *         Dimensions.
	 */
	public int getMaximoCaracteresDim() {
		int maximo = 0;
		try {
			maximo = Integer.parseInt(instance.getProperty("maximoCaracteresGeneral"));
		} catch (NumberFormatException e) {
			throw new PropertiesException("Ocurrio un error al obtener la propiedad 'maximoCaracteresGeneral'");
		}
		return maximo;
	}

	// Conexion a la base de Dimensions

	public String getDataBaseDimHost() {
		return instance.getProperty("dimDbOracleHost");
	}

	public int getDataBaseDimPort() {
		int port = -1;
		try {
			port = Integer.parseInt(instance.getProperty("dimDbOraclePort"));

		} catch (NumberFormatException e) {
			throw new PropertiesException("Ocurrio un error al obtener la propiedad 'dimDbOraclePort'");
		}
		return port;
	}

	public String getDataBaseDimUser() {
		return instance.getProperty("dimDbOracleUser");
	}

	public String getDataBaseDimName() {
		return instance.getProperty("dimDbOracleName");
	}

	public String getDataBaseDimPass() {
		return DesencriptadorPassword.obtenerPassword(instance.getProperty("rutaArchivoKeyBaseDim"), instance.getProperty("rutaArchivoHashBaseDim"));
	}

	public String getDataBaseDimSdi() {
		return instance.getProperty("dimDbOracleSDI");
	}

	// FIN - Conexion a la base de Dimensions

	/**
	 * Devuelve la cantidad maxima de campos comentarios (para los campos
	 * multiples).
	 * 
	 * @return
	 */
	public int getCantidadMaximaCamposMultiples() {
		int cantidadMaxima = -1;
		try {
			cantidadMaxima = Integer.parseInt(instance.getProperty("cantidadMaximaCamposMultiples"));

		} catch (NumberFormatException e) {
			throw new PropertiesException("Ocurrio un error al obtener la propiedad 'cantidadMaximaCamposMultiples'");
		}
		return cantidadMaxima;
	}

	/**
	 * Devuelve el comentario para indicar que se ha superado la maxima cantidad
	 * de campos multiples posibles.
	 * 
	 * @return
	 */
	public String getComentarioFinUltimoCampo() {
		return instance.getProperty("comentarioFinUltimoCampo");
	}

	/**
	 * 
	 * @return la ruta del archivo de mapeo de Estados Excluidos
	 */
	public String getRutaArchMapeoEstadosExcluidos() {
		return instance.getProperty("rutaArchivoMapeoEstadosExcluidos");
	}

	/**
	 * 
	 * @return el host de la base de Qc
	 */
	public String getQcDataBaseHost() {
		return instance.getProperty("qcDbHost");
	}

	/**
	 * 
	 * @return el puerto de la base de Qc
	 */
	public int getQcDataBasePort() {
		int port = -1;
		try {
			port = Integer.parseInt(instance.getProperty("qcDbPort"));

		} catch (NumberFormatException e) {
			throw new PropertiesException("Ocurrio un error al obtener la propiedad 'qcDbPort'");
		}
		return port;
	}

	/**
	 * 
	 * @return el sdi de la base de Qc
	 */
	public String getQcDataBaseSDI() {
		return instance.getProperty("qcDbSDI");
	}

	/**
	 * 
	 * @return el usuario de la base de Qc
	 */
	public String getQcDataBaseUser() {
		return instance.getProperty("qcDbUser");
	}

	/**
	 * 
	 * @return la pass de la base de Qc
	 */
	public String getQcDataBasePass() {
		// Se comenta porque en el banco todavia no esta la clave encriptada y
		// muestra un supuesto error al intentar desencriptar
		/*
		 * try{ if (instance.getProperty("rutaArchivoKeyBaseQc") != null &&
		 * instance.getProperty("rutaArchivoHashBaseQc") != null){ return
		 * DesencriptadorPassword
		 * .obtenerPassword(instance.getProperty("rutaArchivoKeyBaseQc"),
		 * instance.getProperty("rutaArchivoHashBaseQc")); } } catch (Exception e) {
		 * return instance.getProperty("qcDbPass"); }
		 * 
		 * return null;
		 */
		return instance.getProperty("qcDbPass");
	}

	/**
	 * 
	 * @return el tipo de base en la que esta Qc
	 */
	public TipoConexion getQcDataBaseTipo() {

		try {
			if (TipoConexion.ORACLE.getValue().equalsIgnoreCase(instance.getProperty("qcDbTipo"))) {
				return TipoConexion.ORACLE;
			} else if (TipoConexion.SQL_SERVER.getValue().equalsIgnoreCase(instance.getProperty("qcDbTipo"))) {
				return TipoConexion.SQL_SERVER;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new PropertiesException("Ocurrio un error al obtener la propiedad 'qcDbTipo'");
		}
	}

	/**
	 * 
	 * @return si se quiere o no que se logue el/los query/ies que se ejecutan
	 *         para cada aplicacion (SI/NO).
	 */
	public boolean getMostrarQuery() {

		String valor = instance.getProperty("mostrarQuery");

		if ("SI".equalsIgnoreCase(valor)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 
	 * @return el query para Dimensions
	 */
	public String getQueryDimensions() {
		return instance.getProperty("queryDimensions");
	}

	/**
	 * 
	 * @return el query para Qc
	 */
	public String getQueryQuality() {
		return instance.getProperty("queryQuality");
	}

	public String getAtributoDeAsignacionAutomatica() {
		return instance.getProperty("atributoDeAsignacionEnDimensions");
	}

	public String getQueryAsignacionAutomatica(String nombreDeProducto) {
		String consulta = null;

		try {
			consulta = instance.getProperty("sqlAsign_" + nombreDeProducto);
		} catch (PropertiesException e) {
			logger.debug("Se devuelve la consulta sqlAsign_COMUN");
			consulta = instance.getProperty("sqlAsign_COMUN");
		}
		return consulta;
	}

	public String getRolADelegar() {
		return instance.getProperty("rolADelegar");
	}

	public String getCapabilityADelegar() {
		return instance.getProperty("capabilityADelegar");
	}

	public String getEstadoDeAsignacion() {
		return instance.getProperty("estadoDeAsignacion");
	}

	public String getCampoBaselineEnQC() {
		return instance.getProperty("campoBaselineEnQC");
	}

	public String getAliasSQLAsignacion() {
		return instance.getProperty("aliasSQLAsignacion");
	}

	public String getQueryStreamDeCreacion() {
		return instance.getProperty("queryStreamDeCreacionParaRequests");
	}

	public String getAliasStreamDeCreacion() {
		return instance.getProperty("aliasStreamDeCreacionParaRequests");
	}

	public String getDimensionsDownloadFolder() {
		return instance.getProperty("dimensionsDownloadFolder");
	}

	public String getDimensionsDownloadFileName() {
		return instance.getProperty("dimensionsDownloadFileName");
	}
}