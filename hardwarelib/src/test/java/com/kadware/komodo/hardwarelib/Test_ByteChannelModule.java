/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import static org.junit.Assert.*;
import org.junit.*;

/**
 * Unit tests for ByteChannelModule class
 */
public class Test_ByteChannelModule {

    public static class TestModule extends ByteChannelModule {

        TestModule(String name) { super(name); }
        boolean isWorkerActive() { return _workerThread.isAlive(); }
    }

    @Test
    public void create(
    ) {
        ByteChannelModule cm = new ByteChannelModule("CM1-01");
        assertEquals(NodeCategory.ChannelModule, cm._category);
        assertEquals(ChannelModuleType.Byte, cm._channelModuleType);
        assertEquals("CM1-01", cm._name);
    }

    @Test
    public void canConnect_success(
    ) {
        ByteChannelModule cm = new ByteChannelModule("CM1-0");
        InputOutputProcessor iop = new InputOutputProcessor("IOP0", InventoryManager.FIRST_INPUT_OUTPUT_PROCESSOR_UPI_INDEX);
        assertTrue(cm.canConnect(iop));
    }

    @Test
    public void canConnect_failure(
    ) {
        ByteChannelModule cm = new ByteChannelModule("CM1-0");
        assertFalse(cm.canConnect(new FileSystemDiskDevice("DISK0")));
        assertFalse(cm.canConnect(new FileSystemTapeDevice("TAPE0")));
        assertFalse(cm.canConnect(new ByteChannelModule("CM1-0")));
        assertFalse(cm.canConnect(new WordChannelModule("CM1-1")));
        assertFalse(cm.canConnect(new MainStorageProcessor("MSP0",
                                                           InventoryManager.FIRST_MAIN_STORAGE_PROCESSOR_UPI_INDEX,
                                                           InventoryManager.MAIN_STORAGE_PROCESSOR_SIZE)));
        assertFalse(cm.canConnect(new InstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI_INDEX)));
    }

    @Test
    public void threadAlive_false_1(
    ) {
        TestModule cm = new TestModule("CM1-01");
        assertFalse(cm.isWorkerActive());
    }

    @Test
    public void threadAlive_false_2(
    ) {
        TestModule cm = new TestModule("CM1-01");
        cm.initialize();

        try {
            Thread.sleep(1000);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        cm.terminate();
        assertFalse(cm.isWorkerActive());
    }

    @Test
    public void threadAlive_true(
    ) {
        TestModule cm = new TestModule("CM1-01");
        cm.initialize();

        try {
            Thread.sleep(1000);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        assertTrue(cm.isWorkerActive());
        cm.terminate();
    }
}
