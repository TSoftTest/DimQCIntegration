package com.tsoft.dimqc;

import org.apache.log4j.Logger;

import com.tsoft.dimqc.connectors.daemon.DaemonThread;
import com.tsoft.dimqc.connectors.utils.ConnectorProperties;
import com.tsoft.dimqc.connectors.utils.Log4jInit;

public class Integrator {
	private static final String THREAD_NAME = "SCCMIntegrationDeamon";

	public static void main(String[] args) throws Exception {

		// Evitar que las conexiones http queden en TIME_WAIT
		System.setProperty("http.keepAlive", "false");

		new Log4jInit().init();
		Logger logger = Logger.getRootLogger();
		long sleepTime = ConnectorProperties.getInstance().getSleepTime() * 1000 * 60;

		Thread t = new Thread(new DaemonThread(), THREAD_NAME);
		while (true) {
			try {
				if (t.getState().equals(Thread.State.TERMINATED)) {
					esperar(sleepTime);
					t = new Thread(new DaemonThread(), THREAD_NAME);
				}

				if (t.getState().equals(Thread.State.NEW) || t.getState().equals(Thread.State.TERMINATED)) {
					logger.debug("Inicio del Daemon");
					t.start();
				}

				esperar(10);

			} catch (Throwable e) {
				logger.error("Se produjo un error inesperado. Se reiniciara el demonio", e);
			}
		}

	}

	private static void esperar(long sleepTime) {
		try {
			Thread.sleep(sleepTime);
		} catch (Exception _ignore) {
		}
	}
}
