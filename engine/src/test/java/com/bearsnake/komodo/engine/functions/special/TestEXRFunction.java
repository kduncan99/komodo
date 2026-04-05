/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.special;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.AbsoluteAddress;
import com.bearsnake.komodo.engine.BankDescriptor;
import com.bearsnake.komodo.engine.BankType;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.Constants.JFIELD_H2;
import static com.bearsnake.komodo.engine.Constants.JFIELD_U;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestEXRFunction extends TestFunction {

    private long exEM(long x, long h, long i, long b, long d) {
        return fjaxhibd(073, 014, 06, x, h, i, b, d);
    }

    private long laImm(long a, long x, long u) {
        return fjaxu(010, JFIELD_U, a, x, u);
    }

    private long sasEM(long j, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_05, j, 0_06, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testEXR_Zero_EM() throws MachineInterrupt {
        var code = new long[] {
            exEM(0, 0, 0, 0, 01004),
            0,
            0,
            0,
            laImm(4, 0, 01022) // not allowed, but we're never going to attempt it.
        };

        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserRRegister(1).setW(0_0);

        run();

        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter());
        assertEquals(0, _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testEXR_EM() throws MachineInterrupt {
        var code = new long[] {
            exEM(0, 0, 0, 0, 01004),
            0,
            0,
            0,
            sasEM(JFIELD_H2, 3, 1, 0, 2, 0),
        };

        var data = new long[]{
            0_777777_777777L,
            0_777777_777777L,
            0_777777_777777L,
            0_777777_777777L,
            0_777777_777777L,
            0_777777_777777L, // this one should not be changed
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_0)
                                      .setUpperLimit(0_0777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserRRegister(1).setW(0_05);
        _engine.getExecOrUserXRegister(3).setXI(0_01).setXM(0);

        run();

        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter());
        assertEquals(0, _engine.getExecOrUserRRegister(1).getW());
        assertEquals(0_000001_000005, _engine.getExecOrUserXRegister(3).getW());
        assertEquals(0_777777_040040L, data[0]);
        assertEquals(0_777777_040040L, data[1]);
        assertEquals(0_777777_040040L, data[2]);
        assertEquals(0_777777_040040L, data[3]);
        assertEquals(0_777777_040040L, data[4]);
        assertEquals(0_777777_777777L, data[5]);
    }

    @Test
    public void testEXR_Invalid_EM() throws MachineInterrupt {
        var code = new long[] {
            exEM(0, 0, 0, 0, 01004),
            0,
            0,
            0,
            laImm(4, 0, 01022) // not allowed
        };

        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserRRegister(1).setW(0_01);

        assertThrows(InvalidInstructionInterrupt.class, () -> run());
    }
}
