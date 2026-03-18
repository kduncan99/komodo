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
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.Constants.GRS_R1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestDCBFunction extends TestFunction {

    private long dcbEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(033, 015, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testDCB_EM() throws MachineInterrupt {
        var code = new long[] {
            dcbEM(0, 2, 1, 0, 2, 0),
            dcbEM(1, 2, 1, 0, 2, 0),
            dcbEM(2, 2, 1, 0, 2, 0),
            dcbEM(3, 2, 1, 0, 2, 0),
            dcbEM(4, 0, 0, 0, 0, GRS_R1),
            0,
        };
        var data = new long[] {
            0_333333_444444L,// slop
            0_333333_444444L,// more slop,
            0_000000_000000L,
            0_000001_000001L,
            0_141414_151515L,
            0_777777_777777L,
            0_777777_777777L,
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777)
                                      .setBaseAddress(new AbsoluteAddress(0, 1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(0)).setW(0_777777_777777L);
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(1)).setW(0_000000_000001L);
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(2)).setW(0_101010_101010L);
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(3)).setW(0_000001_000001L);
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserXRegisterIndex(2)).setW(0_000001_000002L);
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserRRegisterIndex(1)).setW(0_000333_000333L);
        _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserRRegisterIndex(2)).setW(0_020202_700700L);

        run();

        assertEquals(2, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(0)).getW());
        assertEquals(17, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(1)).getW());
        assertEquals(51, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(2)).getW());
        assertEquals(72, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(3)).getW());
        assertEquals(21, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserARegisterIndex(4)).getW());
        assertEquals(0_000001_000006, _engine.getGeneralRegisterSet().getRegister(_engine.getExecOrUserXRegisterIndex(2)).getW());
    }
}
