/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.apache.logging.log4j.LogManager;

/**
 * Unit tests for SystemProcessor class
 */
public class Test_SystemProcessor {

    private static final Logger LOGGER = LogManager.getLogger("TESTER");

    /**
     * This is not an actual unit test - run this in order to test Console
     */

    boolean _primitiveStopFlag = false;

    @Test
    public void primitive(
    ) throws MaxNodesException  {
        SystemProcessor p = InventoryManager.getInstance().createSystemProcessor();
        _primitiveStopFlag = false;
        long lastStamp = System.currentTimeMillis();
        int inputCount = 0;
        LOGGER.info("Starting");
        p.consoleSendReadOnlyMessage("-- Console Interaction Test Starts --");
        p.consoleSendReadOnlyMessage("Enter H for help, Q to quit");

        while (!_primitiveStopFlag) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastStamp;
            if (elapsed > 5 * 1000) {
                String[] sysmsg = new String [2];
                sysmsg[0] = String.format("Elapsed: %dms", elapsed);
                sysmsg[1] = String.format("Inputs: %d", inputCount);
                p.consoleSendStatusMessage(sysmsg);
                LOGGER.info(sysmsg[0]);
                lastStamp = now;
            }

//            String input = p.consolePollInputMessage();
//            if (input != null) {
//                processInput(p, input.trim().toUpperCase());
//            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }

        p.consoleSendReadOnlyMessage("-- Console Interaction Test Ends --");
        LOGGER.info("Ending");
    }

    public void processInput(
        SystemProcessor p,
        String input
    ) {
        LOGGER.info(String.format("Input:%s", input));
        if (input.equals("H")) {
            p.consoleSendReadOnlyMessage("Yes, you do need help");
        } else if (input.equals("Q")) {
            _primitiveStopFlag = true;
        } else {
            p.consoleSendReadOnlyMessage("What?");
        }
    }

    //  TODO Need a variety of unit tests here
}
