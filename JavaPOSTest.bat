@Echo Off
CLS
REM ************************************************************************
REM **
REM **  The following launches the JavaPOSTest application.
REM **
REM ************************************************************************
REM
REM CLS
Echo.
Echo.
Echo.
Echo    Please wait while loading the JavaPOSTest application...
Echo.
Echo.
java -cp ".\*;.\SupportJars\*" -Djava.library.path=.;.\SupportJars -Dcom.sun.management.jmxremote com.jpos.javapostest.JavaPOSTest
REM 2>&1 >NUL
