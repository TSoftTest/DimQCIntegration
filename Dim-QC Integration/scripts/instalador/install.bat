::#########################################################
::# El script intentara obtener la variable JAVA_HOME 	  #
::# del sistema y en caso de no existir la obtendra de 	  #
::# JAVA_MANUAL, la cual debera ser configurada indicando #
::# el directorio de instalacion de JAVA                  #
::#	Auth: nrusz		                                      #
::#########################################################
@echo off

ECHO *************************************************************************
ECHO *************************INSTALANDO SERVICIO*****************************
ECHO *************************************************************************

SET JAVA_HOME="C:\app\jdk\jdk16\jre"

ECHO Inicio de Instalacion del servicio SCCMIntegration...
ECHO Verificando variable JAVA_HOME..
ECHO Variable JAVA_HOME: 
SET JAVA_HOME
SET BASEDIR=%CD%\..
SET JVM="%JAVA_HOME%\bin\server\jvm.dll"

SET CLASSPATH=%BASEDIR%\jar\SCCMIntegrationDaemon.jar;%BASEDIR%\lib\com.springsource.org.opensaml-1.1.0.jar;%BASEDIR%\lib\commons-logging-api.jar;%BASEDIR%\lib\darius.jar;%BASEDIR%\lib\dmclient.jar;%BASEDIR%\lib\dmfile.jar;%BASEDIR%\lib\dmnet.jar;%BASEDIR%\lib\dmpmcli.jar;%BASEDIR%\lib\js.jar;%BASEDIR%\lib\jsoup-1.7.2.jar;%BASEDIR%\lib\junit.jar;%BASEDIR%\lib\log4j-1.2.17.jar;%BASEDIR%\lib\java-security-encription-client-1.0.1.jar;%BASEDIR%\lib\bcprov-jdk16-1.46.jar;%BASEDIR%\lib\ojdbc6.jar;%BASEDIR%\lib\sqljdbc4.jar
IF "%ERRORLEVEL%" == "0" (
	IF EXIST %JVM% (
		%BASEDIR%\instalador\JavaService.exe -install "Serena-Integracion HP-QC" %JVM% -Dfile.encoding=UTF8 -Djava.class.path=%CLASSPATH% -start ar.com.tssa.serena.MainProgram -err c:/opt/connector/serviceError.log -auto -description "Servicio de Integracion HP-QC" 
		IF "%ERRORLEVEL%" == "0" (
			ECHO Iniciando el servicio...
			CALL %BASEDIR%\instalador\start.bat
			IF NOT %ERRORLEVEL% == 0 (
			   ECHO Error al invocar el Script start.bat
			)
		)	ELSE (
			ECHO Ocurrio un error al instalar el servicio invocando JavaService.exe
		)
	) ELSE ( 
		ECHO No se encuentra la libreria necesaria de JAVA, verifique que la variable JAVA_MANUAL dentro de este mismo archivo apunte al directorio donde se encuentra instalado JAVA.
	)
) ELSE ( 
	ECHO Ocurrio un error configurando las variables.
)
ECHO *************************************************************************
ECHO ************************INSTALACION FINALIZADA***************************
ECHO *************************************************************************
PAUSE



