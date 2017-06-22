package ar.com.tssa.serena.connectors.exceptions;

/**
 * @author nrusz
 * 
 */
public class MapeoException extends RuntimeException {

	private static final long serialVersionUID = -2201380172664070639L;

	public MapeoException(String message) {
		super(message);
	}
	
	public MapeoException(String message, Throwable ex) {
		super(message, ex);
	}
}
