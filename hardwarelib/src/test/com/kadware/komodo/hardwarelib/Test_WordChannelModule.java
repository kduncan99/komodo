/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Unit tests for WordChannelModule class
 */
public class Test_WordChannelModule {

    public static class TestModule extends WordChannelModule {

        public TestModule(final String name) { super(name); }
        public boolean isWorkerActive() { return _workerThread.isAlive(); }
    }

    private final Logger LOGGER = LogManager.getLogger(ByteChannelModule.class);

    @Test
    public void create(
    ) {
        ByteChannelModule cm = new ByteChannelModule("CM1-01");
        assertEquals(Node.NodeCategory.ChannelModule, cm._category);
        assertEquals(ChannelModule.ChannelModuleType.Byte, cm._channelModuleType);
        assertEquals("CM1-01", cm._name);
    }

    @Test
    public void canConnect_success(
    ) {
        WordChannelModule cm = new WordChannelModule("CM1-1");
        assertTrue(cm.canConnect(new InputOutputProcessor("IOP0", InventoryManager.FIRST_IOP_UPI_INDEX)));
    }

    @Test
    public void canConnect_failure(
    ) {
        WordChannelModule cm = new WordChannelModule("CM1-1");
        assertFalse(cm.canConnect(new FileSystemDiskDevice("DISK0")));
        assertFalse(cm.canConnect(new FileSystemTapeDevice("TAPE0")));
        assertFalse(cm.canConnect(new ByteChannelModule("CM1-0")));
        assertFalse(cm.canConnect(new WordChannelModule("CM1-1")));
        assertFalse(cm.canConnect(new MainStorageProcessor("MSP0",
                                                           InventoryManager.FIRST_MSP_UPI_INDEX,
                                                           MainStorageProcessor.MIN_FIXED_SIZE)));
        assertFalse(cm.canConnect(new InstructionProcessor("IP0", InventoryManager.FIRST_IP_UPI_INDEX)));
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
        Thread.onSpinWait();
        cm.terminate();
        assertFalse(cm.isWorkerActive());
    }

    @Test
    public void threadAlive_true(
    ) {
        TestModule cm = new TestModule("CM1-01");
        cm.initialize();
        Thread.onSpinWait();
        assertTrue(cm.isWorkerActive());
        cm.terminate();
    }
}
