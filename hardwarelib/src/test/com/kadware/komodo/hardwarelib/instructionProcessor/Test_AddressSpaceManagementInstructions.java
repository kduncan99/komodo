/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.AbsoluteAddress;
import com.kadware.komodo.baselib.AccessInfo;
import com.kadware.komodo.baselib.GeneralRegisterSet;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.baselib.exceptions.BinaryLoadException;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import com.kadware.komodo.hardwarelib.exceptions.NodeNameConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPIConflictException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.exceptions.UPIProcessorTypeException;
import com.kadware.komodo.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.ReferenceViolationInterrupt;
import java.util.Arrays;
import org.junit.After;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Unit tests for InstructionProcessor class
 */
public class Test_AddressSpaceManagementInstructions extends BaseFunctions {

    @After
    public void after(
    ) throws UPINotAssignedException {
        clear();
    }

    @Test
    public void decelerateActiveBaseTable_extended(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "",
            "          LD        (000024,0) . ext mode, normal regs, processor privilege 1",
            "",
            "          LBU       B2,DBANKBDI",
            "          LBU       B3,DBANKBDI",
            "          LBU       B4,DBANKBDI",
            "          LBU       B5,DBANKBDI",
            "          LBU       B6,DBANKBDI",
            "          LBU       B7,DBANKBDI",
            "          LBU       B8,DBANKBDI",
            "          LBU       B9,DBANKBDI",
            "          LBU       B10,DBANKBDI",
            "          LBU       B11,DBANKBDI",
            "          LBU       B12,DBANKBDI",
            "          LBU       B13,DBANKBDI",
            "          LBU       B14,DBANKBDI",
            "          LBU       B15,DBANKBDI",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15 . NOTE the offset here!",
            "          DABT      DATA,*X2,B2",
            "          HALT      0",
            "",
            "DBANKBDI  + LBDIREF$+DATA,0",
            "",
            "$(0)",
            "DATA      $RES 30",
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        ipl(true);

        long[] expected = {
            0,
            0_100004_000000L,
            0_100004_000000L,
            0_100004_000000L,
            0_100004_000000L,
            0_100004_000000L,
            0_100004_000000L,
            0_100004_000000L,
            0_100004_000000L,
            0_100004_000000L,
            0_100004_000000L,
            0_100004_000000L,
            0_100004_000000L,
            0_100004_000000L,
            0_100004_000000L,
        };

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(1, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.X2).getH1());
        Assert.assertEquals(16, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.X2).getH2());
        long[] dataBank = getBankByBaseRegister(2);
        long[] subset = Arrays.copyOfRange(dataBank, 15, 30);
        Assert.assertArrayEquals(expected, subset);
    }

    @Test
    public void decelerateActiveBaseTable_extended_error1(
    ) throws BinaryLoadException,
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
            "DATA      $RES 30",
            "",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000021,000000)",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK, 0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (000020,000000)",
            ".",
            "          CALL      (LBDIREF$+IH$INIT, IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "          DABT      DATA,*X2,B8",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01010, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.ReferenceViolation.getCode(),
                     _instructionProcessor.getLastInterrupt().getInterruptClass().getCode());
        assertEquals(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation.getCode() << 4,
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void decelerateActiveBaseTable_extended_error2(
    ) throws BinaryLoadException,
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
            "DATA      $RES 30",
            "",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000021,000000)",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK, 0)",
            "          LX        EX0,(0, RCSTACK+RCSSIZE)",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (000020,000000)",
            ".",
            "          CALL      (LBDIREF$+IH$INIT, IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,150",
            "          DABT      DATA,*X2,B2",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01010, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.ReferenceViolation.getCode(),
                     _instructionProcessor.getLastInterrupt().getInterruptClass().getCode());
        assertEquals(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation.getCode() << 4,
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void decelerateActiveBaseTable_extended_error3(
    ) throws BinaryLoadException,
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
            "DATA      $RES 30",
            "",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000021,000000)",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK, 0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (000020,000000)",
            ".",
            "          CALL      (LBDIREF$+IH$INIT, IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            ".",
            "          . GET DESIGNATOR REGISTER FOR PROCESSOR PRIVILEGE = 2",
            "          LD        (000034,000000)",
            "          DABT      DATA,*X2,B8",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     _instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterExec_basic(
    ) throws BinaryLoadException,
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
            "$(2)      . BDI 100005, starts at 02000",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(4)      . BDI 100007, starts at 03000",
            "          . Useful bank data, will be BDI 100006",
            "DATA      $res 16",
            "",
            "$(1)      . BDI 100004, starts at 01000",
            "          . Needs extended mode",
            "          . since we are the IPL interrupt handler.",
            "          . Just CALL into a basic mode bank for the real test.",
            "          . Of course, we need an RCS first...",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        DESREG1,,B0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,RCSBDI",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "          CALL      BMLBI,,B0",
            "",
            "DESREG1   + 000021,000000",
            "RCSBDI    + LBDIREF$+RCSTACK,0",
            "BMLBI     + LBDIREF$+BMSTART,BMSTART",
            "",
            "$(3)      . BDI 100006, starts at 01000",
            "          . Needs to NOT be extended mode",
            "          $BASIC",
            "BMSTART",
            "          LBE       B28,BDENTRY1",
            "          LBE       B29,BDENTRY2",
            "          HALT      0",
            "",
            "BDENTRY1  + LBDIREF$+DATA,0    . 16 words of data",
            "BDENTRY2  + 0,0                . void",
            "",
            "          $END      START"
        };

        buildMultiBank(source, false, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());

        InstructionProcessor.BaseRegister b28 = _instructionProcessor.getBaseRegister(28);
        assertFalse(b28._voidFlag);
        assertFalse(b28._largeSizeFlag);
        assertEquals(03000, b28._lowerLimitNormalized);
        assertEquals(03017, b28._upperLimitNormalized);

        InstructionProcessor.BaseRegister b29 = _instructionProcessor.getBaseRegister(29);
        assertTrue(b29._voidFlag);
    }

    @Test
    public void loadBaseRegisterExec_extended(
    ) throws BinaryLoadException,
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
            "$(2)                . BDI 100005",
            "          $RES      16",
            ".",
            "$(1),START          . BDI 100004",
            "          LBE       B27,BDENTRY1,,B0",
            "          LBE       B29,BDENTRY3,,B0",
            "          HALT      0",
            "",
            "BDENTRY1  + 0100005,0 . 16 words of data",
            "BDENTRY3  + 0,0       . void",
            "",
            "          $END      START"
        };

        buildMultiBank(source, false, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        InstructionProcessor.BaseRegister br27 = _instructionProcessor.getBaseRegister(27);
        assertFalse(br27._voidFlag);
        assertFalse(br27._largeSizeFlag);
        assertEquals(0, br27._lowerLimitNormalized);
        assertEquals(017, br27._upperLimitNormalized);
        assertEquals(new AccessInfo((short) 0, 0), br27._accessLock);

        InstructionProcessor.BaseRegister br29 = _instructionProcessor.getBaseRegister(29);
        assertTrue(br29._voidFlag);
    }

    @Test
    public void loadBaseRegisterExec_BadBank_basic(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $BASIC",
            "",
            "$(2)      . BDI 100005, starts at 02000",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . BDI 100004, starts at 01000",
            "          . Needs extended mode",
            "          . since we are the IPL interrupt handler.",
            "          . Just CALL into a basic mode bank for the real test.",
            "          . Of course, we need an RCS first...",
            "          $EXTEND",
            "          $INFO 10 1",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        DESREG1,,B0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,RCSBDI",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        DESREG2,,B0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      IHINIT",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          . INVOKE THE ACTUAL BASIC MODE TEST",
            "          CALL      BMLBI,,B0",
            "          HALT      0 . should not get here",
            "",
            "DESREG1   + 000021,000000",
            "DESREG2   + 000020,000000",
            "IHINIT    + LBDIREF$+IH$INIT,IH$INIT",
            "RCSBDI    + LBDIREF$+RCSTACK,0",
            "BMLBI     + LBDIREF$+BMSTART,BMSTART",
            "",
            "$(3)      . BDI 100006, starts at 01000",
            "          . Needs to NOT be extended mode",
            "          $BASIC",
            "BMSTART",
            "          LBE       B27,BDENTRY",
            "          HALT      0 . should not get here",
            "",
            "BDENTRY   + 0300006,0 . does not exist (wrong bank level)",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01011, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void loadBaseRegisterExec_BadBank_extended(
    ) throws BinaryLoadException,
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
            "$(2)      . BDI 100005, starts at 02000",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . BDI 100004, starts at 01000",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        DESREG1,,B0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,RCSBDI",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        DESREG2,,B0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      IHINIT",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          LBE       B27,BDENTRY",
            "          HALT      0 . should not get here",
            "",
            "DESREG1   + 000021,000000",
            "DESREG2   + 000020,000000",
            "IHINIT    + LBDIREF$+IH$INIT,IH$INIT",
            "RCSBDI    + LBDIREF$+RCSTACK,0",
            "BDENTRY   + 0300006,0 . does not exist (wrong bank level and index)",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01011, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void loadBaseRegisterExec_BadPP_basic(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $BASIC",
            "",
            "$(2)      . BDI 100005, starts at 02000",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(4)      . BDI 100007, starts at 03000",
            "DATA      $RES 16",
            ".",
            "$(1)      . BDI 100004, starts at 01000",
            "          . Needs extended mode",
            "          . since we are the IPL interrupt handler.",
            "          . Just CALL into a basic mode bank for the real test.",
            "          . Of course, we need an RCS first...",
            "          $EXTEND",
            "          $INFO 10 1",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        DESREG1,,B0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,RCSBDI",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        DESREG2,,B0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      IHINIT",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          . INVOKE THE ACTUAL BASIC MODE TEST",
            "          CALL      BMLBI,,B0",
            "          HALT      0 . should not get here",
            "",
            "DESREG1   + 000001,000000 . extended mode, exec registers, pp=0",
            "DESREG2   + 000000,000000 . extended mode, user registers, pp=0",
            "IHINIT    + LBDIREF$+IH$INIT,IH$INIT",
            "RCSBDI    + LBDIREF$+RCSTACK,0",
            "BMLBI     + LBDIREF$+BMSTART,BMSTART",
            "",
            "$(3)      . BDI 100006, starts at 01000",
            "          . Needs to NOT be extended mode",
            "          $BASIC",
            "BMSTART",
            "          LD        DESREG3",
            "          LBE       B27,BDENTRY",
            "          HALT      0 . should not get here",
            "",
            "DESREG3   + 000016,000000 . basic mode, user registers, pp=3",
            "BDENTRY   + 0100007,0 . data bank",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     _instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterExec_BadPP_extended(
    ) throws BinaryLoadException,
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
            "$(2)      . BDI 100005, starts at 0",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(4)      . BDI 100006, starts at 0",
            "DATA      $RES 16",
            ".",
            "$(1)      . BDI 100004, starts at 01000",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        DESREG1,,B0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,RCSBDI",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        DESREG2,,B0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      IHINIT",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          LD        DESREG3",
            "          LBE       B27,BDENTRY,,B0",
            "          HALT      0 . should not get here",
            "",
            "DESREG1   + 000001,000000 . extended mode, exec registers, pp=0",
            "DESREG2   + 000000,000000 . extended mode, user registers, pp=0",
            "DESREG3   + 000014,000000 . extended mode, user registers, pp=3",
            "IHINIT    + LBDIREF$+IH$INIT,IH$INIT",
            "RCSBDI    + LBDIREF$+RCSTACK,0",
            "BDENTRY   + 0100006,0 . data bank",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     _instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterExecDirect_basic(
    ) throws BinaryLoadException,
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
            "$(2)      . BDI 100005, starts at 0",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1),START . We start in extended mode, we have to call to basic mode bank.",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        DESREG1,,B0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,RCSBDI",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        DESREG2,,B0",
            ".",
            "          CALL      BMODE",
            "DESREG1   + 000001,000000 . extended mode, exec registers, pp=0",
            "DESREG2   + 000000,000000 . extended mode, user registers, pp=0",
            "RCSBDI    + LBDIREF$+RCSTACK,0",
            "BMODE     + LBDIREF$+BMENTRY,BMENTRY",
            "",
            "$(3)",
            "          $BASIC",
            "BMENTRY",
            "          LBED      B27,BDENTRY1   . void bank",
            "          LBED      B28,BDENTRY2   . effectively void",
            "          LBED      B29,BDENTRY3   . not void",
            "          HALT      0",
            "",
            "BDENTRY1  . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            "",
            "BDENTRY2  . void bank with lower > upper",
            "          + 0000000,0000014 . GAP/SAP clear, void clear, ring=0 domain=014",
            "          + 001000,000177   . lower limit 01000, upper limit 0177",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            "",
            "BDENTRY3  . void bank with lower > upper",
            "          + 0770000,0400100 . GAP/SAP erw set, ring=2 domain=0100",
            "          + 001000,01777    . lower limit 01000, upper limit 01777",
            "          + 0               . base address segment 0",
            "          + 040000,070000   . base address UPI,offset 01,070000",
            "",
            "          $END      START"
        };

        buildMultiBank(source, false, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());

        InstructionProcessor.BaseRegister br27 = _instructionProcessor.getBaseRegister(27);
        assertTrue(br27._voidFlag);
        assertEquals(new AccessInfo((short) 3, 010), br27._accessLock);

        InstructionProcessor.BaseRegister br28 = _instructionProcessor.getBaseRegister(28);
        assertTrue(br28._voidFlag);
        assertEquals(new AccessInfo((short) 0, 014), br28._accessLock);

        InstructionProcessor.BaseRegister br29 = _instructionProcessor.getBaseRegister(29);
        assertFalse(br29._voidFlag);
        assertFalse(br29._largeSizeFlag);
        assertEquals(new AccessInfo((short) 2, 0100), br29._accessLock);
        assertEquals(01, br29.getLowerLimit());
        assertEquals(01777, br29.getUpperLimit());
        Assert.assertEquals(new AbsoluteAddress((short)1, 0, 070000), br29._baseAddress);
    }

    @Test
    public void loadBaseRegisterExecDirect_extended(
    ) throws BinaryLoadException,
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
            "$(1),START",
            "          LBED      B27,BDENTRY1,,B0   . void bank",
            "          LBED      B28,BDENTRY2,,B0   . effectively void",
            "          LBED      B29,BDENTRY3,,B0   . not void",
            "          HALT      0",
            "",
            "BDENTRY1  . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            "",
            "BDENTRY2  . void bank with lower > upper",
            "          + 0000000,0000014 . GAP/SAP clear, void clear, ring=0 domain=014",
            "          + 001000,000177   . lower limit 01000, upper limit 0177",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            "",
            "BDENTRY3  . void bank with lower > upper",
            "          + 0770000,0400100 . GAP/SAP erw set, ring=2 domain=0100",
            "          + 001000,01777    . lower limit 01000, upper limit 01777",
            "          + 0               . base address segment 0",
            "          + 040000,070000   . base address UPI,offset 01,070000",
            "",
            "          $END      START"
        };

        buildMultiBank(source, false, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());

        InstructionProcessor.BaseRegister br27 = _instructionProcessor.getBaseRegister(27);
        assertTrue(br27._voidFlag);
        assertEquals(new AccessInfo((short) 3, 010), br27._accessLock);

        InstructionProcessor.BaseRegister br28 = _instructionProcessor.getBaseRegister(28);
        assertTrue(br28._voidFlag);
        assertEquals(new AccessInfo((short) 0, 014), br28._accessLock);

        InstructionProcessor.BaseRegister br29 = _instructionProcessor.getBaseRegister(29);
        assertFalse(br29._voidFlag);
        assertFalse(br29._largeSizeFlag);
        assertEquals(new AccessInfo((short) 2, 0100), br29._accessLock);
        assertEquals(01, br29.getLowerLimit());
        assertEquals(01777, br29.getUpperLimit());
        assertEquals(new AbsoluteAddress((short)1, 0, 070000), br29._baseAddress);
    }

    @Test
    public void loadBaseRegisterExecDirect_BadPP_basic(
    ) throws BinaryLoadException,
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
            "$(2)      . BDI 100005, starts at 0",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1),START . We start in extended mode, we have to call to basic mode bank.",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        DESREG1,,B0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,RCSBDI",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        DESREG2,,B0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      IHINIT",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          LD        DESREG3",
            "          CALL      BMODE",
            "",
            "DESREG1   + 000001,000000 . extended mode, exec registers, pp=0",
            "DESREG2   + 000000,000000 . extended mode, user registers, pp=0",
            "DESREG3   + 000014,000000 . extended mode, user registers, pp=3",
            "IHINIT    + LBDIREF$+IH$INIT,IH$INIT",
            "RCSBDI    + LBDIREF$+RCSTACK,0",
            "BMODE     + LBDIREF$+BMENTRY,BMENTRY",
            "",
            "$(3)",
            "          $BASIC",
            "BMENTRY",
            "          LBED      B27,BDENTRY1   . void bank",
            "          HALT      0",
            "",
            "BDENTRY1  . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     _instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterExecDirect_BadPP_extended(
    ) throws BinaryLoadException,
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
            "$(2)      . BDI 100005, starts at 0",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1),START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        DESREG1,,B0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,RCSBDI",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        DESREG2,,B0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      IHINIT",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          LD        DESREG3",
            "          LBED      B27,BDENTRY1,,B0   . void bank",
            "          HALT      0",
            "",
            "BDENTRY1  . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            "",
            "DESREG1   + 000001,000000 . extended mode, exec registers, pp=0",
            "DESREG2   + 000000,000000 . extended mode, user registers, pp=0",
            "DESREG3   + 000014,000000 . extended mode, user registers, pp=3",
            "IHINIT    + LBDIREF$+IH$INIT,IH$INIT",
            "RCSBDI    + LBDIREF$+RCSTACK,0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     _instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBankName_basic(
    ) throws BinaryLoadException,
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
            "$(4)      . BDI 100006, starts at 0",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(6)      . BDI 100007, starts at 0 - used as level 2 bank descriptor table",
            "BDI200000 . indirect bank",
            "          + 0770300,0",
            "          + 0100004,0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 0",
            "BDI200001 . gate bank",
            "          + 0770200,0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 0",
            "BDI200002 . very large bank, base",
            "          + 0770206,0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 0",
            "BDI200003 . very large bank, displacement 1",
            "          + 0770206,0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 01,0",
            "          + 0",
            "          + 0",
            "          + 0",
            "BDI200004 . very large bank, displacement 2",
            "          + 0770206,0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 02,0",
            "          + 0",
            "          + 0",
            "          + 0",
            "BDI200005 . very large bank, displacement 3",
            "          + 0770206,0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 03,0",
            "          + 0",
            "          + 0",
            "          + 0",
            "BDI200006 . very large bank, displacement 4",
            "          + 0770206,0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 04,0",
            "          + 0",
            "          + 0",
            "          + 0",
            "",
            "BDI200007 . queue bank",
            "          + 0770400,0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 0",
            "BDI200010 . queue bank repository",
            "          + 0770600,0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 0",
            "          + 0",
            "",
            "$(1)      . extended mode i-bank 0100004",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        DESREG1,,B0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,RCSBDI",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . ESTABLISH LEVEL 2 BDT",
            "          LBE       B18,BDT2BDI",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        DESREG2,,B0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      IHINIT",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      BASICVEC",
            "",
            "BASICVEC  + LBDIREF$+BASIC,BASIC",
            "BDT2BDI   + 0100007,0",
            "DESREG1   + 000001,000000 . extended mode, exec registers, pp=0",
            "DESREG2   + 000000,000000 . extended mode, user registers, pp=0",
            "IHINIT    + LBDIREF$+IH$INIT,IH$INIT",
            "RCSBDI    + LBDIREF$+RCSTACK,0",
            "",
            "          $BASIC",
            "$(3)      . basic mode i-bank 0100005",
            "BASIC",
            "          LBN       X1,VADDR1",
            "          HALT      077          . should skip this",
            "          LBN       X2,VADDR2",
            "          HALT      077          . should skip this",
            "          LBN       X3,VADDR3",
            "          HALT      077          . should skip this",
            "          LBN       X4,VADDR4",
            "          J         CONTINUE     . should NOT skip this,",
            "          HALT      077          . should NOT get here",
            "",
            "CONTINUE  .",
            "          LBN       X5,VADDR5",
            "          HALT      077          . should skip this",
            "          LBN       X6,VADDR6",
            "          HALT      077          . should skip this",
            "          LBN       X7,VADDR7",
            "          HALT      077          . should skip this",
            "          LBN       X8,VADDR8",
            "          HALT      077          . should skip this",
            "          LBN       X9,VADDR9    . should cause an interrupt (qbrs are bad)",
            "          HALT      077          . should not get here",
            "",
            "VADDR1    + 0,0         . L,BDI = 0,0;  LBN is 0000000, SKIP",
            "VADDR2    + 037,0       . L,BDI = 0,31; LBN is 0000037, SKIP",
            "VADDR3    + 0100004,0   . ext mode bank, LBN is 0100004, SKIP",
            "VADDR4    + 0100005,0   . basic mode bank, LBN is 0100005, no SKIP",
            "VADDR5    + 0200000,0   . indirect bank, LBN is 0200000, SKIP",
            "VADDR6    + 0200001,0   . gate bank, LBN is 0200001, SKIP",
            "VADDR7    + 0200006,0   . bank with displacement, LBN is 0200002, SKIP",
            "VADDR8    + 0200007,0   . queue bank, LBN is 0200007, SKIP",
            "VADDR9    + 0200010,0   . qb repository, LBN is 0200010, Interrupt",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01011, _instructionProcessor.getLatestStopDetail());
        Assert.assertEquals(0_000000_000000L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.X1).getW());
        Assert.assertEquals(0_000037_000000L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.X2).getW());
        Assert.assertEquals(0_100004_000000L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.X3).getW());
        Assert.assertEquals(0_100005_000000L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.X4).getW());
        Assert.assertEquals(0_200000_000000L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.X5).getW());
        Assert.assertEquals(0_200001_000000L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.X6).getW());
        Assert.assertEquals(0_200002_000000L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.X7).getW());
        Assert.assertEquals(0_200007_000000L, _instructionProcessor.getGeneralRegister(GeneralRegisterSet.X8).getW());
    }

    @Test
    public void loadBankName_bad_basic(
    ) throws BinaryLoadException,
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
            "$(4)      . BDI 100006, starts at 0",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank 0100004",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        DESREG1,,B0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,RCSBDI",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        DESREG2,,B0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      IHINIT",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      BASICVEC",
            "",
            "BASICVEC  + LBDIREF$+BASIC,BASIC",
            "DESREG1   + 000001,000000 . extended mode, exec registers, pp=0",
            "DESREG2   + 000000,000000 . extended mode, user registers, pp=0",
            "IHINIT    + LBDIREF$+IH$INIT,IH$INIT",
            "RCSBDI    + LBDIREF$+RCSTACK,0",
            "",
            "          $BASIC",
            "$(3)      . basic mode i-bank 0100005",
            "BASIC",
            "          LBN       X10,VADDRX   . should cause an interrupt",
            "          HALT      077          . should not get here",
            "",
            "VADDRX    + 0100777,0   . Not that many level 1 banks",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01011, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void loadBankName_badPP_basic(
    ) throws BinaryLoadException,
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
            "$(4)      . BDI 100006, starts at 0",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank 0100004",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          LD        (014, 0),,B0 . ext mode, user regs, pp=2",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            "",
            "          $BASIC",
            "$(3)      . basic mode i-bank 0100005",
            "BASIC",
            "          LBN       X5,DATA",
            "          HALT      0",
            "          HALT      077",
            "DATA      +         0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     _instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterUser_basic(
    ) throws BinaryLoadException,
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
            "$(4)      . BDI 100006",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank 0100004",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            "",
            "          $BASIC",
            "$(3)      . basic mode i-bank 0100005",
            "          $LIT",
            "BASIC",
            "          LBU       B7,(0100006,0)",
            "          LBU       B8,(0,0)",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        InstructionProcessor.BaseRegister br7 = _instructionProcessor.getBaseRegister(7);
        assertFalse(br7._voidFlag);
        assertFalse(br7._largeSizeFlag);
        assertEquals(02000, br7._lowerLimitNormalized);
        assertEquals(02077, br7._upperLimitNormalized);
        assertEquals(new AccessInfo((short) 0, 0), br7._accessLock);

        InstructionProcessor.BaseRegister br8 = _instructionProcessor.getBaseRegister(8);
        assertTrue(br8._voidFlag);
    }

    @Test
    public void loadBaseRegisterUser_extended(
    ) throws BinaryLoadException,
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
            "$(2)      . useful bank data, will be BDI 06",
            "          $res 16",
            "",
            "$(1)",
            "          $LIT",
            "START",
            "          LBU       B7,(0100005,0)",
            "          LBU       B9,(0,0)",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
        InstructionProcessor.BaseRegister br7 = _instructionProcessor.getBaseRegister(7);
        assertFalse(br7._voidFlag);
        assertFalse(br7._largeSizeFlag);
        assertEquals(02000, br7._lowerLimitNormalized);
        assertEquals(02017, br7._upperLimitNormalized);
        assertEquals(new AccessInfo((short) 0, 0), br7._accessLock);

        InstructionProcessor.BaseRegister br9 = _instructionProcessor.getBaseRegister(9);
        assertTrue(br9._voidFlag);
    }

    @Test
    public void loadBaseRegisterUser_BadPP_basic(
    ) throws BinaryLoadException,
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
            "$(4)      . BDI 100006, starts at 0",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank 0100004",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          LD        (014, 0),,B0 . ext mode, user regs, pp=2",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            "",
            "          $BASIC",
            "$(3)      . basic mode i-bank 0100005",
            "BASIC",
            "          LBU       B5,(0100005,0)",
            "          HALT      077",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     _instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterUser_BadBank_basic(
    ) throws BinaryLoadException,
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
            "$(4)      . BDI 100006",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank 0100004",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            "",
            "          $BASIC",
            "$(3)      . basic mode i-bank 0100005",
            "          $LIT",
            "BASIC",
            "          LBU       B5,(0100777,0)",
            "          HALT      077",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01011, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void loadBaseRegisterUser_BadBank_extended(
    ) throws BinaryLoadException,
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
            "$(4)      . BDI 100006, starts at 0",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank 0100004",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          LBU       B5,(0400070,0)    . non-existing bank",
            "          HALT      077               . should not get here",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01011, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void loadBaseRegisterUser_InvalidBank_basic(
    ) throws BinaryLoadException,
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
            "$(4)      . BDI 100006",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank 0100004",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            "",
            "          $BASIC",
            "$(3)      . basic mode i-bank 0100005",
            "          $LIT",
            "BASIC",
            "          LBU       B5,(01,0) . invalid BDI",
            "          HALT      077",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01011, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void loadBaseRegisterUser_InvalidBank_extended(
    ) throws BinaryLoadException,
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
            "$(4)      . BDI 100006, starts at 0",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank 0100004",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          LBU       B5,(31,0)         . invalid BDI",
            "          HALT      077               . should not get here",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01011, _instructionProcessor.getLatestStopDetail());
    }

    @Test
    public void loadBaseRegisterUser_InvalidBR0_basic(
    ) throws BinaryLoadException,
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
            "$(4)      . BDI 100006",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank 0100004",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            "",
            "          $BASIC",
            "$(3)      . basic mode i-bank 0100005",
            "          $LIT",
            "BASIC",
            "          LBU       B0,(LBDIREF$+START,0) . invalid BReg",
            "          HALT      077",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     _instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidBaseRegister.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterUser_InvalidBR0_extended(
    ) throws BinaryLoadException,
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
            "$(4)      . BDI 100006, starts at 0",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank 0100004",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          LBU       B0,(LBDIREF$+START,0) . invalid BReg",
            "          HALT      077                   . should not get here",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     _instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidBaseRegister.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterUser_InvalidBR1_basic(
    ) throws BinaryLoadException,
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
            "$(4)      . BDI 100006",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank 0100004",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            "",
            "          $BASIC",
            "$(3)      . basic mode i-bank 0100005",
            "          $LIT",
            "BASIC",
            "          LBU       B1,(LBDIREF$+START,0) . invalid BReg",
            "          HALT      077",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     _instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidBaseRegister.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterUser_InvalidBR1_extended(
    ) throws BinaryLoadException,
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
            "$(4)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            "",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          LBU       B1,(LBDIREF$+START,0) . invalid BReg",
            "          HALT      077                   . should not get here",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     _instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidBaseRegister.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterUserDirect_basic(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            ".",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            ".",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            ".",
            "          $BASIC",
            "$(3)      . basic mode i-bank",
            "          $LIT",
            "BASIC",
            "          LBUD      B5,BDENTRY1   . void bank",
            "          LBUD      B6,BDENTRY2   . effectively void",
            "          LBUD      B7,BDENTRY3   . not void",
            "          HALT      0",
            ".",
            "BDENTRY1  . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            ".",
            "BDENTRY2  . void bank with lower > upper",
            "          + 0000000,0000014 . GAP/SAP clear, void clear, ring=0 domain=014",
            "          + 001000,000177   . lower limit 01000, upper limit 0177",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            ".",
            "BDENTRY3  . normal bank",
            "          + 0770000,0400100 . GAP/SAP erw set, ring=2 domain=0100",
            "          + 001000,01777    . lower limit 01000, upper limit 01777",
            "          + 0               . base address segment 0",
            "          + 040000,070000   . base address UPI,offset 01,070000",
            ".",
            "          $END      START"
        };

        buildMultiBank(source, false, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());

        InstructionProcessor.BaseRegister br5 = _instructionProcessor.getBaseRegister(5);
        assertTrue(br5._voidFlag);
        assertEquals(new AccessInfo((short) 3, 010), br5._accessLock);

        InstructionProcessor.BaseRegister br6 = _instructionProcessor.getBaseRegister(6);
        assertTrue(br6._voidFlag);
        assertEquals(new AccessInfo((short) 0, 014), br6._accessLock);

        InstructionProcessor.BaseRegister br7 = _instructionProcessor.getBaseRegister(7);
        assertFalse(br7._voidFlag);
        assertFalse(br7._largeSizeFlag);
        assertEquals(new AccessInfo((short) 2, 0100), br7._accessLock);
        assertEquals(01, br7.getLowerLimit());
        assertEquals(01777, br7.getUpperLimit());
        assertEquals(new AbsoluteAddress((short)1, 0, 070000), br7._baseAddress);
    }

    @Test
    public void loadBaseRegisterUserDirect_extended(
    ) throws BinaryLoadException,
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
            "$(2)",
            "BDENTRY1  . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            "",
            "BDENTRY2  . void bank with lower > upper",
            "          + 0000000,0000014 . GAP/SAP clear, void clear, ring=0 domain=014",
            "          + 001000,000177   . lower limit 01000, upper limit 0177",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            "",
            "BDENTRY3  . normal bank",
            "          + 0770004,0400100 . GAP/SAP erw set, large size ring=2 domain=0100",
            "          + 001000,01777    . lower limit 0100000, upper limit 0177700",
            "          + 0               . base address segment 0",
            "          + 040000,070000   . base address UPI,offset 01,070000",
            "",
            "$(1)      $LIT",
            "START",
            "          LD        (0,0)",
            "          LBU       B2,(LBDIREF$+BDENTRY1,0)",
            "          LBUD      B5,BDENTRY1,,B2   . void bank",
            "          LBUD      B6,BDENTRY2,,B2   . effectively void",
            "          LBUD      B7,BDENTRY3,,B2   . not void",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, false, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());

        InstructionProcessor.BaseRegister br5 = _instructionProcessor.getBaseRegister(5);
        assertTrue(br5._voidFlag);
        assertEquals(new AccessInfo((short) 3, 010), br5._accessLock);

        InstructionProcessor.BaseRegister br6 = _instructionProcessor.getBaseRegister(6);
        assertTrue(br6._voidFlag);
        assertEquals(new AccessInfo((short) 0, 014), br6._accessLock);

        InstructionProcessor.BaseRegister br7 = _instructionProcessor.getBaseRegister(7);
        assertFalse(br7._voidFlag);
        assertTrue(br7._largeSizeFlag);
        assertEquals(new AccessInfo((short) 2, 0100), br7._accessLock);
        assertEquals(01, br7.getLowerLimit());
        assertEquals(01777, br7.getUpperLimit());
        assertEquals(new AbsoluteAddress((short)1, 0, 070000), br7._baseAddress);
    }

    @Test
    public void loadBaseRegisterUserDirect_BadPP_basic(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            ".",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          LD        (014,0),,B0 . ext mode, user regs, pp=3",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            ".",
            "          $BASIC",
            "$(3)      . basic mode i-bank",
            "          $LIT",
            "BASIC",
            "          LBUD      B5,BDENTRY1   . void bank",
            "          HALT      0",
            ".",
            "BDENTRY1  . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            ".",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     _instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterUserDirect_BadPP_extended(
    ) throws BinaryLoadException,
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
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(4)",
            "BDENTRY1  . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            "",
            "$(1)      $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          LD        (014,0),,B0 . ext mode, user regs, pp=3",
            "          LBU       B2,(LBDIREF$+BDENTRY1,0)",
            "          LBUD      B5,BDENTRY1,,B2   . void bank",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     _instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterUserDirect_InvalidBR0_basic(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            ".",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            ".",
            "          $BASIC",
            "$(3)      . basic mode i-bank",
            "          $LIT",
            "BASIC",
            "          LBUD      B0,BDENTRY1   . void bank",
            "          HALT      0",
            ".",
            "BDENTRY1  . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            ".",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     _instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidBaseRegister.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterUserDirect_InvalidBR0_extended(
    ) throws BinaryLoadException,
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
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(4)",
            "BDENTRY1  . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
            "",
            "$(1)      $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          LBU       B2,(LBDIREF$+BDENTRY1,0)",
            "          LBUD      B0,BDENTRY1,,B2   . void bank",
            "          HALT      0",
            "",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     _instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidBaseRegister.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void storeBaseRegisterExecDirect_basic(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            ".",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            "",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          LBU       B15,(LBDIREF$+DATA,0)",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            ".",
            "          $BASIC",
            "$(3)      . basic mode i-bank",
            "          $LIT",
            "BASIC",
            "          LXI,U     X8,4",
            "          LXM,U     X8,0",
            "          SBED      B16,DATA,*X8",
            "          SBED      B25,DATA,*X8",
            "          SBED      B26,DATA,*X8",
            "          SBED      B30,DATA,*X8",
            "          HALT      0",
            ".",
            "$(4)      . basic mode d-bank",
            "DATA      $RES      64",
            ".",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());

        Assert.assertEquals(0_000004_000020, _instructionProcessor.getExecOrUserXRegister(8).getW());
        long[] data = getBankByBaseRegister(15);
        InstructionProcessor.BaseRegister br16 = new InstructionProcessor.BaseRegister(Arrays.copyOfRange(data,  0, 4));
        InstructionProcessor.BaseRegister br25 = new InstructionProcessor.BaseRegister(Arrays.copyOfRange(data,  4, 8));
        InstructionProcessor.BaseRegister br26 = new InstructionProcessor.BaseRegister(Arrays.copyOfRange(data,  8, 12));
        InstructionProcessor.BaseRegister br30 = new InstructionProcessor.BaseRegister(Arrays.copyOfRange(data,  12, 16));

        assertFalse(br16._voidFlag);
        assertEquals(0, br16._lowerLimitNormalized);
        assertEquals(0407, br16._upperLimitNormalized);

        assertFalse(br25._voidFlag);
        assertEquals(02000, br25._lowerLimitNormalized);
        assertEquals(02077, br25._upperLimitNormalized);

        assertFalse(br26._voidFlag);
        assertEquals(0, br26._lowerLimitNormalized);
        assertEquals(077, br26._upperLimitNormalized);

        assertTrue(br30._voidFlag);
    }

    @Test
    public void storeBaseRegisterExecDirect_extended(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            ".",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "",
            "          LBU       B2,(LBDIREF$+DATA,0)",
            "          LXI,U     X8,4",
            "          LXM,U     X8,0",
            "          SBED      B16,DATA,*X8,B2",
            "          SBED      B25,DATA,*X8,B2",
            "          SBED      B26,DATA,*X8,B2",
            "          SBED      B30,DATA,*X8,B2",
            "          HALT      0",
            ".",
            "$(4)      . basic mode d-bank",
            "DATA      $RES      64",
            ".",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());

        Assert.assertEquals(0_000004_000020, _instructionProcessor.getExecOrUserXRegister(8).getW());
        long[] data = getBankByBaseRegister(2);
        InstructionProcessor.BaseRegister br16 = new InstructionProcessor.BaseRegister(Arrays.copyOfRange(data,  0, 4));
        InstructionProcessor.BaseRegister br25 = new InstructionProcessor.BaseRegister(Arrays.copyOfRange(data,  4, 8));
        InstructionProcessor.BaseRegister br26 = new InstructionProcessor.BaseRegister(Arrays.copyOfRange(data,  8, 12));
        InstructionProcessor.BaseRegister br30 = new InstructionProcessor.BaseRegister(Arrays.copyOfRange(data,  12, 16));

        assertFalse(br16._voidFlag);
        assertEquals(0, br16._lowerLimitNormalized);
        assertEquals(0407, br16._upperLimitNormalized);

        assertFalse(br25._voidFlag);
        assertEquals(02000, br25._lowerLimitNormalized);
        assertEquals(02077, br25._upperLimitNormalized);

        assertFalse(br26._voidFlag);
        assertEquals(0, br26._lowerLimitNormalized);
        assertEquals(077, br26._upperLimitNormalized);

        assertTrue(br30._voidFlag);
    }

    @Test
    public void storeBaseRegisterExecDirect_BadPP_basic(
    ) throws BinaryLoadException,
            MachineInterrupt,
            MaxNodesException,
            NodeNameConflictException,
            UPIConflictException,
            UPINotAssignedException,
            UPIProcessorTypeException {
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            ".",
            "$(2)",
            ". RETURN CONTROL STACK",
            "RCDEPTH   $EQU      32",
            "RCSSIZE   $EQU      2*RCDEPTH",
            "RCSTACK   $RES      RCSSIZE",
            ".",
            "$(1)      . extended mode i-bank",
            "          $LIT",
            "START",
            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
            ".",
            "          . ESTABLISH RCS ON B25/EX0",
            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
            "          LXI,U     EX0,0",
            "          LXM,U     EX0,RCSTACK+RCSSIZE",
            ".",
            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
            "",
            ".",
            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "",
            "          LBU       B15,(LBDIREF$+DATA,0)",
            "          LD        (014,0)",
            "          CALL      (LBDIREF$+BASIC, BASIC)",
            ".",
            "          $BASIC",
            "$(3)      . basic mode i-bank",
            "          $LIT",
            "BASIC",
            "          LXI,U     X8,4",
            "          LXM,U     X8,0",
            "          SBED      B16,DATA,*X8",
            "          HALT      0",
            ".",
            "$(4)      . basic mode d-bank",
            "DATA      $RES      64",
            ".",
            "          $END      START"
        };

        buildMultiBank(source, true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     _instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void storeBaseRegisterExecDirect_BadPP_extended(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(4)",
            "DATA      $RES      64",
            ".",
            "$(1)",
            "          LBU       B3,(LBDIREF$+DATA, 0)",
            "          LD        (014, 0)",
            "          LXI,U     X8,4",
            "          LXM,U     X8,0",
            "          SBED      B16,DATA,*X8,B3",
            "          HALT      0"
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     _instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void storeBaseRegisterUser_basic(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $BASIC",
            "",
            "$(4)",
            "DATA      $RES 2",
            "",
            "$(3)",
            "          LBU       B13,(LBDIREF$+DATA, 0)",
            "          SBU       B12,DATA",
            "          LXM,U     X2,1",
            "          SBU       B13,DATA,X2",
            "          HALT      0"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());

        long[] data = getBankByBaseRegister(13);
        assertEquals(0_100007_000000L, data[0]);
        assertEquals(0_100010_000000L, data[1]);
    }

    @Test
    public void storeBaseRegisterUser_basic_badPP(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $BASIC",
            "",
            "$(4)",
            "DATA      $RES 2",
            "",
            "$(3)      $LIT",
            "          LD        (016,0) . basic mode, pp=3",
            "          LBU       B13,(LBDIREF$+DATA, 0)",
            "          SBU       B12,DATA",
            "          LXM,U     X2,1",
            "          SBU       B13,DATA,X2",
            "          HALT      0"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     _instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void storeBaseRegisterUser_extended(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(4)",
            "DATA      $RES 2",
            "",
            "$(1)",
            "          LBU       B3,(LBDIREF$+DATA, 0)",
            "          SBU       B0,DATA,,B3",
            "          LXM,U     X2,1",
            "          SBU       B3,DATA,X2,B3",
            "          HALT      0"
        };

        buildMultiBank(wrapForExtendedMode(source), true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());

        long[] data = getBankByBaseRegister(3);
        assertEquals(0_100005_000000L, data[0]);
        assertEquals(0_100007_000000L, data[1]);
    }

    @Test
    public void storeBaseRegisterUserDirect_basic(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $BASIC",
            "",
            "$(4)",
            "DATA      $RES 64",
            "",
            "$(3)      $LIT",
            "          LBU       B13,(LBDIREF$+DATA, 0)",
            "          LXI,U     X8,4",
            "          LXM,U     X8,0",
            "          SBUD      B12,DATA,*X8,B3",
            "          SBUD      B13,DATA,*X8,B3",
            "          SBUD      B14,DATA,*X8,B3",
            "          SBUD      B15,DATA,*X8,B3",
            "          HALT      0"
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());

        Assert.assertEquals(0_000004_000020, _instructionProcessor.getExecOrUserXRegister(8).getW());
        long[] data = getBankByBaseRegister(13);

        //  B12
        Assert.assertEquals(002, Word36.getS1(data[0]));
        Assert.assertEquals(1, Word36.getQ1(data[1]));
        Assert.assertEquals(01010, Word36.getH2(data[1]));

        //  B13
        Assert.assertEquals(003, Word36.getS1(data[4]));
        Assert.assertEquals(03, Word36.getQ1(data[5]));
        Assert.assertEquals(03077, Word36.getH2(data[5]));

        //  B14 (void)
        Assert.assertEquals(02, Word36.getS2(data[8]) | 02); //check for void flag

        //  B15 (void)
        Assert.assertEquals(02, Word36.getS2(data[12]) | 02); //check for void flag
    }

    @Test
    public void storeBaseRegisterUserDirect_extended(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0) . force at least one word into this bank",
            "DUMMY     + 0",
            "",
            "$(4)",
            "DATA      $RES 64",
            ".",
            "$(1)",
            "          GOTO      (LBDIREF$+TARGET, TARGET)",
            ".",
            "          $INFO 10 5",
            "$(5)      $LIT",
            "TARGET*",
            "          LBU       B3,(LBDIREF$+DATA, 0)",
            "          LXI,U     X8,4",
            "          LXM,U     X8,0",
            "          SBUD      B0,DATA,*X8,B3",
            "          SBUD      B2,DATA,*X8,B3",
            "          SBUD      B3,DATA,*X8,B3",
            "          SBUD      B4,DATA,*X8,B3",
            "          HALT      0"
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());

        Assert.assertEquals(0_000004_000020, _instructionProcessor.getExecOrUserXRegister(8).getW());
        long[] data = getBankByBaseRegister(3);

        //  B0
        Assert.assertEquals(002, Word36.getS1(data[0]));
        Assert.assertEquals(1, Word36.getQ1(data[1]));
        Assert.assertEquals(01010, Word36.getH2(data[1]));

        //  B2
        Assert.assertEquals(003, Word36.getS1(data[4]));
        Assert.assertEquals(0, Word36.getQ1(data[5]));

        //  B3
        Assert.assertEquals(003, Word36.getS1(data[8]));
        Assert.assertEquals(0, Word36.getQ1(data[9]));
        Assert.assertEquals(077, Word36.getH2(data[9]));

        //  B4 (void)
        Assert.assertEquals(02, Word36.getS2(data[12]) | 02); //check for void flag
    }

    @Test
    public void storeBaseRegisterUserDirect_BadPP_basic(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "          $BASIC",
            "",
            "$(4)",
            "DATA      $RES 64",
            "",
            "$(3)",
            "          LD        (016,0) . basic mode, pp=3",
            "          LBU       B13,(LBDIREF$+DATA, 0)",
            "          LXI,U     X8,4",
            "          LXM,U     X8,0",
            "          SBUD      B0,DATA,*X8",
            "          HALT      0"
        };

        buildMultiBank(wrapForBasicMode(source), true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void storeBaseRegisterUserDirect_extended_badPP(
    ) throws BinaryLoadException,
             MachineInterrupt,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException,
             UPINotAssignedException,
             UPIProcessorTypeException {
        String[] source = {
            "$(0)",
            "DATA      $RES 64",
            "",
            "$(1)",
            "          LD        (014,0)",
            "          LXI,U     X8,4",
            "          LXM,U     X8,0",
            "          SBUD      B8,DATA,*X8,B3",
            "          HALT      077"
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        ipl(true);

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
        Assert.assertEquals(01016, _instructionProcessor.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
                     _instructionProcessor.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
                     _instructionProcessor.getLastInterrupt().getShortStatusField());
    }

    //TODO
//    @Test
//    public void testRelativeAddress_basic(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "$(0) . this will be 0600005 based on B13",
//            "DATA14    + 0",
//            "",
//            "$(1) . this will be 0600004 based on B12",
//            "START$*",
//            "          TRA       X12,DATA12",
//            "          HALT      077 . should skip",
//            "          TRA       X13,DATA13",
//            "          HALT      076 . should skip",
//            "          TRA       X14,DATA14",
//            "          HALT      075 . should skip",
//            "          TRA       X15,DATA15",
//            "          HALT      074 . should skip",
//            "          TRA       X5,070000",    // doesn't exist
//            "          HALT      0 . this one should NOT be skipped",
//            "          HALT      073",
//            "",
//            "DATA12    + 0",
//            "",
//            "$(2) . this will be 0600007 based on B15",
//            "DATA15    + 0",
//            "",
//            "$(3) . this will be 0600006 based on B14",
//            "DATA13    + 0",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasicMultibank(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//
//        startAndWait(_instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(_instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(_mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
//
//        Assert.assertEquals(0, _instructionProcessor.getExecOrUserXRegister(5).getW());
//        Assert.assertEquals(0400000_000000L, _instructionProcessor.getExecOrUserXRegister(12).getW());
//        Assert.assertEquals(0600000_000000L, _instructionProcessor.getExecOrUserXRegister(13).getW());
//        Assert.assertEquals(0500000_000000L, _instructionProcessor.getExecOrUserXRegister(14).getW());
//        Assert.assertEquals(0700000_000000L, _instructionProcessor.getExecOrUserXRegister(15).getW());
//    }

    //TODO
//    @Test
//    public void testRelativeAddressIndirect_basic(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "$(0) . this will be 0600005 based on B13",
//            "DATA14    NOP       *DATA15",
//            "",
//            "$(1) . this will be 0600004 based on B12",
//            "START$*",
//            "          TRA       X12,*DATA12",
//            "          HALT      077 . should skip",
//            "          HALT      0 . this one should NOT be skipped",
//            "",
//            "DATA12    NOP       *DATA14",
//            "",
//            "$(2) . this will be 0600007 based on B15",
//            "DATA15    NOP       DATA13",
//            "",
//            "$(3) . this will be 0600006 based on B14",
//            "DATA13    $res 32",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeBasicMultibank(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//
//        startAndWait(_instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(_instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(_mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
//
//        Assert.assertEquals(0, _instructionProcessor.getExecOrUserXRegister(5).getW());
//        Assert.assertEquals(0600000_000000L, _instructionProcessor.getExecOrUserXRegister(12).getW());
//    }

    //TODO
//    @Test
//    public void testRelativeAddress_extended(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(0)      $LIT . this will be bank 0600005 based on B2",
//            "DATA",
//            "",
//            "$(1) . this will be bank 0600004 based on B0",
//            "START$*",
//            "          . base data bank on B12 through B15",
//            "          LBU       B12,(0600005000000),,B2",
//            "          LBU       B13,(0600005000000),,B2",
//            "          LBU       B14,(0600005000000),,B2",
//            "          LBU       B15,(0600005000000),,B2",
//            "",
//            "          . do the tests",
//            "          TRA       X12,DATA,,B12",
//            "          HALT      077 . should skip",
//            "          TRA       X13,DATA,,B13",
//            "          HALT      076 . should skip",
//            "          TRA       X14,DATA,,B14",
//            "          HALT      075 . should skip",
//            "          TRA       X15,DATA,,B15",
//            "          HALT      074 . should skip",
//            "          TRA       X5,070000,,B12",       // doesn't exist
//            "          HALT      0 . this one should NOT be skipped",
//            "          HALT      073",
//        };
//
//        AbsoluteModule absoluteModule = buildCodeExtended(source, false);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//
//        startAndWait(_instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(_instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(_mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
//
//        Assert.assertEquals(0, _instructionProcessor.getExecOrUserXRegister(5).getW());
//        Assert.assertEquals(0400000_000000L, _instructionProcessor.getExecOrUserXRegister(12).getW());
//        Assert.assertEquals(0500000_000000L, _instructionProcessor.getExecOrUserXRegister(13).getW());
//        Assert.assertEquals(0600000_000000L, _instructionProcessor.getExecOrUserXRegister(14).getW());
//        Assert.assertEquals(0700000_000000L, _instructionProcessor.getExecOrUserXRegister(15).getW());
//    }

    //TODO
//    @Test
//    public void testRelativeAddressNonRWBanks_basic(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $BASIC",
//            "$(0) . this will be 0600005 based on B14",
//            "DATA14    + 0",
//            "",
//            "$(1) . this will be 0600004 based on B12",
//            "START$*",
//            "          TRA       X13,DATA13",
//            "          J         TARGET1 . should not skip",
//            "          HALT      077",
//            "",
//            "TARGET1",
//            "          TRA       X14,DATA14",
//            "          J         TARGET2 . should not skip",
//            "          HALT      076 . should skip",
//            "",
//            "TARGET2",
//            "          TRA       X15,DATA15",
//            "          J         DONE",
//            "          HALT      075 . should skip",
//            "",
//            "DONE",
//            "          HALT      0",
//            "",
//            "$(2) . this will be 0600007 based on B15",
//            "DATA15    + 0",
//            "",
//            "$(3) . this will be 0600006 based on B13",
//            "DATA13    + 0",
//        };
//
//        //  Special construction of the absolute
//        Assembler.Option[] asmOptions = {};
//        Assembler asm = new Assembler();
//        OldRelocatableModule relModule = asm.assemble("TEST", source, asmOptions);
//
//        Linker.LCPoolSpecification[] bank4PoolSpecs = {
//            new Linker.LCPoolSpecification(relModule, 1),
//        };
//
//        Linker.LCPoolSpecification[] bank5PoolSpecs = {
//            new Linker.LCPoolSpecification(relModule, 0),
//        };
//
//        Linker.LCPoolSpecification[] bank6PoolSpecs = {
//            new Linker.LCPoolSpecification(relModule, 3),
//        };
//
//        Linker.LCPoolSpecification[] bank7PoolSpecs = {
//            new Linker.LCPoolSpecification(relModule, 2),
//        };
//
//        AccessInfo lock = new AccessInfo((short)3, 100);
//        AccessPermissions iBankPerms = new AccessPermissions(true, true, false);
//        AccessPermissions noReadPerms = new AccessPermissions(false, false, true);
//        AccessPermissions noWritePerms = new AccessPermissions(false, true, false);
//        AccessPermissions noPerms = new AccessPermissions(false, false, false);
//
//        Linker.BankDeclaration[] bankDeclarations = {
//            new Linker.BankDeclaration.Builder()
//                .setBankLevel(6)
//                .setBankDescriptorIndex(4)
//                .setBankName("BDI4BR12")
//                .setStartingAddress(01000)
//                .setInitialBaseRegister(12)
//                .setGeneralAccessPermissions(iBankPerms)
//                .setSpecialAccessPermissions(iBankPerms)
//                .setAccessInfo(lock)
//                .setPoolSpecifications(bank4PoolSpecs)
//                .build(),
//            new Linker.BankDeclaration.Builder()
//                .setBankLevel(6)
//                .setBankDescriptorIndex(5)
//                .setBankName("BDI5BR14")
//                .setStartingAddress(03000)
//                .setInitialBaseRegister(14)
//                .setGeneralAccessPermissions(noReadPerms)
//                .setSpecialAccessPermissions(noReadPerms)
//                .setAccessInfo(lock)
//                .setPoolSpecifications(bank5PoolSpecs)
//                .build(),
//            new Linker.BankDeclaration.Builder()
//                .setBankLevel(6)
//                .setBankDescriptorIndex(6)
//                .setBankName("BDI6BR13")
//                .setStartingAddress(05000)
//                .setInitialBaseRegister(13)
//                .setGeneralAccessPermissions(noWritePerms)
//                .setSpecialAccessPermissions(noWritePerms)
//                .setAccessInfo(lock)
//                .setPoolSpecifications(bank6PoolSpecs)
//                .build(),
//            new Linker.BankDeclaration.Builder()
//                .setBankLevel(6)
//                .setBankDescriptorIndex(7)
//                .setBankName("BDI7BR15")
//                .setStartingAddress(07000)
//                .setInitialBaseRegister(15)
//                .setGeneralAccessPermissions(noPerms)
//                .setSpecialAccessPermissions(noPerms)
//                .setAccessInfo(lock)
//                .setPoolSpecifications(bank7PoolSpecs)
//                .build(),
//        };
//
//        Linker.Option[] linkerOptions = {};
//        Linker linker = new Linker();
//        AbsoluteModule absoluteModule = linker.link("TEST", bankDeclarations, 0, linkerOptions);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//
//        startAndWait(_instructionProcessor);
//
//        InventoryManager.getInstance().deleteProcessor(_instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(_mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
//
//        Assert.assertEquals(0, _instructionProcessor.getExecOrUserXRegister(5).getW());
//        Assert.assertEquals(0500000_000000L, _instructionProcessor.getExecOrUserXRegister(13).getW());
//        Assert.assertEquals(0600000_000000L, _instructionProcessor.getExecOrUserXRegister(14).getW());
//        Assert.assertEquals(0700000_000000L, _instructionProcessor.getExecOrUserXRegister(15).getW());
//    }

    //TODO
//    @Test
//    public void testRelativeAddressNonRWBanks_extended(
//    ) throws MachineInterrupt,
//             MaxNodesException,
//             NodeNameConflictException,
//             UPIConflictException,
//             UPINotAssignedException {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(0)      $LIT . this will be bank 0600005 based on B2",
//            "          + 0 . nothing data",
//            "",
//            "$(1) . this will be bank 0600004 based on B0",
//            "START$*",
//            "          TRA       X13,DATA13,,B13",
//            "          J         TARGET1 . should not skip",
//            "          HALT      077",
//            "",
//            "TARGET1",
//            "          TRA       X14,DATA14,,B14",
//            "          J         TARGET2 . should not skip",
//            "          HALT      076 . should skip",
//            "",
//            "TARGET2",
//            "          TRA       X15,DATA15,,B15",
//            "          J         DONE",
//            "          HALT      075 . should skip",
//            "",
//            "DONE",
//            "          HALT      0",
//            "",
//            "$(2)",
//            "DATA13    0 . BDI 0600006 will be based on B13",
//            "",
//            "$(4)",
//            "DATA14    0 . BDI 0600007 will be based on B14",
//            "",
//            "$(6)",
//            "DATA15    0 . BDI 0600010 will be based on B15",
//        };
//
//        //  Special construction of the absolute
//        Assembler.Option[] asmOptions = {};
//        Assembler asm = new Assembler();
//        OldRelocatableModule relModule = asm.assemble("TEST", source, asmOptions);
//
//        Linker.LCPoolSpecification[] bank4PoolSpecs = {
//            new Linker.LCPoolSpecification(relModule, 1),
//        };
//
//        Linker.LCPoolSpecification[] bank5PoolSpecs = {
//            new Linker.LCPoolSpecification(relModule, 0),
//        };
//
//        Linker.LCPoolSpecification[] bank6PoolSpecs = {
//            new Linker.LCPoolSpecification(relModule, 2),
//        };
//
//        Linker.LCPoolSpecification[] bank7PoolSpecs = {
//            new Linker.LCPoolSpecification(relModule, 4),
//        };
//
//        Linker.LCPoolSpecification[] bank8PoolSpecs = {
//            new Linker.LCPoolSpecification(relModule, 6),
//        };
//
//        AccessInfo lock = new AccessInfo((short)3, 100);
//        AccessPermissions iBankPerms = new AccessPermissions(true, true, false);
//        AccessPermissions noReadPerms = new AccessPermissions(false, false, true);
//        AccessPermissions noWritePerms = new AccessPermissions(false, true, false);
//        AccessPermissions noPerms = new AccessPermissions(false, false, false);
//
//        Linker.BankDeclaration[] bankDeclarations = {
//            new Linker.BankDeclaration.Builder()
//                .setBankLevel(6)
//                .setBankDescriptorIndex(4)
//                .setBankName("BDI4BR0")
//                .setStartingAddress(01000)
//                .setInitialBaseRegister(0)
//                .setGeneralAccessPermissions(iBankPerms)
//                .setSpecialAccessPermissions(iBankPerms)
//                .setAccessInfo(lock)
//                .setPoolSpecifications(bank4PoolSpecs)
//                .build(),
//            new Linker.BankDeclaration.Builder()
//                .setBankLevel(6)
//                .setBankDescriptorIndex(5)
//                .setBankName("BDI5BR2")
//                .setStartingAddress(01000)
//                .setInitialBaseRegister(2)
//                .setGeneralAccessPermissions(noWritePerms)
//                .setSpecialAccessPermissions(noWritePerms)
//                .setAccessInfo(lock)
//                .setPoolSpecifications(bank5PoolSpecs)
//                .build(),
//            new Linker.BankDeclaration.Builder()
//                .setBankLevel(6)
//                .setBankDescriptorIndex(6)
//                .setBankName("BDI6BR13")
//                .setStartingAddress(01000)
//                .setInitialBaseRegister(13)
//                .setGeneralAccessPermissions(noReadPerms)
//                .setSpecialAccessPermissions(noReadPerms)
//                .setAccessInfo(lock)
//                .setPoolSpecifications(bank6PoolSpecs)
//                .build(),
//            new Linker.BankDeclaration.Builder()
//                .setBankLevel(6)
//                .setBankDescriptorIndex(7)
//                .setBankName("BDI7BR14")
//                .setStartingAddress(01000)
//                .setInitialBaseRegister(14)
//                .setGeneralAccessPermissions(noWritePerms)
//                .setSpecialAccessPermissions(noWritePerms)
//                .setAccessInfo(lock)
//                .setPoolSpecifications(bank7PoolSpecs)
//                .build(),
//            new Linker.BankDeclaration.Builder()
//                .setBankLevel(6)
//                .setBankDescriptorIndex(8)
//                .setBankName("BDI8BR15")
//                .setStartingAddress(01000)
//                .setInitialBaseRegister(15)
//                .setGeneralAccessPermissions(noPerms)
//                .setSpecialAccessPermissions(noPerms)
//                .setAccessInfo(lock)
//                .setPoolSpecifications(bank8PoolSpecs)
//                .build(),
//        };
//
//        Linker.Option[] linkerOptions = {};
//        Linker linker = new Linker();
//        AbsoluteModule absoluteModule = linker.link("TEST", bankDeclarations, 0, linkerOptions);
//        assert(absoluteModule != null);
//        Processors processors = loadModule(absoluteModule);
//
//        startAndWait(_instructionProcessor);
//        showDebugInfo(processors);
//
//        InventoryManager.getInstance().deleteProcessor(_instructionProcessor._upiIndex);
//        InventoryManager.getInstance().deleteProcessor(_mainStorageProcessor._upiIndex);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, _instructionProcessor.getLatestStopReason());
//        Assert.assertEquals(0, _instructionProcessor.getLatestStopDetail());
//
//        Assert.assertEquals(0500000_000000L, _instructionProcessor.getExecOrUserXRegister(13).getW());
//        Assert.assertEquals(0600000_000000L, _instructionProcessor.getExecOrUserXRegister(14).getW());
//        Assert.assertEquals(0700000_000000L, _instructionProcessor.getExecOrUserXRegister(15).getW());
//    }

    //  TODO testRelativeAddressRange ... some day when we care more about it

    //  TODO lots of testVirtualAddress tests
}
