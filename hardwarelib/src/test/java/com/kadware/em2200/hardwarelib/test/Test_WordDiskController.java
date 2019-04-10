/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test;

import static org.junit.Assert.*;
import org.junit.*;

import com.kadware.em2200.hardwarelib.*;

/**
 * Unit tests for WordDiskController class
 */
public class Test_WordDiskController {

    @Test
    public void create(
    ) {
        WordDiskController c = new WordDiskController("DSKCTL", (short)25);
        assertEquals(Node.Category.Controller, c.getCategory());
        assertEquals("DSKCTL", c.getName());
        assertEquals(Controller.ControllerType.WordDisk, c.getControllerType());
        assertEquals(25, c.getSubsystemIdentifier());
    }

    @Test
    public void canConnect_success(
    ) {
        Controller c = new WordDiskController("DSKCUB", (short)0);
//????        assertTrue(c.canConnect(new SoftwareWordChannelModule("CM1-1")));
    }

    @Test
    public void canConnect_failure(
    ) throws IllegalAccessException,
             InstantiationException,
             NoSuchMethodException {
        Controller c = new WordDiskController("DSKCUB", (short)0);
        assertFalse(c.canConnect(new FileSystemDiskDevice("DISK0", (short)0)));
        assertFalse(c.canConnect(new FileSystemTapeDevice("TAPE0", (short)0)));
        assertFalse(c.canConnect(new ByteDiskController("DSKCTL", (short)0)));
        assertFalse(c.canConnect(new WordDiskController("DSKCTL", (short)0)));
        assertFalse(c.canConnect(new TapeController("TAPCUB", (short)0)));
//????        assertFalse(c.canConnect(new SoftwareByteChannelModule("CM1-0")));
        assertFalse(c.canConnect(new MainStorageProcessor("MSP0",
                                                          InventoryManager.FIRST_MAIN_STORAGE_PROCESSOR_UPI,
                                                          InventoryManager.MAIN_STORAGE_PROCESSOR_SIZE)));
        assertFalse(c.canConnect(new InputOutputProcessor("IOP0", InventoryManager.FIRST_INPUT_OUTPUT_PROCESSOR_UPI)));
        assertFalse(c.canConnect(new InstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI)));
    }
}
