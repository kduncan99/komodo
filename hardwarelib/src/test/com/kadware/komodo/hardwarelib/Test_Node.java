/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.hardwarelib.exceptions.CannotConnectException;
import java.util.Map;
import java.util.Set;
import org.junit.*;
import static org.junit.Assert.*;

/**
 * Unit tests for Node class
 */
public class Test_Node {

    private static class TestDeviceNode extends Node {

        public TestDeviceNode(
            final String name
        ) {
            super(NodeCategory.Device, name);
        }

        @Override
        public boolean canConnect(
            final Node candidateAncestor
        ) {
            return candidateAncestor instanceof InputOutputProcessor;
        }

        @Override
        public void initialize() {}

        @Override
        public void clear() {}

        @Override
        public void terminate(){}
    }

    @Test
    public void create(
    ) {
        Node device = new TestDeviceNode("DISK01");
        assertEquals(Node.NodeCategory.Device, device._category);
        assertEquals("DISK01", device._name);
    }

    @Test
    public void connect_Success(
    ) throws CannotConnectException {
        Node device = new TestDeviceNode("DISK01");
        Node iop = new InputOutputProcessor("IOP0", 10);
        Node.connect(iop, device);

        assertTrue(iop._ancestors.isEmpty());
        Map<Integer, Node> ctlDescendants = iop._descendants;
        assertEquals(1, ctlDescendants.size());
        assertEquals(device, ctlDescendants.get(0));

        assertTrue(device._descendants.isEmpty());
        Set<Node> devAncestors = device._ancestors;
        assertEquals(1, devAncestors.size());
        assertTrue(devAncestors.contains(iop));
    }

    @Test
    public void connect_Multiple_Success(
    ) throws CannotConnectException {
        Node device01 = new TestDeviceNode("DISK01");
        Node device02 = new TestDeviceNode("DISK02");
        Node device03 = new TestDeviceNode("DISK03");
        Node iop = new InputOutputProcessor("IOP1", 11);
        Node.connect(iop, device01);
        Node.connect(iop, device02);
        Node.connect(iop, device03);

        Map<Integer, Node> ctlDescendants = iop._descendants;
        assertEquals(3, ctlDescendants.size());
        assertEquals(device01, ctlDescendants.get(0));
        assertEquals(device02, ctlDescendants.get(1));
        assertEquals(device03, ctlDescendants.get(2));

        assertTrue(device01._ancestors.contains(iop));
        assertTrue(device02._ancestors.contains(iop));
        assertTrue(device03._ancestors.contains(iop));
    }

    @Test(expected = CannotConnectException.class)
    public void connect_Failure(
    ) throws CannotConnectException {
        Node device = new TestDeviceNode("DISK01");
        Node iop = new InputOutputProcessor("IOP2", 12);
        Node.connect(device, iop);
    }

    @Test
    public void disconnect(
    ) throws CannotConnectException {
        Node device = new TestDeviceNode("DISK01");
        Node iop = new InputOutputProcessor("IOP3", 13);
        Node.connect(iop, device);

        //  make sure things are the way we expect before trying to disconnect
        assertFalse(iop._descendants.isEmpty());
        assertFalse(device._ancestors.isEmpty());

        Node.disconnect(iop, device);
        assertTrue(iop._descendants.isEmpty());
        assertTrue(device._ancestors.isEmpty());
    }
}
