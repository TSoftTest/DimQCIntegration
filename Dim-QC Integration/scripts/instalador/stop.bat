@ECHO OFF
net start | find "Serena-Integracion HP-QC" > nul && (net stop "Serena-Integracion HP-QC")