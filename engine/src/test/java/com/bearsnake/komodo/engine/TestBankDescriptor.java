/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestBankDescriptor {

    @Test
    public void testDefaultConstructor() {
        BankDescriptor bd = new BankDescriptor();
        assertEquals(BankType.BasicMode, bd.getBankType());
        assertFalse(bd.isGeneralFault());
        assertFalse(bd.isLargeBank());
        assertFalse(bd.isUpperLimitSuppression());
        assertEquals(0, bd.getLowerLimit());
        assertEquals(0, bd.getUpperLimit());
        assertTrue(bd.isInactive());
        assertEquals(0, bd.getDisplacement());
        assertEquals(new AbsoluteAddress(0, 0, 0), bd.getBaseAddress());
        assertEquals(0, bd.getIndirectLevelAndBDI());
        assertEquals(0, bd.getInactiveQBDListNextPointer());
        assertEquals(new AccessPermissions(false, false, false), bd.getGeneralAccessPermissions());
        assertEquals(new AccessPermissions(false, false, false), bd.getSpecialAccessPermissions());
        assertEquals(new AccessLock(0, (short) 0), bd.getAccessLock());
    }

    @Test
    public void testParameterConstructorSmallBank() {
        AccessLock lock = new AccessLock(10, (short) 1);
        AccessPermissions general = new AccessPermissions(true, true, false);
        AccessPermissions special = new AccessPermissions(true, true, true);
        AbsoluteAddress base = new AbsoluteAddress(1, 2, 3);

        // Not large bank. Lower limit is divided by 2^9 (512) and rounded up.
        // lower limit: 1000 -> 1000/512 = 1.95 -> 2
        // upper limit: 5000 -> 5000 (no shift for small bank)
        BankDescriptor bd = new BankDescriptor(true, lock, general, special, base, false, 1000, 5000, 100);

        assertEquals(BankType.BasicMode, bd.getBankType());
        assertEquals(lock, bd.getAccessLock());
        assertEquals(general, bd.getGeneralAccessPermissions());
        assertEquals(special, bd.getSpecialAccessPermissions());
        assertEquals(base, bd.getBaseAddress());
        assertFalse(bd.isLargeBank());
        assertEquals(2, bd.getLowerLimit());
        assertEquals(5000, bd.getUpperLimit());
        assertEquals(100, bd.getDisplacement());
        assertFalse(bd.isInactive());
    }

    @Test
    public void testParameterConstructorLargeBank() {
        AccessLock lock = new AccessLock(10, (short) 1);
        AccessPermissions general = new AccessPermissions(true, true, false);
        AccessPermissions special = new AccessPermissions(true, true, true);
        AbsoluteAddress base = new AbsoluteAddress(1, 2, 3);

        // Large bank.
        // Lower limit: actualLowerLimit >> 15. If (actualLowerLimit & 077777) != 0, increment.
        // 0x10000 (65536) -> 0x10000 >> 15 = 2. (0x10000 & 077777) = 0. Result: 2.
        // 0x10001 -> 0x10001 >> 15 = 2. (0x10001 & 077777) != 0. Result: 3.
        // Upper limit: actualUpperLimit >> 6. If (actualUpperLimit & 077) != 0, increment.
        // 0x100 (256) -> 0x100 >> 6 = 4. (0x100 & 077) = 0. Result: 4.
        // 0x101 -> 0x101 >> 6 = 4. (0x101 & 077) != 0. Result: 5.

        BankDescriptor bd1 = new BankDescriptor(false, lock, general, special, base, true, 0x10000, 0x100, 100);
        assertEquals(BankType.ExtendedMode, bd1.getBankType());
        assertTrue(bd1.isLargeBank());
        assertEquals(2, bd1.getLowerLimit());
        assertEquals(4, bd1.getUpperLimit());

        BankDescriptor bd2 = new BankDescriptor(false, lock, general, special, base, true, 0x10001, 0x101, 100);
        assertEquals(3, bd2.getLowerLimit());
        assertEquals(5, bd2.getUpperLimit());
    }

    @Test
    public void testGettersAndSetters() {
        BankDescriptor bd = new BankDescriptor();

        bd.setBankType(BankType.Queue)
          .setGeneralFault(true)
          .setLargeBank(true)
          .setUpperLimitSuppression(true)
          .setInactive(false)
          .setDisplacement(1234)
          .setIndirectLevelAndBDI(0xABC)
          .setLowerLimit(0x10)
          .setUpperLimit(0x20)
          .setInactiveQBDListNextPointer(0xFEED)
          .setAccessLock(new AccessLock(5, (short) 2))
          .setBaseAddress(new AbsoluteAddress(4, 5, 6))
          .setGeneralAccessPermissions(new AccessPermissions(true, false, true))
          .setSpecialAccessPermissions(new AccessPermissions(false, true, false));

        assertEquals(BankType.Queue, bd.getBankType());
        assertTrue(bd.isGeneralFault());
        assertTrue(bd.isLargeBank());
        assertTrue(bd.isUpperLimitSuppression());
        assertFalse(bd.isInactive());
        assertEquals(1234, bd.getDisplacement());
        assertEquals(0xABC, bd.getIndirectLevelAndBDI());
        assertEquals(0x10, bd.getLowerLimit());
        assertEquals(0x20, bd.getUpperLimit());
        assertEquals(0xFEED, bd.getInactiveQBDListNextPointer());
        assertEquals(new AccessLock(5, (short) 2), bd.getAccessLock());
        assertEquals(new AbsoluteAddress(4, 5, 6), bd.getBaseAddress());
        assertEquals(new AccessPermissions(true, false, true), bd.getGeneralAccessPermissions());
        assertEquals(new AccessPermissions(false, true, false), bd.getSpecialAccessPermissions());
    }

    @Test
    public void testNormalizedLimits() {
        BankDescriptor bd = new BankDescriptor();

        // Small bank
        bd.setLargeBank(false);
        bd.setLowerLimit(2);
        bd.setUpperLimit(10);
        assertEquals(2 << 9, bd.getLowerLimitNormalized());
        assertEquals(10, bd.getUpperLimitNormalized()); // Wait, looking at code: getUpperLimitNormalized() returns _upperLimit if not large bank

        // Large bank
        bd.setLargeBank(true);
        bd.setLowerLimit(2);
        bd.setUpperLimit(10);
        assertEquals(2L << 15, bd.getLowerLimitNormalized());
        assertEquals(10L << 6, bd.getUpperLimitNormalized());
    }

    @Test
    public void testSerializationRoundTrip() {
        BankDescriptor bd1 = new BankDescriptor();
        bd1.setBankType(BankType.ExtendedMode)
           .setGeneralFault(true)
           .setLargeBank(false)
           .setUpperLimitSuppression(true)
           .setInactive(false)
           .setDisplacement(0x1234)
           .setLowerLimit(0x123)
           .setUpperLimit(0x45678)
           .setAccessLock(new AccessLock(0x1234, (short) 3))
           .setBaseAddress(new AbsoluteAddress(2, 0x123456, 0x789ABCDE))
           .setGeneralAccessPermissions(new AccessPermissions(true, false, true))
           .setSpecialAccessPermissions(new AccessPermissions(false, true, false));

        long[] buffer = new long[6];
        bd1.serialize(buffer);

        BankDescriptor bd2 = new BankDescriptor(buffer);

        assertEquals(bd1.getBankType(), bd2.getBankType());
        assertEquals(bd1.isGeneralFault(), bd2.isGeneralFault());
        assertEquals(bd1.isLargeBank(), bd2.isLargeBank());
        assertEquals(bd1.isUpperLimitSuppression(), bd2.isUpperLimitSuppression());
        assertEquals(bd1.isInactive(), bd2.isInactive());
        assertEquals(bd1.getDisplacement(), bd2.getDisplacement());
        assertEquals(bd1.getLowerLimit(), bd2.getLowerLimit());
        assertEquals(bd1.getUpperLimit(), bd2.getUpperLimit());
        assertEquals(bd1.getAccessLock(), bd2.getAccessLock());
        assertEquals(bd1.getBaseAddress(), bd2.getBaseAddress());
        assertEquals(bd1.getGeneralAccessPermissions(), bd2.getGeneralAccessPermissions());
        assertEquals(bd1.getSpecialAccessPermissions(), bd2.getSpecialAccessPermissions());
    }

    @Test
    public void testSerializationIndirect() {
        BankDescriptor bd1 = new BankDescriptor();
        bd1.setBankType(BankType.Indirect)
           .setIndirectLevelAndBDI(0x3FFFF);

        long[] buffer = new long[6];
        bd1.serialize(buffer);

        BankDescriptor bd2 = new BankDescriptor(buffer);
        assertEquals(BankType.Indirect, bd2.getBankType());
        assertEquals(0x3FFFF, bd2.getIndirectLevelAndBDI());
    }

    @Test
    public void testSerializationQueueInactive() {
        BankDescriptor bd1 = new BankDescriptor();
        bd1.setBankType(BankType.Queue)
           .setInactive(true)
           .setInactiveQBDListNextPointer(0x123456789L)
           .setBaseAddress(new AbsoluteAddress(1, 2, 3));

        long[] buffer = new long[6];
        bd1.serialize(buffer);

        BankDescriptor bd2 = new BankDescriptor(buffer);
        assertEquals(BankType.Queue, bd2.getBankType());
        assertTrue(bd2.isInactive());
        assertEquals(0x123456789L, bd2.getInactiveQBDListNextPointer());
        // For Queue + Inactive, base address word 2 is serialized as 0, but AbsoluteAddress(buffer[2], 0) might not match
        // Let's check the code:
        // serialize: if (_bankType == BankType.Queue && _inactive) { value2 = 0; value3 = _inactiveQBDListNextPointer; }
        // constructor: long addrWord2 = (_bankType == BankType.Queue && _inactive) ? 0 : buffer[3];
        // constructor: _baseAddress = new AbsoluteAddress(buffer[2], addrWord2);
        // buffer[2] was 0 in serialize.
        // So _baseAddress should have segment=0, upi=0, offset=0.
        assertEquals(new AbsoluteAddress(0, 0, 0), bd2.getBaseAddress());
    }
}
