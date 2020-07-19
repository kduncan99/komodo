/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.instructionProcessor;

import com.kadware.komodo.baselib.AbsoluteAddress;
import com.kadware.komodo.baselib.AccessInfo;
import com.kadware.komodo.baselib.GeneralRegisterSet;
import com.kadware.komodo.hardwarelib.BaseRegister;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import static com.kadware.komodo.hardwarelib.interrupts.InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import static com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt.InterruptClass.AddressingException;
import static com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt.InterruptClass.InvalidInstruction;
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
    public void after() {
        clear();
    }

    @Test
    public void decelerateActiveBaseTable_extended(
    ) throws Exception {
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
            "DATA      $RES 30"
        };

        buildMultiBank(wrapForExtendedMode(source), false, false);
        createConfiguration();
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

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        Assert.assertEquals(1, ip.getGeneralRegister(GeneralRegisterSet.X2).getH1());
        Assert.assertEquals(16, ip.getGeneralRegister(GeneralRegisterSet.X2).getH2());
        long[] dataBank = getBankByBaseRegister(ip, 2);
        long[] subset = Arrays.copyOfRange(dataBank, 15, 30);
        Assert.assertArrayEquals(expected, subset);
    }

    @Test
    public void decelerateActiveBaseTable_extended_error1(
    ) throws Exception {
        String[] source = {
            "$(0)",
            "DATA      $RES 30",
            "",
            "$(1)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "          DABT      DATA,*X2,B8",
            "          HALT      0"
        };

        buildMultiBank(wrapForExtendedMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01010, ip.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.ReferenceViolation.getCode(),
                     ip.getLastInterrupt().getInterruptClass().getCode());
        assertEquals(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation.getCode() << 4,
                     ip.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void decelerateActiveBaseTable_extended_error2(
    ) throws Exception {
        String[] source = {
            "$(0)",
            "DATA      $RES 30",
            "",
            "$(1)",
            "          LXI,U     X2,1",
            "          LXM,U     X2,150",
            "          DABT      DATA,*X2,B2",
            "          HALT      0",
        };

        buildMultiBank(wrapForExtendedMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01010, ip.getLatestStopDetail());
        assertEquals(MachineInterrupt.InterruptClass.ReferenceViolation.getCode(),
                     ip.getLastInterrupt().getInterruptClass().getCode());
        assertEquals(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation.getCode() << 4,
                     ip.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void decelerateActiveBaseTable_extended_error3(
    ) throws Exception {
        String[] source = {
            "$(1)",
            "          . GET DESIGNATOR REGISTER FOR PROCESSOR PRIVILEGE = 2",
            "          LD        (000034,000000)",
            ".",
            "          LXI,U     X2,1",
            "          LXM,U     X2,15",
            "          DABT      DATA,*X2,B8",
            "          HALT      0",
            ".",
            "DATA      $RES 30"
        };

        buildMultiBank(wrapForExtendedMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01016, ip.getLatestStopDetail());
        assertEquals(InvalidInstruction,
                     ip.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidProcessorPrivilege.getCode(),
                     ip.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterExec_basic(
    ) throws Exception {
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

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());

        BaseRegister b28 = ip.getBaseRegister(28);
        assertFalse(b28._voidFlag);
        assertFalse(b28._largeSizeFlag);
        assertEquals(03000, b28._lowerLimitNormalized);
        assertEquals(03017, b28._upperLimitNormalized);

        BaseRegister b29 = ip.getBaseRegister(29);
        assertTrue(b29._voidFlag);
    }

    @Test
    public void loadBaseRegisterExec_extended(
    ) throws Exception {
        String[] source = {
            "$(0)",
            "DUMMY     $RES      16",
            "",
            "$(1)",
            "          LBE       B27,BDENTRY1",
            "          LBE       B29,BDENTRY3",
            "          HALT      0",
            "",
            "BDENTRY1  + LBDIREF$+DUMMY,0",
            "BDENTRY3  + 0,0       . void",
        };

        buildMultiBank(wrapForExtendedMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        BaseRegister br27 = ip.getBaseRegister(27);
        assertFalse(br27._voidFlag);
        assertFalse(br27._largeSizeFlag);
        assertEquals(0, br27._lowerLimitNormalized);
        assertEquals(15, br27._upperLimitNormalized);
        assertEquals(new AccessInfo((short) 0, 0), br27._accessLock);

        BaseRegister br29 = ip.getBaseRegister(29);
        assertTrue(br29._voidFlag);
    }

    @Test
    public void loadBaseRegisterExec_BadBank_basic(
    ) throws Exception {
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

        buildMultiBank(source, false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01011, ip.getLatestStopDetail());
    }

    @Test
    public void loadBaseRegisterExec_BadBank_extended(
    ) throws Exception {
        String[] source = {
            "$(1)",
            "          LBE       B27,BDENTRY",
            "          HALT      0 . should not get here",
            "",
            "BDENTRY   + 0300006,0 . does not exist (wrong bank level and index)",
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01011, ip.getLatestStopDetail());
    }

    @Test
    public void loadBaseRegisterExec_badPP_basic(
    ) throws Exception {
        String[] source = {
            "          $INCLUDE 'GEN$DEFS'",
            "          DR$SETPP  03",
            "",
            "          LBE       B27,BDENTRY",
            "          HALT      0 . should not get here",
            "",
            "BDENTRY   + 0100007,0 . data bank",
        };

        buildMultiBank(wrapForBasicMode(source), true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01016, ip.getLatestStopDetail());
        assertEquals(InvalidInstruction,
                     ip.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidProcessorPrivilege.getCode(),
                     ip.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterExec_badPP_extended(
    ) throws Exception {
        String[] source = {
            "          $INCLUDE 'GEN$DEFS'",
            "          DR$SETPP  03",
            "",
            "          LBE       B27,BDENTRY",
            "          HALT      0 . should not get here",
            "",
            "BDENTRY   + 0100006,0 . data bank",
        };

        buildMultiBank(wrapForExtendedMode(source),false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01016, ip.getLatestStopDetail());
        assertEquals(InvalidInstruction,
                     ip.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidProcessorPrivilege.getCode(),
                     ip.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterExecDirect_basic(
    ) throws Exception {
        String[] source = {
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
        };

        buildMultiBank(wrapForBasicMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());

        BaseRegister br27 = ip.getBaseRegister(27);
        assertTrue(br27._voidFlag);
        assertEquals(new AccessInfo((short) 3, 010), br27._accessLock);

        BaseRegister br28 = ip.getBaseRegister(28);
        assertTrue(br28._voidFlag);
        assertEquals(new AccessInfo((short) 0, 014), br28._accessLock);

        BaseRegister br29 = ip.getBaseRegister(29);
        assertFalse(br29._voidFlag);
        assertFalse(br29._largeSizeFlag);
        assertEquals(new AccessInfo((short) 2, 0100), br29._accessLock);
        assertEquals(01, br29.getLowerLimit());
        assertEquals(01777, br29.getUpperLimit());
        Assert.assertEquals(new AbsoluteAddress((short)1, 0, 070000), br29._baseAddress);
    }

    @Test
    public void loadBaseRegisterExecDirect_extended(
    ) throws Exception {
        String[] source = {
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
        };

        buildMultiBank(wrapForExtendedMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());

        BaseRegister br27 = ip.getBaseRegister(27);
        assertTrue(br27._voidFlag);
        assertEquals(new AccessInfo((short) 3, 010), br27._accessLock);

        BaseRegister br28 = ip.getBaseRegister(28);
        assertTrue(br28._voidFlag);
        assertEquals(new AccessInfo((short) 0, 014), br28._accessLock);

        BaseRegister br29 = ip.getBaseRegister(29);
        assertFalse(br29._voidFlag);
        assertFalse(br29._largeSizeFlag);
        assertEquals(new AccessInfo((short) 2, 0100), br29._accessLock);
        assertEquals(01, br29.getLowerLimit());
        assertEquals(01777, br29.getUpperLimit());
        assertEquals(new AbsoluteAddress((short)1, 0, 070000), br29._baseAddress);
    }

    @Test
    public void loadBaseRegisterExecDirect_badPP_basic(
    ) throws Exception {
        String[] source = {
            "          $INCLUDE 'GEN$DEFS'",
            "          DR$SETPP 01",
            "",
            "          LBED      B27,BDENTRY1   . void bank",
            "          HALT      0",
            "",
            "BDENTRY1  . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01016, ip.getLatestStopDetail());
        assertEquals(InvalidInstruction,
                     ip.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidProcessorPrivilege.getCode(),
                     ip.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterExecDirect_badPP_extended(
    ) throws Exception {
        String[] source = {
            "          $INCLUDE 'GEN$DEFS'",
            "          DR$SETPP  03",
            "",
            "          LBED      B27,BDENTRY1,,B0   . void bank",
            "          HALT      0",
            "",
            "BDENTRY1  . void bank with void flag",
            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
            "          + 0               . lower/upper limits 0",
            "          + 0               . base address 0",
            "          + 0               . base address 0",
        };

        buildMultiBank(wrapForExtendedMode(source), true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01016, ip.getLatestStopDetail());
        assertEquals(InvalidInstruction,
                     ip.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidProcessorPrivilege.getCode(),
                     ip.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBankName_basic(
    ) throws Exception {
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

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01011, ip.getLatestStopDetail());
        Assert.assertEquals(0_000000_000000L, ip.getGeneralRegister(GeneralRegisterSet.X1).getW());
        Assert.assertEquals(0_000037_000000L, ip.getGeneralRegister(GeneralRegisterSet.X2).getW());
        Assert.assertEquals(0_100004_000000L, ip.getGeneralRegister(GeneralRegisterSet.X3).getW());
        Assert.assertEquals(0_100005_000000L, ip.getGeneralRegister(GeneralRegisterSet.X4).getW());
        Assert.assertEquals(0_200000_000000L, ip.getGeneralRegister(GeneralRegisterSet.X5).getW());
        Assert.assertEquals(0_200001_000000L, ip.getGeneralRegister(GeneralRegisterSet.X6).getW());
        Assert.assertEquals(0_200002_000000L, ip.getGeneralRegister(GeneralRegisterSet.X7).getW());
        Assert.assertEquals(0_200007_000000L, ip.getGeneralRegister(GeneralRegisterSet.X8).getW());
    }

    @Test
    public void loadBankName_bad_basic(
    ) throws Exception {
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

        buildMultiBank(source, true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01011, ip.getLatestStopDetail());
    }

    @Test
    public void loadBankName_badPP_basic(
    ) throws Exception {
        String[] source = {
            "          $INCLUDE 'GEN$DEFS'",
            "          DR$SETPP  02",
            "",
            "          LBN       X5,DATA",
            "          HALT      0",
            "          HALT      077",
            "",
            "DATA      +         0",
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01016, ip.getLatestStopDetail());
        assertEquals(InvalidInstruction,
                     ip.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidProcessorPrivilege.getCode(),
                     ip.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterUser_basic(
    ) throws Exception {
        String[] source = {
            "$(6),DUMMY $RES 16",
            "",
            "$(3) . back to basic mode code",
            "          LBU       B7,(LBDIREF$+DUMMY,0)",
            "          LBU       B8,(0)",
            "          HALT      0",
        };

        buildMultiBank(wrapForBasicMode(source), true, true);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        BaseRegister br7 = ip.getBaseRegister(7);
        assertFalse(br7._voidFlag);
        assertFalse(br7._largeSizeFlag);
        assertEquals(03000, br7._lowerLimitNormalized);
        assertEquals(03017, br7._upperLimitNormalized);
        assertEquals(new AccessInfo((short) 0, 0), br7._accessLock);

        BaseRegister br8 = ip.getBaseRegister(8);
        assertTrue(br8._voidFlag);
    }

    @Test
    public void loadBaseRegisterUser_extended(
    ) throws Exception {
        String[] source = {
            "$(6),DUMMY $RES 16",
            "",
            "$(1)",
            "          LBU       B7,(LBDIREF$+DUMMY, 0)",
            "          LBU       B9,(0)",
            "          HALT      0",
        };

        buildMultiBank(wrapForExtendedMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(0, ip.getLatestStopDetail());
        BaseRegister br7 = ip.getBaseRegister(7);
        assertFalse(br7._voidFlag);
        assertFalse(br7._largeSizeFlag);
        assertEquals(0, br7._lowerLimitNormalized);
        assertEquals(15, br7._upperLimitNormalized);
        assertEquals(new AccessInfo((short) 0, 0), br7._accessLock);

        BaseRegister br9 = ip.getBaseRegister(9);
        assertTrue(br9._voidFlag);
    }

    @Test
    public void loadBaseRegisterUser_badPP_basic(
    ) throws Exception {
        String[] source = {
            "          $INCLUDE 'GEN$DEFS'",
            "          DR$SETPP 02",
            "",
            "          LBU       B5,(0100005,0)",
            "          HALT      077",
        };

        buildMultiBank(wrapForBasicMode(source), true, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01000 + InvalidInstruction.getCode(),
                            ip.getLatestStopDetail());
        assertEquals(InvalidInstruction, ip.getLastInterrupt().getInterruptClass());
        assertEquals(InvalidProcessorPrivilege.getCode(), ip.getLastInterrupt().getShortStatusField());
    }

    @Test
    public void loadBaseRegisterUser_BadBank_basic(
    ) throws Exception {
        String[] source = {
            "          LBU       B5,(0100777,0)",
            "          HALT      077"
        };

        buildMultiBank(wrapForBasicMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01000 + AddressingException.getCode(), ip.getLatestStopDetail());
    }

    @Test
    public void loadBaseRegisterUser_BadBank_extended(
    ) throws Exception {
        String[] source = {
            "          LBU       B5,(0400070,0)    . non-existing bank",
            "          HALT      077               . should not get here"
        };

        buildMultiBank(wrapForExtendedMode(source), false, false);
        createConfiguration();
        ipl(true);

        InstructionProcessor ip = getFirstIP();

        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
        Assert.assertEquals(01000 + AddressingException.getCode(), ip.getLatestStopDetail());
    }

//    @Test
//    public void loadBaseRegisterUser_InvalidBank_basic(
//    ) throws Exception {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(4)      . BDI 100006",
//            ". RETURN CONTROL STACK",
//            "RCDEPTH   $EQU      32",
//            "RCSSIZE   $EQU      2*RCDEPTH",
//            "RCSTACK   $RES      RCSSIZE",
//            ".",
//            "$(1)      . extended mode i-bank 0100004",
//            "          $LIT",
//            "START",
//            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
//            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
//            ".",
//            "          . ESTABLISH RCS ON B25/EX0",
//            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
//            "          LXI,U     EX0,0",
//            "          LXM,U     EX0,RCSTACK+RCSSIZE",
//            "",
//            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
//            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
//            ".",
//            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
//            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
//            "          LXI,U     X2,1",
//            "          LXM,U     X2,15",
//            "",
//            "          CALL      (LBDIREF$+BASIC, BASIC)",
//            "",
//            "          $BASIC",
//            "$(3)      . basic mode i-bank 0100005",
//            "          $LIT",
//            "BASIC",
//            "          LBU       B5,(01,0) . invalid BDI",
//            "          HALT      077",
//            "",
//            "          $END      START"
//        };
//
//        buildMultiBank(source, true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(01011, ip.getLatestStopDetail());
//    }
//
//    @Test
//    public void loadBaseRegisterUser_InvalidBank_extended(
//    ) throws Exception {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(4)      . BDI 100006, starts at 0",
//            ". RETURN CONTROL STACK",
//            "RCDEPTH   $EQU      32",
//            "RCSSIZE   $EQU      2*RCDEPTH",
//            "RCSTACK   $RES      RCSSIZE",
//            ".",
//            "$(1)      . extended mode i-bank 0100004",
//            "          $LIT",
//            "START",
//            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
//            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
//            ".",
//            "          . ESTABLISH RCS ON B25/EX0",
//            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
//            "          LXI,U     EX0,0",
//            "          LXM,U     EX0,RCSTACK+RCSSIZE",
//            "",
//            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
//            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
//            ".",
//            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
//            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
//            "          LXI,U     X2,1",
//            "          LXM,U     X2,15",
//            "",
//            "          LBU       B5,(31,0)         . invalid BDI",
//            "          HALT      077               . should not get here",
//            "",
//            "          $END      START"
//        };
//
//        buildMultiBank(source, true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(01011, ip.getLatestStopDetail());
//    }
//
//    @Test
//    public void loadBaseRegisterUser_InvalidBR0_basic(
//    ) throws Exception {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(4)      . BDI 100006",
//            ". RETURN CONTROL STACK",
//            "RCDEPTH   $EQU      32",
//            "RCSSIZE   $EQU      2*RCDEPTH",
//            "RCSTACK   $RES      RCSSIZE",
//            ".",
//            "$(1)      . extended mode i-bank 0100004",
//            "          $LIT",
//            "START",
//            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
//            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
//            ".",
//            "          . ESTABLISH RCS ON B25/EX0",
//            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
//            "          LXI,U     EX0,0",
//            "          LXM,U     EX0,RCSTACK+RCSSIZE",
//            "",
//            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
//            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
//            ".",
//            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
//            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
//            "          LXI,U     X2,1",
//            "          LXM,U     X2,15",
//            "",
//            "          CALL      (LBDIREF$+BASIC, BASIC)",
//            "",
//            "          $BASIC",
//            "$(3)      . basic mode i-bank 0100005",
//            "          $LIT",
//            "BASIC",
//            "          LBU       B0,(LBDIREF$+START,0) . invalid BReg",
//            "          HALT      077",
//            "",
//            "          $END      START"
//        };
//
//        buildMultiBank(source, true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(01016, ip.getLatestStopDetail());
//        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
//                     ip.getLastInterrupt().getInterruptClass());
//        assertEquals(InvalidInstructionInterrupt.Reason.InvalidBaseRegister.getCode(),
//                     ip.getLastInterrupt().getShortStatusField());
//    }
//
//    @Test
//    public void loadBaseRegisterUser_InvalidBR0_extended(
//    ) throws Exception {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(4)      . BDI 100006, starts at 0",
//            ". RETURN CONTROL STACK",
//            "RCDEPTH   $EQU      32",
//            "RCSSIZE   $EQU      2*RCDEPTH",
//            "RCSTACK   $RES      RCSSIZE",
//            ".",
//            "$(1)      . extended mode i-bank 0100004",
//            "          $LIT",
//            "START",
//            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
//            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
//            ".",
//            "          . ESTABLISH RCS ON B25/EX0",
//            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
//            "          LXI,U     EX0,0",
//            "          LXM,U     EX0,RCSTACK+RCSSIZE",
//            "",
//            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
//            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
//            ".",
//            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
//            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
//            "          LXI,U     X2,1",
//            "          LXM,U     X2,15",
//            "",
//            "          LBU       B0,(LBDIREF$+START,0) . invalid BReg",
//            "          HALT      077                   . should not get here",
//            "",
//            "          $END      START"
//        };
//
//        buildMultiBank(source, true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(01016, ip.getLatestStopDetail());
//        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
//                     ip.getLastInterrupt().getInterruptClass());
//        assertEquals(InvalidInstructionInterrupt.Reason.InvalidBaseRegister.getCode(),
//                     ip.getLastInterrupt().getShortStatusField());
//    }
//
//    @Test
//    public void loadBaseRegisterUser_InvalidBR1_basic(
//    ) throws Exception {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(4)      . BDI 100006",
//            ". RETURN CONTROL STACK",
//            "RCDEPTH   $EQU      32",
//            "RCSSIZE   $EQU      2*RCDEPTH",
//            "RCSTACK   $RES      RCSSIZE",
//            ".",
//            "$(1)      . extended mode i-bank 0100004",
//            "          $LIT",
//            "START",
//            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
//            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
//            ".",
//            "          . ESTABLISH RCS ON B25/EX0",
//            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
//            "          LXI,U     EX0,0",
//            "          LXM,U     EX0,RCSTACK+RCSSIZE",
//            "",
//            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
//            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
//            ".",
//            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
//            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
//            "          LXI,U     X2,1",
//            "          LXM,U     X2,15",
//            "",
//            "          CALL      (LBDIREF$+BASIC, BASIC)",
//            "",
//            "          $BASIC",
//            "$(3)      . basic mode i-bank 0100005",
//            "          $LIT",
//            "BASIC",
//            "          LBU       B1,(LBDIREF$+START,0) . invalid BReg",
//            "          HALT      077",
//            "",
//            "          $END      START"
//        };
//
//        buildMultiBank(source, true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(01016, ip.getLatestStopDetail());
//        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
//                     ip.getLastInterrupt().getInterruptClass());
//        assertEquals(InvalidInstructionInterrupt.Reason.InvalidBaseRegister.getCode(),
//                     ip.getLastInterrupt().getShortStatusField());
//    }
//
//    @Test
//    public void loadBaseRegisterUser_InvalidBR1_extended(
//    ) throws Exception {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(4)",
//            ". RETURN CONTROL STACK",
//            "RCDEPTH   $EQU      32",
//            "RCSSIZE   $EQU      2*RCDEPTH",
//            "RCSTACK   $RES      RCSSIZE",
//            ".",
//            "$(1)",
//            "          $LIT",
//            "START",
//            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
//            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
//            ".",
//            "          . ESTABLISH RCS ON B25/EX0",
//            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
//            "          LXI,U     EX0,0",
//            "          LXM,U     EX0,RCSTACK+RCSSIZE",
//            "",
//            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
//            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
//            ".",
//            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
//            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
//            "          LXI,U     X2,1",
//            "          LXM,U     X2,15",
//            "",
//            "          LBU       B1,(LBDIREF$+START,0) . invalid BReg",
//            "          HALT      077                   . should not get here",
//            "",
//            "          $END      START"
//        };
//
//        buildMultiBank(source, true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(01016, ip.getLatestStopDetail());
//        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
//                     ip.getLastInterrupt().getInterruptClass());
//        assertEquals(InvalidInstructionInterrupt.Reason.InvalidBaseRegister.getCode(),
//                     ip.getLastInterrupt().getShortStatusField());
//    }
//
//    @Test
//    public void loadBaseRegisterUserDirect_basic(
//    ) throws Exception {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            ".",
//            "$(2)",
//            ". RETURN CONTROL STACK",
//            "RCDEPTH   $EQU      32",
//            "RCSSIZE   $EQU      2*RCDEPTH",
//            "RCSTACK   $RES      RCSSIZE",
//            ".",
//            "$(1)      . extended mode i-bank",
//            "          $LIT",
//            "START",
//            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
//            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
//            ".",
//            "          . ESTABLISH RCS ON B25/EX0",
//            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
//            "          LXI,U     EX0,0",
//            "          LXM,U     EX0,RCSTACK+RCSSIZE",
//            ".",
//            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
//            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
//            ".",
//            "          CALL      (LBDIREF$+BASIC, BASIC)",
//            ".",
//            "          $BASIC",
//            "$(3)      . basic mode i-bank",
//            "          $LIT",
//            "BASIC",
//            "          LBUD      B5,BDENTRY1   . void bank",
//            "          LBUD      B6,BDENTRY2   . effectively void",
//            "          LBUD      B7,BDENTRY3   . not void",
//            "          HALT      0",
//            ".",
//            "BDENTRY1  . void bank with void flag",
//            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
//            "          + 0               . lower/upper limits 0",
//            "          + 0               . base address 0",
//            "          + 0               . base address 0",
//            ".",
//            "BDENTRY2  . void bank with lower > upper",
//            "          + 0000000,0000014 . GAP/SAP clear, void clear, ring=0 domain=014",
//            "          + 001000,000177   . lower limit 01000, upper limit 0177",
//            "          + 0               . base address 0",
//            "          + 0               . base address 0",
//            ".",
//            "BDENTRY3  . normal bank",
//            "          + 0770000,0400100 . GAP/SAP erw set, ring=2 domain=0100",
//            "          + 001000,01777    . lower limit 01000, upper limit 01777",
//            "          + 0               . base address segment 0",
//            "          + 040000,070000   . base address UPI,offset 01,070000",
//            ".",
//            "          $END      START"
//        };
//
//        buildMultiBank(source, false, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(0, ip.getLatestStopDetail());
//
//        BaseRegister br5 = ip.getBaseRegister(5);
//        assertTrue(br5._voidFlag);
//        assertEquals(new AccessInfo((short) 3, 010), br5._accessLock);
//
//        BaseRegister br6 = ip.getBaseRegister(6);
//        assertTrue(br6._voidFlag);
//        assertEquals(new AccessInfo((short) 0, 014), br6._accessLock);
//
//        BaseRegister br7 = ip.getBaseRegister(7);
//        assertFalse(br7._voidFlag);
//        assertFalse(br7._largeSizeFlag);
//        assertEquals(new AccessInfo((short) 2, 0100), br7._accessLock);
//        assertEquals(01, br7.getLowerLimit());
//        assertEquals(01777, br7.getUpperLimit());
//        assertEquals(new AbsoluteAddress((short)1, 0, 070000), br7._baseAddress);
//    }
//
//    @Test
//    public void loadBaseRegisterUserDirect_extended(
//    ) throws Exception {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(2)",
//            "BDENTRY1  . void bank with void flag",
//            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
//            "          + 0               . lower/upper limits 0",
//            "          + 0               . base address 0",
//            "          + 0               . base address 0",
//            "",
//            "BDENTRY2  . void bank with lower > upper",
//            "          + 0000000,0000014 . GAP/SAP clear, void clear, ring=0 domain=014",
//            "          + 001000,000177   . lower limit 01000, upper limit 0177",
//            "          + 0               . base address 0",
//            "          + 0               . base address 0",
//            "",
//            "BDENTRY3  . normal bank",
//            "          + 0770004,0400100 . GAP/SAP erw set, large size ring=2 domain=0100",
//            "          + 001000,01777    . lower limit 0100000, upper limit 0177700",
//            "          + 0               . base address segment 0",
//            "          + 040000,070000   . base address UPI,offset 01,070000",
//            "",
//            "$(1)      $LIT",
//            "START",
//            "          LD        (0,0)",
//            "          LBU       B2,(LBDIREF$+BDENTRY1,0)",
//            "          LBUD      B5,BDENTRY1,,B2   . void bank",
//            "          LBUD      B6,BDENTRY2,,B2   . effectively void",
//            "          LBUD      B7,BDENTRY3,,B2   . not void",
//            "          HALT      0",
//            "",
//            "          $END      START"
//        };
//
//        buildMultiBank(source, false, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(0, ip.getLatestStopDetail());
//
//        BaseRegister br5 = ip.getBaseRegister(5);
//        assertTrue(br5._voidFlag);
//        assertEquals(new AccessInfo((short) 3, 010), br5._accessLock);
//
//        BaseRegister br6 = ip.getBaseRegister(6);
//        assertTrue(br6._voidFlag);
//        assertEquals(new AccessInfo((short) 0, 014), br6._accessLock);
//
//        BaseRegister br7 = ip.getBaseRegister(7);
//        assertFalse(br7._voidFlag);
//        assertTrue(br7._largeSizeFlag);
//        assertEquals(new AccessInfo((short) 2, 0100), br7._accessLock);
//        assertEquals(01, br7.getLowerLimit());
//        assertEquals(01777, br7.getUpperLimit());
//        assertEquals(new AbsoluteAddress((short)1, 0, 070000), br7._baseAddress);
//    }
//
//    @Test
//    public void loadBaseRegisterUserDirect_BadPP_basic(
//    ) throws Exception {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            ".",
//            "$(2)",
//            ". RETURN CONTROL STACK",
//            "RCDEPTH   $EQU      32",
//            "RCSSIZE   $EQU      2*RCDEPTH",
//            "RCSTACK   $RES      RCSSIZE",
//            ".",
//            "$(1)      . extended mode i-bank",
//            "          $LIT",
//            "START",
//            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
//            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
//            ".",
//            "          . ESTABLISH RCS ON B25/EX0",
//            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
//            "          LXI,U     EX0,0",
//            "          LXM,U     EX0,RCSTACK+RCSSIZE",
//            ".",
//            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
//            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
//            ".",
//            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
//            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
//            "          LXI,U     X2,1",
//            "          LXM,U     X2,15",
//            "",
//            "          LD        (014,0),,B0 . ext mode, user regs, pp=3",
//            "          CALL      (LBDIREF$+BASIC, BASIC)",
//            ".",
//            "          $BASIC",
//            "$(3)      . basic mode i-bank",
//            "          $LIT",
//            "BASIC",
//            "          LBUD      B5,BDENTRY1   . void bank",
//            "          HALT      0",
//            ".",
//            "BDENTRY1  . void bank with void flag",
//            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
//            "          + 0               . lower/upper limits 0",
//            "          + 0               . base address 0",
//            "          + 0               . base address 0",
//            ".",
//            "          $END      START"
//        };
//
//        buildMultiBank(source, true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(01016, ip.getLatestStopDetail());
//        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
//                     ip.getLastInterrupt().getInterruptClass());
//        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
//                     ip.getLastInterrupt().getShortStatusField());
//    }
//
//    @Test
//    public void loadBaseRegisterUserDirect_BadPP_extended(
//    ) throws Exception {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(2)",
//            ". RETURN CONTROL STACK",
//            "RCDEPTH   $EQU      32",
//            "RCSSIZE   $EQU      2*RCDEPTH",
//            "RCSTACK   $RES      RCSSIZE",
//            ".",
//            "$(4)",
//            "BDENTRY1  . void bank with void flag",
//            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
//            "          + 0               . lower/upper limits 0",
//            "          + 0               . base address 0",
//            "          + 0               . base address 0",
//            "",
//            "$(1)      $LIT",
//            "START",
//            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
//            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
//            ".",
//            "          . ESTABLISH RCS ON B25/EX0",
//            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
//            "          LXI,U     EX0,0",
//            "          LXM,U     EX0,RCSTACK+RCSSIZE",
//            ".",
//            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
//            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
//            ".",
//            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
//            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
//            "          LXI,U     X2,1",
//            "          LXM,U     X2,15",
//            "",
//            "          LD        (014,0),,B0 . ext mode, user regs, pp=3",
//            "          LBU       B2,(LBDIREF$+BDENTRY1,0)",
//            "          LBUD      B5,BDENTRY1,,B2   . void bank",
//            "          HALT      0",
//            "",
//            "          $END      START"
//        };
//
//        buildMultiBank(source, true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(01016, ip.getLatestStopDetail());
//        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
//                     ip.getLastInterrupt().getInterruptClass());
//        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
//                     ip.getLastInterrupt().getShortStatusField());
//    }
//
//    @Test
//    public void loadBaseRegisterUserDirect_InvalidBR0_basic(
//    ) throws Exception {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            ".",
//            "$(2)",
//            ". RETURN CONTROL STACK",
//            "RCDEPTH   $EQU      32",
//            "RCSSIZE   $EQU      2*RCDEPTH",
//            "RCSTACK   $RES      RCSSIZE",
//            ".",
//            "$(1)      . extended mode i-bank",
//            "          $LIT",
//            "START",
//            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
//            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
//            ".",
//            "          . ESTABLISH RCS ON B25/EX0",
//            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
//            "          LXI,U     EX0,0",
//            "          LXM,U     EX0,RCSTACK+RCSSIZE",
//            ".",
//            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
//            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
//            ".",
//            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
//            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
//            "          LXI,U     X2,1",
//            "          LXM,U     X2,15",
//            "",
//            "          CALL      (LBDIREF$+BASIC, BASIC)",
//            ".",
//            "          $BASIC",
//            "$(3)      . basic mode i-bank",
//            "          $LIT",
//            "BASIC",
//            "          LBUD      B0,BDENTRY1   . void bank",
//            "          HALT      0",
//            ".",
//            "BDENTRY1  . void bank with void flag",
//            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
//            "          + 0               . lower/upper limits 0",
//            "          + 0               . base address 0",
//            "          + 0               . base address 0",
//            ".",
//            "          $END      START"
//        };
//
//        buildMultiBank(source, true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(01016, ip.getLatestStopDetail());
//        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
//                     ip.getLastInterrupt().getInterruptClass());
//        assertEquals(InvalidInstructionInterrupt.Reason.InvalidBaseRegister.getCode(),
//                     ip.getLastInterrupt().getShortStatusField());
//    }
//
//    @Test
//    public void loadBaseRegisterUserDirect_InvalidBR0_extended(
//    ) throws Exception {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            "",
//            "$(2)",
//            ". RETURN CONTROL STACK",
//            "RCDEPTH   $EQU      32",
//            "RCSSIZE   $EQU      2*RCDEPTH",
//            "RCSTACK   $RES      RCSSIZE",
//            ".",
//            "$(4)",
//            "BDENTRY1  . void bank with void flag",
//            "          + 0330200,0600010 . GAP/SAP rw set, void set, ring=3 domain=010",
//            "          + 0               . lower/upper limits 0",
//            "          + 0               . base address 0",
//            "          + 0               . base address 0",
//            "",
//            "$(1)      $LIT",
//            "START",
//            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
//            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
//            ".",
//            "          . ESTABLISH RCS ON B25/EX0",
//            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
//            "          LXI,U     EX0,0",
//            "          LXM,U     EX0,RCSTACK+RCSSIZE",
//            ".",
//            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
//            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
//            ".",
//            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
//            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
//            "          LXI,U     X2,1",
//            "          LXM,U     X2,15",
//            "",
//            "          LBU       B2,(LBDIREF$+BDENTRY1,0)",
//            "          LBUD      B0,BDENTRY1,,B2   . void bank",
//            "          HALT      0",
//            "",
//            "          $END      START"
//        };
//
//        buildMultiBank(source, true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(01016, ip.getLatestStopDetail());
//        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
//                     ip.getLastInterrupt().getInterruptClass());
//        assertEquals(InvalidInstructionInterrupt.Reason.InvalidBaseRegister.getCode(),
//                     ip.getLastInterrupt().getShortStatusField());
//    }
//
//    @Test
//    public void storeBaseRegisterExecDirect_basic(
//    ) throws Exception {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            ".",
//            "$(2)",
//            ". RETURN CONTROL STACK",
//            "RCDEPTH   $EQU      32",
//            "RCSSIZE   $EQU      2*RCDEPTH",
//            "RCSTACK   $RES      RCSSIZE",
//            ".",
//            "$(1)      . extended mode i-bank",
//            "          $LIT",
//            "START",
//            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
//            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
//            ".",
//            "          . ESTABLISH RCS ON B25/EX0",
//            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
//            "          LXI,U     EX0,0",
//            "          LXM,U     EX0,RCSTACK+RCSSIZE",
//            ".",
//            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
//            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
//            "",
//            ".",
//            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
//            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
//            "          LXI,U     X2,1",
//            "          LXM,U     X2,15",
//            "",
//            "          LBU       B15,(LBDIREF$+DATA,0)",
//            "          CALL      (LBDIREF$+BASIC, BASIC)",
//            ".",
//            "          $BASIC",
//            "$(3)      . basic mode i-bank",
//            "          $LIT",
//            "BASIC",
//            "          LXI,U     X8,4",
//            "          LXM,U     X8,0",
//            "          SBED      B16,DATA,*X8",
//            "          SBED      B25,DATA,*X8",
//            "          SBED      B26,DATA,*X8",
//            "          SBED      B30,DATA,*X8",
//            "          HALT      0",
//            ".",
//            "$(4)      . basic mode d-bank",
//            "DATA      $RES      64",
//            ".",
//            "          $END      START"
//        };
//
//        buildMultiBank(source, true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(0, ip.getLatestStopDetail());
//
//        Assert.assertEquals(0_000004_000020, ip.getExecOrUserXRegister(8).getW());
//        long[] data = getBankByBaseRegister(15);
//        BaseRegister br16 = new BaseRegister(Arrays.copyOfRange(data, 0, 4));
//        BaseRegister br25 = new BaseRegister(Arrays.copyOfRange(data, 4, 8));
//        BaseRegister br26 = new BaseRegister(Arrays.copyOfRange(data, 8, 12));
//        BaseRegister br30 = new BaseRegister(Arrays.copyOfRange(data, 12, 16));
//
//        assertFalse(br16._voidFlag);
//        assertEquals(0, br16._lowerLimitNormalized);
//        assertEquals(0407, br16._upperLimitNormalized);
//
//        assertFalse(br25._voidFlag);
//        assertEquals(02000, br25._lowerLimitNormalized);
//        assertEquals(02077, br25._upperLimitNormalized);
//
//        assertFalse(br26._voidFlag);
//        assertEquals(0, br26._lowerLimitNormalized);
//        assertEquals(077, br26._upperLimitNormalized);
//
//        assertTrue(br30._voidFlag);
//    }
//
//    @Test
//    public void storeBaseRegisterExecDirect_extended(
//    ) throws Exception {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            ".",
//            "$(2)",
//            ". RETURN CONTROL STACK",
//            "RCDEPTH   $EQU      32",
//            "RCSSIZE   $EQU      2*RCDEPTH",
//            "RCSTACK   $RES      RCSSIZE",
//            ".",
//            "$(1)      . extended mode i-bank",
//            "          $LIT",
//            "START",
//            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
//            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
//            ".",
//            "          . ESTABLISH RCS ON B25/EX0",
//            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
//            "          LXI,U     EX0,0",
//            "          LXM,U     EX0,RCSTACK+RCSSIZE",
//            ".",
//            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
//            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
//            ".",
//            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
//            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
//            "",
//            "          LBU       B2,(LBDIREF$+DATA,0)",
//            "          LXI,U     X8,4",
//            "          LXM,U     X8,0",
//            "          SBED      B16,DATA,*X8,B2",
//            "          SBED      B25,DATA,*X8,B2",
//            "          SBED      B26,DATA,*X8,B2",
//            "          SBED      B30,DATA,*X8,B2",
//            "          HALT      0",
//            ".",
//            "$(4)      . basic mode d-bank",
//            "DATA      $RES      64",
//            ".",
//            "          $END      START"
//        };
//
//        buildMultiBank(source, true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(0, ip.getLatestStopDetail());
//
//        Assert.assertEquals(0_000004_000020, ip.getExecOrUserXRegister(8).getW());
//        long[] data = getBankByBaseRegister(2);
//        BaseRegister br16 = new BaseRegister(Arrays.copyOfRange(data, 0, 4));
//        BaseRegister br25 = new BaseRegister(Arrays.copyOfRange(data, 4, 8));
//        BaseRegister br26 = new BaseRegister(Arrays.copyOfRange(data, 8, 12));
//        BaseRegister br30 = new BaseRegister(Arrays.copyOfRange(data, 12, 16));
//
//        assertFalse(br16._voidFlag);
//        assertEquals(0, br16._lowerLimitNormalized);
//        assertEquals(0407, br16._upperLimitNormalized);
//
//        assertFalse(br25._voidFlag);
//        assertEquals(02000, br25._lowerLimitNormalized);
//        assertEquals(02077, br25._upperLimitNormalized);
//
//        assertFalse(br26._voidFlag);
//        assertEquals(0, br26._lowerLimitNormalized);
//        assertEquals(077, br26._upperLimitNormalized);
//
//        assertTrue(br30._voidFlag);
//    }
//
//    @Test
//    public void storeBaseRegisterExecDirect_BadPP_basic(
//    ) throws Exception {
//        String[] source = {
//            "          $EXTEND",
//            "          $INFO 10 1",
//            ".",
//            "$(2)",
//            ". RETURN CONTROL STACK",
//            "RCDEPTH   $EQU      32",
//            "RCSSIZE   $EQU      2*RCDEPTH",
//            "RCSTACK   $RES      RCSSIZE",
//            ".",
//            "$(1)      . extended mode i-bank",
//            "          $LIT",
//            "START",
//            "          . GET DESIGNATOR REGISTER FOR EXEC REGISTER SET SELECTION",
//            "          LD        (000001,000000),,B0 . ext mode, exec regs, pp=0",
//            ".",
//            "          . ESTABLISH RCS ON B25/EX0",
//            "          LBE       B25,(LBDIREF$+RCSTACK,0)",
//            "          LXI,U     EX0,0",
//            "          LXM,U     EX0,RCSTACK+RCSSIZE",
//            ".",
//            "          . GET DESIGNATOR REGISTER FOR NO EXEC REGISTER SET SELECTION",
//            "          LD        (0,0),,B0 . ext mode, user regs, pp=0",
//            "",
//            ".",
//            "          . ESTABLISH INTERRUPT HANDLER VECTOR",
//            "          CALL      (LBDIREF$+IH$INIT,IH$INIT)",
//            "          LXI,U     X2,1",
//            "          LXM,U     X2,15",
//            "",
//            "          LBU       B15,(LBDIREF$+DATA,0)",
//            "          LD        (014,0)",
//            "          CALL      (LBDIREF$+BASIC, BASIC)",
//            ".",
//            "          $BASIC",
//            "$(3)      . basic mode i-bank",
//            "          $LIT",
//            "BASIC",
//            "          LXI,U     X8,4",
//            "          LXM,U     X8,0",
//            "          SBED      B16,DATA,*X8",
//            "          HALT      0",
//            ".",
//            "$(4)      . basic mode d-bank",
//            "DATA      $RES      64",
//            ".",
//            "          $END      START"
//        };
//
//        buildMultiBank(source, true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(01016, ip.getLatestStopDetail());
//        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
//                     ip.getLastInterrupt().getInterruptClass());
//        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
//                     ip.getLastInterrupt().getShortStatusField());
//    }
//
//    @Test
//    public void storeBaseRegisterExecDirect_BadPP_extended(
//    ) throws Exception {
//        String[] source = {
//            "$(4)",
//            "DATA      $RES      64",
//            ".",
//            "$(1)",
//            "          LBU       B3,(LBDIREF$+DATA, 0)",
//            "          LD        (014, 0)",
//            "          LXI,U     X8,4",
//            "          LXM,U     X8,0",
//            "          SBED      B16,DATA,*X8,B3",
//            "          HALT      0"
//        };
//
//        buildMultiBank(wrapForExtendedMode(source), true, false);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(01016, ip.getLatestStopDetail());
//        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
//                     ip.getLastInterrupt().getInterruptClass());
//        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
//                     ip.getLastInterrupt().getShortStatusField());
//    }
//
//    @Test
//    public void storeBaseRegisterUser_basic(
//    ) throws Exception {
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(4)",
//            "DATA      $RES 2",
//            "",
//            "$(3)",
//            "          LBU       B13,(LBDIREF$+DATA, 0)",
//            "          SBU       B12,DATA",
//            "          LXM,U     X2,1",
//            "          SBU       B13,DATA,X2",
//            "          HALT      0"
//        };
//
//        buildMultiBank(wrapForBasicMode(source), true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(0, ip.getLatestStopDetail());
//
//        long[] data = getBankByBaseRegister(13);
//        assertEquals(0_100007_000000L, data[0]);
//        assertEquals(0_100010_000000L, data[1]);
//    }
//
//    @Test
//    public void storeBaseRegisterUser_basic_badPP(
//    ) throws Exception {
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(4)",
//            "DATA      $RES 2",
//            "",
//            "$(3)      $LIT",
//            "          LD        (016,0) . basic mode, pp=3",
//            "          LBU       B13,(LBDIREF$+DATA, 0)",
//            "          SBU       B12,DATA",
//            "          LXM,U     X2,1",
//            "          SBU       B13,DATA,X2",
//            "          HALT      0"
//        };
//
//        buildMultiBank(wrapForBasicMode(source), true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(01016, ip.getLatestStopDetail());
//        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
//                     ip.getLastInterrupt().getInterruptClass());
//        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
//                     ip.getLastInterrupt().getShortStatusField());
//    }
//
//    @Test
//    public void storeBaseRegisterUser_extended(
//    ) throws Exception {
//        String[] source = {
//            "$(4)",
//            "DATA      $RES 2",
//            "",
//            "$(1)",
//            "          LBU       B3,(LBDIREF$+DATA, 0)",
//            "          SBU       B0,DATA,,B3",
//            "          LXM,U     X2,1",
//            "          SBU       B3,DATA,X2,B3",
//            "          HALT      0"
//        };
//
//        buildMultiBank(wrapForExtendedMode(source), true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(0, ip.getLatestStopDetail());
//
//        long[] data = getBankByBaseRegister(3);
//        assertEquals(0_100005_000000L, data[0]);
//        assertEquals(0_100007_000000L, data[1]);
//    }
//
//    @Test
//    public void storeBaseRegisterUserDirect_basic(
//    ) throws Exception {
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(4)",
//            "DATA      $RES 64",
//            "",
//            "$(3)      $LIT",
//            "          LBU       B13,(LBDIREF$+DATA, 0)",
//            "          LXI,U     X8,4",
//            "          LXM,U     X8,0",
//            "          SBUD      B12,DATA,*X8,B3",
//            "          SBUD      B13,DATA,*X8,B3",
//            "          SBUD      B14,DATA,*X8,B3",
//            "          SBUD      B15,DATA,*X8,B3",
//            "          HALT      0"
//        };
//
//        buildMultiBank(wrapForBasicMode(source), true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(0, ip.getLatestStopDetail());
//
//        Assert.assertEquals(0_000004_000020, ip.getExecOrUserXRegister(8).getW());
//        long[] data = getBankByBaseRegister(13);
//
//        //  B12
//        Assert.assertEquals(002, Word36.getS1(data[0]));
//        Assert.assertEquals(1, Word36.getQ1(data[1]));
//        Assert.assertEquals(01010, Word36.getH2(data[1]));
//
//        //  B13
//        Assert.assertEquals(003, Word36.getS1(data[4]));
//        Assert.assertEquals(03, Word36.getQ1(data[5]));
//        Assert.assertEquals(03077, Word36.getH2(data[5]));
//
//        //  B14 (void)
//        Assert.assertEquals(02, Word36.getS2(data[8]) | 02); //check for void flag
//
//        //  B15 (void)
//        Assert.assertEquals(02, Word36.getS2(data[12]) | 02); //check for void flag
//    }
//
//    @Test
//    public void storeBaseRegisterUserDirect_extended(
//    ) throws Exception {
//        String[] source = {
//            "$(0) . force at least one word into this bank",
//            "DUMMY     + 0",
//            "",
//            "$(4)",
//            "DATA      $RES 64",
//            ".",
//            "$(1)",
//            "          GOTO      (LBDIREF$+TARGET, TARGET)",
//            ".",
//            "          $INFO 10 5",
//            "$(5)      $LIT",
//            "TARGET*",
//            "          LBU       B3,(LBDIREF$+DATA, 0)",
//            "          LXI,U     X8,4",
//            "          LXM,U     X8,0",
//            "          SBUD      B0,DATA,*X8,B3",
//            "          SBUD      B2,DATA,*X8,B3",
//            "          SBUD      B3,DATA,*X8,B3",
//            "          SBUD      B4,DATA,*X8,B3",
//            "          HALT      0"
//        };
//
//        buildMultiBank(wrapForExtendedMode(source), true, false);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(0, ip.getLatestStopDetail());
//
//        Assert.assertEquals(0_000004_000020, ip.getExecOrUserXRegister(8).getW());
//        long[] data = getBankByBaseRegister(3);
//
//        //  B0
//        Assert.assertEquals(002, Word36.getS1(data[0]));
//        Assert.assertEquals(1, Word36.getQ1(data[1]));
//        Assert.assertEquals(01010, Word36.getH2(data[1]));
//
//        //  B2
//        Assert.assertEquals(003, Word36.getS1(data[4]));
//        Assert.assertEquals(0, Word36.getQ1(data[5]));
//
//        //  B3
//        Assert.assertEquals(003, Word36.getS1(data[8]));
//        Assert.assertEquals(0, Word36.getQ1(data[9]));
//        Assert.assertEquals(077, Word36.getH2(data[9]));
//
//        //  B4 (void)
//        Assert.assertEquals(02, Word36.getS2(data[12]) | 02); //check for void flag
//    }
//
//    @Test
//    public void storeBaseRegisterUserDirect_BadPP_basic(
//    ) throws Exception {
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(4)",
//            "DATA      $RES 64",
//            "",
//            "$(3)",
//            "          LD        (016,0) . basic mode, pp=3",
//            "          LBU       B13,(LBDIREF$+DATA, 0)",
//            "          LXI,U     X8,4",
//            "          LXM,U     X8,0",
//            "          SBUD      B0,DATA,*X8",
//            "          HALT      0"
//        };
//
//        buildMultiBank(wrapForBasicMode(source), true, false);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(01016, ip.getLatestStopDetail());
//        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
//                     ip.getLastInterrupt().getShortStatusField());
//    }
//
//    @Test
//    public void storeBaseRegisterUserDirect_extended_badPP(
//    ) throws Exception {
//        String[] source = {
//            "$(0)",
//            "DATA      $RES 64",
//            "",
//            "$(1)",
//            "          LD        (014,0)",
//            "          LXI,U     X8,4",
//            "          LXM,U     X8,0",
//            "          SBUD      B8,DATA,*X8,B3",
//            "          HALT      077"
//        };
//
//        buildMultiBank(wrapForExtendedMode(source), true, false);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(01016, ip.getLatestStopDetail());
//        assertEquals(MachineInterrupt.InterruptClass.InvalidInstruction,
//                     ip.getLastInterrupt().getInterruptClass());
//        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege.getCode(),
//                     ip.getLastInterrupt().getShortStatusField());
//    }
//
//    @Test
//    public void testRelativeAddress_basic(
//    ) throws Exception {
//        String[] source = {
//            "$(4)     . base this on B13",
//            "         . needs to be an even-numbered lc counter to get a unique address",
//            "DATA13    + 0",
//            "",
//            "$(3)      $LIT . this will automatically be based on B12",
//            "          LBU       B13,(LBDIREF$+DATA13, 0)",
//            "          LBU       B14,(LBDIREF$+DATA14, 0)",
//            "          LBU       B15,(LBDIREF$+DATA15, 0)",
//            "",
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
//            "$(6)     . base this on B15",
//            "         . needs to be an even-numbered lc counter to get a unique address",
//            "DATA15    + 0",
//            "",
//            "$(8)     . base this on B14",
//            "         . needs to be an even-numbered lc counter to get a unique address",
//            "DATA14    + 0"
//        };
//
//        //  code banks need to be writeable for the skip to work
//        buildMultiBank(wrapForBasicMode(source), true, true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(0, ip.getLatestStopDetail());
//
//        Assert.assertEquals(0, ip.getExecOrUserXRegister(5).getW());
//        Assert.assertEquals(0400000_000000L, ip.getExecOrUserXRegister(12).getW());
//        Assert.assertEquals(0500000_000000L, ip.getExecOrUserXRegister(13).getW());
//        Assert.assertEquals(0600000_000000L, ip.getExecOrUserXRegister(14).getW());
//        Assert.assertEquals(0700000_000000L, ip.getExecOrUserXRegister(15).getW());
//    }
//
//    @Test
//    public void testRelativeAddressIndirect_basic(
//    ) throws Exception {
//        String[] source = {
//            "$(3) . this will be based on B12",
//            "          LBU       B13,(LBDIREF$+DATA4, 0)",
//            "          LBU       B14,(LBDIREF$+DATA8, 0)",
//            "          LBU       B15,(LBDIREF$+DATA6, 0)",
//            "",
//            "          TRA       X12,*DATA3",
//            "          HALT      077 . should skip",
//            "          HALT      0 . this one should NOT be skipped",
//            "",
//            "DATA3     NOP       *DATA4",
//            "",
//            "$(4)      . base on B13",
//            "DATA4     NOP       *DATA6",
//            "",
//            "$(6)      . base on B15",
//            "DATA6     NOP       DATA8",
//            "",
//            "$(8)      . base on B14",
//            "DATA8     $res 32"
//        };
//
//        buildMultiBank(wrapForBasicMode(source), true, true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(0, ip.getLatestStopDetail());
//
//        Assert.assertEquals(0, ip.getExecOrUserXRegister(5).getW());
//        Assert.assertEquals(0600000_000000L, ip.getExecOrUserXRegister(12).getW());
//    }
//
//    @Test
//    public void testRelativeAddress_extended(
//    ) throws Exception {
//        String[] source = {
//            "$(0)",
//            "DATA      + 0",
//            "",
//            "$(1) . this will be bank 0600004 based on B0",
//            "          $LIT",
//            "          . base data bank on B12 through B15",
//            "          LBU       B12,(LBDIREF$+DATA, 0)",
//            "          LBU       B13,(LBDIREF$+DATA, 0)",
//            "          LBU       B14,(LBDIREF$+DATA, 0)",
//            "          LBU       B15,(LBDIREF$+DATA, 0)",
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
//            "          TRA       X5,03777,,B12",       // doesn't exist
//            "          HALT      0 . this one should NOT be skipped",
//            "          HALT      073"
//        };
//
//        buildMultiBank(wrapForExtendedMode(source), true, true, true);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(0, ip.getLatestStopDetail());
//
//        Assert.assertEquals(0, ip.getExecOrUserXRegister(5).getW());
//        Assert.assertEquals(0400000_000000L, ip.getExecOrUserXRegister(12).getW());
//        Assert.assertEquals(0500000_000000L, ip.getExecOrUserXRegister(13).getW());
//        Assert.assertEquals(0600000_000000L, ip.getExecOrUserXRegister(14).getW());
//        Assert.assertEquals(0700000_000000L, ip.getExecOrUserXRegister(15).getW());
//    }
//
//    @Test
//    public void testRelativeAddressNonRWBanks_basic(
//    ) throws Exception {
//        String[] source = {
//            "          TRA       X12,$",
//            "          HALT      0                   . should stop here",
//            "          HALT      077                 . should not get here"
//        };
//
//        buildMultiBank(wrapForBasicMode(source), true, false);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(0, ip.getLatestStopDetail());
//        Assert.assertEquals(0400000_000000L, ip.getExecOrUserXRegister(12).getW());
//    }
//
//    @Test
//    public void testRelativeAddressNonRWBanks_extended(
//    ) throws Exception {
//        String[] source = {
//            "          LBU       B15,(LBDIREF$+DATA15, 0)",
//            "STEP1",
//            "          TRA       X12,$,,B0",
//            "          J         STEP2",
//            "          HALT      077                 . should not get here",
//            "",
//            "STEP2",
//            "          TRA       X15,DATA15,,B15",
//            "          HALT      0                   . should stop here",
//            "          HALT      076                 . should not get here",
//            "",
//            "$(5)",
//            "DATA15    + 0"
//        };
//
//        buildMultiBank(wrapForExtendedMode(source), true, false);
//        createConfiguration();
//        ipl(true);
//
//        Assert.assertEquals(InstructionProcessor.StopReason.Debug, ip.getLatestStopReason());
//        Assert.assertEquals(0, ip.getLatestStopDetail());
//
//        Assert.assertEquals(0000000_000000L, ip.getExecOrUserXRegister(12).getW());
//        Assert.assertEquals(0700000_000000L, ip.getExecOrUserXRegister(15).getW());
//    }

    //  TODO testRelativeAddressRange ... some day when we care more about it

    //  TODO lots of testVirtualAddress tests
}
