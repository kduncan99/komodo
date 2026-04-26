/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestVirtualAddress {

    @Test
    public void testGetValueLevel() {
        // level 2, bdIndex 07777, offset 0123456
        long value = VirtualAddress.toCompositeValue((short) 2, 07777, 0123456);
        // value should have level in bits 33-35 (level 2 = 010 in binary)
        // bdIndex in bits 18-32
        // offset in bits 0-17
        assertEquals(2, (int)((value >> 33) & 07));
        assertEquals(07777, (int)((value >> 18) & 077777));
        assertEquals(0123456, (int)(value & 0777777));
    }

    @Test
    public void testVirtualAddressGetters() {
        long v = VirtualAddress.toCompositeValue((short) 6, 012345, 0654321);
        VirtualAddress va = new VirtualAddress(v);
        assertEquals(6, va.getLevel());
        assertEquals(012345, va.getBankDescriptorIndex());
        assertEquals(0654321, va.getOffset());
        assertEquals((6 << 15) | 012345, va.getLBDI());
    }
}
