package com.jpos.example;

import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpos.JposException;
import jpos.Scanner;
import jpos.events.DataEvent;
import jpos.events.DataListener;
import jpos.events.ErrorEvent;
import jpos.events.ErrorListener;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author gturpin
 */
public class HebTest 
        implements StatusUpdateListener, DataListener, ErrorListener {

    static HebTest m_test = null;
    static Scanner m_scanner = null;

    @Override
    public void statusUpdateOccurred(StatusUpdateEvent sue) {
        System.out.println("A Status update occurred.");
    }

    @Override
    public void dataOccurred(DataEvent de) {
        System.out.println("Data occurred.");
    }

    @Override
    public void errorOccurred(ErrorEvent ee) {
        System.out.println("An error occurred. ErrorCode: " +
                Integer.toString(ee.getErrorCode()));
    }

    public static void main(String[] args) {
        ByteArrayOutputStream oResults = new ByteArrayOutputStream();
        String sLogical = "Scanner-O";
        int[] ai_ints = new int[16];

        m_test = new HebTest();
        m_scanner = new Scanner();
        try {
            m_scanner.open(sLogical);
        } catch (JposException ex) {
            System.out.println("Failed to open scanner: " + ex.getMessage());
            System.exit(0);
        }

        m_scanner.addErrorListener(m_test);
        m_scanner.addDataListener(m_test);
        m_scanner.addStatusUpdateListener(m_test);

        try {
            m_scanner.claim(1000);
        } catch (JposException ex) {
            System.out.println("Failed to claim scanner: " + ex.getMessage());
            System.exit(0);
        }

        try {
            m_scanner.setDeviceEnabled(true);
        } catch (JposException ex) {
            System.out.println("Failed to enable scanner: " + ex.getMessage());
            System.exit(0);
        }
        try {
            m_scanner.compareFirmwareVersion("C:\\Temp\\dlsfw-O.dat", ai_ints);
        } catch (JposException ex) {
            System.out.println("Failed to compare firmware: " + ex.toString());
            System.exit(0);
        }

        try {
            m_scanner.setDeviceEnabled(false);
        } catch (JposException ex) {
            System.out.println("Failed to disable scanner: " + ex.getMessage());
            System.exit(0);
        }

        try {
            m_scanner.release();
        } catch (JposException ex) {
            System.out.println("Failed to release scanner: " + ex.getMessage());
            System.exit(0);
        }
        try {
            m_scanner.close();
        } catch (JposException ex) {
            System.out.println("Failed to close scanner: " + ex.getMessage());
            System.exit(0);
        }

        System.out.println("HebTest complete!");
        System.exit(0);
    }
}
