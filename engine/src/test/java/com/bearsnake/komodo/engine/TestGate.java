/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestGate {

    @Test
    public void testDefaultConstructor() {
        Gate gate = new Gate();

        assertEquals(new AccessPermissions(false, false, false), gate.getGeneralAccessPermissions());
        assertEquals(new AccessPermissions(false, false, false), gate.getSpecialAccessPermissions());
        assertFalse(gate.isLibrary());
        assertFalse(gate.isGotoInhibited());
        assertFalse(gate.isDesignatorBitInhibited());
        assertFalse(gate.isAccessKeyInhibited());
        assertFalse(gate.isLatentParameter0Inhibited());
        assertFalse(gate.isLatentParameter1Inhibited());
        assertEquals(new AccessLock(), gate.getAccessLock());
        assertEquals(0, gate.getBankLevel());
        assertEquals(0, gate.getBankDescriptorIndex());
        assertEquals(0, gate.getOffset());
        assertEquals(0, gate.getBasicModeBaseRegister());
        assertEquals(0, gate.getDesignatorRegisterBits12To17());
        assertEquals(new AccessKey(), gate.getAccessKey());
        assertEquals(0L, gate.getLatentParameter0());
        assertEquals(0L, gate.getLatentParameter1());
    }

    @Test
    public void testParameterConstructor() {
        AccessPermissions general = new AccessPermissions(true, false, true);
        AccessPermissions special = new AccessPermissions(false, true, false);
        AccessLock lock = new AccessLock((short) 2, 5);
        AccessKey key = new AccessKey((short) 1, 7);

        Gate gate = new Gate(
            general,
            special,
            true,
            true,
            false,
            true,
            false,
            true,
            lock,
            (short) 3,
            0x1234,
            0x4567,
            (short) 13,
            0x2A,
            key,
            0x123456789L,
            0x6543210L
        );

        assertEquals(new AccessPermissions(true, false, false), gate.getGeneralAccessPermissions());
        assertEquals(new AccessPermissions(false, false, false), gate.getSpecialAccessPermissions());
        assertTrue(gate.isLibrary());
        assertTrue(gate.isGotoInhibited());
        assertFalse(gate.isDesignatorBitInhibited());
        assertTrue(gate.isAccessKeyInhibited());
        assertFalse(gate.isLatentParameter0Inhibited());
        assertTrue(gate.isLatentParameter1Inhibited());
        assertEquals(lock, gate.getAccessLock());
        assertEquals(3, gate.getBankLevel());
        assertEquals(0x1234, gate.getBankDescriptorIndex());
        assertEquals(0x4567, gate.getOffset());
        assertEquals(13, gate.getBasicModeBaseRegister());
        assertEquals(0x2A, gate.getDesignatorRegisterBits12To17());
        assertEquals(key, gate.getAccessKey());
        assertEquals(0x123456789L & 0_777777_777777L, gate.getLatentParameter0());
        assertEquals(0x6543210L & 0_777777_777777L, gate.getLatentParameter1());
    }

    @Test
    public void testSetters() {
        Gate gate = new Gate();

        gate.setGeneralAccessPermissions(new AccessPermissions(true, true, true))
            .setSpecialAccessPermissions(new AccessPermissions(false, true, false))
            .setIsLibrary(true)
            .setIsGotoInhibited(true)
            .setIsDesignatorBitInhibited(true)
            .setIsAccessKeyInhibited(true)
            .setIsLatentParameter0Inhibited(true)
            .setIsLatentParameter1Inhibited(true)
            .setAccessLock(new AccessLock((short) 3, 0x12345))
            .setBankLevel((short) 4)
            .setBankDescriptorIndex(0x2345)
            .setOffset(0x3456)
            .setBasicModeBaseRegister((short) 14)
            .setDesignatorRegisterBits12To17(0x3A)
            .setAccessKey(new AccessKey((short) 1, 0x2222))
            .setLatentParameter0(0x123456789L)
            .setLatentParameter1(0x76543210L);

        assertEquals(new AccessPermissions(true, false, false), gate.getGeneralAccessPermissions());
        assertEquals(new AccessPermissions(false, false, false), gate.getSpecialAccessPermissions());
        assertTrue(gate.isLibrary());
        assertTrue(gate.isGotoInhibited());
        assertTrue(gate.isDesignatorBitInhibited());
        assertTrue(gate.isAccessKeyInhibited());
        assertTrue(gate.isLatentParameter0Inhibited());
        assertTrue(gate.isLatentParameter1Inhibited());
        assertEquals(new AccessLock((short) 3, 0x12345), gate.getAccessLock());
        assertEquals(4, gate.getBankLevel());
        assertEquals(0x2345, gate.getBankDescriptorIndex());
        assertEquals(0x3456, gate.getOffset());
        assertEquals(14, gate.getBasicModeBaseRegister());
        assertEquals(0x3A, gate.getDesignatorRegisterBits12To17());
        assertEquals(new AccessKey((short) 1, 0x2222), gate.getAccessKey());
        assertEquals(0x123456789L & 0_777777_777777L, gate.getLatentParameter0());
        assertEquals(0x76543210L & 0_777777_777777L, gate.getLatentParameter1());
    }

    @Test
    public void testSerializationRoundTrip() {
        Gate gate1 = new Gate();
        gate1.setGeneralAccessPermissions(new AccessPermissions(true, true, false))
             .setSpecialAccessPermissions(new AccessPermissions(false, false, true))
             .setIsLibrary(true)
             .setIsGotoInhibited(true)
             .setIsDesignatorBitInhibited(true)
             .setIsAccessKeyInhibited(true)
             .setIsLatentParameter0Inhibited(true)
             .setIsLatentParameter1Inhibited(true)
             .setAccessLock(new AccessLock((short) 3, 0_7654))
             .setBankLevel((short) 4)
             .setBankDescriptorIndex(0x2345)
             .setOffset(0x3456)
             .setBasicModeBaseRegister((short) 14)
             .setDesignatorRegisterBits12To17(0x3A)
             .setAccessKey(new AccessKey((short) 1, 0_2222))
             .setLatentParameter0(0x123456789L)
             .setLatentParameter1(0x76543210L);

        long[] buffer = new long[8];
        gate1.serialize(buffer, 0);

        Gate gate2 = new Gate(buffer, 0);

        assertEquals(gate1.getGeneralAccessPermissions(), gate2.getGeneralAccessPermissions());
        assertEquals(gate1.getSpecialAccessPermissions(), gate2.getSpecialAccessPermissions());
        assertEquals(gate1.isLibrary(), gate2.isLibrary());
        assertEquals(gate1.isGotoInhibited(), gate2.isGotoInhibited());
        assertEquals(gate1.isDesignatorBitInhibited(), gate2.isDesignatorBitInhibited());
        assertEquals(gate1.isAccessKeyInhibited(), gate2.isAccessKeyInhibited());
        assertEquals(gate1.isLatentParameter0Inhibited(), gate2.isLatentParameter0Inhibited());
        assertEquals(gate1.isLatentParameter1Inhibited(), gate2.isLatentParameter1Inhibited());
        assertEquals(gate1.getAccessLock(), gate2.getAccessLock());
        assertEquals(gate1.getBankLevel(), gate2.getBankLevel());
        assertEquals(gate1.getBankDescriptorIndex(), gate2.getBankDescriptorIndex());
        assertEquals(gate1.getOffset(), gate2.getOffset());
        assertEquals(gate1.getBasicModeBaseRegister(), gate2.getBasicModeBaseRegister());
        assertEquals(gate1.getDesignatorRegisterBits12To17(), gate2.getDesignatorRegisterBits12To17());
        assertEquals(gate1.getAccessKey(), gate2.getAccessKey());
        assertEquals(gate1.getLatentParameter0(), gate2.getLatentParameter0());
        assertEquals(gate1.getLatentParameter1(), gate2.getLatentParameter1());
    }

    @Test
    public void testStaticHelperMethodsFromSerializedBuffer() {
        Gate gate = new Gate();
        gate.setGeneralAccessPermissions(new AccessPermissions(true, false, true))
            .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
            .setIsLibrary(true)
            .setIsGotoInhibited(true)
            .setIsDesignatorBitInhibited(false)
            .setIsAccessKeyInhibited(true)
            .setIsLatentParameter0Inhibited(false)
            .setIsLatentParameter1Inhibited(true)
            .setAccessLock(new AccessLock((short) 2, 0_2345))
            .setBankLevel((short) 5)
            .setBankDescriptorIndex(0_12345)
            .setOffset(0_3456)
            .setBasicModeBaseRegister((short) 13)
            .setDesignatorRegisterBits12To17(0x2A)
            .setAccessKey(new AccessKey((short) 1, 0_2222))
            .setLatentParameter0(0_111111_111111L)
            .setLatentParameter1(0_222222_222222L);

        long[] buffer = new long[8];
        gate.serialize(buffer, 0);

        assertEquals(new AccessPermissions(true, false, false), Gate.getGeneralAccessPermissions(buffer, 0));
        assertEquals(new AccessPermissions(false, false, false), Gate.getSpecialAccessPermissions(buffer, 0));
        assertTrue(Gate.isLibrary(buffer, 0));
        assertTrue(Gate.isGotoInhibited(buffer, 0));
        assertFalse(Gate.isDesignatorBitInhibited(buffer, 0));
        assertTrue(Gate.isAccessKeyInhibited(buffer, 0));
        assertFalse(Gate.isLatentParameter0Inhibited(buffer, 0));
        assertTrue(Gate.isLatentParameter1Inhibited(buffer, 0));
        assertEquals(new AccessLock((short) 2, 0_2345), Gate.getAccessLock(buffer, 0));
        assertEquals(5, Gate.getBankLevel(buffer, 0));
        assertEquals(0_12345, Gate.getBankDescriptorIndex(buffer, 0));
        assertEquals(0_3456, Gate.getOffset(buffer, 0));
        assertEquals(13, Gate.getBasicModeBaseRegister(buffer, 0));
        assertEquals(0x2A, Gate.getDesignatorRegisterBits12To17(buffer, 0));
        assertEquals(new AccessKey((short) 1, 0_2222), Gate.getAccessKey(buffer, 0));
        assertEquals(0_111111_111111L, Gate.getLatentParameter0(buffer, 0));
        assertEquals(0_222222_222222L, Gate.getLatentParameter1(buffer, 0));
    }
}
