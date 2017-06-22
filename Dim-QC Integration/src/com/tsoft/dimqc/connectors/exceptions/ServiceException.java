package ar.com.tssa.serena.connectors.exceptions;

/**
 * @author nrusz
 * 
 */
public class ServiceException extends RuntimeException {

	private static final long serialVersionUID = -2201380172664070686L;

	public ServiceException(String message) {
		super(message);
	}
	
}
