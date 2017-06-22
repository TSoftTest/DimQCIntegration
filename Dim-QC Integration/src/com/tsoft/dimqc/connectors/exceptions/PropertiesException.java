package ar.com.tssa.serena.connectors.exceptions;

/**
 * @author nrusz
 * 
 */
public class PropertiesException extends RuntimeException {

	private static final long serialVersionUID = -2201380172664070639L;

	public PropertiesException(String message) {
		super(message);
	}
	
	public PropertiesException(String message, Throwable ex) {
		super(message, ex);
	}
}
