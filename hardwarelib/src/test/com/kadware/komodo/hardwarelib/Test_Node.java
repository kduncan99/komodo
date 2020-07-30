/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Unit tests for Node class
 */
public class Test_Node {

    @Test
    public void create(
    ) {
        Node device = new ScratchDiskDevice("DISK01");
        assertEquals(Node.NodeCategory.Device, device._category);
        assertEquals("DISK01", device._name);
    }
}
