/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test.misc;

import com.kadware.komodo.baselib.AccessInfo;
import com.kadware.komodo.baselib.AccessPermissions;
import com.kadware.em2200.hardwarelib.InventoryManager;
import com.kadware.em2200.hardwarelib.MainStorageProcessor;
import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.interrupts.AddressingExceptionInterrupt;
import com.kadware.em2200.hardwarelib.misc.AbsoluteAddress;
import com.kadware.em2200.hardwarelib.misc.BaseRegister;
import java.util.Random;

import com.kadware.em2200.hardwarelib.test.instructionProcessor.InstrumentedMainStorageProcessor;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests AbsoluteAddress class - not much to test here
 */
public class Test_BaseRegister {

    private static final Random _random = new Random(System.currentTimeMillis());

    private AccessInfo randomAccessInfo() {
        return new AccessInfo(_random.nextInt(0x100),
                              _random.nextLong());
    }

    @Test
    public void test_voidBankConstructor(
    ) {
        BaseRegister br = new BaseRegister();
        assertTrue(br._voidFlag);
    }

    //TODO need to test various constructors for banks with lower > upper limit, to ensure void flag is set

    @Test
    public void test_loadConstructor(
    ) throws AddressingExceptionInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        int mspSize = 16 * 1024 * 1024;
        MainStorageProcessor[] msps = new MainStorageProcessor[4];
        for (int mx = 0; mx < 4; ++mx) {
            msps[mx] = new InstrumentedMainStorageProcessor(String.format("MSP%d", mx), (short) mx, mspSize);
            InventoryManager.getInstance().addMainStorageProcessor(msps[mx]);
        }

        for (int x = 0; x < 2000; ++x) {
            AbsoluteAddress baseAddress = new AbsoluteAddress((short) _random.nextInt(4),
                                                              0,
                                                              _random.nextInt(mspSize));
            boolean largeSize = _random.nextBoolean();
            int lowerLimit = _random.nextInt(0777);
            int upperLimit = _random.nextInt(0777777);
            int lowerLimitNorm = lowerLimit << (largeSize ? 15 : 9);
            int upperLimitNorm = upperLimit << (largeSize ? 6 : 0) | (largeSize ? 077 : 0);
            if (baseAddress._offset + upperLimitNorm < mspSize) {
                AccessInfo accessLock = randomAccessInfo();
                AccessPermissions gap = new AccessPermissions(false, _random.nextBoolean(), _random.nextBoolean());
                AccessPermissions sap = new AccessPermissions(false, _random.nextBoolean(), _random.nextBoolean());

                long[] data = new long[4];
                if (gap._read) {
                    data[0] |= 0_200000_000000L;
                }
                if (gap._write) {
                    data[0] |= 0_100000_000000L;
                }
                if (sap._read) {
                    data[0] |= 0_020000_000000L;
                }
                if (sap._write) {
                    data[0] |= 0_010000_000000L;
                }
                if (largeSize) {
                    data[0] |= 0_000004_000000L;
                }
                data[0] |= accessLock.get() & 0777777;
                data[1] = ((long) lowerLimit << 27) | upperLimit;
                data[2] = baseAddress._segment;
                data[3] = ((long) baseAddress._upi) << 32 | baseAddress._offset;

                BaseRegister br = new BaseRegister(data);
                assertEquals(gap, br._generalAccessPermissions);
                assertEquals(sap, br._specialAccessPermissions);
                assertEquals(accessLock, br._accessLock);
                assertEquals(br._lowerLimitNormalized > br._upperLimitNormalized, br._voidFlag);
                assertEquals(largeSize, br._largeSizeFlag);
                assertEquals(lowerLimitNorm, br._lowerLimitNormalized);
                assertEquals(upperLimitNorm, br._upperLimitNormalized);
            }
        }

        for (int mx = 0; mx < 4; ++mx) {
            InventoryManager.getInstance().deleteProcessor((short) mx);
        }
    }

}
