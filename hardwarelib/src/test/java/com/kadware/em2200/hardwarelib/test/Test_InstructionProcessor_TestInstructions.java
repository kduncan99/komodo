/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test;

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
            "          NOP       DATA",
            "          NOP       DATA",
            "          NOP       DATA",
            "START$*",
            "          LA        A1,DATA",
            "          TEP,Q4    A1,DATA+1",
            "          HALT      077                 . this should be skipped",
            "",
            "          TEP       A1,DATA+1",
            "          HALT      0                   . should not be skipped",
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

    //TODO need special code in assembler for this to work
//    @Test
//    public void testZeroBasic(
//    ) throws MachineInterrupt,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "          $INFO 1 3",
//            "",
//            "$(0)      $LIT",
//            "DATA      + 0",
//            "          + 0777777777777",
//            "          + 01",
//            "",
//            "$(1),START$*",
//            "          TZ        DATA",
//            "          HALT      077                 . this should be skipped",
//            "",
//            "          TZ        DATA+1",
//            "          HALT      076                 . this should be skipped",
//            "",
//            "          TZ        DATA+2",
//            "          J         TARGET1             . should not be skipped",
//            "          HALT      075",
//            "",
//            "TARGET1",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, true);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//    }

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

    //TODO need special code in assembler for this to work
//    @Test
//    public void testNonZeroBasic(
//    ) throws MachineInterrupt,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "          $INFO 1 3",
//            "",
//            "$(0)      $LIT",
//            "DATA      + 0",
//            "          + 0777777777777",
//            "          + 01",
//            "",
//            "$(1),START$*",
//            "          TNZ       DATA",
//            "          J         TARGET2             . should not be skipped",
//            "          HALT      074",
//            "",
//            "TARGET2",
//            "          TNZ       DATA+1",
//            "          J         TARGET3             . should not be skipped",
//            "          HALT      073",
//            "",
//            "TARGET3",
//            "          TNZ       DATA+2",
//            "          HALT      072                 . should be skipped",
//            "",
//            "          HALT      0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasic(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//        startAndWait(processors._instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
//        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());
//
//        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
//        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());
//    }

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

    //TODO
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

    //  TODO Need special assembler code for TP basic

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

    //TODO need special assember code for TN

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

    //  TODO Need Basic *AND* Extended mode versions of all of the above

    //  TODO TLEM
    //  TODO TGZ
    //  TODO TMZG
    //  TODO TNLZ
    //  TODO TLZ
    //  TODO TPZL
    //  TODO TNMZ
    //  TODO TNPZ
    //  TODO TNGZ
    //  TODO TLE
    //  TODO TG
    //  TODO TGM
    //  TODO TW
    //  TODO TNW

    //  TODO DTGM

    //  TODO MTE
    //  TODO MTNE
    //  TODO MTLE
    //  TODO MTG
    //  TODO MTW
    //  TODO MTNW
    //  TODO MATL
    //  TODO MATG

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
        long bank[] = getBank(processors._instructionProcessor, 13);
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
        long bank[] = getBank(processors._instructionProcessor, 2);
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
        long bank[] = getBank(processors._instructionProcessor, 13);
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
        long bank[] = getBank(processors._instructionProcessor, 2);
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
        long bank[] = getBank(processors._instructionProcessor, 13);
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
        long bank[] = getBank(processors._instructionProcessor, 2);
        assertEquals(0L, bank[0]);
        assertEquals(0_007777_777777L, bank[1]);
    }

    //  TODO CR
}
