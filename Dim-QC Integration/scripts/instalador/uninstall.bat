@ECHO OFF

ECHO *************************************************************************
ECHO ***********************DESINSTALANDO SERVICIO****************************
ECHO *************************************************************************
ECHO Deteniendo el servicio...
SET BASEDIR=%CD%

CALL %BASEDIR%\stop.bat
IF NOT %ERRORLEVEL% == 0 (
   ECHO Error al invocar el Script stop.bat. Puede que el servicio ya se encuentre detenido.
)
%BASEDIR%\JavaService.exe -uninstall "Serena-Integracion HP-QC"
IF NOT %ERRORLEVEL% == 0 (
   ECHO Error al desinstalar el servicio
)
ECHO *************************************************************************
ECHO ***********************DESINSTALACION FINALIZADA*************************
ECHO *************************************************************************
PAUSE