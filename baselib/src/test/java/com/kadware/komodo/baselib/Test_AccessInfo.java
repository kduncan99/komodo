/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import java.util.Random;

/**
 * Unit tests for InstructionWord class
 */
public class Test_AccessInfo {

    private static final Random _random = new Random(System.currentTimeMillis());

    @Test
    public void constructor() {
        AccessInfo ai = new AccessInfo();
        assertEquals(0, ai._domain);
        assertEquals(0, ai._ring);
    }

    @Test
    public void populatingConstructor() {
        for (int x = 0; x < 20000; ++x) {
            int ring = _random.nextInt(0x4);
            long domain = _random.nextInt(0x10000);
            AccessInfo ai = new AccessInfo(ring, domain);
            assertEquals(ring, ai._ring);
            assertEquals(domain, ai._domain);
        }
    }

    @Test
    public void equality() {
        for (int x = 0; x < 20000; ++x) {
            int ring = _random.nextInt(0x4);
            long domain = _random.nextLong() & 0xFFFF;
            AccessInfo ai0 = new AccessInfo(ring, domain);
            AccessInfo ai1 = new AccessInfo(ring, domain);
            assertEquals(ai0, ai1);
        }
    }

    @Test
    public void inequality() {
        for (int x = 0; x < 20000; ++x) {
            int ring0 = _random.nextInt(0x4);
            int ring1 = _random.nextInt(0x4);
            long domain0 = _random.nextLong() & 0xFFFF;
            long domain1 = _random.nextLong() & 0xFFFF;
            if ((ring0 != ring1) || (domain0 != domain1)) {
                AccessInfo ai0 = new AccessInfo(ring0, domain0);
                AccessInfo ai1 = new AccessInfo(ring1, domain1);
                assertNotEquals(ai0, ai1);
            }
        }
    }
}
