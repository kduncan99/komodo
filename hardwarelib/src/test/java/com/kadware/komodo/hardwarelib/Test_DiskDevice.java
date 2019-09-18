/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Unit tests for Device class
 */
public class Test_DiskDevice {

    private static class TestDevice extends DiskDevice {

        private TestDevice(
            final String name
        ) {
            super(Model.FileSystemDisk, name);
        }

        @Override
        public boolean canConnect(Node candidateAncestor) { return true; }

        @Override
        public boolean handleIo(IOInfo ioInfo) { return true; }

        @Override
        public boolean hasByteInterface() { return true; }

        @Override
        public boolean hasWordInterface(
        ) {
            return false;
        }

        @Override
        public void initialize(){}

        @Override
        public void ioGetInfo(IOInfo ioInfo) {}

        @Override
        public void ioRead(IOInfo ioInfo) {}

        @Override
        public void ioReset(IOInfo ioInfo) {}

        @Override
        public void ioUnload(IOInfo ioInfo) {}

        @Override
        public void ioWrite(IOInfo ioInfo) {}

        @Override
        public void terminate(){}

        @Override
        public void writeBuffersToLog(IOInfo ioInfo) {}
    }

    @Test
    public void create(
    ) {
        TestDevice device = new TestDevice("DISK01");
        assertEquals(Node.NodeCategory.Device, device._category);
        assertEquals("DISK01", device._name);
        Assert.assertEquals(Device.Type.Disk, device._deviceType);
        assertEquals(Device.Model.FileSystemDisk, device._deviceModel);
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

    //TODO need more tests here,  maybe
}
