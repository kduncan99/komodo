/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.AbsoluteAddress;
import com.kadware.komodo.baselib.AccessInfo;
import com.kadware.komodo.baselib.AccessPermissions;
import com.kadware.komodo.hardwarelib.exceptions.*;
import com.kadware.komodo.hardwarelib.interrupts.*;
import java.util.Random;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_InstructionProcessor {

    private static final Random _random = new Random(System.currentTimeMillis());

    private AccessInfo randomAccessInfo() {
        return new AccessInfo(_random.nextInt(0x100), _random.nextLong());
    }

    //  Basic Processor tests ------------------------------------------------------------------------------------------------------

    @Test
    public void canConnect(
    ) {
        InstructionProcessor ip = new InstructionProcessor("IP0", InventoryManager.FIRST_IP_UPI_INDEX);
        assertFalse(ip.canConnect(ip));
    }

    //TODO need more tests of nested class operations

    //  Base Register tests --------------------------------------------------------------------------------------------------------

    @Test
    public void baseRegister_voidBankConstructor(
    ) {
        BaseRegister br = new BaseRegister();
        assertTrue(br._voidFlag);
    }

    @Test
    public void baseRegister_loadConstructor(
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
                //noinspection ConstantConditions
                if (largeSize) {
                    data[0] |= 0_000004_000000L;
                }
                data[0] |= accessLock.get() & 0777777;
                data[1] = ((long) lowerLimit << 27) | upperLimit;
                data[2] = baseAddress._segment;
                data[3] = ((long) baseAddress._upiIndex) << 32 | baseAddress._offset;

                BaseRegister br = new BaseRegister(data);
                assertEquals(gap, br._generalAccessPermissions);
                assertEquals(sap, br._specialAccessPermissions);
                assertEquals(accessLock, br._accessLock);
                assertEquals(br._lowerLimitNormalized > br._upperLimitNormalized, br._voidFlag);
                //noinspection ConstantConditions
                assertEquals(largeSize, br._largeSizeFlag);
                assertEquals(lowerLimitNorm, br._lowerLimitNormalized);
                assertEquals(upperLimitNorm, br._upperLimitNormalized);
            }
        }

        for (int mx = 0; mx < 4; ++mx) {
            InventoryManager.getInstance().deleteProcessor((short) mx);
        }
    }

    //  Indicator Key Register tests -----------------------------------------------------------------------------------------------

    @Test
    public void ikr_creator_1(
    ) {
        assertEquals(0, (new InstructionProcessor.IndicatorKeyRegister()).getW());
    }

    @Test
    public void ikr_creator_2(
    ) {
        assertEquals(0_004030_201070L, (new InstructionProcessor.IndicatorKeyRegister(0_004030_201070L)).getW());
    }

    @Test
    public void ikr_getShortStatusField(
    ) {
        assertEquals(037, (new InstructionProcessor.IndicatorKeyRegister(0_371122_334455L)).getShortStatusField());
    }

    @Test
    public void ikr_getMidInstructionDescription(
    ) {
        assertEquals(03, (new InstructionProcessor.IndicatorKeyRegister(0_003000_000000L)).getMidInstructionDescription());
    }

    @Test
    public void ikr_getPendingInterruptInformation(
    ) {
        assertEquals(03, (new InstructionProcessor.IndicatorKeyRegister(0_000300_000000L)).getPendingInterruptInformation());
    }

    @Test
    public void ikr_getInterruptClassField(
    ) {
        assertEquals(075, (new InstructionProcessor.IndicatorKeyRegister(0_000075_000000L)).getInterruptClassField());
    }

    @Test
    public void ikr_getAccessKey(
    ) {
        assertEquals(0112233, (new InstructionProcessor.IndicatorKeyRegister(0_776655_112233L)).getAccessKey());
    }

    @Test
    public void ikr_getInstructionInF0(
    ) {
        assertTrue((new InstructionProcessor.IndicatorKeyRegister(0_004000_000000L).getInstructionInF0()));
    }

    @Test
    public void ikr_getExecuteRepeatedInstruction(
    ) {
        assertTrue((new InstructionProcessor.IndicatorKeyRegister(0_002000_000000L).getExecuteRepeatedInstruction()));
    }

    @Test
    public void ikr_getBreakpointRegisterMatchCondition(
    ) {
        assertTrue((new InstructionProcessor.IndicatorKeyRegister(0_000400_000000L).getBreakpointRegisterMatchCondition()));
    }

    @Test
    public void ikr_getSoftwareBreak(
    ) {
        assertTrue((new InstructionProcessor.IndicatorKeyRegister(0_000200_000000L).getSoftwareBreak()));
    }

    @Test
    public void ikr_setShortStatusField(
    ) {
        InstructionProcessor.IndicatorKeyRegister ikr = new InstructionProcessor.IndicatorKeyRegister();
        ikr.setShortStatusField(037);
        assertEquals(037, ikr.getShortStatusField());
    }

    @Test
    public void ikr_setMidInstructionDescription(
    ) {
        InstructionProcessor.IndicatorKeyRegister ikr = new InstructionProcessor.IndicatorKeyRegister();
        ikr.setMidInstructionDescription(04);
        assertEquals(04, ikr.getMidInstructionDescription());
    }

    @Test
    public void ikr_setPendingInterruptInformation(
    ) {
        InstructionProcessor.IndicatorKeyRegister ikr = new InstructionProcessor.IndicatorKeyRegister();
        ikr.setPendingInterruptInformation(05);
        assertEquals(05, ikr.getPendingInterruptInformation());
    }

    @Test
    public void ikr_setInterruptClassField(
    ) {
        InstructionProcessor.IndicatorKeyRegister ikr = new InstructionProcessor.IndicatorKeyRegister();
        ikr.setInterruptClassField(077);
        assertEquals(077, ikr.getInterruptClassField());
    }

    @Test
    public void ikr_setAccessKey(
    ) {
        InstructionProcessor.IndicatorKeyRegister ikr = new InstructionProcessor.IndicatorKeyRegister();
        ikr.setAccessKey(0754321);
        assertEquals(0754321, ikr.getAccessKey());
    }

    @Test
    public void ikr_setInstructionInF0_false(
    ) {
        InstructionProcessor.IndicatorKeyRegister ikr = new InstructionProcessor.IndicatorKeyRegister(0_777777_77777L);
        ikr.setInstructionInF0(false);
        assertFalse(ikr.getInstructionInF0());
    }

    @Test
    public void ikr_setInstructionInF0_true(
    ) {
        InstructionProcessor.IndicatorKeyRegister ikr = new InstructionProcessor.IndicatorKeyRegister();
        ikr.setInstructionInF0(true);
        assertTrue(ikr.getInstructionInF0());
    }

    @Test
    public void ikr_setExecuteRepeatedInstruction_false(
    ) {
        InstructionProcessor.IndicatorKeyRegister ikr = new InstructionProcessor.IndicatorKeyRegister(0_777777_77777L);
        ikr.setExecuteRepeatedInstruction(false);
        assertFalse(ikr.getExecuteRepeatedInstruction());
    }

    @Test
    public void ikr_setExecuteRepeatedInstruction_true(
    ) {
        InstructionProcessor.IndicatorKeyRegister ikr = new InstructionProcessor.IndicatorKeyRegister();
        ikr.setExecuteRepeatedInstruction(true);
        assertTrue(ikr.getExecuteRepeatedInstruction());
    }

    @Test
    public void ikr_setBreakpointRegisterMatchCondition_false(
    ) {
        InstructionProcessor.IndicatorKeyRegister ikr = new InstructionProcessor.IndicatorKeyRegister(0_777777_77777L);
        ikr.setBreakpointRegisterMatchCondition(false);
        assertFalse(ikr.getBreakpointRegisterMatchCondition());
    }

    @Test
    public void ikr_setBreakpointRegisterMatchCondition_true(
    ) {
        InstructionProcessor.IndicatorKeyRegister ikr = new InstructionProcessor.IndicatorKeyRegister();
        ikr.setBreakpointRegisterMatchCondition(true);
        assertTrue(ikr.getBreakpointRegisterMatchCondition());
    }

    @Test
    public void ikr_setSoftwareBreak_false(
    ) {
        InstructionProcessor.IndicatorKeyRegister ikr = new InstructionProcessor.IndicatorKeyRegister(0_777777_77777L);
        ikr.setSoftwareBreak(false);
        assertFalse(ikr.getSoftwareBreak());
    }

    @Test
    public void ikr_setSoftwareBreak_true(
    ) {
        InstructionProcessor.IndicatorKeyRegister ikr = new InstructionProcessor.IndicatorKeyRegister();
        ikr.setSoftwareBreak(true);
        assertTrue(ikr.getSoftwareBreak());
    }

    @Test
    public void ikr_clear(
    ) {
        InstructionProcessor.IndicatorKeyRegister ikr = new InstructionProcessor.IndicatorKeyRegister(0_777777_77777L);
        ikr.clear();
        assertEquals(0, ikr.getW());
    }
}
