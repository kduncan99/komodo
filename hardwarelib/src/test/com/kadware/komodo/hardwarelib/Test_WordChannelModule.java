/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

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

    @Test
    public void create(
    ) {
        ByteChannelModule cm = new ByteChannelModule("CM1-01");
        assertEquals(Node.NodeCategory.ChannelModule, cm._category);
        assertEquals(ChannelModule.ChannelModuleType.Byte, cm._channelModuleType);
        assertEquals("CM1-01", cm._name);
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
