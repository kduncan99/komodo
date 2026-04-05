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

public class TestTNOPFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short) 0);
    }

    private long tnopEM(long j, long x, long h, long i, long b, long d) {
        return fjaxhibd(050, j, 00, x, h, i, b, d);
    }

    private long tnopEMu(long j, long x, long h, long i, long u) {
        return fjaxhiu(050, j, 00, x, h, i, u);
    }

    @Test
    public void testTNOP_IndexIncrement() throws MachineInterrupt {
        // TNOP, J=W, X=1, H=1 (increment), B=2, D=0
        var code = new long[]{tnopEM(Constants.JFIELD_W, 1, 1, 0, 2, 0), 0};
        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        // Setup X1: XI=1, XM=100
        _engine.getGeneralRegisterSet().getRegister(1).setXI(1).setXM(0100);

        // Setup Bank 2 for operand (though ignored)
        var data = new long[256];
        data[0100] = 0_123456_654321L;
        var bank2 = new ArraySlice(data);
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(2, 0));
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short) 3);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short) 0);

        run();

        // Verify X1 was incremented: XM should be 0101
        assertEquals(0101, _engine.getGeneralRegisterSet().getRegister(1).getXM());
        // Verify no skip: PC should be 1
        assertEquals(1, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTNOP_Indirect() throws MachineInterrupt {
        // TNOP, J=W, X=0, H=0, I=1 (indirect), B=2, D=0
        // Indirect at Bank 2 Offset 0 points to Bank 3 Offset 5
        var code = new long[]{tnopEM(Constants.JFIELD_W, 0, 0, 1, 2, 0), 0};
        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        // Indirect word at Bank 2, Offset 0: G=0 (extended), B=3, D=5
        var indirectData = new long[]{ (3L << 12) | 5L };
        var bank2 = new ArraySlice(indirectData);
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(2, 0));
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);

        // Target data at Bank 3, Offset 5
        var targetData = new long[10];
        targetData[5] = 0_777777_777777L;
        var bank3 = new ArraySlice(targetData);
        var bd3 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(3, 0));
        _engine.getBaseRegister(3).setBankDescriptor(bd3).setStorage(bank3).setSubsetting(0);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short) 3);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short) 0);

        run();

        // Verify no skip
        assertEquals(1, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTNOP_PartialWord() throws MachineInterrupt {
        // TNOP, J=H1, B=2, D=0
        var code = new long[]{tnopEM(Constants.JFIELD_H1, 0, 0, 0, 2, 0), 0};
        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        var data = new long[]{0_123456_654321L};
        var bank2 = new ArraySlice(data);
        var bd2 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(2, 0));
        _engine.getBaseRegister(2).setBankDescriptor(bd2).setStorage(bank2).setSubsetting(0);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short) 3);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short) 0);

        run();

        // Verify no skip
        assertEquals(1, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTNOP_Immediate() throws MachineInterrupt {
        // TNOP, J=XIU (immediate), U=0_123456
        var code = new long[]{tnopEMu(Constants.JFIELD_XU, 0, 0, 0, 0_123456L), 0};
        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0).setUpperLimit(0777).setBaseAddress(new AbsoluteAddress(0, 0));
        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short) 3);
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short) 0);

        run();

        // Verify no skip
        assertEquals(1, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
