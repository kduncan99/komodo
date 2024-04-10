/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Unit tests for Device class
 */
public class Test_Device {

    private static class TestDevice extends Device {

        TestDevice(
            final String name
        ) {
            super(NodeType.Disk, NodeModel.FileSystemDisk, name);
        }

        @Override
        public boolean canConnect(
            final Node candidateAncestor
        ) {
            return true;
        }

        @Override
        public boolean handleIo(IOInfo ioInfo) { return true; }

        @Override
        public boolean hasByteInterface() { return true; }

        @Override
        public boolean hasWordInterface() { return false; }

        @Override
        public void initialize() {}

        @Override
        public void terminate() {}

        @Override
        public void writeBuffersToLog(IOInfo ioInfo) {}
    }

    @Test
    public void create(
    ) {
        Device device = new TestDevice("DISK01");
        assertEquals(NodeCategory.Device, device._category);
        assertEquals("DISK01", device._name);
        assertEquals(NodeType.Disk, device._deviceNodeType);
        assertEquals(NodeModel.FileSystemDisk, device._deviceNodeModel);
        assertFalse(device._readyFlag);
        assertFalse(device._unitAttentionFlag);
    }

    @Test
    public void setReady_False(
    ) {
        Device device = new TestDevice("DISK01");
        device.setReady(true);
        assertTrue(device.setReady(false));
        assertFalse(device._readyFlag);
        assertFalse(device._unitAttentionFlag);
    }

    @Test
    public void setReady_True(
    ) {
        Device device = new TestDevice("DISK01");
        assertTrue(device.setReady(true));
        assertTrue(device._readyFlag);
        assertTrue(device._unitAttentionFlag);
    }
}
