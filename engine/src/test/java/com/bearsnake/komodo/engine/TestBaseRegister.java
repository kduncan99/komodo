/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestBaseRegister {

    @Test
    public void testDefaultConstructor() {
        BankDescriptor bd = new BankDescriptor();
        long[] storageArray = new long[100];
        ArraySlice storage = new ArraySlice(storageArray);
        BaseRegister br = new BaseRegister(bd, storage, 0400L);

        assertSame(bd, br.getBankDescriptor());
        assertSame(storage, br.getStorage());
        assertEquals(0400L, br.getSubsetting());
        assertFalse(br.isVoid());
    }

    @Test
    public void testCreateVoid() {
        BaseRegister br = BaseRegister.createVoid();
        assertTrue(br.isVoid());
        assertNull(br.getBankDescriptor());
        assertNull(br.getStorage());
        assertEquals(0, br.getSubsetting());
    }

    @Test
    public void testSetters() {
        BaseRegister br = BaseRegister.createVoid();
        BankDescriptor bd = new BankDescriptor();
        long[] storageArray = new long[100];
        ArraySlice storage = new ArraySlice(storageArray);

        br.setBankDescriptor(bd);
        br.setStorage(storage);
        br.setSubsetting(1234L);

        assertSame(bd, br.getBankDescriptor());
        assertSame(storage, br.getStorage());
        assertEquals(1234L, br.getSubsetting());
        assertFalse(br.isVoid());
    }

    @Test
    public void testFromBankDescriptor() {
        BaseRegister br = BaseRegister.createVoid();
        BankDescriptor bd = new BankDescriptor();
        long[] storageArray = new long[100];
        ArraySlice storage = new ArraySlice(storageArray);

        br.fromBankDescriptor(bd, storage);
        assertSame(bd, br.getBankDescriptor());
        assertSame(storage, br.getStorage());
        assertEquals(0, br.getSubsetting());

        br.fromBankDescriptor(bd, storage, 5678L);
        assertEquals(5678L, br.getSubsetting());
    }

    @Test
    public void testMakeVoid() {
        BankDescriptor bd = new BankDescriptor();
        BaseRegister br = new BaseRegister(bd, null, 100);
        assertFalse(br.isVoid());

        br.makeVoid();
        assertTrue(br.isVoid());
        assertNull(br.getBankDescriptor());
        assertNull(br.getStorage());
        assertEquals(0, br.getSubsetting());
    }

    @Test
    public void testCheckAccessLimits() throws ReferenceViolationInterrupt {
        // Basic mode, not large bank. Normalized limits: lower=ll<<9, upper=ul
        // We set ll=1, ul=1000.
        // normalized lower = 1 << 9 = 512.
        // normalized upper = 1000.
        BankDescriptor bd = new BankDescriptor()
                .setLargeBank(false)
                .setLowerLimit(1)
                .setUpperLimit(1000);

        BaseRegister br = new BaseRegister(bd, null, 0);

        // Within limits
        br.checkAccessLimits(512, false);
        br.checkAccessLimits(1000, true);
        br.checkAccessLimits(750, false);

        // Below lower limit
        assertThrows(ReferenceViolationInterrupt.class, () -> br.checkAccessLimits(511, false));

        // Above upper limit
        assertThrows(ReferenceViolationInterrupt.class, () -> br.checkAccessLimits(1001, true));
    }

    @Test
    public void testCheckAccessLimitsVoid() {
        BaseRegister br = BaseRegister.createVoid();
        assertThrows(ReferenceViolationInterrupt.class, () -> br.checkAccessLimits(0, false));
    }

    @Test
    public void testCheckAccessLimitsLargeBank() throws ReferenceViolationInterrupt {
        // Large bank. Normalized limits: lower=ll<<15, upper=ul<<6
        // We set ll=2, ul=10000.
        // normalized lower = 2 << 15 = 65536.
        // normalized upper = 10000 << 6 = 640000.
        BankDescriptor bd = new BankDescriptor()
                .setLargeBank(true)
                .setLowerLimit(2)
                .setUpperLimit(10000);

        BaseRegister br = new BaseRegister(bd, null, 0);

        // Within limits
        br.checkAccessLimits(65536, false);
        br.checkAccessLimits(640000, true);

        // Out of limits
        assertThrows(ReferenceViolationInterrupt.class, () -> br.checkAccessLimits(65535, false));
        assertThrows(ReferenceViolationInterrupt.class, () -> br.checkAccessLimits(640001, true));
    }

    @Test
    public void testGetEffectivePermissions() {
        AccessPermissions gen = new AccessPermissions(false, true, false); // R
        AccessPermissions spec = new AccessPermissions(true, true, true);  // ERW
        AccessLock lock = new AccessLock(100, (short) 2);

        BankDescriptor bd = new BankDescriptor()
                .setAccessLock(lock)
                .setGeneralAccessPermissions(gen)
                .setSpecialAccessPermissions(spec);

        BaseRegister br = new BaseRegister(bd, null, 0);

        // Master Key (ring 0, domain 0) -> ALL
        AccessKey masterKey = new AccessKey(0, (short) 0);
        assertEquals(AccessPermissions.ALL, br.getEffectivePermissions(masterKey));

        // Ring < Lock Ring (1 < 2) -> Special
        AccessKey key1 = new AccessKey(200, (short) 1);
        assertEquals(spec, br.getEffectivePermissions(key1));

        // Domain == Lock Domain (100 == 100) -> Special
        AccessKey key2 = new AccessKey(100, (short) 3);
        assertEquals(spec, br.getEffectivePermissions(key2));

        // Ring >= Lock Ring (2 >= 2) AND Domain != Lock Domain (200 != 100) -> General
        AccessKey key3 = new AccessKey(200, (short) 2);
        assertEquals(gen, br.getEffectivePermissions(key3));
    }
}
