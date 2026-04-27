/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTNOPFunction extends FunctionUnitTest {

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
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
        var code = new long[] {
            tnopEM(Constants.JFIELD_W, 1, 1, 0, 2, 0),
            0
        };

        loadBaseRegister((short) 0, false, 0_1000, 01777, AbsoluteAddress.encodeToLong(0, 0), code);

        // Setup Bank 2 for operand (though ignored)
        var data = new long[256];
        data[0100] = 0_123456_654321L;
        loadBaseRegister((short) 2, false, 0, 0777, AbsoluteAddress.encodeToLong(2, 0), data);

        // Setup X1: XI=1, XM=100
        _engine.getGeneralRegisterSet().getRegister(1).setXI(1).setXM(0100);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short) 3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0).setBankLevel((short) 0);

        run();

        // Verify X1 was incremented: XM should be 0101
        assertEquals(0101, _engine.getGeneralRegisterSet().getRegister(1).getXM());
        // Verify no skip: PC should be 1
        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTNOP_PartialWord() throws MachineInterrupt {
        // TNOP, J=H1, B=2, D=0
        var code = new long[] {
            tnopEM(Constants.JFIELD_H1, 0, 0, 0, 2, 0),
            0
        };
        var data = new long[] { 0_123456_654321L };


        loadBaseRegister((short) 0, false, 0_1000, 01777, AbsoluteAddress.encodeToLong(0, 0), code);
        loadBaseRegister((short) 2, false, 0, 0777, AbsoluteAddress.encodeToLong(2, 0), data);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short) 3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0).setBankLevel((short) 0);

        run();

        // Verify no skip
        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testTNOP_Immediate() throws MachineInterrupt {
        // TNOP, J=XIU (immediate), U=0_123456
        var code = new long[] {
            tnopEMu(Constants.JFIELD_XU, 0, 0, 0, 0_123456L),
            0
        };

        loadBaseRegister((short) 0, false, 0_1000, 0_01777, AbsoluteAddress.encodeToLong(0, 0), code);

        _engine.getDesignatorRegister().setBasicModeEnabled(false).setProcessorPrivilege((short) 3);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0).setBankLevel((short) 0);

        run();

        // Verify no skip
        assertEquals(0_1001, _engine.getProgramAddressRegister().getProgramCounter());
    }
}
