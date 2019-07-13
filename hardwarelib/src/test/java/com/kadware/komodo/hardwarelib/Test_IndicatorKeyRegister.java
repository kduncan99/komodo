/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import static org.junit.Assert.*;

import com.kadware.komodo.hardwarelib.IndicatorKeyRegister;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for IndexRegister class
 */
public class Test_IndicatorKeyRegister {

    @Test
    public void creator_1(
    ) {
        Assert.assertEquals(0, (new IndicatorKeyRegister()).getW());
    }

    @Test
    public void creator_2(
    ) {
        assertEquals(0_004030_201070l, (new IndicatorKeyRegister(0_004030_201070l)).getW());
    }

    @Test
    public void getShortStatusField(
    ) {
        assertEquals(037, (new IndicatorKeyRegister(0_371122_334455l)).getShortStatusField());
    }

    @Test
    public void getMidInstructionDescription(
    ) {
        assertEquals(03, (new IndicatorKeyRegister(0_003000_000000l)).getMidInstructionDescription());
    }

    @Test
    public void getPendingInterruptInformation(
    ) {
        assertEquals(03, (new IndicatorKeyRegister(0_000300_000000l)).getPendingInterruptInformation());
    }

    @Test
    public void getInterruptClassField(
    ) {
        assertEquals(075, (new IndicatorKeyRegister(0_000075_000000l)).getInterruptClassField());
    }

    @Test
    public void getAccessKey(
    ) {
        assertEquals(0112233, (new IndicatorKeyRegister(0_776655_112233l)).getAccessKey());
    }

    @Test
    public void getInstructionInF0(
    ) {
        assertTrue((new IndicatorKeyRegister(0_004000_000000l).getInstructionInF0()));
    }

    @Test
    public void getExecuteRepeatedInstruction(
    ) {
        assertTrue((new IndicatorKeyRegister(0_002000_000000l).getExecuteRepeatedInstruction()));
    }

    @Test
    public void getBreakpointRegisterMatchCondition(
    ) {
        assertTrue((new IndicatorKeyRegister(0_000400_000000l).getBreakpointRegisterMatchCondition()));
    }

    @Test
    public void getSoftwareBreak(
    ) {
        assertTrue((new IndicatorKeyRegister(0_000200_000000l).getSoftwareBreak()));
    }

    @Test
    public void setShortStatusField(
    ) {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        ikr.setShortStatusField(037);
        assertEquals(037, ikr.getShortStatusField());
    }

    @Test
    public void setMidInstructionDescription(
    ) {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        ikr.setMidInstructionDescription(04);
        assertEquals(04, ikr.getMidInstructionDescription());
    }

    @Test
    public void setPendingInterruptInformation(
    ) {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        ikr.setPendingInterruptInformation(05);
        assertEquals(05, ikr.getPendingInterruptInformation());
    }

    @Test
    public void setInterruptClassField(
    ) {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        ikr.setInterruptClassField(077);
        assertEquals(077, ikr.getInterruptClassField());
    }

    @Test
    public void setAccessKey(
    ) {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        ikr.setAccessKey(0754321);
        assertEquals(0754321, ikr.getAccessKey());
    }

    @Test
    public void setInstructionInF0_false(
    ) {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister(0_777777_77777l);
        ikr.setInstructionInF0(false);
        assertFalse(ikr.getInstructionInF0());
    }

    @Test
    public void setInstructionInF0_true(
    ) {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        ikr.setInstructionInF0(true);
        assertTrue(ikr.getInstructionInF0());
    }

    @Test
    public void setExecuteRepeatedInstruction_false(
    ) {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister(0_777777_77777l);
        ikr.setExecuteRepeatedInstruction(false);
        assertFalse(ikr.getExecuteRepeatedInstruction());
    }

    @Test
    public void setExecuteRepeatedInstruction_true(
    ) {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        ikr.setExecuteRepeatedInstruction(true);
        assertTrue(ikr.getExecuteRepeatedInstruction());
    }

    @Test
    public void setBreakpointRegisterMatchCondition_false(
    ) {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister(0_777777_77777l);
        ikr.setBreakpointRegisterMatchCondition(false);
        assertFalse(ikr.getBreakpointRegisterMatchCondition());
    }

    @Test
    public void setBreakpointRegisterMatchCondition_true(
    ) {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        ikr.setBreakpointRegisterMatchCondition(true);
        assertTrue(ikr.getBreakpointRegisterMatchCondition());
    }

    @Test
    public void setSoftwareBreak_false(
    ) {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister(0_777777_77777l);
        ikr.setSoftwareBreak(false);
        assertFalse(ikr.getSoftwareBreak());
    }

    @Test
    public void setSoftwareBreak_true(
    ) {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister();
        ikr.setSoftwareBreak(true);
        assertTrue(ikr.getSoftwareBreak());
    }

    @Test
    public void clear(
    ) {
        IndicatorKeyRegister ikr = new IndicatorKeyRegister(0_777777_77777l);
        ikr.clear();
        assertEquals(0, ikr.getW());
    }
}
