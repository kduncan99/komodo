/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.decimal;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.AbsoluteAddress;
import com.bearsnake.komodo.engine.BankDescriptor;
import com.bearsnake.komodo.engine.BankType;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestBDEFunction extends TestDecimalFunction {

    // Sign values for BDE (from BDEFunction.java)
    private static final int BDE_POSITIVE_SIGN = 012;
    private static final int BDE_NEGATIVE_SIGN = 015;

    private static final int START_CHAR_IN_A = 0;
    private static final int START_CHAR_IN_X = 1;

    private static final int START_CHAR_Q1 = 0;
    private static final int START_CHAR_Q2 = 1;
    private static final int START_CHAR_Q3 = 2;
    private static final int START_CHAR_Q4 = 3;

    private static final int CHAR_FORMAT_ASCII = 0;
    private static final int CHAR_FORMAT_EXTERNAL_COMPUTATIONAL_3 = 1;

    private static final int ASCII_NO_SIGN = 0;
    private static final int ASCII_NO_SIGN_0 = 0;
    private static final int ASCII_NO_SIGN_1 = 1;
    private static final int ASCII_NO_SIGN_2 = 2;
    private static final int ASCII_NO_SIGN_3 = 3;
    private static final int ASCII_TRAILING_INCLUDED_SIGN = 4;
    private static final int ASCII_TRAILING_SEPARATE_SIGN = 5;
    private static final int ASCII_LEADING_INCLUDED_SIGN = 6;
    private static final int ASCII_LEADING_SEPARATE_SIGN = 7;

    private static final int COMP_NO_SIGN = 0;
    private static final int COMP_NO_SIGN_0 = 0;
    private static final int COMP_NO_SIGN_1 = 1;
    private static final int COMP_NO_SIGN_2 = 2;
    private static final int COMP_NO_SIGN_3 = 3;
    private static final int COMP_TRAILING_SEPARATE_SIGN = 4;
    private static final int COMP_LEADING_SEPARATE_SIGN_4 = 4;
    private static final int COMP_LEADING_SEPARATE_SIGN_5 = 5;
    private static final int COMP_LEADING_SEPARATE_SIGN_6 = 6;
    private static final int COMP_LEADING_SEPARATE_SIGN_7 = 7;

    private long bdeEM(long a, long x, long b, long d) {
        return fjaxhibd(072, 010, a, x, 0, 0, b, d);
    }

    private long qw(long q1, long q2, long q3, long q4) {
        return ((q1 & 07777) << 27)
            | ((q2 & 07777) << 18)
            | ((q3 & 07777) << 9)
            | (q4 & 07777);
    }

    private long aParameter(
        long startCharLoc,
        long aReg,
        long startChar,
        long charFmt,
        long signLoc,
        long numARegs,
        long skipCount,
        long digitCount
    ) {
        return ((startCharLoc & 01) << 35)
            | ((aReg & 017) << 22)
            | ((startChar & 03) << 18)
            | ((charFmt & 01) << 16)
            | ((signLoc & 07) << 13)
            | ((numARegs & 03) << 11)
            | ((skipCount & 037) << 6)
            | (digitCount & 037);
    }

    private long xParameter18(
        long startChar,
        long modifier
    ) {
        return ((startChar & 03) << 30) | (modifier & 0_777777);
    }

    private long xParameter24(
        long startChar,
        long modifier
    ) {
        return ((startChar & 03) << 30) | (modifier & 0_77_777777);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testBDE_DigitCount0_EM() throws MachineInterrupt {
        var code = new long[]{
            bdeEM(02, 0, 02, 0),
            0,
        };

        var data = new long[]{
            0,
            0,
            0,
            0,
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        var aParam = aParameter(START_CHAR_IN_A, 6, START_CHAR_Q1, CHAR_FORMAT_ASCII, ASCII_NO_SIGN, 1, 0, 0);
        _engine.getExecOrUserARegister(2).setW(aParam);
        _engine.getExecOrUserARegister(6).setW(0_777777_777777L);

        run();

        assertEquals(decWord(0, 0, 0, 0, 0, 0, 0, 0, BDE_POSITIVE_SIGN), _engine.getExecOrUserARegister(6).getW());
    }

    @Test
    public void testBDE_Simple_EM() throws MachineInterrupt {
        var code = new long[]{
            bdeEM(02, 0, 02, 0),
            0,
            };

        var data = new long[]{
            qw('1', '2', '3', '4'),
            0,
            0,
            0,
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        var aParam = aParameter(START_CHAR_IN_A, 6, START_CHAR_Q1, CHAR_FORMAT_ASCII, ASCII_NO_SIGN, 1, 4, 4);
        _engine.getExecOrUserARegister(2).setW(aParam);
        _engine.getExecOrUserARegister(6).setW(0_777777_777777L);

        run();

        assertEquals(decWord(0, 0, 0, 0, 1, 2, 3, 4, BDE_POSITIVE_SIGN), _engine.getExecOrUserARegister(6).getW());
    }


    @Test
    public void testBDE_Simple_SCHInX_EM() throws MachineInterrupt {
        var code = new long[]{
            bdeEM(02, 0, 02, 0),
            0,
            };

        var data = new long[]{
            qw('1', '2', '3', '4'),
            0,
            0,
            0,
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        var aParam = aParameter(START_CHAR_IN_A, 6, START_CHAR_Q1, CHAR_FORMAT_ASCII, ASCII_NO_SIGN, 1, 4, 4);
        _engine.getExecOrUserARegister(2).setW(aParam);
        _engine.getExecOrUserARegister(6).setW(0_777777_777777L);

        run();

        assertEquals(decWord(0, 0, 0, 0, 1, 2, 3, 4, BDE_POSITIVE_SIGN), _engine.getExecOrUserARegister(6).getW());
    }

    @Test
    public void testBDE_Offset_LeadingSeparateNeg_OnePad_EM() throws MachineInterrupt {
        var code = new long[]{
            bdeEM(02, 3, 02, 0),
            0,
            };

        var data = new long[]{
            qw('1', '-', '3', '4'),
            qw('5', '6', '7', '8'),
            qw('9', '0', '1', '2'),
            0,
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        var aParam = aParameter(START_CHAR_IN_X, 6, 0, CHAR_FORMAT_ASCII, ASCII_LEADING_SEPARATE_SIGN, 2, 6, 11);
        _engine.getExecOrUserARegister(2).setW(aParam);
        var xParam = xParameter18(START_CHAR_Q2, 0);
        _engine.getExecOrUserXRegister(3).setW(xParam);
        _engine.getExecOrUserARegister(6).setW(0_777777_777777L);

        run();

        assertEquals(decWord(0, 0, 0, 0, 0, 0, 3, 4, 5), _engine.getExecOrUserARegister(6).getW());
        assertEquals(decWord(6, 7, 8, 9, 0, 1, 2, 0, BDE_NEGATIVE_SIGN), _engine.getExecOrUserARegister(7).getW());
    }

    @Test
    public void testBDE_Offset_TrailingSeparateNeg_EM() throws MachineInterrupt {
        var code = new long[]{
            bdeEM(02, 0, 02, 0),
            0,
            };

        var data = new long[]{
            qw('1', '2', '3', '4'),
            qw('5', '6', '7', '8'),
            qw('9', '0', '1', '-'),
            0,
            };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        var aParam = aParameter(START_CHAR_IN_A, 6, START_CHAR_Q1, CHAR_FORMAT_ASCII, ASCII_TRAILING_SEPARATE_SIGN, 2, 6, 12);
        _engine.getExecOrUserARegister(2).setW(aParam);
        _engine.getExecOrUserARegister(6).setW(0_777777_777777L);

        run();

        System.out.printf("... %9x\n", _engine.getExecOrUserARegister(6).getW());
        System.out.printf("... %9x\n", _engine.getExecOrUserARegister(7).getW());
        assertEquals(decWord(0, 0, 0, 0, 0, 0, 1, 2, 3), _engine.getExecOrUserARegister(6).getW());
        assertEquals(decWord(4, 5, 6, 7, 8, 9, 0, 1, BDE_NEGATIVE_SIGN), _engine.getExecOrUserARegister(7).getW());
    }
}
