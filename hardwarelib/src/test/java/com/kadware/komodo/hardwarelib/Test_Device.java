/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.hardwarelib.Device;
import com.kadware.komodo.hardwarelib.Node;
import java.nio.ByteBuffer;
import org.junit.*;
import static org.junit.Assert.*;

/**
 * Unit tests for Device class
 */
public class Test_Device {

    private static class TestDevice extends Device {

        public TestDevice(
            final String name
        ) {
            super(DeviceType.Disk, DeviceModel.FileSystemDisk, name);
        }

        @Override
        public boolean canConnect(
            final Node candidateAncestor
        ) {
            return true;
        }

        @Override
        public boolean handleIo(DeviceIOInfo ioInfo) { return true; }

        @Override
        public boolean hasByteInterface() { return true; }

        @Override
        public boolean hasWordInterface() { return false; }

        @Override
        public void initialize() {}

        @Override
        public void terminate() {}

        @Override
        public void writeBuffersToLog(DeviceIOInfo ioInfo) {}
    }

    @Test
    public void create(
    ) {
        Device device = new TestDevice("DISK01");
        assertEquals(NodeCategory.Device, device._category);
        assertEquals("DISK01", device._name);
        assertEquals(DeviceType.Disk, device._deviceType);
        assertEquals(DeviceModel.FileSystemDisk, device._deviceModel);
        assertEquals(false, device._readyFlag);
        assertEquals(false, device._unitAttentionFlag);
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
