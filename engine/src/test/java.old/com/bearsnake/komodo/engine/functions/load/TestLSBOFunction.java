/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.AbsoluteAddress;
import com.bearsnake.komodo.engine.BankDescriptor;
import com.bearsnake.komodo.engine.BankType;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bearsnake.komodo.engine.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLSBOFunction extends FunctionUnitTest {

    private long lsboEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(060, j, a, x, h, i, b, d);
    }

    private long lsboEMImm(long j, long a, long x, long u) {
        return fjaxu(060, j, a, x, u);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
    }

    @Test
    public void testLSBO_EM() throws MachineInterrupt {
        var code = new long[] {
            lsboEM(0, 0, 0, 0, 0, 0, GRS_R5),
            lsboEM(JFIELD_H2, 1, 0, 0, 0, 0, GRS_R5),   // should be full-word
            lsboEM(JFIELD_S3, 2, 0, 0, 0, 2, 0),
            lsboEMImm(JFIELD_U, 3, 0, 0_2222),
            lsboEM(0, 4, 8, 0, 0, 2, 0),
            0,
        };

        var data = new long[] {
            0_112233_445566L,
            0,
            0,
            0,
            0_44,
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(new AbsoluteAddress(1, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);
        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        // set all X registers to -1.
        for (int gx = GRS_X0 ; gx <= GRS_X15; gx++) {
            _engine.getGeneralRegisterSet().getRegister(gx).setW(0_777777_777777L);
        }
        _engine.getGeneralRegisterSet().getRegister(GRS_R5).setW(0_655443_322110L);
        _engine.getGeneralRegisterSet().getRegister(GRS_X8).setW(0_000000_000004L);

        run();

        assertEquals(0_107777_777777L, _engine.getGeneralRegisterSet().getRegister(GRS_X0).getW());
        assertEquals(0_107777_777777L, _engine.getGeneralRegisterSet().getRegister(GRS_X1).getW());
        assertEquals(0_337777_777777L, _engine.getGeneralRegisterSet().getRegister(GRS_X2).getW());
        assertEquals(0_227777_777777L, _engine.getGeneralRegisterSet().getRegister(GRS_X3).getW());
        assertEquals(0_447777_777777L, _engine.getGeneralRegisterSet().getRegister(GRS_X4).getW());
    }
}
