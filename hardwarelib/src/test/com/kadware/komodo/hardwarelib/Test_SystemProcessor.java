/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.hardwarelib.exceptions.InvalidMessageIdException;
import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.apache.logging.log4j.LogManager;

/**
 * Unit tests for SystemProcessor class
 */
public class Test_SystemProcessor {

    private static final Logger LOGGER = LogManager.getLogger("TESTER");

    private static class ConsoleExerciser {

        private static final String padChars = "                                                                                ";
        private int _inputCount = 0;
        boolean _stop = false;

        public void exercise(
        ) throws MaxNodesException {
            SystemProcessor sp = InventoryManager.getInstance().createSystemProcessor();

            LOGGER.info("Console Exerciser Starting");
            sp.consoleSendReadOnlyMessage("-- Console Interaction Test Starts --");
            sp.consoleSendReadOnlyMessage("Enter H for help, Q to quit");

            Instant next5SecondAction = Instant.now().plusSeconds(5);
            Instant next5MinuteAction = Instant.now().plusSeconds(5 * 60);

            while (!_stop) {
                //  Any input?
                try {
                    String msg = sp.consolePollInputMessage();
                    //TODO do something with it
                } catch (InvalidMessageIdException ex) {
                    //TODO bitch about it
                }

                //  Do periodic things
                Instant now = Instant.now();
                if (!now.isBefore(next5SecondAction)) {
                    fiveSecondAction(sp, now);
                    next5SecondAction = now.plusSeconds(5);
                }

                if (!now.isBefore(next5MinuteAction)) {
                    fiveMinuteAction(sp, now);
                    next5MinuteAction = now.plusSeconds(5 * 60);
                }

                //  Sleep for a little bit
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    LOGGER.catching(ex);
                }
            }

            LOGGER.info("Console Exerciser Done");
        }

        public void fiveSecondAction(
            final SystemProcessor sp,
            final Instant now
        ) {
            String[] messages = new String[2];
            String timeStr = now.toString().split("\\.")[0];
            messages[0] = String.format("Time %s%s", timeStr, padChars).substring(0, 80);
            messages[1] = String.format("Inputs:%d  Jobs:%d%s", _inputCount, 0, padChars).substring(0, 80);
            sp.consoleSendStatusMessage(messages);
            LOGGER.info("Poll");
        }

        public void fiveMinuteAction(
            final SystemProcessor sp,
            final Instant now
        ) {
            String msg = String.format("%sT/D %s", padChars, now.toString());
            msg = msg.substring(msg.length() - 80);
            sp.consoleSendReadOnlyMessage(msg);
        }
    }

    //  Note that this ONLY works if you run THIS TEST CASE EXPLICITLY.
    //  If you try to run the module, the environment variables are not set up.
    @Test
    public void exercise(
    ) throws MaxNodesException {
        new ConsoleExerciser().exercise();
    }

    //  TODO Need a variety of unit tests here
}
