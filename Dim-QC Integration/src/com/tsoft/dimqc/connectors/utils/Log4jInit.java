package ar.com.tssa.serena.connectors.utils;

import org.apache.log4j.PropertyConfigurator;

public class Log4jInit {

	public void init() {
		String file = ConnectorProperties.getInstance().getRutaLog4j();

		if (file != null) {
			PropertyConfigurator.configure(file);
		}
	}
}