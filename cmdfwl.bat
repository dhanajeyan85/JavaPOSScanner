@echo off 
REM Compare Firmware Result Codes
REM JPOS_CFV_FIRMWARE_OLDER 	= 1
REM JPOS_CFV_FIRMWARE_SAME 		= 2
REM JPOS_CFV_FIRMWARE_NEWER 	= 3
REM JPOS_CFV_FIRMWARE_DIFFERENT = 4
REM JPOS_CFV_FIRMWARE_UNKNOWN 	= 5
REM DLS_CFV_INVALID_VID			= 6
REM DLS_CFV_INVALID_PID			= 7

REM Command line options take precedence over the options in the ./SupportJars/cmdfw.properties file.
REM help  	   	-h 	--help     	print usage information
REM info  		-i, --info      save info to file (default: <DeviceClass>_info.txt*), --scanner <ScannerName> is required
REM list  	   	-l, --list      list all available ScannerNames in jpos.xml file
REM compare     -c, --compare   compare <FileName> with firmware in <ScannerName>, --scanner <ScannerName> & --filename <FileName> are required
REM scanner	   	-s, --scanner   scanner target (logical name from jpos.xml)
REM file        -f, --filename  file target for updating/comparing
REM startBaud   -b, --startBaud set the starting baud rate for serial devices
REM endBaud     -t, --endBaud   set the ending baud rate for serial devices
REM force       -u, --force     force update regardless of file version passed in
REM enabled     -e, --enabled   leave scanner enabled after update/compare/info command
REM verbose     -v, --verbose   print operational information to the console
REM * <DeviceClass> refers to the DeviceClass property from jpos.xml
java -cp ".\*;.\SupportJars\*" -Djava.library.path=.\;.\SupportJars cmdfw.Cmdfw -l -v
echo %errorlevel%