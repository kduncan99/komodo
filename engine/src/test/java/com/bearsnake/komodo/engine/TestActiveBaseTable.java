/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestActiveBaseTable {

    @Test
    public void testEntryDefaultValues() {
        ActiveBaseTable.Entry entry = new ActiveBaseTable.Entry();
        assertEquals(0, entry.getBankLevel());
        assertEquals(0, entry.getBankDescriptorIndex());
        assertEquals(0, entry.getSubsetSpecification());
    }

    @Test
    public void testEntrySet() {
        ActiveBaseTable.Entry entry = new ActiveBaseTable.Entry();
        entry.set((short) 5, 012345, 0654321);
        assertEquals(5, entry.getBankLevel());
        assertEquals(012345, entry.getBankDescriptorIndex());
        assertEquals(0654321, entry.getSubsetSpecification());
    }

    @Test
    public void testEntrySetBankLevelMasking() {
        ActiveBaseTable.Entry entry = new ActiveBaseTable.Entry();
        // Mask is 0_07 (octal 7)
        entry.setBankLevel((short) 012); // octal 12 = 1010 binary -> 0010 binary = 2
        assertEquals(2, entry.getBankLevel());
        
        entry.setBankLevel((short) 07);
        assertEquals(7, entry.getBankLevel());

        entry.setBankLevel((short) 017); // octal 17 = 1111 binary -> 0111 binary = 7
        assertEquals(7, entry.getBankLevel());
    }

    @Test
    public void testEntrySetBankDescriptorIndexMasking() {
        ActiveBaseTable.Entry entry = new ActiveBaseTable.Entry();
        // Mask is 0_077777 (15 bits)
        entry.setBankDescriptorIndex(0177777); // 16 bits set -> should be 077777
        assertEquals(077777, entry.getBankDescriptorIndex());

        entry.setBankDescriptorIndex(012345);
        assertEquals(012345, entry.getBankDescriptorIndex());
    }

    @Test
    public void testEntrySetSubsetSpecificationMasking() {
        ActiveBaseTable.Entry entry = new ActiveBaseTable.Entry();
        // Mask is 0_777777 (18 bits)
        entry.setSubsetSpecification(01777777); // 19 bits set -> should be 0777777
        assertEquals(0777777, entry.getSubsetSpecification());

        entry.setSubsetSpecification(0654321);
        assertEquals(0654321, entry.getSubsetSpecification());
    }

    @Test
    public void testEntryFluentSetters() {
        ActiveBaseTable.Entry entry = new ActiveBaseTable.Entry();
        ActiveBaseTable.Entry returned = entry.setBankLevel((short) 1)
                                              .setBankDescriptorIndex(2)
                                              .setSubsetSpecification(3);
        assertSame(entry, returned);
        assertEquals(1, entry.getBankLevel());
        assertEquals(2, entry.getBankDescriptorIndex());
        assertEquals(3, entry.getSubsetSpecification());
    }

    @Test
    public void testTableInitialization() {
        ActiveBaseTable abt = new ActiveBaseTable();
        // entry 0 is null
        assertNull(abt.getEntry(0));
        
        // entries 1-15 are non-null
        for (int i = 1; i < 16; i++) {
            assertNotNull(abt.getEntry(i));
        }
    }

    @Test
    public void testGetEntryBounds() {
        ActiveBaseTable abt = new ActiveBaseTable();
        
        Exception ex1 = assertThrows(RuntimeException.class, () -> abt.getEntry(-1));
        assertEquals("Invalid index=-1", ex1.getMessage());

        Exception ex2 = assertThrows(RuntimeException.class, () -> abt.getEntry(16));
        assertEquals("Invalid index=16", ex2.getMessage());
    }

    @Test
    public void testTableModifiability() {
        ActiveBaseTable abt = new ActiveBaseTable();
        ActiveBaseTable.Entry entry1 = abt.getEntry(1);
        entry1.setBankLevel((short) 4);
        
        // Verify that it is the same object
        assertEquals(4, abt.getEntry(1).getBankLevel());
    }
}
