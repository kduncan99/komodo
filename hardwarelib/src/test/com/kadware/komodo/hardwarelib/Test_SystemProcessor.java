/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for SystemProcessor class
 */
public class Test_SystemProcessor {

    @Test
    public void create(
    ) throws MaxNodesException  {
        SystemProcessor p = InventoryManager.getInstance().createSystemProcessor(2200);
        try {
            Thread.sleep(120000);
        } catch (InterruptedException ex) {
        }
    }
}
