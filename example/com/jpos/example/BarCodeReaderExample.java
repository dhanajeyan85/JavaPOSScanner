
package com.jpos.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import jpos.JposException;
import jpos.Scanner;
import jpos.ScannerConst;
import jpos.events.DataEvent;
import jpos.events.DataListener;

/**
 * BarCodeReaderExample class demonstrates a simple implementation of JavaPOS
 * that connects to a scanner and receives barcode data.
 *
 * JavaPOS is the implementation of the jpos specification. As such, all POS
 * applications will interact with jpos defined interfaces that will invoke the
 * JavaPOS implementations of those methods.
 */
public class BarCodeReaderExample implements DataListener {

    private Scanner scanner;

    public BarCodeReaderExample() {
        //create a new generic jpos scanner instance.
        scanner = new Scanner();
    }

    public static void main(String[] args) {
        BarCodeReaderExample example = new BarCodeReaderExample();
        //the jpos.xml profile name being used for this example. profile names
        //refer to the logicalName field under each entry.
        String jposProfileName = "DL-Gryphon-GD4430-USB-OEM";
        if (!example.connectScanner(jposProfileName)) {
            System.exit(1);
        }

        //run until enter is pressed
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.println("Press enter to exit");
            try {
                //block until enter is pressed
                br.readLine();
            } catch (IOException ioe) {
                System.err.println("ERROR: " + ioe);
            }

            //disconnect scanner and exit program
            example.disconnectScanner();
            System.out.println("Exiting...");
            System.exit(0);
        }
    }

    /**
     * Connects scanning device, enabling barcode reading.
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

        //claim the scanner with a timeout of one second.
        try {
            scanner.claim(1000);
        } catch (JposException je) {
            //handle failed claim here
            closeScanner();
            System.err.println("ERROR: Failed to claim, " + je);
            return false;
        }

        //enable barcode reading for scanning device.
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
        //retrieve the barcode data.
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

        //add this class as a data listener for the scanner to receive barcode
        //data events.
        scanner.addDataListener(this);

        System.out.println("INFO: Scanner connected.");
        return true;
    }

    /**
     * Disconnects scanning device, disabling barcode reading.
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


    @Override
    public void dataOccurred(DataEvent de) {
        //Data event handler for barcode reads from the scanner.
        byte[] scanData = new byte[]{};
        byte[] scanDataLabel = new byte[]{};
        int scanDataType = -1;
        try {
            scanData = scanner.getScanData(); //Get raw label data
            scanDataLabel = scanner.getScanDataLabel(); //get decoded label data
            scanDataType = scanner.getScanDataType(); //get label symbology
        } catch (JposException je) {
            System.err.println("ERROR JposException during DataEvent, " + je);
        }

        //Verify the triggered event was a label read
        if (scanDataLabel.length > 0) {
            String sScanData = new String(scanData);
            String sScanDataLabel = new String(scanDataLabel);
            String sType = getBarcodeTypeName(scanDataType);
            System.out.println("Raw Data: " + sScanData + ", Label Data: "
                    + sScanDataLabel + ", Type: " + sType);
        }

        //data events are auto-disabled after event trigger, must re-enable
        //to get future barcode reads
        try {
            scanner.setDataEventEnabled(true);
        } catch (JposException je) {
            System.err.println("ERROR: Could not enable data event after "
                    + "trigger, will be unable to receive barcodes: " + je);
        }
    }

    /**
     * Convenience method that decodes the barcode symbology name from the
     * barcode type.
     *
     * @param code int indicating the jpos symbology type code
     * @return String containing barcode symbology name
     */
    private String getBarcodeTypeName(int code) {
        String val = "";
        //this is just a quick representation of different symbologies that are
        //contained within ScannerConst. Please refer to ScannerConst for full
        //list.
        switch (code) {
            case ScannerConst.SCAN_SDT_UPCA:
                val = "UPC-A";
                break;
            case ScannerConst.SCAN_SDT_UPCE:
                val = " UPC-E";
                break;
            case ScannerConst.SCAN_SDT_Code39:
                val = "Code 39";
                break;
            case ScannerConst.SCAN_SDT_Code128:
                val = "Code 128";
                break;
            case ScannerConst.SCAN_SDT_EAN8_S:
                val = "EAN-8 with Supplemental";
                break;
            case ScannerConst.SCAN_SDT_EAN13_S:
                val = "EAN-13 with Supplemental";
                break;
            case ScannerConst.SCAN_SDT_EAN128:
                val = "EAN-128";
                break;
            case ScannerConst.SCAN_SDT_QRCODE:
                val = "QR Code";
                break;
            case ScannerConst.SCAN_SDT_OTHER:
                val = "Other";
                break;
            default:
                val = "Unknown";
        }
        return val;
    }
}
