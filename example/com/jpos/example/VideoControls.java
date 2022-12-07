package com.jpos.example;

import com.dls.jpos.interpretation.*;
import com.dls.jpos.common.*;

import jpos.*;
import jpos.events.*;

import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Script to test the video on/off controls.
 * This script must be ran from the root JavaPOS directory.
 *     Windows C:\Program Files\Datalogic\JavaPOS
 *     Linux /opt/dls/JavaPOS
 *
 * @author jivy
 */
public class VideoControls implements StatusUpdateListener, DataListener, ErrorListener {
    static Scanner scanner = null;
    static VideoControls script = null;
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
     * For instance logical name could be passed in as an argument from the command line.
     */
    public static void main(String[] args) {
        ByteArrayOutputStream dioResult = new ByteArrayOutputStream();
        String logicalName = "DLS-Magellan-9800i-USB-OEM-Scanner-Scale";
        int numIterations = 1000;
        int timeout = 5000;

        if(args!=null){
            if(args.length>0) {
                //First argument is logical name from C:\Program Files\Datalogic\JavaPOS\jpos.xml
                logicalName = (args[0]!=null && !args[0].equals(""))?logicalName:"DLS-Magellan-9800i-USB-OEM-Scanner-Scale";
            } else {
                System.out.println("Using default value for logicalName: "+logicalName);
            }

            if(args.length>1) {
                //Second argument is the number of iterations for Video Controls.
                try {
                    numIterations = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    System.out.println("Using default value for numIterations: "+numIterations);
                }
            } else {
                System.out.println("Using default value for numIterations: "+numIterations);
            }


            if(args.length>2) {
                //Third argument is the timeout for Video Controls.
                try {
                    timeout = Integer.parseInt(args[2])*1000;
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    System.out.println("Using default value for timeout: "+timeout);
                }
            } else {
                System.out.println("Using default value for timeout: "+timeout);
            }
        }
        System.out.println(" ");
        int[] iResult = new int[50];
        String[] results;
        //Fancy self instantiating class
        script = new VideoControls();
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

            //Disable Power Notify (if desired)
            scanner.setPowerNotify(Scanner.JPOS_PN_DISABLED);

            //Enable the device
            scanner.setDeviceEnabled(true);
            
            //Enable data events (label reading)
            scanner.setDataEventEnabled(true);


            int i = 0;
            long start_time;
            long end_time;
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

            while(i < numIterations){
                //Turn video stream on, report results, set timeout.
                start_time = System.currentTimeMillis();
                scanner.directIO(DLSJposConst.DIO_EXT_VIDEO_STREAM_ON, iResult, dioResult);
                end_time = System.currentTimeMillis();
                results = VideoControls.processDIOResults(iResult, dioResult);
                System.out.println(dateFormat.format(new Date()) + " Video ON  ["+results[0]+"] Scanner Response after " + (end_time-start_time) + " milliseconds: " + results[1]);
                Thread.sleep(timeout);

                //Turn video streaming off, report results, set timeout.
                start_time = System.currentTimeMillis();
                scanner.directIO(DLSJposConst.DIO_EXT_VIDEO_STREAM_OFF, iResult, dioResult);
                end_time = System.currentTimeMillis();
                results = VideoControls.processDIOResults(iResult, dioResult);
                System.out.println(dateFormat.format(new Date()) + " Video OFF ["+results[0]+"] Scanner Response after " + (end_time-start_time) + " milliseconds: " + results[1]);
                Thread.sleep(timeout);

                i++;
            }

        } catch (JposException je) {
            System.out.println("JPOS Exception: "+je.getMessage()+"\\n"+je.getStackTrace());
        } catch (Exception e) {
            System.out.println("Exception: "+e.getMessage()+"\\n"+e.getStackTrace());
        }
        System.exit(0);
        return;
    }
    
    public static String[] processDIOResults(int[] iResult, ByteArrayOutputStream dioResult){
        String[] results = new String[2];
        //error
        results[0] = "NULL";
        if(iResult !=null) {
            results[0] = (iResult.length>0)?convertResponseCodeToString(iResult[0]):"Unknown";
        }

        //data
        results[1] = "NULL";
        if(dioResult != null) {
            byte[] aR = dioResult.toByteArray();
            results[1] = (aR !=null && aR.length>5)?convertResponseCodeToString(aR[5]):"Unknown";
        }
        return results;
    }
    
    @Override
    public void statusUpdateOccurred(StatusUpdateEvent sue) {
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

    @Override
    public void dataOccurred(DataEvent de) {
        doDataUpdate();
    }

    public void doDataUpdate() {
        try {
            scanData = scanner.getScanData();
            scanDataLabel = scanner.getScanDataLabel();
            scanDataType = scanner.getScanDataType();
            autoDisable = scanner.getAutoDisable();
            if (scanDataLabel.length > 0) {
                System.out.println("Item Count"+Integer.toString(++itemDataCount));
            }
            // this setting of the DataEventEnable is because the tester
            // doesn't want to have to continually check the box after
            // every single scan
            scanner.setDataEventEnabled(true);
            dataEventEnabled = scanner.getDataEventEnabled();

            if (scanner.getClaimed()) {
                deviceEnabled = scanner.getDeviceEnabled();
            }

            freezeEvents = scanner.getFreezeEvents();
            decodeData = scanner.getDecodeData();
            System.out.println("Data Count: "+Integer.toString(scanner.getDataCount()));
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

    private static String convertResponseCodeToString(int resCode) {
        String codeString = "";
        switch (resCode) {
            case 0x00:
                codeString = "OK";
                break;
            case 0x01:
                codeString = "NOT-OK";
                break;
            case 0x06:
                codeString = "ACK";
                break;
            case 0x15:
                codeString = "NAK";
                break;
            case 0x17:
                codeString = "ETB";
                break;
            case 0x18:
                codeString = "CAN";
                break;
            case 0x07:
                codeString = "BEL";
                break;
            case 0xFF:
                codeString = "UKN";
                break;
            case 0x54: // 'T'
                codeString = "OK: 'T' Toggle mode activated";
                break;
            case 0x74: // 't'
                codeString = "OK: 't' Toggle mode deactivated";
                break;
            case -1: // 't'
                codeString = "Error: " + resCode;
                break;

            default:
                codeString = "Response unknown";
                break;
        }
        return codeString;
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
            case DeviceErrorStatusListener.ERR_DEVICE_REATTACHED:
                codeString = "ERR_DEVICE_REATTACHED";
                break;
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
}
