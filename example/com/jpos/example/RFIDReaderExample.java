package com.jpos.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import jpos.JposException;
import jpos.RFIDScanner;
import jpos.RFIDScannerConst;
import jpos.events.DataEvent;
import jpos.events.DataListener;
import jpos.events.ErrorEvent;
import jpos.events.ErrorListener;
import jpos.events.OutputCompleteEvent;
import jpos.events.OutputCompleteListener;

/**
 * RFIDReaderExample class demonstrates a simple implementation of JavaPOS
 * that connects to a RFID scanner and receives tag data.
 *
 * JavaPOS is the implementation of the jpos specification. As such, all POS
 * applications will interact with jpos defined interfaces that will invoke the
 * JavaPOS implementations of those methods.
 */
public class RFIDReaderExample implements DataListener, OutputCompleteListener, ErrorListener {

    private static final int NUMBER_COLUMN_WIDTH = 2;
    private static final int ID_COLUMN_WIDTH = 24;
    private static final int PROTOCOL_COLUMN_WIDTH = 18;
    private static final String SEP = System.getProperty("line.separator");
    private volatile boolean canStartNewRead = true;
    
    private RFIDScanner scanner;

    public RFIDReaderExample() {
        //create a new generic jpos scanner instance.
        scanner = new RFIDScanner();
    }

    public static void main(String[] args) {
        RFIDReaderExample example = new RFIDReaderExample();
        //the jpos.xml profile name being used for this example. profile names
        //refer to the logicalName field under each entry.
        String jposProfileName = "DL-RFID-DK001";
        if (!example.connectScanner(jposProfileName)) {
            System.exit(1);
        }
        
        //run until enter is pressed
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String userInput;
        boolean contMode = false;
        System.out.println("");
        System.out.println("Press 1 for single read");
        System.out.println("Press 2 to start/stop continuous read");
        System.out.println("Press enter to exit");
        while (true) {
            try {
                //block until enter is pressed
                userInput = br.readLine();
                
            } catch (IOException ioe) {
                System.err.println("ERROR: " + ioe);
                break;
            }

            if (userInput.contains("1")) {
                example.startSingleRead();
            } else if (userInput.contains("2")) {
                if (contMode) {
                    example.stopContinuousRead();
                    contMode = false;
                } else {
                    if (example.startContinuousRead(1000)) { //use 1 second to allow reading of the data on the console
                        contMode = true;
                    }
                }
            } else {
                break;
            }
            
        }
        //disconnect scanner and exit program
        if (contMode) {
            example.stopContinuousRead();
        }
        example.disconnectScanner();
        System.out.println("Exiting...");
        System.exit(0);
    }

    /**
     * Connects scanning device, enabling tag reading.
     *
     * Should always follow flow of open -> claim -> enable -> enable data
     *
     * @param profile String containing jpos.xml profile for scanner
     * @return boolean indicating connection status
     */
    public boolean connectScanner(String profile) {
        System.out.println("INFO: Connecting to scanner...");

        //open the jpos.xml profile for the desired scanner
        try {
            scanner.open(profile);
        } catch (JposException je) {
            System.err.println("ERROR: Failed to open " + profile + " profile, " + je);
            return false;
        }

        //claim the scanner with a timeout of 5 seconds.
        try {
            scanner.claim(5000);
        } catch (JposException je) {
            //handle failed claim here
            closeScanner();
            System.err.println("ERROR: Failed to claim, " + je);
            return false;
        }

        //enable tag reading for scanning device.
        try {
            scanner.setDeviceEnabled(true);
        } catch (JposException je) {
            //handle failed enable here, should release the scanner before
            //calling close. For this example, we just call close.
            closeScanner();
            System.err.println("ERROR: Failed to enable, " + je);
            return false;
        }

        //enable data events for the scanner instance. This is necessary to
        //retrieve the tag data.
        try {
            scanner.setDataEventEnabled(true);
        } catch (JposException je) {
            //handle failed enable here, should disable and release the
            //scanner before calling close. For this example, we just call
            //close.
            closeScanner();
            System.err.println("ERROR: Failed to enable data, " + je);
            return false;
        }

        //add this class as a data listener for the scanner to receive tag
        //data events.
        scanner.addDataListener(this);

        System.out.println("INFO: Scanner connected.");
        return true;
    }

    /**
     * Disconnects scanning device, disabling tag reading.
     *
     * Should always follow flow of disable -> release -> close
     * (though you could choose not to disable device before releasing on
     * certain interfaces)
     *
     */
    public void disconnectScanner() {
        System.out.println("INFO: Disconnecting scanner...");

        //remove this class as a data event listener.
        scanner.removeDataListener(this);
        
        //for this example, going to ignore exception handling. For actual
        //applications, a similar format of handling each statement found in
        //connectScanner() should be followed.
        try {
            scanner.setDeviceEnabled(false);
            scanner.release();
            scanner.close();
        } catch (JposException je) {
            //ignoring exceptions for this example
        }

        System.out.println("INFO: Scanner disconnected.");
    }

    /**
     * Convenience method for this example.
     */
    private void closeScanner() {
        try {
            scanner.close();
        } catch (JposException je) {
            //ignoring exception for this example
        }
    }

    /**
     * Initiates a single scan inventory on the scanner.
     */
    public void startSingleRead() {
        //load some default values for example
        int cmd = RFIDScannerConst.RFID_RT_ID_PARTIALUSERDATA;
        byte[] filterID = new byte[]{}; //empty byte array for no filtering
        byte[] filtermask = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}; //all zeros for matching all tags (empty byte array also accepted)
        int start = 0;
        int length = 4; //read a small amount of data for the example
        int timeout = -1; //wait as long as needed
        byte[] password = new byte[]{}; //empty for no password
        if (canStartNewRead) {
            try {
                scanner.readTags(cmd, filterID, filtermask, start, length, timeout, password);
                canStartNewRead = false;
            } catch (JposException je) {
                System.err.println("ERROR: JposException trying to start single read, " + je);
            }
        } else {
            System.err.println("ERROR: Cannot read tags until data or error event occur.");
        }
    }
    
    /**
     * Initiates the continuous read mode for the scanner if supported.
     * 
     * @param interval int indicating the polling time between reads
     * @return boolean indicating successful start of continuous mode
     */
    public boolean startContinuousRead(int interval) {
        System.out.println("INFO: Starting continuous read mode...");
        boolean success = false;
        //load some default values for example
        int cmd = RFIDScannerConst.RFID_RT_ID;
        byte[] filterID = new byte[]{}; //empty byte array for no filtering
        byte[] filtermask = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}; //all zeros for matching all tags (empty byte array also accepted)
        int start = 0; //not applicable for RFID_RT_ID
        int length = 4; //not applicable for RFID_RT_ID
        byte[] password = new byte[]{}; //empty for no password
        try {
            if (scanner.getCapReadTimer()) {
                scanner.setReadTimerInterval(interval);
            }
            if (scanner.getCapContinuousRead()) {
                if (!scanner.getContinuousReadMode()) {
                    scanner.startReadTags(cmd, filterID, filtermask, start, length, password);
                    System.out.println("INFO: Continuous read mode started.");
                    success = true;
                } else {
                    System.err.println("ERROR: Continuous mode already running.");
                }
            } else {
                System.err.println("ERROR: Device does not have continuous mode capability.");
            }
        } catch (JposException je) {
            System.err.println("ERROR: JposException trying to start continuous mode, " + je);
        }
        return success;
    }
    
    /**
     * Halts continuous read mode
     */
    public void stopContinuousRead() {
        System.out.println("INFO: Stopping continuous read mode...");
        //load some default values for example
        byte[] password = new byte[]{}; //empty for no password
        try {
            if (scanner.getCapContinuousRead()) {
                if (scanner.getContinuousReadMode()) {
                    scanner.stopReadTags(password);
                    System.out.println("INFO: Continuous read mode stopped.");
                } else {
                    System.err.println("ERROR: Continuous mode is not running.");
                }
            } else {
                System.err.println("ERROR: Device does not have continuous mode capability.");
            }
        } catch (JposException je) {
            System.err.println("ERROR: JposException trying to stop continuous mode, " + je);
        }
    }
    
    @Override
    public void dataOccurred(DataEvent de) {
        //Data event handler for tag reads from the scanner.
        byte[] tagID;
        byte[] tagUserData;
        int tagType;
        int tagCount = 0;
        StringBuilder sb = new StringBuilder();
        canStartNewRead = true;
        //build the header
        sb.append(padRight("#", NUMBER_COLUMN_WIDTH + 1));
        sb.append(padRight("TAG_ID", ID_COLUMN_WIDTH + 1));
        sb.append(padRight("TAG_PROTOCOL", PROTOCOL_COLUMN_WIDTH + 1));
        sb.append("TAG_DATA");
        sb.append(SEP);
        try {
            tagCount = scanner.getTagCount();
            for (int i = 0; i < tagCount; i++) {
                tagID = scanner.getCurrentTagID(); //Get tag ID data
                tagUserData = scanner.getCurrentTagUserData(); //get tag user data
                tagType = scanner.getCurrentTagProtocol(); //get tag protocol
                sb.append(padRight(Integer.toString(i), NUMBER_COLUMN_WIDTH));
                sb.append(",");
                sb.append(formatTagData(tagID, tagType, tagUserData));
                sb.append(SEP);
                if (i < tagCount - 1) {
                    //move to next tag in the list
                    scanner.nextTag();
                }
            }
        } catch (JposException je) {
            System.err.println("ERROR: JposException during DataEvent, " + je);
        }

        System.out.println("Tag Count = " + Integer.toString(tagCount));
        if (tagCount > 0) {
            System.out.println(sb.toString());
        }

        //data events are auto-disabled after event trigger, must re-enable
        //to get future tag reads
        try {
            scanner.setDataEventEnabled(true);
        } catch (JposException je) {
            System.err.println("ERROR: Could not enable data event after "
                    + "trigger, will be unable to receive tags: " + je);
        }
    }

    @Override
    public void outputCompleteOccurred(OutputCompleteEvent oce) {
        System.out.println("OutputCompleteEvent: ID = " + Integer.toString(oce.getOutputID()));
    }

    @Override
    public void errorOccurred(ErrorEvent ee) {
        canStartNewRead = true;
        System.err.println("ErrorEvent: code = " + Integer.toString(ee.getErrorCode()));
    }
    
    /**
     * Convenience method that decodes the tag class name from the
     * tag type.
     *
     * @param code int indicating the jpos class type code
     * @return String containing tag class name
     */
    private static String getTagProtocolName(int code) {
        switch (code) {
            case RFIDScannerConst.RFID_PR_EPC0:
                return "RFID_PR_EPC0";
            case RFIDScannerConst.RFID_PR_0PLUS:
                return "RFID_PR_0PLUS";
            case RFIDScannerConst.RFID_PR_EPC1:
                return "RFID_PR_EPC1";
            case RFIDScannerConst.RFID_PR_EPC1G2:
                return "RFID_PR_EPC1G2";
            case RFIDScannerConst.RFID_PR_EPC2:
                return "RFID_PR_EPC2";
            case RFIDScannerConst.RFID_PR_ISO14443A:
                return "RFID_PR_ISO14443A";
            case RFIDScannerConst.RFID_PR_ISO14443B:
                return "RFID_PR_ISO14443B";
            case RFIDScannerConst.RFID_PR_ISO15693:
                return "RFID_PR_ISO15693";
            case RFIDScannerConst.RFID_PR_ISO180006B:
                return "RFID_PR_ISO180006B";
            case RFIDScannerConst.RFID_PR_OTHER:
                return "RFID_PR_OTHER";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Convenience method for formatting the data to output to the console.
     * 
     * @param id byte array containing the tag ID
     * @param type int indicating the tag protocol
     * @param data byte array containing the user data
     * @return String containing the formatted data
     */
    private static String formatTagData(byte[] id, int type, byte[] data) {
        StringBuilder sb = new StringBuilder();
        sb.append(padRight(byteArrayToHexString(id), ID_COLUMN_WIDTH));
        sb.append(",");
        sb.append(padRight(getTagProtocolName(type), PROTOCOL_COLUMN_WIDTH));
        sb.append(",");
        sb.append(byteArrayToHexString(data));
        return sb.toString();
    }

    /**
     * Convenience method for console alignment. Pads strings with leading white space.
     * 
     * @param text String containing text to pad
     * @param length int indicating the minimum length of the string
     * @return String containing padded text
     */
    private static String padLeft(String text, int length) {
        return String.format("%" + length + "." + length + "s", text);
    }

    /**
     * Convenience method for console alignment. Pads strings with trailing white space.
     * 
     * @param text String containing text to pad
     * @param length int indicating the minimum length of the string
     * @return String containing padded text
     */
    private static String padRight(String text, int length) {
        return String.format("%-" + length + "." + length + "s", text);
    }

    /**
     * Formats raw bytes into human readable format for example.
     * 
     * @param data byte array containing data
     * @return String containing hex data in the form {@code <byte1><byte2>...}
     */
    private static String byteArrayToHexString(byte[] data) {
        if (data == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

}
