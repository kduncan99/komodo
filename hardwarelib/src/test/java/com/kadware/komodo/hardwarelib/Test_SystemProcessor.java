/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import java.util.Random;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.apache.logging.log4j.LogManager;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for SystemProcessor class
 */
public class Test_SystemProcessor {

    private static final Logger LOGGER = LogManager.getLogger("TESTER");

    @Test
    public void create(
    ) throws MaxNodesException  {
        SystemProcessor p = InventoryManager.getInstance().createSystemProcessor(2200);
        InstructionProcessor ip0 = InventoryManager.getInstance().createInstructionProcessor();
        InstructionProcessor ip1 = InventoryManager.getInstance().createInstructionProcessor();
        InputOutputProcessor iop = InventoryManager.getInstance().createInputOutputProcessor();
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        Random r = new Random(System.currentTimeMillis());
        while (true) {
            LOGGER.trace(String.format("%d", System.currentTimeMillis()));
            try {
                Thread.sleep(Math.abs(r.nextInt() % 10) * 1000);
            } catch (InterruptedException ex) {
            }
        }
    }
}
