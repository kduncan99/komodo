/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import java.nio.ByteBuffer;
import static org.junit.Assert.*;

import com.kadware.komodo.hardwarelib.Device;
import com.kadware.komodo.hardwarelib.Node;
import org.junit.*;

/**
 * Unit tests for Device class
 */
public class Test_Device {

//    private static class TestDevice extends Device {
//
//        public TestDevice(
//            final String name,
//            final short subsystemIdentifier
//        ) {
//            super(DeviceType.Disk, DeviceModel.FileSystemDisk, name, subsystemIdentifier);
//        }
//
//        @Override
//        public boolean canConnect(
//            final Node candidateAncestor
//        ) {
//            return true;
//        }
//
//        @Override
//        public void handleIo(
//            final IOInfo ioInfo
//        ) {
//        }
//
//        @Override
//        public boolean hasByteInterface(
//        ) {
//            return true;
//        }
//
//        @Override
//        public boolean hasWordInterface(
//        ) {
//            return false;
//        }
//
//        @Override
//        public void initialize(){}
//
//        @Override
//        public void signal(
//            final Node source
//        ){
//        }
//
//        @Override
//        public void terminate(){}
//
//        @Override
//        public void writeBuffersToLog(
//            final IOInfo ioInfo
//        ) {
//
//        }
//    }
//
//    @Test
//    public void create(
//    ) {
//        Device device = new TestDevice("DISK01", (short)25);
//        assertEquals(Node.Category.Device, device.getCategory());
//        assertEquals("DISK01", device.getName());
//        assertEquals(Device.DeviceType.Disk, device.getDeviceType());
//        assertEquals(Device.DeviceModel.FileSystemDisk, device.getDeviceModel());
//        assertEquals(25, device.getSubsystemIdentifier());
//        assertEquals(false, device.isReady());
//        assertEquals(false, device.getUnitAttentionFlag());
//    }
//
//    @Test
//    public void deviceInfo_SerializeDeserialize(
//    ) {
//        Device.DeviceInfo info = new Device.DeviceInfo(Device.DeviceType.Symbiont,
//                                                       Device.DeviceModel.FileSystemPrinter,
//                                                       false,
//                                                       (short)10,
//                                                       true);
//        byte[] buffer = new byte[256];
//        ByteBuffer bb1 = ByteBuffer.wrap(buffer);
//        info.serialize(bb1);
//
//        ByteBuffer bb2 = ByteBuffer.wrap(buffer);
//        Device.DeviceInfo newInfo = new Device.DeviceInfo();
//        newInfo.deserialize(bb2);
//
//        assertTrue(info.equals(newInfo));
//    }
//
//    @Test
//    public void setReady_False(
//    ) {
//        Device device = new TestDevice("DISK01", (short)25);
//        device.setReady(true);
//        assertTrue(device.setReady(false));
//        assertFalse(device.isReady());
//        assertFalse(device.getUnitAttentionFlag());
//    }
//
//    @Test
//    public void setReady_True(
//    ) {
//        Device device = new TestDevice("DISK01", (short)25);
//        assertTrue(device.setReady(true));
//        assertTrue(device.isReady());
//        assertTrue(device.getUnitAttentionFlag());
//    }
//
//    @Test
//    public void IOFunction_isRead(
//    ) {
//        assertTrue(Device.IOFunction.Read.isReadFunction());
//        assertTrue(Device.IOFunction.ReadBackward.isReadFunction());
//        assertFalse(Device.IOFunction.Write.isReadFunction());
//    }
//
//    @Test
//    public void IOFunction_isWrite(
//    ) {
//        assertFalse(Device.IOFunction.Read.isWriteFunction());
//        assertTrue(Device.IOFunction.Write.isWriteFunction());
//        assertTrue(Device.IOFunction.WriteEndOfFile.isWriteFunction());
//    }
}
