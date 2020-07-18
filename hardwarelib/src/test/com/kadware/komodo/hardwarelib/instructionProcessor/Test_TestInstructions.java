/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.exceptions.BinaryLoadException;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.CannotConnectException;
import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.exceptions.UPIProcessorTypeException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import org.junit.*;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_TestInstructions extends BaseFunctions {

    @After
    public void after(
    ) throws UPINotAssignedException {
        clear();
    }

    @Test
    public void testEvenParityBasic(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE  'GEN$DEFS'",
            "",
            "          DR$SETQWORD                   . set q-word mode",
            "          LA        A1,DATA",
            "          TEP,Q4    A1,DATA+1",
            "          HALT      077                 . this should be skipped",
            "",
            "          TEP       A1,DATA+1",
            "          HALT      0                   . should not be skipped",
            "          HALT      076",
            "",
            "DATA      + 0777777771356",
            "          + 000000007777"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testEvenParityExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE  'GEN$DEFS'",
            "",
            "$(0)",
            "DATA      + 0777777771356",
            "          + 0000000007777",
            "",
            "$(1)      $LIT",
            "          DR$SETQWORD                   . set q-word mode",
            "          LA        A1,DATA,,B2",
            "          TEP,Q4    A1,DATA+1,,B2",
            "          HALT      077                 . this should be skipped",
            "",
            "          TEP       A1,DATA+1,,B2",
            "          HALT      0                   . should not be skipped",
            "          HALT      076"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testOddParityBasic(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE  'GEN$DEFS'",
            "",
            "          DR$SETQWORD                   . set q-word mode",
            "          LA        A1,DATA",
            "          TOP,H2    A1,DATA+1",
            "          HALT      077                 . this should be skipped",
            "",
            "          TOP,Q4    A1,DATA+1",
            "          HALT      0                   . should not be skipped",
            "          HALT      076",
            "",
            "DATA      + 0777777771356",
            "          + 000000007777"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testOddParityExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE  'GEN$DEFS'",
            "",
            "$(0)",
            "DATA      + 0777777771356",
            "          + 0000000007777",
            "",
            "$(1)      $LIT",
            "          DR$SETQWORD                   . set q-word mode",
            "          LA        A1,DATA,,B2",
            "          TOP,H2    A1,DATA+1,,B2",
            "          HALT      077                 . this should be skipped",
            "",
            "          TOP,Q4    A1,DATA+1,,B2",
            "          HALT      0                   . should not be skipped",
            "          HALT      076"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testZeroBasic(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
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
            "",
            "DATA      + 0",
            "          + 0777777777777",
            "          + 01"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testZeroExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0",
            "          + 0777777777777",
            "          + 01",
            "",
            "$(1)      $LIT",
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
            "          HALT      0"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testNonZeroBasic(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
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
            "",
            "DATA      + 0",
            "          + 0777777777777",
            "          + 01"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testNonZeroExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0",
            "          + 0777777777777",
            "          + 01",
            "",
            "$(1)      $LIT",
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
            "          HALT      0"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testPosZeroExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0",
            "          + 0777777777777",
            "          + 01",
            "",
            "$(1)      $LIT",
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
            "          HALT      0"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  There is no TMZ for basic mode

    @Test
    public void testMinusZeroExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0",
            "          + 0777777777777",
            "          + 01",
            "",
            "$(1)      $LIT",
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
            "          HALT      0"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testPosBasic(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          TP        DATA",
            "          HALT      077        . skipped",
            "",
            "          TP        DATA+1",
            "          J         TARGET1    . not skipped",
            "          HALT      076",
            "",
            "TARGET1",
            "          HALT      0",
            "",
            "DATA      + 0",
            "          + 0777777777777"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testPosExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0",
            "          + 0777777777777",
            "",
            "$(1)      $LIT",
            "          TP        DATA,,B2",
            "          HALT      077        . skipped",
            "",
            "          TP        DATA+1,,B2",
            "          J         TARGET1    . not skipped",
            "          HALT      076",
            "",
            "TARGET1",
            "          HALT      0"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testNegBasic(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          TN        DATA",
            "          J         TARGET2    . not skipped",
            "          HALT      075",
            "",
            "TARGET2",
            "          TN        DATA+1",
            "          HALT      074        . skipped",
            "          HALT      0",
            "",
            "DATA      + 0",
            "          + 0777777777777"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testNegExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0",
            "          + 0777777777777",
            "",
            "$(1)      $LIT",
            "          TN        DATA,,B2",
            "          J         TARGET2    . not skipped",
            "          HALT      075",
            "",
            "TARGET2",
            "          TN        DATA+1,,B2",
            "          HALT      074        . skipped",
            "          HALT      0",
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  No basic mode version of TNOP

    @Test
    public void testNOPExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0",
            "",
            "$(1)      $LIT",
            "          LXM,U     X2,0",
            "          LXI,U     X2,1",
            "          TNOP      DATA,*X2,B2",
            "          J         TARGET      . never skipped",
            "          HALT      076",
            "",
            "TARGET",
            "          HALT      0",
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  No basic mode version of TSKP

    @Test
    public void testSkipExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0",
            "",
            "$(1)      $LIT",
            "          LXM,U     X2,0",
            "          LXI,U     X2,1",
            "          TSKP      DATA,*X2,B2",
            "          HALT      076          . always skipped",
            "          HALT      0"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testEqualBasic(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          LA,U      A10,0",
            "          TE        A10,DATA          . should skip",
            "          HALT      077",
            "          TE        A10,DATA+1        . should not skip",
            "          HALT      0",
            "          HALT      076",
            "",
            "DATA      + 0",
            "          + 0777777777777"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testEqualExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0",
            "          + 0777777777777",
            "",
            "$(1)      $LIT",
            "          LA,U      A10,0",
            "          TE        A10,DATA,,B2      . should skip",
            "          HALT      077",
            "          TE        A10,DATA+1,,B2    . should not skip",
            "          HALT      0",
            "          HALT      076"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testNotEqualBasic(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          LA,U      A10,0",
            "          TNE       A10,DATA+1        . should skip",
            "          HALT      077",
            "          TNE       A10,DATA          . should not skip",
            "          HALT      0",
            "          HALT      076",
            "",
            "DATA      + 0",
            "          + 0777777777777"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testNotEqualExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0",
            "          + 0777777777777",
            "",
            "$(1)      $LIT",
            "          LA,U      A10,0",
            "          TNE       A10,DATA+1,,B2    . should skip",
            "          HALT      077",
            "          TNE       A10,DATA,,B2      . should not skip",
            "          HALT      0",
            "          HALT      076"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testLessOrEqualToModifierBasic(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          LXI,U     X5,2",
            "          LXM,U     X5,061234",
            "          TLEM      X5,ARM            . should not skip",
            "          TNGM,S5   X5,ARM            . alias for TLEM, should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
            "",
            "ARM       + 000135,0471234"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(2, _instructionProcessor.getExecOrUserXRegister(5).getXI());
        Assert.assertEquals(061240, _instructionProcessor.getExecOrUserXRegister(5).getXM());
    }

    @Test
    public void testLessOrEqualToModifierExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "ARM       + 000135,0471234",
            "",
            "$(1)      $LIT",
            "          LXI,U     X5,2",
            "          LXM,U     X5,061234",
            "          TLEM      X5,ARM,,B2        . should not skip",
            "          TNGM,S5   X5,ARM,,B2        . alias for TLEM, should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(2, _instructionProcessor.getExecOrUserXRegister(5).getXI());
        Assert.assertEquals(061240, _instructionProcessor.getExecOrUserXRegister(5).getXM());
    }

    //  no basic mode version of TGZ

    @Test
    public void testGreaterThanZeroExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE  'GEN$DEFS'",
            "",
            "$(0)",
            "TEST      + 01,0777776",
            "",
            "$(1)      $LIT",
            "          DR$CLRQWORD",
            "          TGZ,XH2   TEST,,B2          . should not skip",
            "          TGZ       TEST,,B2          . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  no basic mode version of TMZG

    @Test
    public void testMinusZeroOrGreaterThanZeroExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE  'GEN$DEFS'",
            "",
            "$(0)",
            "TEST      + 0777775000002",
            "",
            "$(1)      $LIT",
            "          DR$CLRQWORD",
            "          TMZG,XH1  TEST,,B2          . should not skip",
            "          TMZG,XH2  TEST,,B2          . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  no basic mode version of TNLZ

    @Test
    public void testNotLessThanZeroExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE  'GEN$DEFS'",
            "",
            "$(0)",
            "DATA      + 0555500007775",
            "",
            "$(1)      $LIT",
            "          DR$CLRQWORD",
            "          TNLZ,T3   DATA,,B2          . should not skip",
            "          TNLZ,T2   DATA,,B2          . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  no basic mode version of TLZ

    @Test
    public void testLessThanZeroExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE  'GEN$DEFS'",
            "",
            "$(0)",
            "DATA      + 0,0777775",
            "",
            "$(1)      $LIT",
            "          DR$CLRQWORD",
            "          TLZ,H2    DATA,,B2          . should not skip",
            "          TLZ,XH2   DATA,,B2          . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  no basic mode version of TPZL

    @Test
    public void testPositiveZeroOrLessThanZeroExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE  'GEN$DEFS'",
            "",
            "$(1)      $LIT",
            "          DR$CLRQWORD",
            "          TPZL,U    5                 . should not skip",
            "          TPZL,XU   -5                . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  no basic mode version of TNMZ

    @Test
    public void testNotMinusZeroExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE  'GEN$DEFS'",
            "",
            "$(0)",
            "DATA      + 0777777777777",
            "",
            "$(1)      $LIT",
            "          DR$SETQWORD",
            "          TNMZ      DATA,,B2          . should not skip",
            "          TNMZ,Q1   DATA,,B2          . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  no basic mode version of TNPZ

    @Test
    public void testNotPositiveZeroExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE  'GEN$DEFS'",
            "",
            "$(0)",
            "DATA      + 000111222333",
            "",
            "$(1)      $LIT",
            "          DR$SETQWORD",
            "          TNPZ,Q1   DATA,,B2          . should not skip",
            "          TNPZ,Q2   DATA,,B2          . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  no basic mode version of TNGZ

    @Test
    public void testNotGreaterThanZeroExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE  'GEN$DEFS'",
            "",
            "$(0)",
            "DATA      + 0444555666777",
            "",
            "$(1)      $LIT",
            "          DR$CLRQWORD",
            "          TNGZ,H1   DATA,,B2          . should not skip",
            "          TNMZ,XH1  DATA,,B2          . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testLessThanOrEqualBasic(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          LA,U      A9,03567",
            "          TLE       A9,DATA           . should not skip",
            "          TNG,H1    A9,DATA           . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
            "",
            "DATA      + 062,003567"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testLessThanOrEqualExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE  'GEN$DEFS'",
            "",
            "$(0)",
            "DATA      + 062,003567",
            "",
            "$(1)      $LIT",
            "          DR$CLRQWORD",
            "          LA,U      A9,03567",
            "          TLE       A9,DATA,,B2       . should not skip",
            "          TNG,H1    A9,DATA,,B2       . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen"
            };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testGreaterBasic(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          LA        A3,DATA1",
            "          LA        A8,DATA2",
            "          TG        A3,COMP1          . should not skip",
            "          TG,H1     A8,COMP2          . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
            "",
            "DATA1     + 000074416513",
            "DATA2     + 055167",
            "COMP1     + 02,211334",
            "COMP2     + 077665215761"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testGreaterExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA1     + 000074416513",
            "DATA2     + 055167",
            "COMP1     + 02,211334",
            "COMP2     + 077665215761",
            "",
            "$(1)      $LIT",
            "          LA        A3,DATA1,,B2",
            "          LA        A8,DATA2,,B2",
            "          TG        A3,COMP1,,B2      . should not skip",
            "          TG,H1     A8,COMP2,,B2      . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  No TGM for basic mode

    @Test
    public void testGreaterMagnitudeExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0777777777577",
            "",
            "$(1)      $LIT",
            "          LA,U      A3,0144",
            "          TGM       A3,DATA,,B2       . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen",
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testWithinRangeBasic(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
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
            "",
            "DATA1     + 0443"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testWithinRangeExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA1     + 0443",
            "",
            "$(1)      $LIT",
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

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testNotWithinRangeBasic(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        //Skip NI if (U) ≤ (Aa) or (U) > (Aa+1)
        String[] source = {
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
            "",
            "DATA1     + 0443"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testNotWithinRangeExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA1     + 0443",
            "",
            "$(1)      $LIT",
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
            "          HALT      0                 . should happen"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  No DTGM for basic mode

    @Test
    public void testDoubleTestGreaterMagnitudeExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0777777777577",
            "          + 0222222222222",
            "",
            "$(1)      $LIT",
            "STEP1",
            "          LA,U      A1,0200",
            "          LA        A2,(0555555555555)",
            "          DTGM      A1,DATA,,B2       . should not skip",
            "          J         DONE",
            "          HALT      077               . should not happen",
            "",
            "DONE",
            "          HALT      0                 . should happen"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  No MTE for basic mode

    @Test
    public void testMaskedTestEqualExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0253444123457",
            "",
            "$(1)      $LIT",
            "          LR        R2,(0777000000001)",
            "          LA        A2,(0253012333403)",
            "          MTE       A2,DATA,,B2       . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should happen"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  No MTNE for basic mode

    @Test
    public void testMaskedTestNotEqualExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0253444123457",
            "",
            "$(1)      $LIT",
            "          LR        R2,(0777000000001)",
            "          LA        A2,(0253012333403)",
            "          MTNE      A2,DATA,,B2       . should not skip",
            "          HALT      0                 . should happen",
            "          HALT      077               . should not happen"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  No MTLE for basic mode
    //  MTNG is an alias for MTLE

    @Test
    public void testMaskedTestLessThanOrEqualExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "SIX       + 044444012034",
            "",
            "$(1)      $LIT",
            "          LR,U      R2,077",
            "          LA        A1,(0123456012345)",
            "          MTLE      A1,SIX,,B2        . should skip",
            "          HALT      077               . should not happen",
            "          MTNG      A1,SIX,,B2        . should skip",
            "          HALT      076               . should not happen",
            "          HALT      0                 . should stop here"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  No MTG for basic mode

    @Test
    public void testMaskedTestGreaterExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 044444012034",
            "",
            "$(1)      $LIT",
            "          LR,U      R2,077",
            "          LA        A3,(0123456012345)",
            "          MTG       A3,DATA,,B2       . should not skip",
            "          HALT      0                 . should stop here",
            "          HALT      077               . should not happen"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  No MTW for basic mode

    @Test
    public void testMaskedTestWithinRangeExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 066",
            "",
            "$(1)      $LIT",
            "          LR,U      R2,45",
            "          LA        A1,(012345000123)",
            "          LA        A2,(0115451234777)",
            "          MTW       A1,DATA,,B2       . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should stop here"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  No MTNW for basic mode

    @Test
    public void testMaskedTestNotWithinRangeExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0711711",
            "",
            "$(1)      $LIT",
            "          LR        R2,(0543321)",
            "          LA        A6,(01)",
            "          LA        A7,(0144)",
            "          MTNW      A6,DATA,,B2       . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should stop here"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  No MATL for basic mode

    @Test
    public void testMaskedAlphaTestLessThanOrEqualExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0311753276514",
            "",
            "$(1)      $LIT",
            "          LR        R2,(0466123111111)",
            "          LA        A7,(0157724561)",
            "          MATL      A7,DATA,,B2       . should skip",
            "          HALT      077               . should not happen",
            "          HALT      0                 . should stop here"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    //  No MATG for basic mode

    @Test
    public void testMaskedAlphaTestGreaterExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0311753276514",
            "",
            "$(1)      $LIT",
            "          LR        R2,(0466123111111)",
            "          LA        A7,(0157724561)",
            "          MATG      A7,DATA,,B2       . should not skip",
            "          HALT      0                 . should stop here",
            "          HALT      077               . should not happen"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testAndSetBasic(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          LBU       B13,(LBDIREF$+CLEAR, 0)",
            "          TS        CLEAR",
            "          TS        SET",
            "          HALT      077               . should not get here",
            "",
            "$(4)",
            "CLEAR     + 0",
            "SET       + 0770000,0"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01000 + 13, _instructionProcessor.getLatestStopDetail());
        long[] bank = getBankByBaseRegister(13);
        Assert.assertEquals(0_010000_000000L, bank[0]);
        Assert.assertEquals(0_0770000_000000L, bank[1]);
    }

    @Test
    public void testAndSetExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "CLEAR     + 0",
            "SET       + 0770000,0",
            "",
            "$(1)      $LIT",
            "          TS        CLEAR,,B2",
            "          TS        SET,,B2",
            "          HALT      077               . should not get here"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01000 + 13, _instructionProcessor.getLatestStopDetail());
        long[] bank = getBankByBaseRegister(2);
        Assert.assertEquals(0_010000_000000L, bank[0]);
        Assert.assertEquals(0_0770000_000000L, bank[1]);
    }

    @Test
    public void testAndSetAndSkipBasic(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          LBU       B13,(LBDIREF$+CLEAR, 0)",
            "          TSS       CLEAR",
            "          HALT      077               . should skip this",
            "          TSS       SET",
            "          HALT      0                 . we should stop here",
            "          HALT      076               . should not get here",
            "",
            "$(4)",
            "CLEAR     + 0",
            "SET       + 0770000,0"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        long[] bank = getBankByBaseRegister(13);
        Assert.assertEquals(0_010000_000000L, bank[0]);
        Assert.assertEquals(0_0770000_000000L, bank[1]);
    }

    @Test
    public void testAndSetAndSkipExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "CLEAR     + 0",
            "SET       + 0770000,0",
            "",
            "$(1)      $LIT",
            "          TSS       CLEAR,,B2",
            "          HALT      077               . should skip this",
            "          TSS       SET,,B2",
            "          HALT      0                 . we should stop here",
            "          HALT      076               . should not get here"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        long[] bank = getBankByBaseRegister(2);
        Assert.assertEquals(0_010000_000000L, bank[0]);
        Assert.assertEquals(0_0770000_000000L, bank[1]);
    }

    @Test
    public void testAndClearAndSkipBasic(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          LBU       B13,(LBDIREF$+CLEAR, 0)",
            "          TCS       SET",
            "          HALT      077               . should skip this",
            "          TCS       CLEAR",
            "          HALT      0,                . should stop here",
            "          HALT      077               . should not get here",
            "",
            "$(4)",
            "CLEAR     + 0",
            "SET       + 0777777,0777777"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        long[] bank = getBankByBaseRegister(13);
        Assert.assertEquals(0L, bank[0]);
        Assert.assertEquals(0_007777_777777L, bank[1]);
    }

    @Test
    public void testAndClearAndSkipExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "CLEAR     + 0",
            "SET       + 0777777,0777777",
            "",
            "$(1)      $LIT",
            "          TCS       SET,,B2",
            "          HALT      077               . should skip this",
            "          TCS       CLEAR,,B2",
            "          HALT      0,                . should stop here",
            "          HALT      077               . should not get here"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        long[] bank = getBankByBaseRegister(2);
        Assert.assertEquals(0L, bank[0]);
        Assert.assertEquals(0_007777_777777L, bank[1]);
    }

    @Test
    public void testConditionalReplaceBasic(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          LBU       B13,(LBDIREF$+DATA1, 0)",
            "          LA,U      A0,010",
            "          LA,U      A1,020",
            "          LA,U      A2,030",
            "          CR        A0,DATA1          . should skip NI",
            "          HALT      077",
            "",
            "          CR        A1,DATA2          . should not skip NI",
            "          HALT      0                 . should stop get here",
            "          HALT      076               . should not get here",
            "",
            "$(4)",
            "DATA1     + 010",
            "DATA2     + 014"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        long[] bank = getBankByBaseRegister(13);
        Assert.assertEquals(020L, bank[0]);
        Assert.assertEquals(014L, bank[1]);
    }

    @Test
    public void testConditionalReplaceBasicBadPP(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $INCLUDE 'GEN$DEFS'",
            "",
            "$(4)",
            "DATA1     + 010",
            "DATA2     + 014",
            "",
            "$(3)      $LIT",
            "          LBU       B13,(LBDIREF$+DATA1, 0)",
            "          LA,U      A0,010",
            "          LA,U      A1,020",
            "          LA,U      A2,030",
            "",
            "          DR$SETPP03                  . set proc priv 03",
            "          CR        A0,DATA1          . should cause interrupt",
            "          HALT      077               . should not get here",
            "          HALT      076               . should not get here"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testConditionalReplaceExtended(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA1     + 010",
            "DATA2     + 014",
            "",
            "$(1)      $LIT",
            "          LA,U      A0,010",
            "          LA,U      A1,020",
            "          LA,U      A2,030",
            "          CR        A0,DATA1,,B2      . should skip NI",
            "          HALT      077",
            "",
            "          CR        A1,DATA2,,B2      . should not skip NI",
            "          HALT      0                 . should stop get here",
            "          HALT      076               . should not get here"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        long[] bank = getBankByBaseRegister(2);
        Assert.assertEquals(020L, bank[0]);
        Assert.assertEquals(014L, bank[1]);
    }

    @Test
    public void testReferenceViolationBasic1(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          TZ        DATA              . should skip",
            "          HALT      077               . should skip this",
            "          LXM,U     X5,0100",
            "          TZ        DATA,X5           . should fail",
            "          HALT      076               . should not get here",
            "",
            "DATA      +0"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01010, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testReferenceViolationBasic2(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(4)",
            "DATA      +0",
            "",
            "$(1),START$*",
            "          LBU       B13,(LBDIREF$+DATA, 0)",
            "          DTE       A0,DATA           . should fail",
            "          HALT      076               . should not get here",
            "          HALT      077               . should not get here"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01010, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testReferenceViolationExtended1(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0",
            "",
            "$(1)      $LIT",
            "          TZ        DATA,,B2          . should skip",
            "          HALT      077               . should skip this",
            "          LXM,U     X5,0100",
            "          TZ        DATA,X5,,B2       . should fail",
            "          HALT      076               . should not get here"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01010, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testReferenceViolationExtended2(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0",
            "",
            "$(1)      $LIT",
            "          TZ        DATA,,B2          . should skip",
            "          HALT      077               . should skip this",
            "          TZ        DATA,,B7          . should fail",
            "          HALT      076               . should not get here"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01010, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void testReferenceViolationExtended3(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      + 0",
            "",
            "$(1)      $LIT",
            "          DTE       A0,DATA,,B2       . should fail",
            "          HALT      076               . should not get here",
            "          HALT      077               . should not get here"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        createProcessors();
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01010, _instructionProcessor.getLatestStopDetail());
    }
}
