package com.tsoft.dimqc.connectors.dimensions;

import com.serena.dmclient.api.DimensionsConnection;
import com.serena.dmclient.api.DimensionsConnectionDetails;
import com.serena.dmclient.api.DimensionsConnectionManager;
import com.tsoft.dimqc.connectors.utils.ConnectorProperties;

public class DimensionsConnectionFactory {

	public static DimensionsConnection getConnection() throws Exception {
		DimensionsConnectionDetails details = new DimensionsConnectionDetails();

		details.setUsername(ConnectorProperties.getInstance().getDbUserDim());
		String pass = ConnectorProperties.getInstance().getDbPasswordDim();
		if (pass == null) {
			throw new Exception("Error al desencriptar la password para Dimensions.");
		}

		details.setPassword(pass);
		details.setDbName(ConnectorProperties.getInstance().getDbNameDim());
		details.setDbConn(ConnectorProperties.getInstance().getDbConnDim());
		details.setServer(ConnectorProperties.getInstance().getDbServerDim());

		DimensionsConnection connection = DimensionsConnectionManager.getConnection(details);

		return connection;
	}
}
