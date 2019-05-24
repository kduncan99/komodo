/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test.instructionProcessor;

import com.kadware.em2200.hardwarelib.*;
import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.interrupts.*;
import com.kadware.em2200.minalib.AbsoluteModule;
import static org.junit.Assert.*;
import org.junit.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_InstructionProcessor_TestInstructions extends Test_InstructionProcessor {

    @Test
    public void testEvenParityBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 0777777771356",
            "          + 0000000007777",
            "",
            "$(1)",
            "          HALT      075",
            "          HALT      075",
            "          HALT      075",
            "START$*",
            "          LA        A1,DATA",
            "          TEP,Q4    A1,DATA+1",
            "          HALT      077                 . this should be skipped",
            "",
            "          TEP       A1,DATA+1",
            "          HALT      0                   . should not be skipped",
            "          HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, true);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testEvenParityExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      + 0777777771356",
            "          + 0000000007777",
            "",
            "$(1)",
            "          NOP       DATA",
            "          NOP       DATA",
            "          NOP       DATA",
            "START$*",
            "          LA        A1,DATA,,B2",
            "          TEP,Q4    A1,DATA+1,,B2",
            "          HALT      077                 . this should be skipped",
            "",
            "          TEP       A1,DATA+1,,B2",
            "          HALT      0                   . should not be skipped",
            "          HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testOddParityBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 0777777771356",
            "          + 0000000007777",
            "",
            "$(1),START$*",
            "          LA        A1,DATA",
            "          TOP,H2    A1,DATA+1",
            "          HALT      077                 . this should be skipped",
            "",
            "          TOP,Q4    A1,DATA+1",
            "          HALT      0                   . should not be skipped",
            "          HALT      076"
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testOddParityExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      + 0777777771356",
            "          + 0000000007777",
            "",
            "$(1),START$*",
            "          LA        A1,DATA,,B2",
            "          TOP,H2    A1,DATA+1,,B2",
            "          HALT      077                 . this should be skipped",
            "",
            "          TOP,Q4    A1,DATA+1,,B2",
            "          HALT      0                   . should not be skipped",
            "          HALT      076"
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testZeroBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 0",
            "          + 0777777777777",
            "          + 01",
            "",
            "$(1),START$*",
            "          TZ        DATA",
            "          HALT      077                 . this should be skipped",
            "",
            "          TZ        DATA+1",
            "          HALT      076                 . this should be skipped",
            "",
            "          TZ        DATA+2",
            "          J         TARGET1             . should not be skipped",
            "          HALT      075",
            "",
            "TARGET1",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testZeroExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      + 0",
            "          + 0777777777777",
            "          + 01",
            "",
            "$(1),START$*",
            "          TZ        DATA,,B2",
            "          HALT      077                 . this should be skipped",
            "",
            "          TZ        DATA+1,,B2",
            "          HALT      076                 . this should be skipped",
            "",
            "          TZ        DATA+2,,B2",
            "          J         TARGET1             . should not be skipped",
            "          HALT      075",
            "",
            "TARGET1",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testNonZeroBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 0",
            "          + 0777777777777",
            "          + 01",
            "",
            "$(1),START$*",
            "          TNZ       DATA",
            "          J         TARGET2             . should not be skipped",
            "          HALT      074",
            "",
            "TARGET2",
            "          TNZ       DATA+1",
            "          J         TARGET3             . should not be skipped",
            "          HALT      073",
            "",
            "TARGET3",
            "          TNZ       DATA+2",
            "          HALT      072                 . should be skipped",
            "",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testNonZeroExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      + 0",
            "          + 0777777777777",
            "          + 01",
            "",
            "$(1),START$*",
            "          TNZ       DATA,,B2",
            "          J         TARGET2             . should not be skipped",
            "          HALT      074",
            "",
            "TARGET2",
            "          TNZ       DATA+1,,B2",
            "          J         TARGET3             . should not be skipped",
            "          HALT      073",
            "",
            "TARGET3",
            "          TNZ       DATA+2,,B2",
            "          HALT      072                 . should be skipped",
            "",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testPosZeroExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      + 0",
            "          + 0777777777777",
            "          + 01",
            "",
            "$(1),START$*",
            "          TPZ       DATA,,B2",
            "          HALT      071                 . should be skipped",
            "",
            "          TPZ       DATA+1,,B2",
            "          J         TARGET4             . should not be skipped",
            "          HALT      070",
            "",
            "TARGET4",
            "          TPZ       DATA+2,,B2",
            "          J         TARGET5             . should not be skipped",
            "          HALT      067",
            "",
            "TARGET5",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  There is no TMZ for basic mode

    @Test
    public void testMinusZeroExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      + 0",
            "          + 0777777777777",
            "          + 01",
            "",
            "$(1),START$*",
            "          TMZ       DATA,,B2",
            "          J         TARGET6             . should not be skipped",
            "          HALT      066",
            "",
            "TARGET6",
            "          TMZ       DATA+1,,B2",
            "          HALT      065                 . should be skipped",
            "",
            "          TMZ       DATA+2,,B2",
            "          J         TARGET7             . should not be skipped",
            "          HALT      064",
            "",
            "TARGET7",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testPosBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 0",
            "          + 0777777777777",
            "",
            "$(1),START$*",
            "          TP        DATA",
            "          HALT      077        . skipped",
            "",
            "          TP        DATA+1",
            "          J         TARGET1    . not skipped",
            "          HALT      076",
            "",
            "TARGET1",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testPosExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      + 0",
            "          + 0777777777777",
            "",
            "$(1),START$*",
            "          TP        DATA,,B2",
            "          HALT      077        . skipped",
            "",
            "          TP        DATA+1,,B2",
            "          J         TARGET1    . not skipped",
            "          HALT      076",
            "",
            "TARGET1",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testNegBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 0",
            "          + 0777777777777",
            "",
            "$(1),START$*",
            "          TN        DATA",
            "          J         TARGET2    . not skipped",
            "          HALT      075",
            "",
            "TARGET2",
            "          TN        DATA+1",
            "          HALT      074        . skipped",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testNegExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      + 0",
            "          + 0777777777777",
            "",
            "$(1),START$*",
            "          TN        DATA,,B2",
            "          J         TARGET2    . not skipped",
            "          HALT      075",
            "",
            "TARGET2",
            "          TN        DATA+1,,B2",
            "          HALT      074        . skipped",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  No basic mode version of TNOP

    @Test
    public void testNOPExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      + 0",
            "",
            "$(1),START$*",
            "          LXM,U     X2,0",
            "          LXI,U     X2,1",
            "          TNOP      DATA,*X2,B2",
            "          J         TARGET      . never skipped",
            "          HALT      076",
            "",
            "TARGET",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  No basic mode version of TSKP

    @Test
    public void testSkipExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      + 0",
            "",
            "$(1),START$*",
            "          LXM,U     X2,0",
            "          LXI,U     X2,1",
            "          TSKP      DATA,*X2,B2",
            "          HALT      076          . always skipped",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testEqualBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 0",
            "          + 0777777777777",
            "",
            "$(1),START$*",
            "          LA,U      A10,0",
            "          TE        A10,DATA          . should skip",
            "          HALT      077",
            "          TE        A10,DATA+1        . should not skip",
            "          HALT      0",
            "          HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testEqualExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      + 0",
            "          + 0777777777777",
            "",
            "$(1),START$*",
            "          LA,U      A10,0",
            "          TE        A10,DATA,,B2      . should skip",
            "          HALT      077",
            "          TE        A10,DATA+1,,B2    . should not skip",
            "          HALT      0",
            "          HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testNotEqualBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 0",
            "          + 0777777777777",
            "",
            "$(1),START$*",
            "          LA,U      A10,0",
            "          TNE       A10,DATA+1        . should skip",
            "          HALT      077",
            "          TNE       A10,DATA          . should not skip",
            "          HALT      0",
            "          HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testNotEqualExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "DATA      + 0",
            "          + 0777777777777",
            "",
            "$(1),START$*",
            "          LA,U      A10,0",
            "          TNE       A10,DATA+1,,B2    . should skip",
            "          HALT      077",
            "          TNE       A10,DATA,,B2      . should not skip",
            "          HALT      0",
            "          HALT      076",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testLessOrEqualToModifierBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "ARM       + 000135,0471234",
            "",
            "$(1),START$*",
            "          LXI,U     X5,2",
            "          LXM,U     X5,061234",
            "          TLEM      X5,ARM            . should not skip",
            "          TNGM,S5   X5,ARM            . alias for TLEM, should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(2,processors._instructionProcessor.getExecOrUserXRegister(5).getXI());
        assertEquals(061240,processors._instructionProcessor.getExecOrUserXRegister(5).getXM());
    }

    @Test
    public void testLessOrEqualToModifierExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "ARM       + 000135,0471234",
            "",
            "$(1),START$*",
            "          LXI,U     X5,2",
            "          LXM,U     X5,061234",
            "          TLEM      X5,ARM,,B2        . should not skip",
            "          TNGM,S5   X5,ARM,,B2        . alias for TLEM, should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        assertEquals(2,processors._instructionProcessor.getExecOrUserXRegister(5).getXI());
        assertEquals(061240,processors._instructionProcessor.getExecOrUserXRegister(5).getXM());
    }

    //  no basic mode version of TGZ

    @Test
    public void testGreaterThanZeroExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "TEST      + 01,0777776",
            "",
            "$(1),START$*",
            "          TGZ,XH2   TEST,,B2          . should not skip",
            "          TGZ       TEST,,B2          . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  no basic mode version of TMZG

    @Test
    public void testMinusZeroOrGreaterThanZeroExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 5",
            "",
            "$(0)      $LIT",
            "TEST      + 0777775000002",
            "",
            "$(1),START$*",
            "          TMZG,XH1  TEST,,B2          . should not skip",
            "          TMZG,XH2  TEST,,B2          . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  no basic mode version of TNLZ

    @Test
    public void testNotLessThanZeroExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 5",
            "",
            "$(0)      $LIT",
            "DATA      + 0555500007775",
            "",
            "$(1),START$*",
            "          TNLZ,T3   DATA,,B2          . should not skip",
            "          TNLZ,T2   DATA,,B2          . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  no basic mode version of TLZ

    @Test
    public void testLessThanZeroExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 5",
            "",
            "$(0)      $LIT",
            "DATA      + 0,0777775",
            "",
            "$(1),START$*",
            "          TLZ,H2    DATA,,B2          . should not skip",
            "          TLZ,XH2   DATA,,B2          . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  no basic mode version of TPZL

    @Test
    public void testPositiveZeroOrLessThanZeroExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 5",
            "",
            "$(0)      $LIT",
            "",
            "$(1),START$*",
            "          TPZL,U    5                 . should not skip",
            "          TPZL,XU   -5                . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  no basic mode version of TNMZ

    @Test
    public void testNotMinusZeroExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 0777777777777",
            "",
            "$(1),START$*",
            "          TNMZ      DATA,,B2          . should not skip",
            "          TNMZ,Q1   DATA,,B2          . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  no basic mode version of TNPZ

    @Test
    public void testNotPositiveZeroExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 000111222333",
            "",
            "$(1),START$*",
            "          TNPZ,Q1   DATA,,B2          . should not skip",
            "          TNPZ,Q2   DATA,,B2          . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  no basic mode version of TNGZ

    @Test
    public void testNotGreaterThanZeroExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 5",
            "",
            "$(0)      $LIT",
            "DATA      + 0444555666777",
            "",
            "$(1),START$*",
            "          TNGZ,H1   DATA,,B2          . should not skip",
            "          TNMZ,XH1  DATA,,B2          . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testLessThanOrEqualBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 062,003567",
            "",
            "$(1),START$*",
            "          LA,U      A9,03567",
            "          TLE       A9,DATA           . should not skip",
            "          TNG,H1    A9,DATA           . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
            };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testLessThanOrEqualExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 062,003567",
            "",
            "$(1),START$*",
            "          LA,U      A9,03567",
            "          TLE       A9,DATA,,B2       . should not skip",
            "          TNG,H1    A9,DATA,,B2       . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
            };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testGreaterBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA1     + 000074416513",
            "DATA2     + 055167",
            "COMP1     + 02,211334",
            "COMP2     + 077665215761",
            "",
            "$(1),START$*",
            "          LA        A3,DATA1",
            "          LA        A8,DATA2",
            "          TG        A3,COMP1          . should not skip",
            "          TG,H1     A8,COMP2          . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testGreaterExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA1     + 000074416513",
            "DATA2     + 055167",
            "COMP1     + 02,211334",
            "COMP2     + 077665215761",
            "",
            "$(1),START$*",
            "          LA        A3,DATA1,,B2",
            "          LA        A8,DATA2,,B2",
            "          TG        A3,COMP1,,B2      . should not skip",
            "          TG,H1     A8,COMP2,,B2      . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  No TGM for basic mode

    @Test
    public void testGreaterMagnitudeExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 0777777777577",
            "",
            "$(1),START$*",
            "          LA,U      A3,0144",
            "          TGM       A3,DATA,,B2       . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testWithinRangeBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA1     + 0443",
            "",
            "$(1),START$*",
            "STEP1",
            "          LA,U      A2,0441",
            "          LA,U      A3,0443",
            "          TW        A2,DATA1          . should skip",
            "          HALT      077               . should not happen",
            "",
            "STEP2",
            "          LA,U      A2,0300",
            "          LA,U      A3,0301",
            "          TW,U      A2,0300           . no skip, A2 is not less than 0300",
            "          J         STEP3",
            "          HALT      076",
            "",
            "STEP3",
            "          LA,U      A2,0277",
            "          LA,U      A3,0277",
            "          TW,U      A2,0300           . no skip, A3 is less than 0300",
            "          J         DONE",
            "          HALT      075",
            "",
            "DONE",
            "          HALT      0                 . should happen",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testWithinRangeExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA1     + 0443",
            "",
            "$(1),START$*",
            "STEP1",
            "          LA,U      A2,0441",
            "          LA,U      A3,0443",
            "          TW        A2,DATA1,,B2      . should skip",
            "          HALT      077               . should not happen",
            "",
            "STEP2",
            "          LA,U      A2,0300",
            "          LA,U      A3,0301",
            "          TW,U      A2,0300           . no skip, A2 is not less than 0300",
            "          J         STEP3",
            "          HALT      076",
            "",
            "STEP3",
            "          LA,U      A2,0277",
            "          LA,U      A3,0277",
            "          TW,U      A2,0300           . no skip, A3 is less than 0300",
            "          J         DONE",
            "          HALT      075",
            "",
            "DONE",
            "          HALT      0                 . should happen",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testNotWithinRangeBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        //Skip NI if (U) ≤ (Aa) or (U) > (Aa+1)
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA1     + 0443",
            "",
            "$(1),START$*",
            "STEP1",
            "          LA,U      A2,0441",
            "          LA,U      A3,0443",
            "          TNW       A2,DATA1          . should not skip",
            "          J         STEP2",
            "          HALT      077               . should not happen",
            "",
            "STEP2",
            "          LA,U      A2,0300",
            "          LA,U      A3,0301",
            "          TNW,U     A2,0300           . skips, A2 is not less than 0300",
            "          HALT      076",
            "",
            "STEP3",
            "          LA,U      A2,0277",
            "          LA,U      A3,0277",
            "          TNW,U     A2,0300           . skips, A3 is less than 0300",
            "          HALT      075",
            "",
            "DONE",
            "          HALT      0                 . should happen",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testNotWithinRangeExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA1     + 0443",
            "",
            "$(1),START$*",
            "STEP1",
            "          LA,U      A2,0441",
            "          LA,U      A3,0443",
            "          TNW       A2,DATA1,,B2      . should not skip",
            "          J         STEP2",
            "          HALT      077               . should not happen",
            "",
            "STEP2",
            "          LA,U      A2,0300",
            "          LA,U      A3,0301",
            "          TNW,U     A2,0300           . skips, A2 is not less than 0300",
            "          HALT      076",
            "",
            "STEP3",
            "          LA,U      A2,0277",
            "          LA,U      A3,0277",
            "          TNW,U     A2,0300           . skips, A3 is less than 0300",
            "          HALT      075",
            "",
            "DONE",
            "          HALT      0                 . should happen",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  No DTGM for basic mode

    @Test
    public void testDoubleTestGreaterMagnitudeExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 0777777777577",
            "          + 0222222222222",
            "",
            "$(1),START$*",
            "STEP1",
            "          LA,U      A1,0200",
            "          LA        A2,(0555555555555),,B2",
            "          DTGM      A1,DATA,,B2       . should not skip",
            "          J         DONE",
            "          HALT      077               . should not happen",
            "",
            "DONE",
            "          HALT      0                 . should happen",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  No MTE for basic mode

    @Test
    public void testMaskedTestEqualExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 0253444123457",
            "",
            "$(1),START$*",
            "          LR        R2,(0777000000001),,B2",
            "          LA        A2,(0253012333403),,B2",
            "          MTE       A2,DATA,,B2       . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  No MTNE for basic mode

    @Test
    public void testMaskedTestNotEqualExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 0253444123457",
            "",
            "$(1),START$*",
            "          LR        R2,(0777000000001),,B2",
            "          LA        A2,(0253012333403),,B2",
            "          MTNE      A2,DATA,,B2       . should not skip",
            "          HALT      0                 . should happen",
            "          HALT      077               . should not happen",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  No MTLE for basic mode
    //  MTNG is an alias for MTLE

    @Test
    public void testMaskedTestLessThanOrEqualExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "SIX       + 044444012034",
            "",
            "$(1),START$*",
            "          LR,U      R2,077",
            "          LA        A1,(0123456012345),,B2",
            "          MTLE      A1,SIX,,B2        . should skip",
            "          HALT      077               . should not happen",
            "          MTNG      A1,SIX,,B2        . should skip",
            "          HALT      076               . should not happen",
            "          HALT      0                 . should stop here",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, true);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  No MTG for basic mode

    @Test
    public void testMaskedTestGreaterExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 044444012034",
            "",
            "$(1),START$*",
            "          LR,U      R2,077",
            "          LA        A3,(0123456012345),,B2",
            "          MTG       A3,DATA,,B2       . should not skip",
            "          HALT      0                 . should stop here",
            "          HALT      077               . should not happen",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, true);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  No MTW for basic mode

    @Test
    public void testMaskedTestWithinRangeExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 066",
            "",
            "$(1),START$*",
            "          LR,U      R2,45",
            "          LA        A1,(012345000123),,B2",
            "          LA        A2,(0115451234777),,B2",
            "          MTW       A1,DATA,,B2       . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should stop here",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, true);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  No MTNW for basic mode

    @Test
    public void testMaskedTestNotWithinRangeExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 0711711",
            "",
            "$(1),START$*",
            "          LR        R2,(0543321),,B2",
            "          LA        A6,(01),,B2",
            "          LA        A7,(0144),,B2",
            "          MTNW      A6,DATA,,B2       . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should stop here",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, true);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  No MATL for basic mode

    @Test
    public void testMaskedAlphaTestLessThanOrEqualExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 0311753276514",
            "",
            "$(1),START$*",
            "          LR        R2,(0466123111111),,B2",
            "          LA        A7,(0157724561),,B2",
            "          MATL      A7,DATA,,B2       . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should stop here",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, true);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    //  No MATG for basic mode

    @Test
    public void testMaskedAlphaTestGreaterExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      + 0311753276514",
            "",
            "$(1),START$*",
            "          LR        R2,(0466123111111),,B2",
            "          LA        A7,(0157724561),,B2",
            "          MATG      A7,DATA,,B2       . should not skip",
            "          HALT      0                 . should stop here",
            "          HALT      077               . should not happen",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, true);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testAndSetBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "CLEAR     + 0",
            "SET       + 0770000,0",
            "",
            "$(1),START$*",
            "          TS        CLEAR",
            "          TS        SET",
            "          HALT      077               . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01000+13, processors._instructionProcessor.getLatestStopDetail());
        long[] bank = getBank(processors._instructionProcessor, 13);
        assertEquals(0_010000_000000L, bank[0]);
        assertEquals(0_0770000_000000L, bank[1]);
    }

    @Test
    public void testAndSetExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "CLEAR     + 0",
            "SET       + 0770000,0",
            "",
            "$(1),START$*",
            "          TS        CLEAR,,B2",
            "          TS        SET,,B2",
            "          HALT      077               . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01000+13, processors._instructionProcessor.getLatestStopDetail());
        long[] bank = getBank(processors._instructionProcessor, 2);
        assertEquals(0_010000_000000L, bank[0]);
        assertEquals(0_0770000_000000L, bank[1]);
    }

    @Test
    public void testAndSetAndSkipBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "CLEAR     + 0",
            "SET       + 0770000,0",
            "",
            "$(1),START$*",
            "          TSS       CLEAR",
            "          HALT      077               . should skip this",
            "          TSS       SET",
            "          HALT      0                 . we should stop here",
            "          HALT      076               . should not get here"
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        long[] bank = getBank(processors._instructionProcessor, 13);
        assertEquals(0_010000_000000L, bank[0]);
        assertEquals(0_0770000_000000L, bank[1]);
    }

    @Test
    public void testAndSetAndSkipExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)      $LIT",
            "CLEAR     + 0",
            "SET       + 0770000,0",
            "",
            "$(1),START$*",
            "          TSS       CLEAR,,B2",
            "          HALT      077               . should skip this",
            "          TSS       SET,,B2",
            "          HALT      0                 . we should stop here",
            "          HALT      076               . should not get here"
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        long[] bank = getBank(processors._instructionProcessor, 2);
        assertEquals(0_010000_000000L, bank[0]);
        assertEquals(0_0770000_000000L, bank[1]);
    }

    @Test
    public void testAndClearAndSkipBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "CLEAR     + 0",
            "SET       + 0777777,0777777",
            "",
            "$(1),START$*",
            "          TCS       SET",
            "          HALT      077               . should skip this",
            "          TCS       CLEAR",
            "          HALT      0,                . should stop here",
            "          HALT      077               . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        long[] bank = getBank(processors._instructionProcessor, 13);
        assertEquals(0L, bank[0]);
        assertEquals(0_007777_777777L, bank[1]);
    }

    @Test
    public void testAndClearAndSkipExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "CLEAR     + 0",
            "SET       + 0777777,0777777",
            "",
            "$(1),START$*",
            "          TCS       SET,,B2",
            "          HALT      077               . should skip this",
            "          TCS       CLEAR,,B2",
            "          HALT      0,                . should stop here",
            "          HALT      077               . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        long[] bank = getBank(processors._instructionProcessor, 2);
        assertEquals(0L, bank[0]);
        assertEquals(0_007777_777777L, bank[1]);
    }

    @Test
    public void testConditionalReplaceBasic(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA1     + 010",
            "DATA2     + 014",
            "",
            "$(1),START$*",
            "          LA,U      A0,010",
            "          LA,U      A1,020",
            "          LA,U      A2,030",
            "          CR        A0,DATA1          . should skip NI",
            "          HALT      077",
            "",
            "          CR        A1,DATA2          . should not skip NI",
            "          HALT      0                 . should stop get here",
            "          HALT      076               . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        long[] bank = getBank(processors._instructionProcessor, 13);
        assertEquals(020L, bank[0]);
        assertEquals(014L, bank[1]);
    }

    @Test
    public void testConditionalReplaceBasicBadPP(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA1     + 010",
            "DATA2     + 014",
            "",
            "$(1),START$*",
            "          LA,U      A0,010",
            "          LA,U      A1,020",
            "          LA,U      A2,030",
            "          CR        A0,DATA1          . should skip NI",
            "          HALT      077",
            "",
            "          CR        A1,DATA2          . should not skip NI",
            "          HALT      0                 . should stop get here",
            "          HALT      076               . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01016, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testConditionalReplaceExtended(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA1     + 010",
            "DATA2     + 014",
            "",
            "$(1),START$*",
            "          LA,U      A0,010",
            "          LA,U      A1,020",
            "          LA,U      A2,030",
            "          CR        A0,DATA1,,B2      . should skip NI",
            "          HALT      077",
            "",
            "          CR        A1,DATA2,,B2      . should not skip NI",
            "          HALT      0                 . should stop get here",
            "          HALT      076               . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        processors._instructionProcessor.getDesignatorRegister().setProcessorPrivilege(0);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
        long[] bank = getBank(processors._instructionProcessor, 2);
        assertEquals(020L, bank[0]);
        assertEquals(014L, bank[1]);
    }

    @Test
    public void testReferenceViolationBasic1(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      +0",
            "",
            "$(1),START$*",
            "          TZ        DATA              . should skip",
            "          HALT      077               . should skip this",
            "          LXM,U     X5,0100",
            "          TZ        DATA,X5           . should fail",
            "          HALT      076               . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01010, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testReferenceViolationBasic2(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $BASIC",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      +0",
            "",
            "$(1),START$*",
            "          DTE       A0,DATA           . should fail",
            "          HALT      076               . should not get here",
            "          HALT      077               . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01010, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testReferenceViolationExtended1(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      +0",
            "",
            "$(1),START$*",
            "          TZ        DATA,,B2          . should skip",
            "          HALT      077               . should skip this",
            "          LXM,U     X5,0100",
            "          TZ        DATA,X5,,B2       . should fail",
            "          HALT      076               . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01010, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testReferenceViolationExtended2(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      +0",
            "",
            "$(1),START$*",
            "          TZ        DATA,,B2          . should skip",
            "          HALT      077               . should skip this",
            "          TZ        DATA,,B3          . should fail",
            "          HALT      076               . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01010, processors._instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testReferenceViolationExtended3(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "          $INFO 1 3",
            "",
            "$(0)      $LIT",
            "DATA      +0",
            "",
            "$(1),START$*",
            "          DTE       A0,DATA,,B2       . should fail",
            "          HALT      076               . should not get here",
            "          HALT      077               . should not get here",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(01010, processors._instructionProcessor.getLatestStopDetail());
    }
}
