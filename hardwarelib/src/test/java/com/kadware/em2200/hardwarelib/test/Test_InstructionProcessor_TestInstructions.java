/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.hardwarelib.*;
import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.interrupts.*;
import com.kadware.em2200.hardwarelib.misc.*;
import com.kadware.em2200.minalib.AbsoluteModule;
import static org.junit.Assert.*;
import org.junit.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_InstructionProcessor_TestInstructions extends Test_InstructionProcessor {

    @Test
    public void testEvenParity(
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
    public void testOddParity(
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
    public void testZero(
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
    public void testNonZero(
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
    public void testPosZero(
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

    @Test
    public void testMinusZero(
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
    public void testPos(
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
    public void testNeg(
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

    @Test
    public void testNOP(
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

    @Test
    public void testSkip(
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
    public void testEqual(
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
    public void testNotEqual(
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

    //  TLEM
    //  TGZ
    //  TMZG
    //  TNLZ
    //  TLZ
    //  TPZL
    //  TNMZ
    //  TNPZ
    //  TNGZ
    //  TLE
    //  TG
    //  TGM
    //  TW
    //  TNW

    //  DTGM

    //  MTE
    //  MTNE
    //  MTLE
    //  MTG
    //  MTW
    //  MTNW
    //  MATL
    //  MATG

    //  TS
    //  TSS
    //  TCS
    //  CR
}
