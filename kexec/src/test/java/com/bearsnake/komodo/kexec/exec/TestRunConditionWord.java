/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

import com.bearsnake.komodo.baselib.Word36;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestRunConditionWord {

    @Test
    public void testDefaultConstructor() {
        RunConditionWord rcw = new RunConditionWord();
        assertEquals(0, rcw.getWord36());
        assertFalse(rcw.getInhibitPageEjects());
        assertFalse(rcw.getRunRescheduledAfterBoot());
        assertFalse(rcw.getInhibitRunTerminationOnTaskError());
        assertFalse(rcw.getAtLeastOnePriorTaskError());
        assertFalse(rcw.getMostRecentTaskError());
        assertFalse(rcw.getMostRecentActivityAbort());
        assertFalse(rcw.getMostRecentActivityError());
        assertFalse(rcw.getCurrentTaskActivityError());
    }

    @Test
    public void testSettersAndGetters() {
        RunConditionWord rcw = new RunConditionWord();

        rcw.setInhibitPageEjects(true);
        assertTrue(rcw.getInhibitPageEjects());
        assertEquals(Word36.MASK_B3, rcw.getWord36());

        rcw.setRunRescheduledAfterBoot(true);
        assertTrue(rcw.getRunRescheduledAfterBoot());
        assertEquals(Word36.MASK_B3 | Word36.MASK_B4, rcw.getWord36());

        rcw.setInhibitRunTerminationOnTaskError(true);
        assertTrue(rcw.getInhibitRunTerminationOnTaskError());
        assertEquals(Word36.MASK_B3 | Word36.MASK_B4 | Word36.MASK_B5, rcw.getWord36());

        rcw.setAtLeastOnePriorTaskError(true);
        assertTrue(rcw.getAtLeastOnePriorTaskError());

        rcw.setMostRecentTaskError(true);
        assertTrue(rcw.getMostRecentTaskError());

        rcw.setMostRecentActivityAbort(true);
        assertTrue(rcw.getMostRecentActivityAbort());

        rcw.setMostRecentActivityError(true);
        assertTrue(rcw.getMostRecentActivityError());

        rcw.setCurrentTaskActivityError(true);
        assertTrue(rcw.getCurrentTaskActivityError());

        rcw.setCSISetCValue(01234);
        assertEquals(01234, rcw.getCSISetCValue());
        assertEquals(01234, Word36.getT2(rcw.getWord36()));

        rcw.setERSetCValue(05670);
        assertEquals(05670, rcw.getERSetCValue());
        assertEquals(05670, Word36.getT3(rcw.getWord36()));

        // Masking checks for T2 and T3
        rcw.setCSISetCValue(077777); // Should be masked to 07777
        assertEquals(07777, rcw.getCSISetCValue());

        rcw.setERSetCValue(077777); // Should be masked to 07777
        assertEquals(07777, rcw.getERSetCValue());
    }

    @Test
    public void testSetWord36() {
        long value = Word36.MASK_B3 | Word36.MASK_B7 | Word36.MASK_B11;
        value = Word36.setT2(value, 01111);
        value = Word36.setT3(value, 02222);

        RunConditionWord rcw = new RunConditionWord();
        rcw.setWord36(value);

        assertTrue(rcw.getInhibitPageEjects());
        assertFalse(rcw.getRunRescheduledAfterBoot());
        assertFalse(rcw.getInhibitRunTerminationOnTaskError());
        assertTrue(rcw.getAtLeastOnePriorTaskError());
        assertFalse(rcw.getMostRecentTaskError());
        assertFalse(rcw.getMostRecentActivityAbort());
        assertFalse(rcw.getMostRecentActivityError());
        assertTrue(rcw.getCurrentTaskActivityError());

        assertEquals(01111, Word36.getT2(rcw.getWord36()));
        assertEquals(02222, Word36.getT3(rcw.getWord36()));
        assertEquals(value, rcw.getWord36());
    }

    @Test
    public void testTaskStarted() {
        RunConditionWord rcw = new RunConditionWord();
        long value = Word36.MASK_B9 | Word36.MASK_B10 | Word36.MASK_B11 | Word36.MASK_B3;
        rcw.setWord36(value);

        assertTrue(rcw.getInhibitPageEjects());
        assertTrue(rcw.getMostRecentActivityAbort());
        assertTrue(rcw.getMostRecentActivityError());
        assertTrue(rcw.getCurrentTaskActivityError());

        rcw.taskStarted();

        assertTrue(rcw.getInhibitPageEjects()); // Should remain
        assertFalse(rcw.getMostRecentActivityAbort()); // Should be cleared
        assertFalse(rcw.getMostRecentActivityError()); // Should be cleared
        assertFalse(rcw.getCurrentTaskActivityError()); // Should be cleared
        
        assertEquals(Word36.MASK_B3, rcw.getWord36());
    }

    @Test
    public void testToString() {
        RunConditionWord rcw = new RunConditionWord();
        rcw.setInhibitPageEjects(true);
        assertEquals("040000000000", rcw.toString());
    }

    @Test
    public void testFluentSetters() {
        RunConditionWord rcw = new RunConditionWord()
                .setInhibitPageEjects(true)
                .setRunRescheduledAfterBoot(true)
                .setInhibitRunTerminationOnTaskError(true)
                .setCSISetCValue(01)
                .setERSetCValue(02);

        assertTrue(rcw.getInhibitPageEjects());
        assertTrue(rcw.getRunRescheduledAfterBoot());
        assertTrue(rcw.getInhibitRunTerminationOnTaskError());
        assertEquals(01, Word36.getT2(rcw.getWord36()));
        assertEquals(02, Word36.getT3(rcw.getWord36()));
    }

    @Test
    public void testAllT1Bits() {
        // Test bits 3, 4, 5, 7, 8, 9, 10, 11
        long[] masks = {
                Word36.MASK_B3, Word36.MASK_B4, Word36.MASK_B5,
                Word36.MASK_B7, Word36.MASK_B8, Word36.MASK_B9,
                Word36.MASK_B10, Word36.MASK_B11
        };

        for (long mask : masks) {
            RunConditionWord rcw = new RunConditionWord();
            rcw.setWord36(mask);
            assertEquals(mask, rcw.getWord36(), "Failed for mask: " + Long.toOctalString(mask));
        }
    }
}
