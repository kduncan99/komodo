/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.Constants;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.Constants.*;
import static com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt.ErrorType.GRSViolation;
import static org.junit.jupiter.api.Assertions.*;

public class TestLRSFunction extends TestFunction {

    private long lrsBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(072, 017, a, x, h, i, u);
    }

    private long lrsEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(072, 017, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testLRS_EM_NOP() throws MachineInterrupt {
        var code = new long[] {
            lrsEM(7, 0, 0, 0, 2, 0),
            0,
        };

        var data = new long[] {
            0_100L,
            0_200L,
            0_300L,
            0_400L,
            0_500L,
            0_600L,
            0_700L,
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

        // init all of GRS to magic number
        var magic = 0_112344_765230L;
        for (int rx = 0; rx < 128; rx++) {
            _engine.getGeneralRegisterSet().getRegister(rx).setW(magic);
        }

        _engine.getExecOrUserARegister(7).setW(0); // both counts and indices are 0

        run();

        for (int rx = 0; rx < 128; rx++) {
            if (rx == GRS_A7) {
                assertEquals(0, _engine.getGeneralRegisterSet().getRegister(rx).getW());
            } else {
                assertEquals(magic, _engine.getGeneralRegisterSet().getRegister(rx).getW());
            }
        }
    }

    @Test
    public void testLRS_EM_RRegs() throws MachineInterrupt {
        var code = new long[] {
            lrsEM(15, 0, 0, 0, 2, 0),
            0,
            };

        var data = new long[] { 010, 011, 012, 013, 014, 015, 016, 017,
                                020, 021, 022, 023, 024, 025, 026, 027 };

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

        // init all of GRS to magic number
        var magic = 0_112344_765230L;
        for (int rx = 0; rx < 128; rx++) {
            _engine.getGeneralRegisterSet().getRegister(rx).setW(magic);
        }

        _engine.getExecOrUserARegister(15).setQ1(0);        // area-2 count
        _engine.getExecOrUserARegister(15).setQ2(0);        // area-2 grs index
        _engine.getExecOrUserARegister(15).setQ3(020);      // area-1 count
        _engine.getExecOrUserARegister(15).setQ4(GRS_R0);   // area-1 grs index

        run();

        for (int rx = 0; rx < 128; rx++) {
            if (rx == GRS_A15) {
                assertEquals(0_000000_020000L | GRS_R0, _engine.getGeneralRegisterSet().getRegister(rx)
                                                               .getW());
            } else if (rx >= GRS_R0 && rx <= GRS_R15) {
                assertEquals(rx - GRS_R0 + 010, _engine.getGeneralRegisterSet().getRegister(rx).getW());
            } else {
                assertEquals(magic, _engine.getGeneralRegisterSet().getRegister(rx).getW());
            }
        }
    }

    @Test
    public void testLRS_EM_GRSWriteViolation_PP() throws MachineInterrupt {
        var code = new long[] {
            lrsEM(15, 0, 0, 0, 2, 0),
            0,
            };

        var data = new long[] { 010, 011, 012, 013, 014, 015, 016, 017,
                                020, 021, 022, 023, 024, 025, 026, 027 };

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
               .setProcessorPrivilege((short)1)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(15).setQ1(020);      // area-2 count
        _engine.getExecOrUserARegister(15).setQ2(GRS_ER0);  // exec R register not accessible to PP > 0
        _engine.getExecOrUserARegister(15).setQ3(0);        // area-1 count
        _engine.getExecOrUserARegister(15).setQ4(0);        // area-1 grs index

        ReferenceViolationInterrupt mi = assertThrows(ReferenceViolationInterrupt.class, () -> run());
        assertEquals(GRSViolation, mi._errorType);
        assertFalse(mi._fetchFlag);

        for (int rx = GRS_ER0; rx <= GRS_ER15; rx++) {
            assertEquals(0, _engine.getGeneralRegisterSet().getRegister(rx).getW());
        }
    }

    @Test
    public void testLRS_EM_NoGRSWriteViolation_PP() throws MachineInterrupt {
        var code = new long[] {
            lrsEM(15, 0, 0, 0, 2, 0),
            0,
            };

        var data = new long[] { 010, 011, 012, 013, 014, 015, 016, 017,
                                020, 021, 022, 023, 024, 025, 026, 027 };

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
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(true);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(15).setQ1(020);      // area-2 count
        _engine.getExecOrUserARegister(15).setQ2(GRS_ER0);  // exec R register not accessible to PP > 0
        _engine.getExecOrUserARegister(15).setQ3(0);        // area-1 count
        _engine.getExecOrUserARegister(15).setQ4(0);        // area-1 grs index

        run();

        for (int rx = GRS_ER0; rx <= GRS_ER15; rx++) {
            assertEquals(rx - GRS_ER0 + 010, _engine.getGeneralRegisterSet().getRegister(rx).getW());
        }
    }

    @Test
    public void testLRS_EM_GRSWriteViolation_HW() throws MachineInterrupt {
        var code = new long[] {
            lrsEM(15, 0, 0, 0, 2, 0),
            0,
            };

        var data = new long[] { 010, 011, 012, 013, 014, 015, 016, 017,
                                020, 021, 022, 023, 024, 025, 026, 027 };

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
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        // init all of GRS to magic number
        var magic = 0_112344_765230L;
        for (int rx = 0; rx < 128; rx++) {
            _engine.getGeneralRegisterSet().getRegister(rx).setW(magic);
        }

        _engine.getExecOrUserARegister(15).setQ1(0);    // area-2 count
        _engine.getExecOrUserARegister(15).setQ2(0);    // area-2 grs index
        _engine.getExecOrUserARegister(15).setQ3(020);  // area-1 count
        _engine.getExecOrUserARegister(15).setQ4(036);  // start 2 words ahead of hardware-reserved registers

        ReferenceViolationInterrupt mi = assertThrows(ReferenceViolationInterrupt.class, () -> run());
        assertEquals(GRSViolation, mi._errorType);
        assertFalse(mi._fetchFlag);

        assertEquals(010, _engine.getGeneralRegisterSet().getRegister(036).getW());
        assertEquals(011, _engine.getGeneralRegisterSet().getRegister(037).getW());
        assertEquals(magic, _engine.getGeneralRegisterSet().getRegister(040).getW()); // unchanged
    }

    @Test
    public void testLRS_BM_XandR() throws MachineInterrupt {
        var code = new long[] {
            lrsBM(1, 0, 0, 0, 040000),
            0,
            };

        var data = new long[] { 0100, 0101, 0102, 0103, 0104 }; // for R5, R6, R7, X10, and X11

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_22)
                                      .setUpperLimit(0_22777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_40)
                                      .setUpperLimit(0_40777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(14).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(15).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(1).setQ1(2);        // area-2 count
        _engine.getExecOrUserARegister(1).setQ2(GRS_X10);  // area-2 grs index
        _engine.getExecOrUserARegister(1).setQ3(3);        // area-1 count
        _engine.getExecOrUserARegister(1).setQ4(GRS_R5);   // start 2 words ahead of hardware-reserved registers

        run();

        assertEquals(0100, _engine.getGeneralRegisterSet().getRegister(GRS_R5).getW());
        assertEquals(0101, _engine.getGeneralRegisterSet().getRegister(GRS_R6).getW());
        assertEquals(0102, _engine.getGeneralRegisterSet().getRegister(GRS_R7).getW());
        assertEquals(0103, _engine.getGeneralRegisterSet().getRegister(GRS_X10).getW());
        assertEquals(0104, _engine.getGeneralRegisterSet().getRegister(GRS_X11).getW());
    }

    @Test
    public void testLRS_BM_GRSWrap() throws MachineInterrupt {
        var code = new long[] {
            lrsBM(1, 0, 0, 0, 040000),
            0,
            };

        var data = new long[] { 0100, 0101, 0102, 0103, 0104 };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_22)
                                      .setUpperLimit(0_22777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_40)
                                      .setUpperLimit(0_40777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(14).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(15).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(1).setQ1(5);     // area-2 count
        _engine.getExecOrUserARegister(1).setQ2(126);   // area-2 grs index
        _engine.getExecOrUserARegister(1).setQ3(0);     // area-1 count
        _engine.getExecOrUserARegister(1).setQ4(0);     // start 2 words ahead of hardware-reserved registers

        run();

        assertEquals(0100, _engine.getGeneralRegisterSet().getRegister(126).getW());
        assertEquals(0101, _engine.getGeneralRegisterSet().getRegister(127).getW());
        assertEquals(0102, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X0).getW());
        assertEquals(0103, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X1).getW());
        assertEquals(0104, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X2).getW());
    }
}
