package com.tsoft.dimqc.connectors.alm;

import java.io.IOException;
import java.util.Map;

import com.tsoft.dimqc.connectors.utils.ConnectorQc;

public class QCConnectionFactory {

	public static ALMConnection getConnection(Map<String, String> datos) throws IOException {

		ALMConnection alm = new ALMConnection();

		alm.setAlmUser(datos.get(ConnectorQc.USER_NAME_ALM));
		alm.setAlmPassword(datos.get(ConnectorQc.PASSWORD_ALM));
		alm.setAlmDomain(datos.get(ConnectorQc.DOMAIN_ALM));
		alm.setAlmProject(datos.get(ConnectorQc.PROJECT_ALM));
		alm.setServerURL(datos.get(ConnectorQc.SERVER_ALM));
		alm.setServerPort(Integer.parseInt(datos.get(ConnectorQc.PORT_ALM)));

		return alm;
	}
}
