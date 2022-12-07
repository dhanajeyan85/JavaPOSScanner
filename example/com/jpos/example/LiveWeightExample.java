
package com.jpos.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import jpos.JposException;
import jpos.Scale;
import jpos.ScaleConst;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;

/**
 * LiveWeightExample class demonstrates a simple implementation of JavaPOS
 * that connects to a scale and receives live weight data.
 *
 * JavaPOS is the implementation of the jpos specification. As such, all POS
 * applications will interact with jpos defined interfaces that will invoke the
 * JavaPOS implementations of those methods.
 */
public class LiveWeightExample implements StatusUpdateListener {

    private Scale scale;
    private boolean bAsyncMode = false;
    private boolean bUseFiveDigits = false;
    private String sUnits = "";


    public LiveWeightExample() {
        //create a new generic jpos scale instance.
        scale = new Scale();
    }

    public static void main(String[] args) {
        LiveWeightExample example = new LiveWeightExample();
        //the jpos.xml profile name being used for this example. profile names
        //refer to the logicalName field under each entry.
        String jposProfileName = "DL-Magellan-9800i-USB-OEM-Scale";
        if (!example.connectScale(jposProfileName)) {
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

            //disconnect scale and exit program
            example.disconnectScale();
            System.out.println("Exiting...");
            System.exit(0);
        }
    }

    /**
     * Connects scale device, enabling live weight reading.
     *
     * Should always follow flow of:
     * open -> claim -> enable status notify -> enable
     *
     * @param profile String containing jpos.xml profile for scale
     * @return boolean indicating connection status
     */
    public boolean connectScale(String profile) {
        System.out.println("INFO: Connecting to scale...");

        //open the jpos.xml profile for the desired scale
        try {
            scale.open(profile);
        } catch (JposException je) {
            System.err.println("ERROR: Failed to open " + profile + " profile, " + je);
            return false;
        }

        //claim the scale with a timeout of one second.
        try {
            scale.claim(1000);
        } catch (JposException je) {
            //handle failed claim here
            closeScale();
            System.err.println("ERROR: Failed to claim, " + je);
            return false;
        }
        
        //get configuration data
        getScaleInfo();

        //enables status notify for the scale instance. This must be done before
        //enabling the scale for live weight to start.
        try {
            scale.setStatusNotify(ScaleConst.SCAL_SN_ENABLED);
        } catch (JposException je) {
            //handle failed status notify enable here, should release the scale
            //before calling close. For this example, we just call close.
            closeScale();
            System.err.println("ERROR: Failed to enable status notify, " + je);
            return false;
        }

        //enable weight reading for scale device.
        try {
            scale.setDeviceEnabled(true);
        } catch (JposException je) {
            //handle failed enable here, should release the scale before
            //calling close. For this example, we just call close.
            closeScale();
            System.err.println("ERROR: Failed to enable, " + je);
            return false;
        }

        //add this class as a status update listener for the scale to recieve
        //live weight data
        scale.addStatusUpdateListener(this);

        System.out.println("INFO: Scale connected.");
        return true;
    }

    /**
     * Disconnects scale device, disabling live weight reading.
     *
     * Should always follow flow of:
     * disable status notify -> disable -> release -> close
     *
     */
    public void disconnectScale() {
        System.out.println("INFO: Disconnecting scale...");

        //remove this class as a status update event listener.
        scale.removeStatusUpdateListener(this);
        
        //for this example, going to ignore exception handling. For actual
        //applications, a similar format of handling each statement found in
        //connectScale() should be followed.
        try {
            scale.setStatusNotify(ScaleConst.SCAL_SN_DISABLED);
            scale.setDeviceEnabled(false);
            scale.release();
            scale.close();
        } catch (JposException je) {
            //ignoring exceptions for this example
        }

        System.out.println("INFO: Scale disconnected.");
    }

    /**
     * Convenience method for this example.
     */
    private void closeScale() {
        try {
            scale.close();
        } catch (JposException je) {
            //ignoring exception for this example
        }
    }

    @Override
    public void statusUpdateOccurred(StatusUpdateEvent sue) {
        int nStatus = sue.getStatus();

        switch (nStatus) {
            case ScaleConst.SCAL_SUE_STABLE_WEIGHT:
                int weight = 0;
                try {
                    weight = scale.getScaleLiveWeight();
                } catch (JposException je) {
                    System.err.println("ERROR: could not get weight data, "
                            + je);
                    break;
                }
                //verify that asynchronous mode is not set since it is not
                //currently supported
                if (!bAsyncMode) {
                    //format weight data from raw integer
                    DecimalFormat formatter = new DecimalFormat(
                            "Stable Weight: 0.00 " + sUnits);
                    if (bUseFiveDigits) {
                        formatter.setMinimumFractionDigits(3);
                    }
                    System.out.println(formatter.format((float) weight / 1000));
                }
                break;
            case ScaleConst.SCAL_SUE_WEIGHT_OVERWEIGHT:
                System.out.println("Over Weight: --.--");
                break;
            case ScaleConst.SCAL_SUE_WEIGHT_UNDER_ZERO:
                System.out.println("Under Zero: --.--");
                break;
            case ScaleConst.SCAL_SUE_WEIGHT_UNSTABLE:
                System.out.println("Unstable Weight: --.--");
                break;
            case ScaleConst.SCAL_SUE_WEIGHT_ZERO:
                System.out.println("Zero Weight: 0");
                break;
            case ScaleConst.SCAL_SUE_NOT_READY:
                System.out.println("Scale not Ready: --.--");
                break;
            default:
                break;

        }
    }

    /**
     * Convenience method for this example.
     */
    private void getScaleInfo() {
        try {
            //scale asynchronous mode
            bAsyncMode = scale.getAsyncMode();
            //scale digit length
            bUseFiveDigits = (Integer.toString(
                    scale.getMaximumWeight()).length() == 5 ? true : false);
            //scale units
            sUnits = getUnitName(scale.getWeightUnit());
        } catch (JposException je) {
            System.err.println("ERROR: could not get scale info, " + je);
        }
    }

    /**
     * Convenience method that decodes the weight unit name from the unit code.
     * 
     * @param code int indicating the unit code
     * @return String containing the unit name
     */
    private String getUnitName(int code) {
        String sName = "";
        switch (code) {
            case ScaleConst.SCAL_WU_GRAM:
                sName = "grams";
                break;
            case ScaleConst.SCAL_WU_KILOGRAM:
                sName = "kilograms";
                break;
            case ScaleConst.SCAL_WU_OUNCE:
                sName = "ounces";
                break;
            case ScaleConst.SCAL_WU_POUND:
                sName = "pounds";
                break;
            default:
                sName = "pounds";
                break;
        }
        return sName;
    }

}
