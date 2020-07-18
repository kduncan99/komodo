/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.exceptions.BinaryLoadException;
import com.kadware.komodo.hardwarelib.exceptions.CannotConnectException;
import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.exceptions.UPIProcessorTypeException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_StackInstructions extends BaseFunctions {

    @After
    public void after(
    ) throws UPINotAssignedException {
        clear();
    }

    @Test
    public void buySimple18(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      . lower limit will be 0",
            "STACKSIZE $EQU 128",
            "FRAMESIZE $EQU 16",
            "STACK     $RES STACKSIZE",
            "",
            "$(1)      $LIT",
            "START",
            "          LD        (0,0)",
            "          LBU       B2,(LBDIREF$+STACK,0)",
            "          LXI,U     X5,FRAMESIZE",
            "          LXM,U     X5,STACK+STACKSIZE",
            "          BUY       0,*X5,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildDualBank(source);
        createProcessors();
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());
        assertEquals(128 - 16, _instructionProcessor.getExecOrUserXRegister(5).getXM());
        assertEquals(16, _instructionProcessor.getExecOrUserXRegister(5).getXI());
    }

    @Test
    public void buySimple24(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)      . lower limit will be 0",
            "STACKSIZE $EQU 128",
            "FRAMESIZE $EQU 16",
            "STACK     $RES STACKSIZE",
            "XVALUE    $GFORM 12,FRAMESIZE,24,STACK+STACKSIZE",
            "",
            "$(1)      $LIT",
            "START",
            "          LD        (000100,0) . EXEC 24-bit indexing",
            "          LBU       B2,(LBDIREF$+STACK,0)",
            "          LXSI,U    X5,FRAMESIZE",
            "          LXLM      X5,XVALUE,,B2",
            "          BUY       0,*X5,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildDualBank(source);
        createProcessors();
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());
        assertEquals(128 - 16, _instructionProcessor.getExecOrUserXRegister(5).getXM24());
        assertEquals(16, _instructionProcessor.getExecOrUserXRegister(5).getXI12());
    }

    @Test
    public void sellSimple18(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0) .",
            "STACKSIZE $EQU 128",
            "FRAMESIZE $EQU 16",
            "STACK     $RES STACKSIZE",
            "",
            "$(1)      $LIT",
            "START",
            "          LD        (0,0)",
            "          LBU       B2,(LBDIREF$+STACK,0)",
            "          LXI,U     X5,FRAMESIZE",
            "          LXM,U     X5,STACK+STACKSIZE-FRAMESIZE",
            "          SELL      0,*X5,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildDualBank(source);
        createProcessors();
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());
        assertEquals(128, _instructionProcessor.getExecOrUserXRegister(5).getXM());
        assertEquals(16, _instructionProcessor.getExecOrUserXRegister(5).getXI());
    }

    @Test
    public void sellSimple24(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0) .",
            "STACKSIZE $EQU 128",
            "FRAMESIZE $EQU 16",
            "STACK     $RES STACKSIZE",
            "XVALUE    $GFORM 12,FRAMESIZE,24,STACK+STACKSIZE-FRAMESIZE",
            "",
            "$(1)      $LIT",
            "START",
            "          LD        (000100,0) . EXEC 24-bit indexing",
            "          LBU       B2,(LBDIREF$+STACK,0)",
            "          LXSI,U    X5,FRAMESIZE",
            "          LXLM      X5,XVALUE,,B2",
            "          SELL      0,*X5,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildDualBank(source);
        createProcessors();
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());
        assertEquals(128, _instructionProcessor.getExecOrUserXRegister(5).getXM24());
        assertEquals(16, _instructionProcessor.getExecOrUserXRegister(5).getXI12());
    }

    @Test
    public void buyWithDisplacement(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)",
            "STACKSIZE $EQU 128",
            "FRAMESIZE $EQU 16",
            "STACK     $RES STACKSIZE",
            "",
            "$(1)      $LIT",
            "START",
            "          LD        (0,0)",
            "          LBU       B2,(LBDIREF$+STACK,0)",
            "          LXI,U     X5,FRAMESIZE",
            "          LXM,U     X5,STACK+STACKSIZE",
            "          BUY       010,*X5,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildDualBank(source);
        createProcessors();
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(0, _instructionProcessor.getLatestStopDetail());
        assertEquals((128 - 16) - 010, _instructionProcessor.getExecOrUserXRegister(5).getXM());
        assertEquals(16, _instructionProcessor.getExecOrUserXRegister(5).getXI());
    }

    @Test
    public void buyOverflow(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)",
            "STACKSIZE $EQU 128",
            "FRAMESIZE $EQU 16",
            "STACK     $RES STACKSIZE",
            "",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            "",
            "$(1)      $LIT",
            "START",
            "",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LD        (000001,000000) . ext mode, exec regs, pp=0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "          LBU       B2,(LBDIREF$+STACK,0)",
            "",
            "          LD        (0,0) . ext mode, user regs, pp=0",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          LBU       B2,(LBDIREF$+STACK,0)",
            "          LXI,U     X5,FRAMESIZE",
            "          LXM,U     X5,0",
            "          BUY       0,*X5,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        createProcessors();
        ipl(true);

        assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        assertEquals(01013, _instructionProcessor.getLatestStopDetail());
        assertEquals(0, _instructionProcessor.getLastInterrupt().getShortStatusField());
        assertEquals(0, _instructionProcessor.getGeneralRegister(5).getH2());
        assertEquals(16, _instructionProcessor.getGeneralRegister(5).getH1());
    }

    @Test
    public void sellUnderflow(
    ) throws BinaryLoadException,
             CannotConnectException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)",
            "STACKSIZE $EQU 128",
            "FRAMESIZE $EQU 16",
            "STACK     $RES STACKSIZE",
            "",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            "",
            "$(1)      $LIT",
            "START",
            "",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LD        (000001,000000) . ext mode, exec regs, pp=0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "          LBU       B2,(LBDIREF$+STACK,0)",
            "",
            "          LD        (0,0) . ext mode, user regs, pp=0",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          LBU       B2,(LBDIREF$+STACK,0)",
            "          LXI,U     X5,FRAMESIZE",
            "          LXM,U     X5,STACK+STACKSIZE",
            "          SELL      0,*X5,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        createProcessors();
        ipl(true);

        assertEquals(01013, _instructionProcessor.getLatestStopDetail());
        assertEquals(01, _instructionProcessor.getLastInterrupt().getShortStatusField());
        assertEquals(128, _instructionProcessor.getGeneralRegister(5).getH2());
        assertEquals(16, _instructionProcessor.getGeneralRegister(5).getH1());
    }
}
