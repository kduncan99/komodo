/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestIndicatorKeyRegister {

    private static final Random _random = new Random(System.currentTimeMillis());

    @Test
    public void defaultConstructor() {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        assertEquals(0, ikr.getShortStatusField());
        assertEquals(0, ikr.getMidInstructionDescription());
        assertEquals(0, ikr.getPendingInterruptInformation());
        assertEquals(0, ikr.getInterruptClassField());
        assertEquals(0, ikr.getAccessKey().toComposite());
        assertEquals(0, ikr.getWord36());
    }

    @Test
    public void valueConstructor() {
        // S1: 055, S2: 043, S3: 021, H2: 0123456
        // S2 is split into MID (3 bits) and PII (3 bits)
        // 043 octal is 100 011 binary. MID=100 (4), PII=011 (3)
        long value = 0_55_43_21_123456L;
        IndicatorKeyRegister ikr = new IndicatorKeyRegister(value);
        assertEquals(055, ikr.getShortStatusField());
        assertEquals(4, ikr.getMidInstructionDescription());
        assertEquals(3, ikr.getPendingInterruptInformation());
        assertEquals(021, ikr.getInterruptClassField());
        assertEquals(0123456, ikr.getAccessKey().toComposite());
        assertEquals(value, ikr.getWord36());
    }

    @Test
    public void setShortStatusField() {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        ikr.setShortStatusField(077);
        assertEquals(077, ikr.getShortStatusField());
        ikr.setShortStatusField(0177); // Should be masked to 6 bits
        assertEquals(077, ikr.getShortStatusField());
    }

    @Test
    public void setMidInstructionDescription() {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        ikr.setMidInstructionDescription(07);
        assertEquals(07, ikr.getMidInstructionDescription());
        ikr.setMidInstructionDescription(017); // Should be masked to 3 bits
        assertEquals(07, ikr.getMidInstructionDescription());
    }

    @Test
    public void setPendingInterruptInformation() {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        ikr.setPendingInterruptInformation(07);
        assertEquals(07, ikr.getPendingInterruptInformation());
        ikr.setPendingInterruptInformation(017); // Should be masked to 3 bits
        assertEquals(07, ikr.getPendingInterruptInformation());
    }

    @Test
    public void setInterruptClassField() {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        ikr.setInterruptClassField(077);
        assertEquals(077, ikr.getInterruptClassField());
        ikr.setInterruptClassField(0177); // Should be masked to 6 bits
        assertEquals(077, ikr.getInterruptClassField());
    }

    @Test
    public void setAccessKey() {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        ikr.setAccessKey(0777777);
        assertEquals(0777777, ikr.getAccessKey().toComposite());
        ikr.setAccessKey(01777777); // Should be masked to 18 bits
        assertEquals(0777777, ikr.getAccessKey().toComposite());
    }

    @Test
    public void instructionInF0() {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        assertFalse(ikr.getInstructionInF0());
        ikr.setInstructionInF0(true);
        assertTrue(ikr.getInstructionInF0());
        assertEquals(4, ikr.getMidInstructionDescription());
        ikr.setInstructionInF0(false);
        assertFalse(ikr.getInstructionInF0());
        assertEquals(0, ikr.getMidInstructionDescription());
    }

    @Test
    public void executeRepeatedInstruction() {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        assertFalse(ikr.isExecuteRepeatedInstruction());
        ikr.setExecuteRepeatedInstruction(true);
        assertTrue(ikr.isExecuteRepeatedInstruction());
        assertEquals(2, ikr.getMidInstructionDescription());
        ikr.setExecuteRepeatedInstruction(false);
        assertFalse(ikr.isExecuteRepeatedInstruction());
        assertEquals(0, ikr.getMidInstructionDescription());
    }

    @Test
    public void breakpointRegisterMatchCondition() {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        assertFalse(ikr.getBreakpointRegisterMatchCondition());
        ikr.setBreakpointRegisterMatchCondition(true);
        assertTrue(ikr.getBreakpointRegisterMatchCondition());
        assertEquals(4, ikr.getPendingInterruptInformation());
        ikr.setBreakpointRegisterMatchCondition(false);
        assertFalse(ikr.getBreakpointRegisterMatchCondition());
        assertEquals(0, ikr.getPendingInterruptInformation());
    }

    @Test
    public void softwareBreak() {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        assertFalse(ikr.getSoftwareBreak());
        ikr.setSoftwareBreak(true);
        assertTrue(ikr.getSoftwareBreak());
        assertEquals(2, ikr.getPendingInterruptInformation());
        ikr.setSoftwareBreak(false);
        assertFalse(ikr.getSoftwareBreak());
        assertEquals(0, ikr.getPendingInterruptInformation());
    }

    @Test
    public void word36Consistency() {
        for (int i = 0; i < 1000; i++) {
            long originalValue = _random.nextLong() & 0777777777777L;
            IndicatorKeyRegister ikr = new IndicatorKeyRegister(originalValue);
            long newValue = ikr.getWord36();
            assertEquals(originalValue, newValue, String.format("Failed for 0%o", originalValue));
        }
    }

    @Test
    public void accessInfo() {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        ikr.setAccessKey(0123456);
        AccessKey ak = ikr.getAccessKey();
        assertEquals(0123456, ak.toComposite());
        
        AccessInfo ai = ikr.getAccessInfo();
        assertEquals(ak.getDomain(), ai.getDomain());
        assertEquals(ak.getRing(), ai.getRing());
        assertEquals(ak, ikr.getAccessKey());
    }
}
