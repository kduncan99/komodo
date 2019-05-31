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
public class Test_TapeController {

    @Test
    public void create(
    ) {
        TapeController c = new TapeController("TAPCTL", (short)25);
        assertEquals(Node.Category.Controller, c.getCategory());
        assertEquals("TAPCTL", c.getName());
        assertEquals(Controller.ControllerType.Tape, c.getControllerType());
        assertEquals(25, c.getSubsystemIdentifier());
    }

    //????
//    @Test
//    public void canConnect_success(
//    ) {
//        TapeController c = new TapeController("TAPCUA", (short)0);
//        SoftwareByteChannelModule cm = new SoftwareByteChannelModule("CM1-0");
//        assertTrue(c.canConnect(cm));
//    }

    @Test
    public void canConnect_failure(
    ) throws IllegalAccessException,
             InstantiationException,
             NoSuchMethodException {
        TapeController c = new TapeController("TAPCUA", (short)0);
        assertFalse(c.canConnect(new FileSystemDiskDevice("DISK", (short)0)));
        assertFalse(c.canConnect(new FileSystemTapeDevice("TAPE0", (short)0)));
        assertFalse(c.canConnect(new ByteDiskController("DSKCTL", (short)0)));
        assertFalse(c.canConnect(new WordDiskController("DSKCTL", (short)0)));
        assertFalse(c.canConnect(new TapeController("TAPCUB", (short)0)));
//????        assertFalse(c.canConnect(new SoftwareWordChannelModule("CM1-1")));
        assertFalse(c.canConnect(new StaticMainStorageProcessor("MSP0",
                                                                InventoryManager.FIRST_MAIN_STORAGE_PROCESSOR_UPI,
                                                                InventoryManager.MAIN_STORAGE_PROCESSOR_SIZE)));
        assertFalse(c.canConnect(new InputOutputProcessor("IOP0", InventoryManager.FIRST_INPUT_OUTPUT_PROCESSOR_UPI)));
        assertFalse(c.canConnect(new InstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI)));
    }
}
