/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.Constants.GRS_A5;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestTOPFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    private long topImm(long j, long a, long x, long u) {
        return fjaxu(0_45, j, a, x, u);
    }

    private long topBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(0_45, j, a, x, h, i, u);
    }

    private long topEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_45, j, a, x, h, i, b, d);
    }

    @Test
    public void testTOP_Immediate_EM() throws MachineInterrupt {
        var code = new long[] {
            topImm(Constants.JFIELD_U, 2, 0, 0112233),
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_1)   // 01000 base address
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        // bitCount(0_707070_707070L & 0112233)
        // 0112233 is 001 001 010 010 011 011 in binary
        // bits at octal: 14, 12, 9, 6, 4, 3, 1, 0
        // 0_707070_707070 has all bits set in those positions?
        // Let's use simpler values.
        // A=1, U=1 -> 1 bit (ODD) -> SKIP
        _engine.getExecOrUserARegister(2).setW(1L);

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTOP_Immediate_NoSkip_EM() throws MachineInterrupt {
        var code = new long[] {
            topImm(Constants.JFIELD_U, 2, 0, 0112233),
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_1)   // 01000 base address
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        // A=3, U=0112233 -> 011 & 0112233 = 011 -> 2 bits (EVEN) -> NO SKIP
        _engine.getExecOrUserARegister(2).setW(3L);

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTOP_W_EM() throws MachineInterrupt {
        var code = new long[] {
            topEM(Constants.JFIELD_W, 2, 0, 0, 0, 2, 0),
            0,
            0,
            };

        var data = new long[]{ 0_111111_111111L }; // bit count is 18 (EVEN) -> NO SKIP

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)   // 01000 base address
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0777)
                                      .setBaseAddress(new AbsoluteAddress(2, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_777777_777777L); // AND with data (18 bits) -> 18 bits (EVEN)

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTOP_GRS_EM() throws MachineInterrupt {
        var code = new long[] {
            topEM(Constants.JFIELD_W, 2, 0, 0, 0, 0, GRS_A5),
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_1)   // 01000 base address
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_000001L);
        _engine.getExecOrUserARegister(5).setW(0_000000_000001L); // 1 AND 1 = 1 -> ODD -> SKIP

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTOP_GRS_ReferenceViolation_EM() throws MachineInterrupt {
        var code = new long[] {
            topEM(Constants.JFIELD_W, 2, 0, 0, 0, 0, 040),
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_1)   // 01000 base address
                                     .setUpperLimit(0_1777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000003_000003L);
        _engine.getExecOrUserARegister(5).setW(0_000003_000003L);

        ReferenceViolationInterrupt i = assertThrows(ReferenceViolationInterrupt.class, () -> run());
        assertEquals(ReferenceViolationInterrupt.ErrorType.GRSViolation, i._errorType);

        assertEquals(0_01000, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTOP_Q3_EM() throws MachineInterrupt {
        var code = new long[] {
            topEM(Constants.JFIELD_W, 2, 0, 0, 0, 2, 0),
            0,
            0,
            };

        var data = new long[]{ 0_000000_000001L }; // 1 bit (ODD)

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)   // 01000 base address
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0777)
                                      .setBaseAddress(new AbsoluteAddress(2, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_000001L); // 1 AND 1 = 1 -> ODD -> SKIP

        run();

        assertEquals(0_01002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTOP_Q3_NoSkip_EM() throws MachineInterrupt {
        var code = new long[] {
            topEM(Constants.JFIELD_W, 2, 0, 0, 0, 2, 0),
            0,
            0,
            };

        var data = new long[]{ 0_000000_000003L }; // 2 bits (EVEN)

        var bank0 = new ArraySlice(code);
        var bank2 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)   // 01000 base address
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0777)
                                      .setBaseAddress(new AbsoluteAddress(2, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_000003L); // 3 AND 3 = 3 -> EVEN -> NO SKIP

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTOPImmediate_BM() throws MachineInterrupt {
        var code = new long[] {
            topImm(Constants.JFIELD_U, 2, 0, 01),
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_22)   // 022000 base address
                                     .setUpperLimit(0_22777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(13).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_000001L); // 1 AND 1 = 1 -> ODD -> SKIP

        run();

        assertEquals(0_022002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTOPImmediate_Indexed_BM() throws MachineInterrupt {
        var code = new long[] {
            topImm(Constants.JFIELD_U, 2, 2, 01),
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_22)   // 022000 base address
                                     .setUpperLimit(0_22777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(13).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_000001L);
        _engine.getExecOrUserXRegister(2).setXM(0_000000_000000); // 1 AND 1 = 1 -> ODD -> SKIP

        run();

        assertEquals(0_022002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTOPIndirect_BM() throws MachineInterrupt {
        var code = new long[] {
            topBM(Constants.JFIELD_W, 2, 0, 0, 1, 022004),
            0,
            0,
            fjaxhiu(0, 0, 0, 0, 0, 0, 022005),
            fjaxhiu(0, 0, 0, 0, 0, 1, 022003),
            01, // 1 bit (ODD)
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_22)   // 022000 base address
                                     .setUpperLimit(0_22777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(13).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_000000_000001L); // 1 AND 1 = 1 -> ODD -> SKIP

        run();

        assertEquals(0_022002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTOP_ReferenceViolation_BM() throws MachineInterrupt {
        var code = new long[] {
            topImm(Constants.JFIELD_W, 2, 0, 0112233),
            0,
            0,
            };

        var bank = new ArraySlice(code);
        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                     .setLowerLimit(0_22)   // 022000 base address
                                     .setUpperLimit(0_22777)
                                     .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(13).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(2).setW(0_707070_707070L);

        assertThrows(ReferenceViolationInterrupt.class, () -> run());

        assertEquals(0_022000, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
