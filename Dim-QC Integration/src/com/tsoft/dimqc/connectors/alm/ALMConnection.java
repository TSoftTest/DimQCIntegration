package com.tsoft.dimqc.connectors.alm;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.opensaml.artifact.NullArgumentException;

import com.tsoft.dimqc.connectors.utils.ConnectorProperties;
import com.tsoft.dimqc.connectors.utils.xml.Audits;
import com.tsoft.dimqc.connectors.utils.xml.Audits.Audit;
import com.tsoft.dimqc.connectors.utils.xml.FieldsQc;
import com.tsoft.dimqc.database.ConsultasSql;
import com.tsoft.dimqc.database.TipoConexion;

/**
 * ALM web service interaction. Allows to open a connection to ALM REST web
 * service through user authentication and starting a session to use for further
 * requests.
 * 
 * @author Bjorn Weitzel (bjoern.weitzel@hp.com)
 * @version 21th November 2011
 * @company Hewlett-Packard Company
 */
public class ALMConnection extends AbstractALM {

	/**
	 * User to use for authentication
	 */
	private String almUser = null;
	/**
	 * Password for almUser
	 */
	private String almPassword = null;
	/**
	 * Domain to be used
	 */
	private String almDomain = null;
	/**
	 * Project of almDomain to be used
	 */
	private String almProject = null;
	/**
	 * URL of the ALM server
	 */
	private String serverURL = null;
	/**
	 * Port of the application server ALM is running on
	 */
	private int serverPort = 0;

	/**
	 * User is authenticated
	 */
	private boolean isAuthenticated = false;

	/**
	 * LWSSO cookie
	 */
	private String cookieLWSSO_COOKIE_KEY = null;
	/**
	 * QC session cookie
	 */
	private String cookieQCSession = null;

	private Logger logger = Logger.getRootLogger();

	/**
	 * Creates a connection to be used for interaction with ALM REST web service.
	 */
	public ALMConnection() {
		super();
	}

	/**
	 * @return ALM user to be logged on with.
	 */
	public String getAlmUser() {
		return this.almUser;
	}

	/**
	 * @param almUser
	 *          ALM user to be logged on with.
	 */
	public void setAlmUser(String almUser) {
		this.almUser = almUser;
	}

	/**
	 * @return ALM password for almUser to be used.
	 */
	public String getAlmPassword() {
		return this.almPassword;
	}

	/**
	 * @param almPassword
	 *          Password for almUser to be used for login.
	 */
	public void setAlmPassword(String almPassword) {
		this.almPassword = almPassword;
	}

	/**
	 * @return ALM domain to be used.
	 */
	public String getAlmDomain() {
		return this.almDomain;
	}

	/**
	 * @param almDomain
	 *          ALM domain to be used.
	 */
	public void setAlmDomain(String almDomain) {
		this.almDomain = almDomain;
	}

	/**
	 * @return ALM project to be used
	 */
	public String getAlmProject() {
		return this.almProject;
	}

	/**
	 * @param almProject
	 *          Specifies the project to log on to
	 */
	public void setAlmProject(String almProject) {
		this.almProject = almProject;
	}

	/**
	 * @return URL of the server ALM is running on
	 */
	public String getServerURL() {
		return this.serverURL;
	}

	/**
	 * @param serverURL
	 *          URL of the server ALM is running on
	 */
	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}

	/**
	 * @return Port of the application server ALM on the server
	 */
	public int getServerPort() {
		return serverPort;
	}

	/**
	 * @param serverPort
	 *          Port of the application server ALM on the server
	 */
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	/**
	 * @return True if authenticated against ALM
	 */
	public boolean isAuthenticated() {
		return this.isAuthenticated;
	}

	/**
	 * @return Assembled server URL based on serverUrl, serverPort and /qcbin/
	 */
	public String buildBasicURL() {
		return this.serverURL + ":" + this.serverPort + "/qcbin/";
	}

	/**
	 * @return Assembled server URL based on serverUrl, serverPort and /qcbin/ and
	 *         domain+project
	 */
	public String buildFullURL() {
		StringBuilder sb = new StringBuilder();

		sb.append(buildBasicURL());

		if (this.isAuthenticated) {
			sb.append("rest/domains/" + this.almDomain + "/projects/" + this.almProject + "/");
		}

		return sb.toString();
	}

	/**
	 * Authenticate against ALM web service
	 * 
	 * @return True if authentication was successful
	 */
	public boolean Authenticate() {

		if (this.serverURL == null || this.serverPort == 0)
			throw new IllegalStateException("Error en la URL o en el puerto.");
		if (this.almUser == null || this.almPassword == null)
			throw new IllegalStateException("Error con el user y/o en los datos para obtener la password.");
		if (this.almDomain == null || this.almProject == null)
			throw new IllegalStateException("No ALM project information set.");

		String authPoint = buildFullURL() + "authentication-point/authenticate";
		HttpURLConnection con = null;

		try {
			// Prepare authentication request
			try {
				logger.debug("Authenticate - preparing HTTP connection a " + authPoint);
				con = prepareHttpConnection(authPoint, "GET");

				byte[] authenticationBytes = (this.almUser + ":" + this.almPassword).getBytes(ConnectorProperties.getInstance().getEncodingQc());
				String encodedAuthentication = "Basic " + Base64Converter.encode(authenticationBytes);
				con.setRequestProperty("Authorization", encodedAuthentication);
			} catch (Exception e) {
				errorHandler(e);
			}

			Response res = getHTTPResponse(con);

			try {
				this.isAuthenticated = (res.getStatusCode() == HttpURLConnection.HTTP_OK);
				if (!this.isAuthenticated)
					return this.isAuthenticated;

				// Get LWSSO Cookie
				Iterable<String> newCookies = res.getResponseHeaders().get("Set-Cookie");
				logger.debug("Authenticate - iterating cookies");
				if (newCookies != null) {
					for (String cookie : newCookies) {
						logger.debug("Authenticate - cookie received: " + cookie);
						if (cookie.startsWith("LWSSO")) {
							logger.debug("Authenticate - cookie LWSSO found");
							this.cookieLWSSO_COOKIE_KEY = cookie;
							break;
						}
					}
				}

				logger.debug("Authenticate - releasing connection");
				if (con != null) {
					con.disconnect();
				}

				String sessionPoint = buildBasicURL() + "rest/site-session";
				logger.debug("Authenticate - Next request will be to: " + sessionPoint);

				con = prepareHttpConnection(sessionPoint, "POST");

				logger.debug("Authenticate - Connection created");

				res = getHTTPResponse(con);
				logger.debug("Authenticate - 2nd response received - Status code: " + res.getStatusCode());
				logger.debug("Authenticate - 2nd response received - Body: " + Arrays.toString(res.getResponseData()));
				// Get LWSSO Cookie
				logger.debug("Authenticate - reading 2nd responde cookies");
				obtenerQCSession(res);
				logger.debug("Authenticate - 2nd responde cookies read");
			} catch (Exception e) {
				errorHandler(e);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			errorHandler(e);
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}

		return this.isAuthenticated;
	}

	/**
	 * Authenticates and starts a session
	 * 
	 * @return
	 */
	public boolean AuthenticateAndStartSession() {
		if (!this.Authenticate()) {
			throw new IllegalStateException("No pudo autenticar");
		}

		return true;
	}

	/**
	 * Ends the session with the web service.
	 * 
	 * @return Closes the session on server side and restores connection object to
	 *         initial status.
	 * @throws Exception
	 *           Connection or server error during logout request
	 */
	public boolean logout() throws Exception {

		String logoutPoint = buildBasicURL() + "authentication-point/logout";

		HttpURLConnection con = null;
		Response res = null;

		try {
			con = prepareHttpConnection(logoutPoint, "GET");

			con.connect();
			res = retrieveHtmlResponse(con);

			this.almUser = null;
			this.almPassword = null;
			this.almDomain = null;
			this.almProject = null;
			this.serverURL = null;
			this.serverPort = 0;
			this.cookieLWSSO_COOKIE_KEY = null;
			this.cookieQCSession = null;
			this.isAuthenticated = false;

		} catch (Exception e) {
			errorHandler(e);
			return false;
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}

		return (res.getStatusCode() == HttpURLConnection.HTTP_OK);
	}

	/**
	 * Getting a HTTP Response from a HTTPURLConnecion.
	 * 
	 * @param con
	 *          Connection to get the response from
	 * @return Response of the connection attemt
	 */
	private Response getHTTPResponse(HttpURLConnection con) {
		Response res = null;
		try {
			con.connect();
			res = retrieveHtmlResponse(con);
		} catch (Exception e) {
			errorHandler(e);
			res = new Response();
			res.setFailure(e);
		}
		return res;
	}

	/**
	 * Reading and evaluation HTTP response
	 * 
	 * @param con
	 *          Already established HTTP URL connection awaiting a response
	 * @return A response from the server to the previously submitted HTTP request
	 * @throws Exception
	 */
	private Response retrieveHtmlResponse(HttpURLConnection con) throws Exception {
		Response res = new Response();
		res.setStatusCode(con.getResponseCode());
		res.setResponseHeaders(con.getHeaderFields());
		InputStream inputStream;

		// Select the source of the input bytes, first try "regular" input.
		try {
			inputStream = con.getInputStream();
		} catch (Exception e) {
			inputStream = con.getErrorStream();
			res.setFailure(e);
		}

		// This takes the data from the stream and stores it in a byte[] inside the
		// response.
		ByteArrayOutputStream container = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int read;
		while ((read = inputStream.read(buf, 0, 1024)) > 0) {
			container.write(buf, 0, read);
			container.flush();
		}

		res.setResponseData(container.toByteArray());
		return res;
	}

	/**
	 * @param url
	 *          Address of the connections destination
	 * @param method
	 *          HTTP method (GET, POST, etc.)
	 * @return A prepared HTTPURLConnection
	 */
	private HttpURLConnection prepareHttpConnection(String url, String method) {
		HttpURLConnection con = null;
		logger.debug("prepareHttpConnection - url: " + url);
		logger.debug("prepareHttpConnection - method: " + method);

		try {
			con = (HttpURLConnection) new URL(url).openConnection();
			if (method.toLowerCase().equals("post")) {
				logger.debug("prepareHttpConnection - setting xml properties in header");
				con.setRequestProperty("Content-Type", "application/xml");
				con.setRequestProperty("Accept", "application/xml");
			}
		} catch (Exception e) {
			errorHandler(e);
		}

		return prepareHttpConnection(con, method);
	}

	/**
	 * @param con
	 *          A connection to be prepared with request method and required
	 *          cookies
	 * @param method
	 *          HTTP method (GET, POST, etc.)
	 * @return A prepared HTTPURLConnection
	 */
	private HttpURLConnection prepareHttpConnection(HttpURLConnection con, String method) {
		if (con == null)
			throw new NullArgumentException("No connection object.");

		// Set method and cookies if required
		try {
			con.setRequestMethod(method);

			logger.debug("prepareHttpConnection2 - checking if authenticated");
			if (this.isAuthenticated) {
				logger.debug("prepareHttpConnection2 - authenticated - LWSSO Cooie: " + this.cookieLWSSO_COOKIE_KEY);
				logger.debug("prepareHttpConnection2 - authenticated - Cookie QCSessin: " + this.cookieQCSession);
				if (this.cookieQCSession != null) {
					con.setRequestProperty("Cookie", this.cookieLWSSO_COOKIE_KEY + "; " + this.cookieQCSession);
					logger.debug("prepareHttpConnection2 - cookieQCSession detected");
				} else {
					con.setRequestProperty("Cookie", this.cookieLWSSO_COOKIE_KEY);
					logger.debug("prepareHttpConnection2 - cookieQCSession NOT detected");
				}
			}
		} catch (Exception e) {
			errorHandler(e);
		}

		return con;
	}

	/**
	 * Ejecuta una consulta que devuelve entidades
	 * 
	 * @param filter
	 *          es el query rest
	 * @return Entities
	 * @throws Exception
	 */
	public Entities listEntities(String filter) throws Exception {
		boolean success = false;
		String resultPoint = buildFullURL() + "defects" + filter;

		HttpURLConnection con = null;
		Entities list = null;

		try {
			con = prepareHttpConnection(resultPoint, "GET");

			Response res = getHTTPResponse(con);
			this.obtenerQCSession(res);

			success = res.getStatusCode() == HttpURLConnection.HTTP_OK;
			if (!success) {
				logger.error(res);
				throw new Exception(res.toString());
			}
			String xml = new String(res.getResponseData());
			list = EntityMarshallingUtils.marshal(Entities.class, xml);
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new Exception(e.getMessage());
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}

		return list;
	}

	public boolean actualizarEntidad(Entity entity) throws Exception {

		boolean success = false;

		byte[] data = null;

		String resultPoint = buildFullURL() + "defects/" + entity.getFields().getFieldByName("id").getValue().get(0).getValue();

		HttpURLConnection con = null;

		String entityXML = null;
		try {
			entityXML = EntityMarshallingUtils.unmarshal(Entity.class, entity).trim();
		} catch (Exception e) {
			errorHandler(e);
		}

		try {
			// Send the data to the REST web service
			try {
				con = prepareHttpConnection(resultPoint, "PUT");
				con.setRequestProperty("Content-Type", "application/xml");
				con.setRequestProperty("Accept", "application/xml");

				con.setDoOutput(true);

				data = entityXML.getBytes();
				OutputStream out = con.getOutputStream();
				out.write(data);
				out.flush();
				out.close();

			} catch (Exception e) {
				errorHandler(e);
			}

			// Check response if submission was successful
			try {
				Response res = getHTTPResponse(con);
				this.obtenerQCSession(res);
				String originalRes = res.toString();
				res.setResponseData(data);

				success = res.getStatusCode() == HttpURLConnection.HTTP_OK;
				if (!success) {
					logger.error(originalRes);
					throw new Exception(originalRes);
				}
			} catch (Exception e) {
				errorHandler(e);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new Exception(e.getMessage());
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}

		return success;
	}

	public Entities archivos(String idEntidad) throws Exception {

		boolean success = false;
		Entities entities = null;

		String resultPoint = buildFullURL() + "defects/" + idEntidad + "/attachments";

		HttpURLConnection con = null;

		try {
			con = prepareHttpConnection(resultPoint, "GET");

			Response res = getHTTPResponse(con);
			this.obtenerQCSession(res);
			success = res.getStatusCode() == HttpURLConnection.HTTP_OK;
			if (!success)
				throw new Exception(res.getFailure().getMessage());

			String xml = new String(res.getResponseData());
			entities = EntityMarshallingUtils.marshal(Entities.class, xml);
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new Exception(e.getMessage());
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}

		return entities;
	}

	public String descargarArchivo(String idEntidad, String nombreArchivo) throws Exception {

		boolean success = false;
		nombreArchivo = URLEncoder.encode(nombreArchivo, ConnectorProperties.getInstance().getEncodingQc());
		nombreArchivo = nombreArchivo.replace("+", "%20");

		String resultPoint = buildFullURL() + "defects/" + idEntidad + "/attachments/" + nombreArchivo;

		HttpURLConnection con = null;
		String path = "";

		try {
			con = prepareHttpConnection(resultPoint, "GET");

			Response res = new Response();
			this.obtenerQCSession(res);

			path = ConnectorProperties.getInstance().getRutaArchivosTemporales() + "/";

			try {

				res.setStatusCode(con.getResponseCode());
				res.setResponseHeaders(con.getHeaderFields());

				InputStream fileInput = null;
				FileOutputStream fileOutput = null;
				BufferedOutputStream container = null;

				File folder = new File(path);
				folder.mkdirs(); // esto es para crear la carpeta

				path += nombreArchivo;
				try {
					fileInput = con.getInputStream();

					fileOutput = new FileOutputStream(path);
					container = new BufferedOutputStream(fileOutput);

					byte[] buf = new byte[1024];
					int read;
					while ((read = fileInput.read(buf, 0, 1024)) > 0) {
						container.write(buf, 0, read);
						container.flush();
					}

				} catch (Exception e) {
					fileInput = con.getErrorStream();
					res.setFailure(e);

				} finally {
					if (fileOutput != null) {
						fileOutput.close();
					}
				}

			} catch (Exception e) {
				errorHandler(e);
				res.setFailure(e);
			}

			success = res.getStatusCode() == HttpURLConnection.HTTP_OK;
			if (!success)
				throw new Exception(res.getFailure().getMessage());
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new Exception(e.getMessage());
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}

		return path;
	}

	public boolean subirArchivo(String idEntidad, String nombreArchivo) throws Exception {
		logger.info("Comenzando subir archivo");

		HttpURLConnection connection = null;

		try {
			String resultPoint = buildFullURL() + "defects/" + idEntidad + "/attachments";

			String path = ConnectorProperties.getInstance().getRutaArchivosTemporales() + "/";
			String fileFullPath = path + nombreArchivo;
			logger.info("Intentando enviar: " + fileFullPath);
			File binaryFile = new File(fileFullPath);

			String boundary = Long.toHexString(System.currentTimeMillis());
			String CRLF = "\r\n";
			String mediaType = "application/octet-stream";

			logger.info("Conectando a: " + resultPoint);
			logger.info("Content type a enviar es del tipo: " + mediaType);

			connection = prepareHttpConnection(resultPoint, "POST");

			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

			OutputStream output = connection.getOutputStream();
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true);

			writer.append("--" + boundary).append(CRLF);
			writer.append("Content-Disposition: form-data; name=\"filename\"").append(CRLF);
			writer.append(CRLF);
			writer.append(binaryFile.getName()).append(CRLF);

			writer.append("--" + boundary).append(CRLF);
			writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + binaryFile.getName() + "\"").append(CRLF);
			writer.append("Content-Type: " + mediaType).append(CRLF);
			writer.append("Content-Transfer-Encoding: binary").append(CRLF);
			writer.append(CRLF).flush();
			sendFileToOputStream(fileFullPath, output);
			output.flush();
			writer.append(CRLF);
			writer.append("--" + boundary + "--").append(CRLF).flush();

			Response res = getHTTPResponse(connection);
			this.obtenerQCSession(res);
			boolean success = res.getStatusCode() == HttpURLConnection.HTTP_CREATED;
			if (!success) {
				logger.error(res.toString());
				throw new Exception(res.getFailure().getMessage());
			}

			writer.close();

			logger.info("Archivo subido");

			return true;
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new Exception(e.getMessage());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	public void sendFileToOputStream(String pathToFile, OutputStream output) throws IOException {
		File f = new File(pathToFile);
		byte[] buf = new byte[8192];
		InputStream is = new FileInputStream(f);

		int c = 0;

		while ((c = is.read(buf, 0, buf.length)) > 0) {
			output.write(buf, 0, c);
			output.flush();
		}
		is.close();
	}

	public boolean verifyObjectStatus(Entity entity) throws JAXBException, Exception {
		boolean success = false;
		boolean locked = false;
		LockStatusEntity lockedEntity = null;

		String resultPoint = buildFullURL() + "defects/" + entity.getFields().getFieldByName("id").getValue().get(0).getValue() + "/lock";

		HttpURLConnection con = null;

		try {
			con = prepareHttpConnection(resultPoint, "GET");
			Response res = getHTTPResponse(con);
			this.obtenerQCSession(res);
			success = res.getStatusCode() == HttpURLConnection.HTTP_OK;
			if (!success)
				throw new Exception(res.getFailure().getMessage());

			String xml = new String(res.getResponseData());

			lockedEntity = EntityMarshallingUtils.marshal(LockStatusEntity.class, xml);

			locked = lockedEntity.isLocked();
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new Exception(e.getMessage());
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}

		return locked;
	}

	public Entity getEntityBySummary(String query) throws Exception {
		boolean success = false;
		String resultPoint = buildFullURL() + "defects" + query;

		HttpURLConnection con = null;

		try {
			con = prepareHttpConnection(resultPoint, "GET");

			Response res = getHTTPResponse(con);
			this.obtenerQCSession(res);

			success = res.getStatusCode() == HttpURLConnection.HTTP_OK;

			if (!success) {
				logger.error(res);
				throw new Exception(res.toString());
			}
			String xml = new String(res.getResponseData());
			Entities entities;
			entities = EntityMarshallingUtils.marshal(Entities.class, xml);
			if (entities.getTotalResults() != 0) {
				return entities.getEntities().get(0);
			} else {
				return null;
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new Exception(e.getMessage());
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}

	}

	private void obtenerQCSession(Response res) {
		logger.debug("obtenerQCSession - getting new session");

		if (this.cookieQCSession == null && res.getStatusCode() == HttpURLConnection.HTTP_OK) {
			// Get Session cookie
			Iterable<String> newCookies = res.getResponseHeaders().get("Set-Cookie");
			if (newCookies != null && this.cookieQCSession == null) {
				for (String cookie : newCookies) {
					logger.debug("obtenerQCSession - new Cookie: " + cookie);
					if (cookie.startsWith("QCSession")) {
						this.cookieQCSession = cookie;
						break;
					}
				}
			}
		}

		logger.debug("obtenerQCSession - ending");
	}

	public FieldsQc getFieldsQc() throws Exception {
		boolean success = false;
		String resultPoint = buildFullURL() + "customization/entities/defect/fields";

		HttpURLConnection con = null;
		FieldsQc fieldsQc = null;

		try {
			con = prepareHttpConnection(resultPoint, "GET");

			Response res = getHTTPResponse(con);
			this.obtenerQCSession(res);

			success = res.getStatusCode() == HttpURLConnection.HTTP_OK;

			if (!success) {
				logger.error(res);
				throw new Exception(res.toString());
			}

			String xml = new String(res.getResponseData());
			fieldsQc = EntityMarshallingUtils.marshal(FieldsQc.class, xml);
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new Exception(e.getMessage());
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}

		return fieldsQc;
	}

	public Audit getUltimaAuditoria(String id, String nombreEsquemaBaseQc) throws Exception {

		boolean success = false;
		String resultPoint = buildFullURL() + "defects/" + id + "/audits?page-size=1&order-by={time[DESC]}";

		HttpURLConnection con = null;

		try {
			con = prepareHttpConnection(resultPoint, "GET");

			Response res = getHTTPResponse(con);
			this.obtenerQCSession(res);

			success = res.getStatusCode() == HttpURLConnection.HTTP_OK;

			if (!success) {
				logger.error(res);
				throw new Exception(res.toString());
			}

			String xml = new String(res.getResponseData());
			Audits audits = EntityMarshallingUtils.marshal(Audits.class, xml);

			if (audits != null && !audits.getAudits().isEmpty() && audits.getTotalResults() == 1) {
				return audits.getAudits().get(0);
			}
		} catch (Exception e) {
			return getUltimaAuditoriaQuery(id, nombreEsquemaBaseQc);
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}

		return null;
	}

	private Audit getUltimaAuditoriaQuery(String idBug, String nombreEsquemaBaseQc) throws Exception {
		try {
			String user = null;
			// Obtengo los datos de la base directamente
			if (TipoConexion.ORACLE.equals(ConnectorProperties.getInstance().getQcDataBaseTipo())) {

				user = ConsultasSql.obtenerUltimaAuditoriaQc(idBug, ConnectorProperties.getInstance().getQcDataBaseTipo(), ConnectorProperties.getInstance().getQcDataBaseSDI(),
				    nombreEsquemaBaseQc);

			} else if (TipoConexion.SQL_SERVER.equals(ConnectorProperties.getInstance().getQcDataBaseTipo())) {
				user = ConsultasSql.obtenerUltimaAuditoriaQc(idBug, ConnectorProperties.getInstance().getQcDataBaseTipo(), nombreEsquemaBaseQc, "");
			}

			Audit audit = new Audit();
			audit.setUser(user);// Solo pongo usuario, porque actualmente solo se usa
			                    // ese dato.
			return audit;
		} catch (Exception e) {
			logger.error("Se produjo un error al obtener los datos de auditoria en Qc desde la base:" + e.getMessage());
			throw new Exception(e.getMessage());
		}
	}
}