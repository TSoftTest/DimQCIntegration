package ar.com.tssa.serena.connectors.alm;

import org.apache.log4j.Logger;

/**
 * 
 * @author mavo
 * Abstract class to be used will all REST webservices - pls. add common functions here
 *
 */
public abstract class AbstractALM {
	
	private Logger cat;
	
	public  AbstractALM(){		
	}
	
	public Logger getLogger(Class<?> cName){
		return cat;
	}

	public void errorHandler(Throwable a)  {
		if (null == cat) cat = getLogger(getClass());
	}

}
