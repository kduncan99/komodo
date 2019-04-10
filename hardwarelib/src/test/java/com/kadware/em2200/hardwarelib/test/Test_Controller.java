/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test;

import java.util.Map;

import static org.junit.Assert.*;
import org.junit.*;

import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.FileSystemDiskDevice;
import com.kadware.em2200.hardwarelib.Node;
import com.kadware.em2200.hardwarelib.Controller;
import com.kadware.em2200.hardwarelib.Device;

/**
 * Unit tests for Controller class
 */
public class Test_Controller {

    private static class TestController extends Controller {

        public TestController(
            final String name,
            final short subsystemIdentifier
        ) {
            super(ControllerType.ByteDisk, name, subsystemIdentifier);
        }

        @Override
        public boolean canConnect(
            final Node ancestor
        ) {
            return false;
        }

        @Override
        public void initialize(){}

        @Override
        public void signal(
            final Node source
        ){
        }

        @Override
        public void terminate(){}
    }

    private static class TestDevice extends FileSystemDiskDevice {
        public TestDevice (
            final String name,
            final short subsystemIdentifier
        ) {
            super(name, subsystemIdentifier);
        }

        @Override
        public boolean canConnect(
            final Node ancestor
        ) {
            return true;
        }
    }

    @Test
    public void create(
    ) {
        TestController c = new TestController("DSKCTL", (short)25);
        assertEquals(Node.Category.Controller, c.getCategory());
        assertEquals("DSKCTL", c.getName());
        assertEquals(Controller.ControllerType.ByteDisk, c.getControllerType());
        assertEquals(25, c.getSubsystemIdentifier());
    }

    @Test
    public void routeIo_failure(
    ) throws CannotConnectException {
        TestController c = new TestController("DSKCTL", (short)25);
        TestDevice d = new TestDevice("DISK01", (short)25);
        Node.connect(c, d);

        Device.IOInfo ioInfo = new Device.IOInfo(c, Device.IOFunction.None);
        c.routeIo(20, ioInfo);
        assertEquals(Device.IOStatus.InvalidDeviceAddress, ioInfo.getStatus());
    }

    @Test
    public void routeIo_success(
    ) throws CannotConnectException {
        TestController c = new TestController("DSKCTL", (short)25);
        TestDevice d = new TestDevice("DISK01", (short)25);
        Node.connect(c, d);

        Device.IOInfo ioInfo = new Device.IOInfo(c, Device.IOFunction.None);
        c.routeIo(0, ioInfo);
        assertEquals(Device.IOStatus.Successful, ioInfo.getStatus());
    }
}
