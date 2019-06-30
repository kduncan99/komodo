/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test.instructionProcessor;

import com.kadware.em2200.hardwarelib.*;
import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.interrupts.*;
import com.kadware.komodo.minalib.AbsoluteModule;
import static org.junit.Assert.*;
import org.junit.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_GeneralStoreInstructions extends BaseFunctions {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Testing Instructions which store data
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void generalStore_PartialWords_QuarterWordMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0),DATA $RES 32",
            "",
            "$(1),START$*",
            "          LA,XU     A0,0444444",
            "          LXI,U     X1,1",
            "          LXM,U     X1,0",
            "",
            "          SA,W      A0,DATA,*X1,B2",
            "          SA,H2     A0,DATA,*X1,B2",
            "          SA,H1     A0,DATA,*X1,B2",
            "          SA,XH2    A0,DATA,*X1,B2",
            "          SA,Q2     A0,DATA,*X1,B2",
            "          SA,Q4     A0,DATA,*X1,B2",
            "          SA,Q3     A0,DATA,*X1,B2",
            "          SA,Q1     A0,DATA,*X1,B2",
            "          SA,S6     A0,DATA,*X1,B2",
            "          SA,S5     A0,DATA,*X1,B2",
            "          SA,S4     A0,DATA,*X1,B2",
            "          SA,S3     A0,DATA,*X1,B2",
            "          SA,S2     A0,DATA,*X1,B2",
            "          SA,S1     A0,DATA,*X1,B2",
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

        long[] bankData = getBank(processors._instructionProcessor, 2);
        assertEquals(0_777777_444444L, bankData[0]);
        assertEquals(0_000000_444444L, bankData[1]);
        assertEquals(0_444444_000000L, bankData[2]);
        assertEquals(0_000000_444444L, bankData[3]);
        assertEquals(0_000444_000000L, bankData[4]);
        assertEquals(0_000000_000444L, bankData[5]);
        assertEquals(0_000000_444000L, bankData[6]);
        assertEquals(0_444000_000000L, bankData[7]);
        assertEquals(0_000000_000044L, bankData[8]);
        assertEquals(0_000000_004400L, bankData[9]);
        assertEquals(0_000000_440000L, bankData[10]);
        assertEquals(0_000044_000000L, bankData[11]);
        assertEquals(0_004400_000000L, bankData[12]);
        assertEquals(0_440000_000000L, bankData[13]);
    }

    @Test
    public void generalStore_PartialWords_ThirdWordMode(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 5",
            "          $INFO 10 1",
            "",
            "$(0),DATA $RES 32",
            "",
            "$(1),START$*",
            "          LA,XU     A0,0444444",
            "          LXI,U     X1,1",
            "          LXM,U     X1,0",
            "",
            "          SA,XH1    A0,DATA,*X1,B2",
            "          SA,T3     A0,DATA,*X1,B2",
            "          SA,T2     A0,DATA,*X1,B2",
            "          SA,T1     A0,DATA,*X1,B2",
            "          SA,XU     A0,DATA,*X1,B2",
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

        long[] bankData = getBank(processors._instructionProcessor, 2);
        assertEquals(0_444444_000000L, bankData[0]);
        assertEquals(0_000000_004444L, bankData[1]);
        assertEquals(0_000044_440000L, bankData[2]);
        assertEquals(0_444400_000000L, bankData[3]);
        assertEquals(0_000000_000000L, bankData[5]);
    }

    @Test
    public void generalStore(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0),DATA $RES 32",
            "",
            "$(1),START$*",
            "          LA,XU     A0,0444444",
            "          LA,U      A1,0444444",
            "          LR,U      R2,010110",
            "          LX,U      X5,020220",
            "          SNA       A0,DATA,,B2",
            "          SNA       A1,DATA+1,,B2",
            "          SMA       A0,DATA+2,,B2",
            "          SMA       A1,DATA+3,,B2",
            "          SR        R2,DATA+4,,B2",
            "          SX        X5,DATA+5,,B2",
            "          DS        A0,DATA+6,,B2",
            "",
            "          LXI,U     X5,0",
            "          LXM,U     X5,0",
            "          SAQW      A1,DATA+16,X5,B2",
            "          LXI,U     X5,010000",
            "          LXM,U     X5,01",
            "          SAQW      A1,DATA+16,X5,B2",
            "          LXI,U     X5,020000",
            "          LXM,U     X5,02",
            "          SAQW      A1,DATA+16,X5,B2",
            "          LXI,U     X5,030000",
            "          LXM,U     X5,03",
            "          SAQW      A1,DATA+16,X5,B2",
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

        long[] bankData = getBank(processors._instructionProcessor, 2);
        assertEquals(0_000000_333333L, bankData[0]);
        assertEquals(0_777777_333333L, bankData[1]);
        assertEquals(0_000000_333333L, bankData[2]);
        assertEquals(0_000000_444444L, bankData[3]);
        assertEquals(0_000000_010110L, bankData[4]);
        assertEquals(0_000000_020220L, bankData[5]);
        assertEquals(0_777777_444444L, bankData[6]);
        assertEquals(0_000000_444444L, bankData[7]);
        assertEquals(0_444000_000000L, bankData[16]);
        assertEquals(0_000444_000000L, bankData[17]);
        assertEquals(0_000000_444000L, bankData[18]);
        assertEquals(0_000000_000444L, bankData[19]);
    }

    @Test
    public void generalStore_FixedValues(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND               .",
            "          $INFO 1 3             .",
            "          $INFO 10 1            .",
            "                                .",
            "$(0),DATA                       .",
            "          + 0343434343434       .",
            "          + 0434343434343       .",
            "          + 0343434343434       .",
            "          + 0434343434343       .",
            "          + 0343434343434       .",
            "          + 0434343434343       .",
            "          + 0343434343434       .",
            "          + 0434343434343       .",
            "                                .",
            "$(1),START$*                    .",
            "          SZ        DATA,,B2    .",
            "          SNZ       DATA+1,,B2  .",
            "          SP1,H1    DATA+2,,B2  .",
            "          SN1       DATA+3,,B2  .",
            "          SFS,H2    DATA+4,,B2  .",
            "          SFZ       DATA+5,,B2  .",
            "          SAS       DATA+6,,B2  .",
            "          SAZ       DATA+7,,B2  .",
            "                                .",
            "          HALT      0           .",
        };

        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        long[] bankData = getBank(processors._instructionProcessor, 2);
        assertEquals(0_000000_000000L, bankData[0]);
        assertEquals(0_777777_777777L, bankData[1]);
        assertEquals(0_000001_343434L, bankData[2]);
        assertEquals(0_777777_777776L, bankData[3]);
        assertEquals(0_343434_050505L, bankData[4]);
        assertEquals(0_606060_606060L, bankData[5]);
        assertEquals(0_040040_040040L, bankData[6]);
        assertEquals(0_060060_060060L, bankData[7]);
    }

    @Test
    public void storeRegisterSet(
    ) throws MachineInterrupt,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 1 3",
            "          $INFO 10 1",
            "",
            "$(0)",
            "DATA      $RES 10         . target for data, based on B2",
            "",
            "$(2)      $LIT            . descriptor data based on B3",
            "ADDR1     $EQU R0",
            "COUNT1    $EQU 5",
            "ADDR2     $EQU A3",
            "COUNT2    $EQU 3",
            "DESCRIPTOR  + COUNT2,ADDR2,COUNT1,ADDR1",
            "",
            "$(1),START$*",
            "          LA,U      A0,1",
            "          LA,U      A1,2",
            "          LA,U      A2,3",
            "          LA,U      A3,4",
            "          LA,U      A4,5",
            "          LA,U      A5,6",
            "          LA,U      A6,7",
            "          LA,U      A7,010",
            "",
            "          LR,U      R0,021",
            "          LR,U      R1,031",
            "          LR,U      R2,041",
            "          LR,U      R3,051",
            "          LR,U      R4,061",
            "          LR,U      R5,071",
            "          LR,U      R6,072",
            "          LR,U      R7,073",
            "",
            "          LA        A10,DESCRIPTOR,,B3",
            "          SRS       A10,DATA+2,,B2",
            "          HALT      0",
        };

        AbsoluteModule absoluteModule = buildCodeExtendedMultibank(source, false);
        assert(absoluteModule != null);
        Processors processors = loadModule(absoluteModule);
        startAndWait(processors._instructionProcessor);

        InventoryManager.getInstance().deleteProcessor(processors._instructionProcessor.getUPI());
        InventoryManager.getInstance().deleteProcessor(processors._mainStorageProcessor.getUPI());

        assertEquals(InstructionProcessor.StopReason.Debug, processors._instructionProcessor.getLatestStopReason());
        assertEquals(0, processors._instructionProcessor.getLatestStopDetail());

        long[] bankData = getBank(processors._instructionProcessor, 2);
        assertEquals(021, bankData[2]);
        assertEquals(031, bankData[3]);
        assertEquals(041, bankData[4]);
        assertEquals(051, bankData[5]);
        assertEquals(061, bankData[6]);
        assertEquals(04, bankData[7]);
        assertEquals(05, bankData[8]);
        assertEquals(06, bankData[9]);
    }

    //TODO Need SRS minalib for illegal reference condition(s)
}
