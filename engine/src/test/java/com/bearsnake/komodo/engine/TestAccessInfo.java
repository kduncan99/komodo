/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for InstructionWord class
 */
public class TestAccessInfo {

    private static final Random _random = new Random(System.currentTimeMillis());

    @Test
    public void constructor() {
        AccessInfo ai = new AccessInfo();
        assertEquals(0, ai.getDomain());
        assertEquals(0, ai.getRing());
    }

    @Test
    public void populatingConstructor() {
        for (int x = 0; x < 20000; ++x) {
            var ring = (short)_random.nextInt(0x4);
            var domain = _random.nextInt(0x10000);
            AccessInfo ai = new AccessInfo(domain, ring);
            assertEquals(ring, ai.getRing());
            assertEquals(domain, ai.getDomain());
        }
    }

    @Test
    public void equality() {
        for (int x = 0; x < 20000; ++x) {
            var ring = (short)_random.nextInt(0x4);
            var domain = _random.nextInt(0xFFFF);
            AccessInfo ai0 = new AccessInfo(domain, ring);
            AccessInfo ai1 = new AccessInfo(domain, ring);
            assertEquals(ai0, ai1);
        }
    }

    @Test
    public void inequality() {
        for (int x = 0; x < 20000; ++x) {
            var ring0 = (short)_random.nextInt(0x4);
            var ring1 = (short)_random.nextInt(0x4);
            var domain0 = _random.nextInt(0xFFFF);
            var domain1 = _random.nextInt(0xFFFF);
            if ((ring0 != ring1) || (domain0 != domain1)) {
                AccessInfo ai0 = new AccessInfo(domain0, ring0);
                AccessInfo ai1 = new AccessInfo(domain1, ring1);
                assertNotEquals(ai0, ai1);
            }
        }
    }
}
