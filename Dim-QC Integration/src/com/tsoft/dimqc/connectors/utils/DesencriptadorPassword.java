package ar.com.tssa.serena.connectors.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.Security;

import org.apache.log4j.Logger;

import coop.bancocredicoop.batch.security.Configuration;
import coop.bancocredicoop.service.security.ClaveHashService;


public class DesencriptadorPassword {

	private static Logger logger = Logger.getRootLogger();
	
	public static String obtenerPassword(String rutaArchivoKey, String rutaArchivoHash){
		
		//logger.debug("Desencriptando la password que se encuentra en el archivo " + rutaArchivoKey + " y en " + rutaArchivoHash);
		Security.addProvider( new org.bouncycastle.jce.provider.BouncyCastleProvider());
		String claveNumerica = claveNumerica(rutaArchivoHash);
		if (claveNumerica != null){
			
			Configuration configuration = new Configuration();
			configuration.setPublicKeyPath(rutaArchivoKey);
			
			ClaveHashService claveHashService = new ClaveHashService(configuration);
			
			try {
				return (claveHashService.desencriptar(claveNumerica));
			} catch (Exception e) {
				logger.error("Error al intentar desencriptar la clave que posee Key en el archivo " + rutaArchivoKey + " y Hash en " + rutaArchivoHash);
				logger.error(e);
			}

		}
		
		return null;
	}
	
	private static String claveNumerica(String rutaArchivoHash){
		FileReader fr = null;
		
		try{    
			
			File archivo = new File(rutaArchivoHash);
			fr = new FileReader(archivo);
			BufferedReader br = new BufferedReader(fr);
			String linea = br.readLine();
			if (linea != null){
				linea = linea.trim();
			}
			
			try{
				new BigInteger(linea);
			}catch (NumberFormatException ex) {
				logger.error("Error, el Hash no es una clave numérica.");
				logger.error(ex);
				return null;
			}
			
			return linea;			
			
		} catch (IOException e) {
			
			logger.error("Error al intentar obtener la clave numérica.");
			logger.error(e);
			return null;
		} finally{
			if (fr != null){
				try {
					fr.close();
				} catch (IOException e) {
					logger.error("Error al intentar cerrar el archivo:" + rutaArchivoHash);
					logger.error(e);
				}
			}
		}
	}
}
