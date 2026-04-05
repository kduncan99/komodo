/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTLEMFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short) 0);
    }

    private long tlemBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(047, j, a, x, h, i, u);
    }

    private long tlemEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(047, j, a, x, h, i, b, d);
    }

    @Test
    public void testTLEM_BasicMode() throws MachineInterrupt {
        // TLEM, J=W, A=1 (X1), X=0, H=0, I=0, U=01000
        // X1: XI=0, XM=02000
        // U=01000 <= XM=02000 -> Skip
        var code = new long[1024];
        code[0] = tlemBM(Constants.JFIELD_W, 1, 0, 0, 0, 01000);
        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode).setLowerLimit(0).setUpperLimit(0777777).setBaseAddress(new AbsoluteAddress(0, 0));
        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X1).setXI(0).setXM(02000);

        _engine.getDesignatorRegister().setBasicModeEnabled(true).setProcessorPrivilege((short) 3);
        _engine.getDesignatorRegister().setBasicModeBaseRegisterSelection(false);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0x00000C);

        run();

        // Skip NI: PC should be 2 (instruction at 0, next instruction at 1 skipped)
        assertEquals(2, _engine.getProgramAddressRegister().getProgramCounter());
        // Verify X1 XM updated: 02000 + 0 = 02000
        assertEquals(02000, _engine.getGeneralRegisterSet().getRegister(Constants.GRS_X1).getXM());
    }

    @Test
    public void testTLEM_ExtendedMode_NoSkip() throws MachineInterrupt {
        // TLEM, J=W, A=1, X=0, H=0, I=0, B=2, D=0
        // Bank 2 Offset 0: 03000
        // X1: XI=0, XM=02000
        // U=03000 > XM=02000 -> No skip
        var code = new long[1024];
        code[0] = tlemEM(Constants.JFIELD_W, 1, 0, 0, 0, 2, 0);
        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        var data = new long[]{03000};
        var bank2 = new ArraySlice(data);
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(2, 0));
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);

        _engine.getGeneralRegisterSet().getRegister(1).setXI(0).setXM(02000);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short) 3);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0);

        run();

        // No skip: PC should be 1
        assertEquals(1, _engine.getProgramAddressRegister().getProgramCounter());
        // Verify X1 XM updated: 02000 + 0 = 02000
        assertEquals(02000, _engine.getGeneralRegisterSet().getRegister(1).getXM());
    }

    @Test
    public void testTLEM_Indexing() throws MachineInterrupt {
        // TLEM, J=W, A=1, X=2, H=0, I=0, B=2, D=0
        // X2: XM=5
        // Bank 2 Offset 5: 01500
        // X1: XI=10, XM=02000
        // U=01500 <= XM=02000 -> Skip
        var code = new long[1024];
        code[0] = tlemEM(Constants.JFIELD_W, 1, 2, 0, 0, 2, 0);
        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        var data = new long[10];
        data[5] = 01500;
        var bank2 = new ArraySlice(data);
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(2, 0));
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);

        _engine.getGeneralRegisterSet().getRegister(1).setXI(10).setXM(02000);
        _engine.getGeneralRegisterSet().getRegister(2).setXM(5);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short) 3);

        run();

        // Skip NI: PC should be 2
        assertEquals(2, _engine.getProgramAddressRegister().getProgramCounter());
        // Verify X1 XM updated: 02000 + 10 (decimal 8) = 02012 (octal)
        assertEquals(02012, _engine.getGeneralRegisterSet().getRegister(1).getXM());
    }

    @Test
    public void testTLEM_Indirect() throws MachineInterrupt {
        // TLEM, J=W, A=1, X=0, H=0, I=1 (Indirect), B=2, D=0
        // Bank 2 Offset 0 (Indirect Word): G=0 (Extended), B=3, D=5
        // Bank 3 Offset 5: 03000
        // X1: XI=0, XM=04000
        // U=03000 <= XM=04000 -> Skip
        var code = new long[1024];
        code[0] = tlemEM(Constants.JFIELD_W, 1, 0, 0, 1, 2, 0);
        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        // Indirect word at Bank 2, Offset 0: G=0 (extended), B=3, D=5
        // Bit 33 must be 1 for Extended Mode indirect?
        // Wait, how does Engine.java identify indirect words?
        // I'll use 0_400000_000000L | (3L << 18) | 5L
        var indirectData = new long[1024];
        indirectData[0] = 0_400000_000000L | (3L << 18) | 5L;
        var bank2 = new ArraySlice(indirectData);
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(2, 0));
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);

        var targetData = new long[10];
        targetData[5] = 03000;
        var bank3 = new ArraySlice(targetData);
        var bd3 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(3, 0));
        _engine.getBaseRegister(3).setBankDescriptor(bd3).setStorage(bank3).setSubsetting(0);

        _engine.getGeneralRegisterSet().getRegister(1).setXI(0).setXM(04000);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short) 3);

        run();

        // Skip NI: PC should be 2
        assertEquals(2, _engine.getProgramAddressRegister().getProgramCounter());
        // Verify X1 XM updated: 04000 + 0 = 04000
        assertEquals(04000, _engine.getGeneralRegisterSet().getRegister(1).getXM());
    }

    @Test
    public void testTLEM_Immediate() throws MachineInterrupt {
        // TLEM, J=XU (Immediate), A=1, U=03000
        // X1: XI=5, XM=03000
        // U=03000 <= XM=03000 -> Skip
        var code = new long[1024];
        code[0] = fjaxhiu(047, Constants.JFIELD_XU, 1, 0, 0, 0, 03000);
        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        _engine.getGeneralRegisterSet().getRegister(1).setXI(5).setXM(03000);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short) 3);

        run();

        // Skip NI: PC should be 2
        assertEquals(2, _engine.getProgramAddressRegister().getProgramCounter());
        // Verify X1 XM updated: 03000 + 5 = 03005
        assertEquals(03005, _engine.getGeneralRegisterSet().getRegister(1).getXM());
    }

    @Test
    public void testTLEM_Canonical_Case1() throws MachineInterrupt {
        var code = new long[1024];
        code[0] = tlemEM(Constants.JFIELD_W, 5, 0, 0, 0, 2, 0);
        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        var armData = new long[]{0_000135_471234L};
        var bank2 = new ArraySlice(armData);
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(2, 0));
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);

        _engine.getGeneralRegisterSet().getRegister(5).setW(0_000002_061234L);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short) 3);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0);

        run();

        // No skip: PC should be 1
        assertEquals(1, _engine.getProgramAddressRegister().getProgramCounter());
        // Verify X5 after: 000002 061236
        assertEquals(0_000002_061236L, _engine.getGeneralRegisterSet().getRegister(5).getW());
    }

    @Test
    public void testTNGM_Canonical_Case2() throws MachineInterrupt {
        var code = new long[1024];
        code[0] = tlemEM(Constants.JFIELD_S5, 5, 0, 0, 0, 2, 0);
        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        var armData = new long[]{0_000135_471234L};
        var bank2 = new ArraySlice(armData);
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(2, 0));
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);

        _engine.getGeneralRegisterSet().getRegister(5).setW(0_000002_061236L);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short) 3);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0);

        run();

        // Skip NI: PC should be 2
        assertEquals(2, _engine.getProgramAddressRegister().getProgramCounter());
        // Verify X5 after: 000002 061240
        assertEquals(0_000002_061240L, _engine.getGeneralRegisterSet().getRegister(5).getW());
    }
}
