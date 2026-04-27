/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.decimal;

import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Double Subtract Decimal instruction
 * (DSDE) Subtracts decimal (U)|(U+1) from decimal A(a)|A(a+1) and stores the result in A(a)|A(a+1).
 */
public class TestDSDEFunction extends TestDecimalFunction {

    private long dsdeBM(long a, long x, long h, long i, long u) {
        return fjaxhiu(0_07, 0_03, a, x, h, i, u);
    }

    private long dsdeEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_07, 0_03, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

//    private void setupExtendedMode(long[] code) {
//        var bd = new BankDescriptor().setBankType(BankType.ExtendedMode)
//                                     .setLowerLimit(0)
//                                     .setUpperLimit(code.length - 1)
//                                     .setBaseAddress(AbsoluteAddress.construct(0, 0));
//        bd.setInactive(false);
//        _engine.getBaseRegister(0).setBankDescriptor(bd).setStorage(bank).setSubsetting(0);
//        _engine.getDesignatorRegister().setBasicModeEnabled(false);
//        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0);
//    }

    @Test
    public void testDSDE_Positive_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = dsdeEM(4, 0, 0, 0, 2, 0_400);
        code[1] = 0;

        var data = new long[0_1000];
        data[0_400] = decWord(0, 1, 0, 0, 0, 0, 0, 0, 0);
        data[0_401] = decWord(0, 0, 0, 0, 0, 0, 0, 1, POSITIVE_SIGN);

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0, 0_777, AbsoluteAddress.encodeToLong(1, 0), data);

        _engine.getExecOrUserARegister(4).setW(decWord(0, 2, 0, 0, 0, 0, 0, 0, 0));
        _engine.getExecOrUserARegister(5).setW(decWord(0, 0, 0, 0, 0, 0, 0, 2, POSITIVE_SIGN));

        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        assertEquals(decWord(0, 1, 0, 0, 0, 0, 0, 0, 0), _engine.getExecOrUserARegister(4).getW());
        assertEquals(decWord(0, 0, 0, 0, 0, 0, 0, 1, POSITIVE_SIGN), _engine.getExecOrUserARegister(5).getW());
        assertFalse(_engine.getDesignatorRegister().isOverflow());
    }

    @Test
    public void testDSDE_Negative_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = dsdeEM(4, 0, 0, 0, 2, 0_400);
        code[1] = 0;

        var data = new long[0_1000];
        data[0_400] = decWord(0, 0, 0, 0, 0, 0, 0, 0, 0);
        data[0_401] = decWord(0, 5, 0, 0, 0, 0, 0, 0, NEGATIVE_SIGN);

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0, 0_777, AbsoluteAddress.encodeToLong(1, 0), data);

        _engine.getExecOrUserARegister(4).setW(decWord(0, 0, 0, 0, 0, 0, 0, 0, 0));
        _engine.getExecOrUserARegister(5).setW(decWord(1, 0, 0, 0, 0, 0, 0, 0, POSITIVE_SIGN));

        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        assertEquals(decWord(0, 0, 0, 0, 0, 0, 0, 0, 0), _engine.getExecOrUserARegister(4).getW());
        assertEquals(decWord(1, 5, 0, 0, 0, 0, 0, 0, POSITIVE_SIGN), _engine.getExecOrUserARegister(5).getW());
        assertFalse(_engine.getDesignatorRegister().isOverflow());
    }

    @Test
    public void testDSDE_Overflow_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = dsdeEM(4, 0, 0, 0, 2, 0_400);
        code[1] = 0;

        var data = new long[0_1000];
        data[0_400] = decWord(0, 0, 0, 0, 0, 0, 0, 0, 0);
        data[0_401] = decWord(0, 0, 0, 0, 0, 0, 0, 1, POSITIVE_SIGN);

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0, 0_777, AbsoluteAddress.encodeToLong(1, 0), data);

        _engine.getExecOrUserARegister(4).setW(decWord(9, 9, 9, 9, 9, 9, 9, 9, 9));
        _engine.getExecOrUserARegister(5).setW(decWord(9, 9, 9, 9, 9, 9, 9, 9, NEGATIVE_SIGN));

        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        assertTrue(_engine.getDesignatorRegister().isOverflow());
    }

    @Test
    public void testDSDE_GRS_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        code[0] = dsdeEM(4, 0, 0, 0, 0, 0_100);
        code[1] = 0;

        _engine.getExecOrUserARegister(4).setW(decWord(0, 2, 0, 0, 0, 0, 0, 0, 0));
        _engine.getExecOrUserARegister(5).setW(decWord(0, 0, 0, 0, 0, 0, 0, 2, POSITIVE_SIGN));
        _engine.getExecOrUserRRegister(0).setW(decWord(0, 1, 0, 0, 0, 0, 0, 0, 0));
        _engine.getExecOrUserRRegister(1).setW(decWord(0, 0, 0, 0, 0, 0, 0, 1, POSITIVE_SIGN));

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, AbsoluteAddress.encodeToLong(0, 0), code);

        _engine.getDesignatorRegister().setBasicModeEnabled(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0);

        run();

        assertEquals(decWord(0, 1, 0, 0, 0, 0, 0, 0, 0), _engine.getExecOrUserARegister(4).getW());
        assertEquals(decWord(0, 0, 0, 0, 0, 0, 0, 1, POSITIVE_SIGN), _engine.getExecOrUserARegister(5).getW());
        assertFalse(_engine.getDesignatorRegister().isOverflow());
    }
}
