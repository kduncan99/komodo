/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.Word36;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestDesignatorRegister {

    @Test
    public void testDefaultConstructor() {
        DesignatorRegister dr = new DesignatorRegister();
        assertFalse(dr.isActivityLevelQueueMonitorEnabled());
        assertFalse(dr.isFaultHandlingInProgress());
        assertFalse(dr.isExecutive24BitIndexingEnabled());
        assertFalse(dr.isQuantumTimerEnabled());
        assertFalse(dr.isDeferrableInterruptEnabled());
        assertEquals(0, dr.getProcessorPrivilege());
        assertFalse(dr.isBasicModeEnabled());
        assertFalse(dr.isExecRegisterSetSelected());
        assertFalse(dr.isCarry());
        assertFalse(dr.isOverflow());
        assertFalse(dr.isCharacteristicUnderflow());
        assertFalse(dr.isCharacteristicOverflow());
        assertFalse(dr.isDivideCheck());
        assertFalse(dr.isOperationTrapEnabled());
        assertFalse(dr.isArithmeticExceptionEnabled());
        assertFalse(dr.getBasicModeBaseRegisterSelection());
        assertFalse(dr.isQuarterWordModeEnabled());
    }

    @Test
    public void testGettersAndSetters() {
        DesignatorRegister dr = new DesignatorRegister();

        dr.setActivityLevelQueueMonitorEnabled(true)
          .setFaultHandlingInProgress(true)
          .setExecutive24BitIndexingEnabled(true)
          .setQuantumTimerEnabled(true)
          .setDeferrableInterruptEnabled(true)
          .setProcessorPrivilege((short) 2)
          .setBasicModeEnabled(true)
          .setExecRegisterSetSelected(true)
          .setCarry(true)
          .setOverflow(true)
          .setCharacteristicUnderflow(true)
          .setCharacteristicOverflow(true)
          .setDivideCheck(true)
          .setOperationTrapEnabled(true)
          .setArithmeticExceptionEnabled(true)
          .setBasicModeBaseRegisterSelection(true)
          .setQuarterWordModeEnabled(true);

        assertTrue(dr.isActivityLevelQueueMonitorEnabled());
        assertTrue(dr.isFaultHandlingInProgress());
        assertTrue(dr.isExecutive24BitIndexingEnabled());
        assertTrue(dr.isQuantumTimerEnabled());
        assertTrue(dr.isDeferrableInterruptEnabled());
        assertEquals(2, dr.getProcessorPrivilege());
        assertTrue(dr.isBasicModeEnabled());
        assertTrue(dr.isExecRegisterSetSelected());
        assertTrue(dr.isCarry());
        assertTrue(dr.isOverflow());
        assertTrue(dr.isCharacteristicUnderflow());
        assertTrue(dr.isCharacteristicOverflow());
        assertTrue(dr.isDivideCheck());
        assertTrue(dr.isOperationTrapEnabled());
        assertTrue(dr.isArithmeticExceptionEnabled());
        assertTrue(dr.getBasicModeBaseRegisterSelection());
        assertTrue(dr.isQuarterWordModeEnabled());
    }

    @Test
    public void testClear() {
        DesignatorRegister dr = new DesignatorRegister();
        dr.setCarry(true).setOverflow(true).setProcessorPrivilege((short) 3);
        dr.clear();

        assertFalse(dr.isCarry());
        assertFalse(dr.isOverflow());
        assertEquals(0, dr.getProcessorPrivilege());
        // Check a few others
        assertFalse(dr.isBasicModeEnabled());
    }

    @Test
    public void testProcessorPrivilegeMasking() {
        DesignatorRegister dr = new DesignatorRegister();
        dr.setProcessorPrivilege((short) 0xFFFF);
        assertEquals(3, dr.getProcessorPrivilege());

        dr.setProcessorPrivilege((short) 4);
        assertEquals(0, dr.getProcessorPrivilege());
    }

    @Test
    public void testWord36RoundTrip() {
        DesignatorRegister dr1 = new DesignatorRegister();
        dr1.setActivityLevelQueueMonitorEnabled(true)
           .setFaultHandlingInProgress(false)
           .setExecutive24BitIndexingEnabled(true)
           .setQuantumTimerEnabled(false)
           .setDeferrableInterruptEnabled(true)
           .setProcessorPrivilege((short) 1)
           .setBasicModeEnabled(false)
           .setExecRegisterSetSelected(true)
           .setCarry(false)
           .setOverflow(true)
           .setCharacteristicUnderflow(false)
           .setCharacteristicOverflow(true)
           .setDivideCheck(false)
           .setOperationTrapEnabled(true)
           .setArithmeticExceptionEnabled(false)
           .setBasicModeBaseRegisterSelection(true)
           .setQuarterWordModeEnabled(false);

        long word = dr1.getWord36();

        DesignatorRegister dr2 = new DesignatorRegister();
        dr2.setWord36(word);

        assertEquals(dr1.isActivityLevelQueueMonitorEnabled(), dr2.isActivityLevelQueueMonitorEnabled());
        assertEquals(dr1.isFaultHandlingInProgress(), dr2.isFaultHandlingInProgress());
        assertEquals(dr1.isExecutive24BitIndexingEnabled(), dr2.isExecutive24BitIndexingEnabled());
        assertEquals(dr1.isQuantumTimerEnabled(), dr2.isQuantumTimerEnabled());
        assertEquals(dr1.isDeferrableInterruptEnabled(), dr2.isDeferrableInterruptEnabled());
        assertEquals(dr1.getProcessorPrivilege(), dr2.getProcessorPrivilege());
        assertEquals(dr1.isBasicModeEnabled(), dr2.isBasicModeEnabled());
        assertEquals(dr1.isExecRegisterSetSelected(), dr2.isExecRegisterSetSelected());
        assertEquals(dr1.isCarry(), dr2.isCarry());
        assertEquals(dr1.isOverflow(), dr2.isOverflow());
        assertEquals(dr1.isCharacteristicUnderflow(), dr2.isCharacteristicUnderflow());
        assertEquals(dr1.isCharacteristicOverflow(), dr2.isCharacteristicOverflow());
        assertEquals(dr1.isDivideCheck(), dr2.isDivideCheck());
        assertEquals(dr1.isOperationTrapEnabled(), dr2.isOperationTrapEnabled());
        assertEquals(dr1.isArithmeticExceptionEnabled(), dr2.isArithmeticExceptionEnabled());
        assertEquals(dr1.getBasicModeBaseRegisterSelection(), dr2.getBasicModeBaseRegisterSelection());
        assertEquals(dr1.isQuarterWordModeEnabled(), dr2.isQuarterWordModeEnabled());
    }

    @Test
    public void testSpecificBitPositions() {
        DesignatorRegister dr = new DesignatorRegister();

        // ActivityLevelQueueMonitorEnabled: MASK_B0 = 1L << 35
        dr.setWord36(Word36.MASK_B0);
        assertTrue(dr.isActivityLevelQueueMonitorEnabled());
        assertEquals(Word36.MASK_B0, dr.getWord36());

        dr.clear();
        // FaultHandlingInProgress: MASK_B6 = 1L << 29
        dr.setWord36(Word36.MASK_B6);
        assertTrue(dr.isFaultHandlingInProgress());
        assertEquals(Word36.MASK_B6, dr.getWord36());

        dr.clear();
        // ProcessorPrivilege: MASK_B14 | MASK_B15 = (1L << 21) | (1L << 20)
        // Shifted by 20 in code
        dr.setProcessorPrivilege((short) 2);
        assertEquals(2L << 20, dr.getWord36());
        assertEquals(Word36.MASK_B14, dr.getWord36());

        dr.clear();
        dr.setProcessorPrivilege((short) 1);
        assertEquals(1L << 20, dr.getWord36());
        assertEquals(Word36.MASK_B15, dr.getWord36());

        dr.clear();
        dr.setProcessorPrivilege((short) 3);
        assertEquals(3L << 20, dr.getWord36());
        assertEquals(Word36.MASK_B14 | Word36.MASK_B15, dr.getWord36());

        dr.clear();
        // BasicModeBaseRegisterSelection: MASK_B31 = 1L << 4
        dr.setWord36(Word36.MASK_B31);
        assertTrue(dr.getBasicModeBaseRegisterSelection());
        assertEquals(Word36.MASK_B31, dr.getWord36());

        dr.clear();
        // QuarterWordModeEnabled: MASK_B32 = 1L << 3
        dr.setWord36(Word36.MASK_B32);
        assertTrue(dr.isQuarterWordModeEnabled());
        assertEquals(Word36.MASK_B32, dr.getWord36());
    }
}
