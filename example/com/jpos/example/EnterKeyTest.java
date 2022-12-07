package com.jpos.example;

import com.dls.jpos.interpretation.*;
import jpos.*;
import jpos.events.*;
import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Script to test basic scanner functionality.
 * This script must be ran from the root JavaPOS directory.
 *     Windows C:\Program Files\Datalogic\JavaPOS
 *     Linux /opt/dls/JavaPOS
 *
 * @author Jesse Ivy
 */
public class EnterKeyTest implements StatusUpdateListener, DataListener, ErrorListener {
    static Scanner scanner = null;
    static EnterKeyTest script = null;
    String FIRMWARE_PATH;
    String CONFIGURATION_PATH;
    byte[] scanData = new byte[]{};
    byte[] scanDataLabel = new byte[]{};
    int itemDataCount = 0;
    String scanDataString;
    String scanDataLabelString;
    int scanDataType = -1;
    boolean autoDisable;
    boolean dataEventEnabled;
    boolean deviceEnabled;
    boolean freezeEvents;
    boolean decodeData;
    boolean updateDevice = true;
    private static String sep = System.getProperty("line.separator");
    /**
     * @param args the command line arguments
     * Could potentially include arguments if specific scripts are intended to be reusable.
     * For instance logical name could be passed in as an argument from the command line
     * in future versions.
     */
    public static void main(String[] args) {
        String logicalName = "DLS-Powerscan-M9500-USB-COM";
        script = new EnterKeyTest();
        try {
            //Instantiate a scanner object
            scanner = new Scanner();

            //Open the scanner specifying which scanner you are using from jpos.xml
            scanner.open(logicalName);

            //Add listeners for errors, data events, and status update events
            scanner.addErrorListener(script);
            scanner.addDataListener(script);
            scanner.addStatusUpdateListener(script);

            //Claim the Scanner (depending on interface it could be usable)
            scanner.claim(1000);

            //Wait thirty seconds for the device to reset after pulling stats during claim.
            //@TODO: Finish Power Notify update so that this is unnecessary.
            Thread.sleep(30000);

            //Enable the device
            scanner.setDeviceEnabled(true);

            //Enable data events (label reading)
            scanner.setDataEventEnabled(true);

            try {

                String firmwarePath = "C:\\Program Files\\datalogic\\JavaPOS\\firmware.S37";

                int[] result = new int[1];

                File firmware = new File(firmwarePath);
                if(firmware.exists()) {
                    scanner.compareFirmwareVersion(firmwarePath, result);
                    switch(result[0]){
                        case jpos.JposConst.JPOS_CFV_FIRMWARE_NEWER :
                            System.out.println("The firmware in the file newer than the firmware loaded in the device. Updating firmware...");
                            scanner.updateFirmware(firmwarePath);
                            break;
                        case jpos.JposConst.JPOS_CFV_FIRMWARE_OLDER :
                            System.out.println("The firmware in the file is older than the firmware loaded in the device.");
                            scanner.updateFirmware(firmwarePath);
                            break;
                        case jpos.JposConst.JPOS_CFV_FIRMWARE_SAME :
                            System.out.println("The firmware in the file is the same as what is already loaded in the device. Updating firmware...");
                            scanner.updateFirmware(firmwarePath);
                            break;
                        case jpos.JposConst.JPOS_CFV_FIRMWARE_DIFFERENT :
                            System.out.println("The firmware in the file is different than the firmware in the device. In what way was unable to be determined.");
                            break;
                        case jpos.JposConst.JPOS_CFV_FIRMWARE_UNKNOWN :
                            System.out.println("The firmware version in the file is unknown. Please contact your support representative.");
                            break;
                    }
                } else {
                    System.out.println("File Not Found: " + firmwarePath);
                }

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            //Wait until enter key is pressed to exit
            System.in.read();

        } catch (JposException je) {
            System.out.println("JPOS Exception: "+je.getMessage()+"\\n"+je.getStackTrace());
        } catch (Exception e) {
            System.out.println("Exception: "+e.getMessage()+"\\n"+e.getStackTrace());
        }
        System.exit(0);
        return;
    }

    /**
     * Call a function to process data received from scanner.
     * @param de
     */
    @Override
    public void dataOccurred(DataEvent de) {
        doDataUpdate();
    }

    /**
     * Processes the data event sent from JavaPOS.
     */
    public void doDataUpdate() {
        try {
            scanData = scanner.getScanData();
            scanDataLabel = scanner.getScanDataLabel();
            scanDataType = scanner.getScanDataType();
            autoDisable = scanner.getAutoDisable();
            if (scanDataLabel.length > 0) {
                System.out.println("Item Count: "+Integer.toString(++itemDataCount));
            }
            // this setting of the DataEventEnable is because the tester
            // doesn't want to have to continually check the box after
            // every single scan
            scanner.setDataEventEnabled(true);
            dataEventEnabled = scanner.getDataEventEnabled();

            System.out.println("Scan Data: " + trimUnprintable(scanData));
            System.out.println("Scan Data Type: " + scanDataType);
            try {

                ByteArrayOutputStream dioResult = new ByteArrayOutputStream();
                int[] iResult = new int[50];
                String[] results;

                //Return Quantity
                scanner.directIO(com.dls.jpos.common.DLSJposConst.DIO_RETURN_QUANTITY, iResult, dioResult);
                COMDIOResults("DIO_RETURN_QUANTITY", iResult, dioResult.toByteArray());

                //Return Data Type
                scanner.directIO(com.dls.jpos.common.DLSJposConst.DIO_RETURN_DATA_TYPE, iResult, dioResult);
                COMDIOResults("DIO_RETURN_DATA_TYPE", iResult, dioResult.toByteArray());

            } catch(Exception e) {
                    System.out.println("Exception: " + e.getMessage());
            }

            if (scanner.getClaimed()) {
                deviceEnabled = scanner.getDeviceEnabled();
            }

            freezeEvents = scanner.getFreezeEvents();
            decodeData = scanner.getDecodeData();
            System.out.println("Data Count: "+Integer.toString(scanner.getDataCount()));
			System.out.println(sep + "********************************************************************" + sep);
        } catch (JposException je) {
            System.out.println("Exception in doDataUpdate(): " + je.getMessage());
        }
    }

    @Override
    public void errorOccurred(ErrorEvent ee) {
        try {
            // Post error event data to dialog doDataUpdate();
            int errCode = ee.getErrorCode();
            int errCodeEx = ee.getErrorCodeExtended();
            int errCodeRes = ee.getErrorResponse();

            System.out.println("Error event occured: " + convertErrorCodeToString(errCode) + " : " + convertErrorCodeToString(errCodeEx) + " : " + convertErrorCodeToString(errCodeRes) + sep);

            scanner.setDataEventEnabled(true);
        } catch (JposException ex) {
            System.err.println("JposException: " + ex.getLocalizedMessage());
        }
    }

	/**
     * This function converts a byte array into a string representation of the
	 * hex data
     * @param data
     * @return sRawData - the string interpretation of the byte array passed in.
     */
    public static String byteArrayToString(byte[] data) {
        String logData = "";
        String sResult = "";
        String sRawData = "";

        // if any data is NOT NULL then we assume it's valid (see monkies below).
        boolean discard = true;

        if (data != null && data.length > 0) {
            // Convert input data to hex buffer
            for (int i = 0; i < data.length; i++) {
                sResult = String.format(" 0x%02X", data[i]);
                logData += sResult;
                // here is where we keep the monkies that check each byte for null.
                if (data[i] != 0x00) {
                    discard = false;
                }
            }
            if (discard == true) {
                sRawData = "[" + 0 + "] **invalid packet - contained all nulls.";
            } else {
                sRawData = "[" + data.length + "]" + logData;
            }
            //log.log(this, "#concatinated bytes received[" + data.length + "]" + logData);
        } else if (data == null) {
            sRawData = "[" + 0 + "] **invalid packet - byte array is null)";
        } else {
            sRawData = "[" + 0 + "] **invalid packet - byte array is empty)";
        }
        return sRawData;
    }

    /**
     * Process the result of a direct input/output call.
     * @param label - The text representation to output indicating which command was executed.
     * @param iResult - The integer array passed into the JavaPOS directIO function by reference.
     * @param dioResult - The byte array returned by the scanner via reference variable.
     */
    private void COMDIOResults(String label, int[] iResult, byte[] dioResult) {
        try {
            //Print the integer response if any.
            if(null != dioResult && 0 < iResult.length) {
                System.out.println(label + " Result: " + iResult[0]);
            }
            //Print the raw data returned from the scanner.
            if(null != dioResult && 0 < dioResult.length) {
                System.out.println(label + " Raw Data: " + byteArrayToString(dioResult));
            }
        } catch (Exception e) {
            System.out.println("Exception in COMDIOResults(): " + e.getMessage());
        }
    }

    /**
     * Echo status event to console.
     * @param sue - The status update event fired by JavaPOS.
     */
    @Override
    public void statusUpdateOccurred(StatusUpdateEvent sue) {
        int status = sue.getStatus();
        if (status >= JposConst.JPOS_SUE_UF_PROGRESS && status < JposConst.JPOS_SUE_UF_COMPLETE) {
            int value = status - JposConst.JPOS_SUE_UF_PROGRESS;
            // add a little animation to let the user know something is actually happening
            String outString = String.format("The update firmware process is continuing... %d%%" + sep, value);
            System.out.print(outString);
        }
        if ((status - JposConst.JPOS_SUE_UF_PROGRESS) == 95) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    System.exit(0);
                }
            }, 1 * 60 * 1000);
        }

        switch (status) {
            case JposConst.JPOS_SUE_UF_COMPLETE:
                    System.out.print(sep + "The update firmware process has completed successfully." + sep);
                break;
            case JposConst.JPOS_SUE_UF_COMPLETE_DEV_NOT_RESTORED:
                System.out.print(sep + "***********************************************************************" + sep
                        + "The update firmware process succeeded, however the Service and/or " + sep
                        + "the physical device cannot be returned to the state they were in before the " + sep
                        + "update firmware process started. The Service has restored all properties to " + sep
                        + "their default initialization values. To ensure consistent Service and " + sep
                        + "physical device states, the application needs to close the Service, then " + sep
                        + "open, claim, and enable again, and also restore all custom application settings." + sep
                        + "**********************************************************************" + sep);
                break;
            case JposConst.JPOS_SUE_UF_FAILED_DEV_OK:
                System.out.print(sep + "***********************************************************************" + sep
                        + "The update firmware process failed but the device is still operational. " + sep
                        + "**********************************************************************" + sep);
                break;
            case JposConst.JPOS_SUE_UF_FAILED_DEV_UNRECOVERABLE:
                System.out.print(sep + "***********************************************************************" + sep
                        + "The update firmware process failed and the device is neither usable" + sep
                        + "nor recoverable through software. The device requires service to be returned to an " + sep
                        + "operational state." + sep
                        + "**********************************************************************" + sep);
                break;
            case JposConst.JPOS_SUE_UF_FAILED_DEV_NEEDS_FIRMWARE:
                System.out.print(sep + "***********************************************************************" + sep
                        + "The update firmware process failed and the device will" + sep
                        + "not be operational until another attempt to update the" + sep
                        + "firmware is successful." + sep
                        + "**********************************************************************" + sep);
                break;
            case JposConst.JPOS_SUE_UF_FAILED_DEV_UNKNOWN:
                System.out.print(sep + "**********************************************************************" + sep
                        + "The update firmware process failed and the device is in an indeterminate state." + sep
                        + "**********************************************************************" + sep);
                break;
        }

        int nStatus = sue.getStatus();
        if (nStatus == jpos.JposConst.JPOS_PS_OFF) {
            System.out.println("Power Off");
        } else if (nStatus == jpos.JposConst.JPOS_PS_OFFLINE) {
            System.out.println("Power Offline");
        } else if (nStatus == jpos.JposConst.JPOS_PS_OFF_OFFLINE) {
            System.out.println("Power Off and Offline");
        } else if (nStatus == jpos.JposConst.JPOS_PS_ONLINE) {
            System.out.println("Power Online");
        } else if (nStatus == jpos.JposConst.JPOS_PS_UNKNOWN) {
            System.out.println("Power Unknown");
        }
    }

    private String convertErrorCodeToString(int errorCode) {
        String codeString = "";
        switch (errorCode) {
            case DeviceErrorStatusListener.ERR_CMD:
                codeString = "ERR_CMD";
                break;
            case DeviceErrorStatusListener.ERR_NO_WEIGHT:
                codeString = "ERR_NO_WEIGHT";
                break;
            case DeviceErrorStatusListener.ERR_DATA:
                codeString = "ERR_DATA";
                break;
            case DeviceErrorStatusListener.ERR_READ:
                codeString = "ERR_READ";
                break;
            case DeviceErrorStatusListener.ERR_NO_DISPLAY:
                codeString = "ERR_NO_DISPLAY";
                break;
            case DeviceErrorStatusListener.ERR_HARDWARE:
                codeString = "ERR_HARDWARE";
                break;
            case DeviceErrorStatusListener.ERR_CMD_REJECT:
                codeString = "ERR_CMD_REJECT";
                break;
            case DeviceErrorStatusListener.ERR_CAPACITY:
                codeString = "ERR_CAPACITY";
                break;
            case DeviceErrorStatusListener.ERR_REQUIRES_ZEROING:
                codeString = "ERR_REQUIRES_ZEROING";
                break;
            case DeviceErrorStatusListener.ERR_WARMUP:
                codeString = "ERR_WARMUP";
                break;
            case DeviceErrorStatusListener.ERR_DUPLICATE:
                codeString = "ERR_DUPLICATE";
                break;
            case DeviceErrorStatusListener.ERR_FLASHING:
                codeString = "ERR_FLASHING";
                break;
            case DeviceErrorStatusListener.ERR_BUSY:
                codeString = "ERR_BUSY";
                break;
            case DeviceErrorStatusListener.ERR_CHECKDIGIT:
                codeString = "ERR_CHECKDIGIT";
                break;
            case DeviceErrorStatusListener.ERR_DIO_NOT_ALLOWED:
                codeString = "ERR_DIO_NOT_ALLOWED";
                break;
            case DeviceErrorStatusListener.ERR_DIO_UNDEFINED:
                codeString = "ERR_DIO_UNDEFINED";
                break;
            case DeviceErrorStatusListener.ERR_DEVICE_REMOVED:
                codeString = "ERR_DEVICE_REMOVED";
                break;
            case DeviceErrorStatusListener.ERR_SCALE_AT_ZERO:
                codeString = "ERR_SCALE_AT_ZERO";
                break;
            case DeviceErrorStatusListener.ERR_SCALE_UNDER_ZERO:
                codeString = "ERR_SCALE_UNDER_ZERO";
                break;
				/*
            case DeviceErrorStatusListener.ERR_DEVICE_REATTACHED:
                codeString = "ERR_DEVICE_REATTACHED";
                break;
				*/
            case DeviceErrorStatusListener.STATUS_ALIVE:
                codeString = "STATUS_ALIVE";
                break;
            case DeviceErrorStatusListener.STATUS_NOT_ALIVE:
                codeString = "STATUS_NOT_ALIVE";
                break;
            case DeviceErrorStatusListener.STATUS_ENABLED:
                codeString = "STATUS_ENABLED";
                break;
            case DeviceErrorStatusListener.STATUS_NOT_ENABLED:
                codeString = "STATUS_NOT_ENABLED";
                break;
            case JposConst.JPOS_S_CLOSED:
                codeString = "JPOS_S_CLOSED";
                break;
            case JposConst.JPOS_S_IDLE:
                codeString = "JPOS_S_IDLE";
                break;
            case JposConst.JPOS_S_BUSY:
                codeString = "JPOS_S_BUSY";
                break;
            case JposConst.JPOS_S_ERROR:
                codeString = "JPOS_S_ERROR";
                break;
            case JposConst.JPOSERR:
                codeString = "JPOSERR";
                break;
            case JposConst.JPOSERREXT:
                codeString = "JPOSERREXT";
                break;
            case JposConst.JPOS_SUCCESS:
                codeString = "JPOS_SUCCESS";
                break;
            case JposConst.JPOS_E_CLOSED:
                codeString = "JPOS_E_CLOSED";
                break;
            case JposConst.JPOS_E_CLAIMED:
                codeString = "JPOS_E_CLAIMED";
                break;
            case JposConst.JPOS_E_NOTCLAIMED:
                codeString = "JPOS_E_NOTCLAIMED";
                break;
            case JposConst.JPOS_E_NOSERVICE:
                codeString = "JPOS_E_NOSERVICE";
                break;
            case JposConst.JPOS_E_DISABLED:
                codeString = "JPOS_E_DISABLED";
                break;
            case JposConst.JPOS_E_ILLEGAL:
                codeString = "JPOS_E_ILLEGAL";
                break;
            case JposConst.JPOS_E_NOHARDWARE:
                codeString = "JPOS_E_NOHARDWARE";
                break;
            case JposConst.JPOS_E_OFFLINE:
                codeString = "JPOS_E_OFFLINE";
                break;
            case JposConst.JPOS_E_NOEXIST:
                codeString = "JPOS_E_NOEXIST";
                break;
            case JposConst.JPOS_E_EXISTS:
                codeString = "JPOS_E_EXISTS";
                break;
            case JposConst.JPOS_E_FAILURE:
                codeString = "JPOS_E_FAILURE";
                break;
            case JposConst.JPOS_E_TIMEOUT:
                codeString = "JPOS_E_TIMEOUT";
                break;
            case JposConst.JPOS_E_BUSY:
                codeString = "JPOS_E_BUSY";
                break;
            case JposConst.JPOS_E_EXTENDED:
                codeString = "JPOS_E_EXTENDED";
                break;
            case JposConst.JPOS_E_DEPRECATED:
                codeString = "JPOS_E_DEPRECATED";
                break;
            case JposConst.JPOS_ESTATS_ERROR:
                codeString = "JPOS_ESTATS_ERROR";
                break;
            case JposConst.JPOS_EFIRMWARE_BAD_FILE:
                codeString = "JPOS_EFIRMWARE_BAD_FILE";
                break;
            case JposConst.JPOS_ESTATS_DEPENDENCY:
                codeString = "JPOS_ESTATS_DEPENDENCY";
                break;
            case JposConst.JPOS_PS_UNKNOWN:
                codeString = "JPOS_PS_UNKNOWN";
                break;
            case JposConst.JPOS_PS_ONLINE:
                codeString = "JPOS_PS_ONLINE";
                break;
            case JposConst.JPOS_PS_OFF:
                codeString = "JPOS_PS_OFF";
                break;
            case JposConst.JPOS_PS_OFFLINE:
                codeString = "JPOS_PS_OFFLINE";
                break;
            case JposConst.JPOS_PS_OFF_OFFLINE:
                codeString = "JPOS_PS_OFF_OFFLINE";
                break;
            case JposConst.JPOS_ER_RETRY:
                codeString = "JPOS_ER_RETRY";
                break;
            case JposConst.JPOS_ER_CLEAR:
                codeString = "JPOS_ER_CLEAR";
                break;
            case JposConst.JPOS_ER_CONTINUEINPUT:
                codeString = "JPOS_ER_CONTINUEINPUT";
                break;
            case JposConst.JPOS_SUE_UF_PROGRESS:
                codeString = "JPOS_SUE_UF_PROGRESS";
                break;
            case JposConst.JPOS_SUE_UF_COMPLETE:
                codeString = "JPOS_SUE_UF_COMPLETE";
                break;
            case JposConst.JPOS_SUE_UF_FAILED_DEV_OK:
                codeString = "JPOS_SUE_UF_FAILED_DEV_OK";
                break;
            case JposConst.JPOS_SUE_UF_FAILED_DEV_UNRECOVERABLE:
                codeString = "JPOS_SUE_UF_FAILED_DEV_UNRECOVERABLE";
                break;
            case JposConst.JPOS_SUE_UF_FAILED_DEV_NEEDS_FIRMWARE:
                codeString = "JPOS_SUE_UF_FAILED_DEV_NEEDS_FIRMWARE";
                break;
            case JposConst.JPOS_SUE_UF_FAILED_DEV_UNKNOWN:
                codeString = "JPOS_SUE_UF_FAILED_DEV_UNKNOWN";
                break;
            case JposConst.JPOS_SUE_UF_COMPLETE_DEV_NOT_RESTORED:
                codeString = "JPOS_SUE_UF_COMPLETE_DEV_NOT_RESTORED";
                break;
            case JposConst.JPOS_FOREVER:
                codeString = "JPOS_FOREVER";
                break;
        }
        return codeString;
    }

    /**
     * Converts a byte array into a String stripping any unprintable characters.
     * @param aR - The byte array to convert.
     * @return
     */
    private String trimUnprintable(byte[] aR) {
        int i = 0;
        byte ch;
        int bPad = 0;

        if (aR == null || aR.length <= 0) {
            return null;
        }

        StringBuffer out = new StringBuffer(aR.length);

        while (i < aR.length) {
            ch = aR[i];

            if ((ch > 31) && (ch < 127)) {
                out.append((char) ch);
                bPad = 1;
            } else if ((ch >= 0) && (ch <= 9)) {
                out.append((char) (ch + 48));
            } else if (bPad == 1) {
                out.append(" ");
                bPad = 0;
            }

            i++;
        }
        String rslt = new String(out);
        return rslt;
    }
}