/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for SystemProcessor class
 */
public class Test_SystemProcessor {

    @Test
    public void create(
    ) throws MaxNodesException  {
        SystemProcessor p = InventoryManager.getInstance().createSystemProcessor(2200);
        Random r = new Random(System.currentTimeMillis());
        while (true) {
            p.jumpKeysSet(r.nextLong() & 0_777777_777777L);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
            }
        }
    }
}
