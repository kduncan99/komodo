/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.hardwarelib.exceptions.CannotConnectException;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;
import org.junit.rules.*;
import org.junit.*;

/**
 * Unit tests for Node class
 */
public class Test_Node {

    @Rule
    public ExpectedException _exception = ExpectedException.none();

    private static class TestControllerNode extends Node {

        public TestControllerNode(
            final String name
        ) {
            super(Category.Controller, name);
        }

        @Override
        public boolean canConnect(
            final Node candidateAncestor
        ) {
            return false;
        }

        @Override
        public void initialize() {}


        @Override
        public void signal(
            final Node source
        ){
        }

        @Override
        public void terminate(){}
    }

    private static class TestDeviceNode extends Node {

        public TestDeviceNode(
            final String name
        ) {
            super(Category.Device, name);
        }

        @Override
        public boolean canConnect(
            final Node candidateAncestor
        ) {
            return candidateAncestor instanceof TestControllerNode;
        }

        @Override
        public void initialize() {}

        @Override
        public void signal(
            final Node source
        ){
        }

        @Override
        public void terminate(){}
    }

    @Test
    public void create(
    ) {
        _exception = ExpectedException.none();
        Node device = new TestDeviceNode("DISK01");
        assertEquals(Node.Category.Device, device.getCategory());
        assertEquals("DISK01", device.getName());
    }

    @Test
    public void connect_Success(
    ) throws CannotConnectException {
        _exception = ExpectedException.none();
        Node device = new TestDeviceNode("DISK01");
        Node controller = new TestControllerNode("DSKCTL");
        Node.connect(controller, device);

        assertEquals(true, controller.getAncestors().isEmpty());
        Map<Integer, Node> ctlDescendants = controller.getDescendants();
        assertEquals(1, ctlDescendants.size());
        assertEquals(device, ctlDescendants.get(0));

        assertEquals(true, device.getDescendants().isEmpty());
        Set<Node> devAncestors = device.getAncestors();
        assertEquals(1, devAncestors.size());
        assertTrue(devAncestors.contains(controller));
    }

    @Test
    public void connect_Multiple_Success(
    ) throws CannotConnectException {
        _exception = ExpectedException.none();
        Node device01 = new TestDeviceNode("DISK01");
        Node device02 = new TestDeviceNode("DISK02");
        Node device03 = new TestDeviceNode("DISK03");
        Node controller = new TestControllerNode("DSKCTL");
        Node.connect(controller, device01);
        Node.connect(controller, device02);
        Node.connect(controller, device03);

        Map<Integer, Node> ctlDescendants = controller.getDescendants();
        assertEquals(3, ctlDescendants.size());
        assertEquals(device01, ctlDescendants.get(0));
        assertEquals(device02, ctlDescendants.get(1));
        assertEquals(device03, ctlDescendants.get(2));

        assertTrue(device01.getAncestors().contains(controller));
        assertTrue(device02.getAncestors().contains(controller));
        assertTrue(device03.getAncestors().contains(controller));
    }

    @Test
    public void connect_Failure(
    ) throws CannotConnectException {
        _exception.expect(CannotConnectException.class);
        Node device = new TestDeviceNode("DISK01");
        Node controller = new TestControllerNode("DSKCTL");
        Node.connect(device, controller);
    }

    @Test
    public void disconnect(
    ) throws CannotConnectException {
        _exception = ExpectedException.none();
        Node device = new TestDeviceNode("DISK01");
        Node controller = new TestControllerNode("DSKCTL");
        Node.connect(controller, device);

        //  make sure things are the way we expect before trying to disconnect
        assertFalse(controller.getDescendants().isEmpty());
        assertFalse(device.getAncestors().isEmpty());

        Node.disconnect(controller, device);
        assertTrue(controller.getDescendants().isEmpty());
        assertTrue(device.getAncestors().isEmpty());
    }
}
