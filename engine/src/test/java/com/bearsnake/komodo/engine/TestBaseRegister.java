/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestBaseRegister {
    @Test
    public void testInitialState() {
        BaseRegister br = new BaseRegister();
        assertTrue(br.isVoid());
        assertEquals(AccessPermissions.NONE, br.getGeneralAccessPermissions());
        assertEquals(AccessPermissions.NONE, br.getSpecialAccessPermissions());
        assertNotNull(br.getAccessLock());
        assertEquals(0, br.getAccessLock().getRing());
        assertEquals(0, br.getAccessLock().getDomain());
    }

    @Test
    public void testSettersAndGetters() {
        BaseRegister br = new BaseRegister();
        AccessPermissions gap = new AccessPermissions(true, false, true);
        AccessPermissions sap = new AccessPermissions(false, true, false);
        AccessLock lock = new AccessLock(123, (short) 2);
        AbsoluteAddress addr = new AbsoluteAddress(0123, 0456);

        br.setGeneralAccessPermissions(gap)
          .setSpecialAccessPermissions(sap)
          .setIsVoid(false)
          .setIsLargeBank(true)
          .setAccessLock(lock)
          .setLowerLimit(010)
          .setUpperLimit(020)
          .setBaseAddress(addr);

        assertEquals(gap, br.getGeneralAccessPermissions());
        assertEquals(sap, br.getSpecialAccessPermissions());
        assertFalse(br.isVoid());
        assertTrue(br.isLargeBank());
        assertEquals(lock, br.getAccessLock());
        assertEquals(010, br.getLowerLimit());
        assertEquals(020, br.getUpperLimit());
        assertEquals(addr, br.getBaseAddress());
    }

    @Test
    public void testSetLimitsNormalizedSmallBank() {
        BaseRegister br = new BaseRegister();
        // For small bank: lowerShift = 9, upperShift = 0
        // lowerLimitNormalized must have lower 9 bits zero
        // upperLimitNormalized has no shift for upper limit but it's used as is? Wait.
        // Let's re-examine BaseRegister.java
        // _lowerLimit = lowerLimitNormalized >> 9;
        // _upperLimit = upperLimitNormalized >> 0;
        br.setLimitsNormalized(false, 02000, 04000);
        assertFalse(br.isVoid());
        assertFalse(br.isLargeBank());
        assertEquals(02000 >> 9, br.getLowerLimit());
        assertEquals(04000, br.getUpperLimit());
        assertEquals(02000, br.getLowerLimitNormalized());
        assertEquals(04000, br.getUpperLimitNormalized());
    }

    @Test
    public void testSetLimitsNormalizedLargeBank() {
        BaseRegister br = new BaseRegister();
        // For large bank: lowerShift = 15, upperShift = 6
        // lowerLimitNormalized must have lower 15 bits zero: 0x8000 = 0100000
        // upperLimitNormalized must have lower 6 bits zero: 0x40 = 0100
        br.setLimitsNormalized(true, 0100000, 0200000);
        assertFalse(br.isVoid());
        assertTrue(br.isLargeBank());
        assertEquals(0100000 >> 15, br.getLowerLimit());
        assertEquals(0200000 >> 6, br.getUpperLimit());
        assertEquals(0100000, br.getLowerLimitNormalized());
        assertEquals(0200000, br.getUpperLimitNormalized());
    }

    @Test
    public void testCheckAccessLimits() throws ReferenceViolationInterrupt {
        BaseRegister br = new BaseRegister();
        br.setLimitsNormalized(false, 02000, 04000);

        // Within limits
        br.checkAccessLimits(02000, false);
        br.checkAccessLimits(03000, true);
        br.checkAccessLimits(04000, false);

        // Below limits
        assertThrows(ReferenceViolationInterrupt.class, () -> br.checkAccessLimits(01777, false));

        // Above limits
        assertThrows(ReferenceViolationInterrupt.class, () -> br.checkAccessLimits(04001, true));
    }

    @Test
    public void testGetEffectivePermissions() {
        BaseRegister br = new BaseRegister();
        AccessPermissions gap = new AccessPermissions(true, false, false); // Read only
        AccessPermissions sap = new AccessPermissions(true, true, true);  // All
        AccessLock lock = new AccessLock(10, (short) 2);
        br.setGeneralAccessPermissions(gap).setSpecialAccessPermissions(sap).setAccessLock(lock);

        // Master key (ring 0, domain 0) -> ALL
        assertEquals(AccessPermissions.ALL, br.getEffectivePermissions(new AccessKey(0, (short) 0)));

        // Ring < lock ring (1 < 2) -> SAP
        assertEquals(sap, br.getEffectivePermissions(new AccessKey(20, (short) 1)));

        // Domain == lock domain (10 == 10) -> SAP
        assertEquals(sap, br.getEffectivePermissions(new AccessKey(10, (short) 3)));

        // Otherwise -> GAP
        assertEquals(gap, br.getEffectivePermissions(new AccessKey(20, (short) 3)));
    }

    @Test
    public void testCreateVoidAndMakeVoid() {
        BaseRegister br = BaseRegister.createVoid();
        assertTrue(br.isVoid());
        assertNull(br.getStorage());

        br.setIsVoid(false).setLimitsNormalized(false, 0, 01000);
        assertFalse(br.isVoid());

        br.makeVoid();
        assertTrue(br.isVoid());
        assertNull(br.getStorage());
    }
}
