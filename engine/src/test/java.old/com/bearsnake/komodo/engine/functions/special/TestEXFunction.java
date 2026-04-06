/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.special;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.AbsoluteAddress;
import com.bearsnake.komodo.engine.BankDescriptor;
import com.bearsnake.komodo.engine.BankType;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.Constants.JFIELD_U;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestEXFunction extends FunctionUnitTest {

    private long exEM(long x, long h, long i, long b, long d) {
        return fjaxhibd(073, 014, 05, x, h, i, b, d);
    }

    private long exBM(long x, long h, long i, long u) {
        return fjaxhiu(072, 010, 0, x, h, i, u);
    }

    private long laImm(long a, long x, long u) {
        return fjaxu(010, JFIELD_U, a, x, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testEX_EM() throws MachineInterrupt {
        var code = new long[] {
            exEM(0, 0, 0, 0, 01004),
            0,
            0,
            0,
            laImm(4, 0, 01022)
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

        run();

        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter());
        assertEquals(0_1022, _engine.getExecOrUserARegister(4).getW());
    }

    @Test
    public void testEX_BM() throws MachineInterrupt {
        var code = new long[] {
            exBM(0, 0, 0, 022002),
            0,
            exBM(0, 0, 0, 022003),
            exBM(0, 0, 0, 022004),
            exBM(0, 0, 0, 022005),
            laImm(8, 0, 01),
            };

        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_22) // 022000
                                      .setUpperLimit(0_22777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_22001, _engine.getProgramAddressRegister().getProgramCounter());
        assertEquals(01, _engine.getExecOrUserARegister(8).getW());
    }
}
