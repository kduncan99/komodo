/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.fixed;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.OperationTrapInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestDANFunction extends FunctionUnitTest {

    private long danBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(071, j, a, x, h, i, u);
    }

    private long danEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(071, j, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testDAN_Zeros_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = danEM(011, 5, 0, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = 0;
        data[6] = 0;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(5).setW(0);
        _engine.getExecOrUserARegister(6).setW(0);

        run();

        // 0 + (-0) = 0
        assertEquals(0_777777_777777L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0_777777_777777L, _engine.getExecOrUserARegister(6).getW());
        assertFalse(_engine.getDesignatorRegister().isCarry());
        assertFalse(_engine.getDesignatorRegister().isOverflow());
    }

    @Test
    public void testDAN_SimpleSubtraction_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = danEM(011, 5, 0, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = 0_000000_000001L; // MSW
        data[6] = 0_000000_000002L; // LSW

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(5).setW(0_000000_100000L);
        _engine.getExecOrUserARegister(6).setW(0_000000_200000L);

        run();

        // 100000:200000 - 1:2 = 077777:177776
        assertEquals(0_000000_077777L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0_000000_177776L, _engine.getExecOrUserARegister(6).getW());
        assertTrue(_engine.getDesignatorRegister().isCarry()); // 100000:200000 + (777777_777776:777777_777775)
    }

    @Test
    public void testDAN_InternalCarry_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = danEM(011, 5, 0, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = 0_777777_777777L;
        data[6] = 0_377777_777777L; // Negated: 0:400000_000000

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(5).setW(0);
        _engine.getExecOrUserARegister(6).setW(0_400000_000000L);

        run();

        // 0:400000_000000 + (-0:400000_000000) = 0:400000_000000 + 0:400000_000000 = 1:0
        assertEquals(1, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0, _engine.getExecOrUserARegister(6).getW());
        assertFalse(_engine.getDesignatorRegister().isCarry());
    }

    @Test
    public void testDAN_EndAroundCarry_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = danEM(011, 5, 0, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = 0_777777_777777L;
        data[6] = 0_777777_777776L; // Negated: 0:1

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(5).setW(0_777777_777777L);
        _engine.getExecOrUserARegister(6).setW(0_777777_777777L);

        run();

        // -0 + -(77...7:77...6) = -0 + 0:1 = 0:1
        assertEquals(0, _engine.getExecOrUserARegister(5).getW());
        assertEquals(1, _engine.getExecOrUserARegister(6).getW());
        assertTrue(_engine.getDesignatorRegister().isCarry());
    }

    @Test
    public void testDAN_Overflow_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = danBM(011, 5, 0, 0, 0, 0_2000);
        code[1] = 0;
        data[0] = 0_677777_777777L; // Negated: 0_100000_000000
        data[1] = 0_777777_777777L; // Negated: 0

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 13, false, 0_2000, 0_2777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(5).setW(0_300000_000000L);
        _engine.getExecOrUserARegister(6).setW(0);

        run();

        // 300000:0 + 100000:0 = 400000:0 (Overflow)
        assertEquals(0_400000_000000L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0, _engine.getExecOrUserARegister(6).getW());
        assertTrue(_engine.getDesignatorRegister().isOverflow());
    }

    @Test
    public void testDAN_NegativeZero_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = danBM(011, 5, 0, 0, 0, 0_2000);
        code[1] = 0;
        data[0] = 0; // Negated: -0
        data[1] = 0; // Negated: -0

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 13, false, 0_2000, 0_2777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(5).setW(0_777777_777777L);
        _engine.getExecOrUserARegister(6).setW(0_777777_777777L);

        run();

        // -0 + -0 = -0
        assertEquals(0_777777_777777L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0_777777_777777L, _engine.getExecOrUserARegister(6).getW());
    }

    @Test
    public void testDAN_Indirect_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = danBM(011, 10, 0, 0, 1, 0_1010);
        code[0_10] = fjaxhiu(0, 0, 0, 0, 0, 0, 0_22002);
        code[1] = 0;

        data[2] = 0_777777_777654L; // Negated: 0...123
        data[3] = 0_777777_777321L; // Negated: 0...456

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 13, false, 0_22000, 0_22777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short) 0_7);
        _engine.getExecOrUserARegister(10).setW(0);
        _engine.getExecOrUserARegister(11).setW(0);

        run();

        assertEquals(0_000000_000123L, _engine.getExecOrUserARegister(10).getW());
        assertEquals(0_000000_000456L, _engine.getExecOrUserARegister(11).getW());
    }

    @Test
    public void testDAN_OperationTrap_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = danBM(011, 5, 0, 0, 0, 0_2000);
        code[1] = 0;
        data[0] = 0_577777_777777L; // Negated: 200000_000000
        data[1] = 0_777777_777777L; // Negated: 0

        loadBaseRegister((short) 12, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 13, false, 0_2000, 0_2777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false)
               .setOperationTrapEnabled(true);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short) 0_7);
        _engine.getExecOrUserARegister(5).setW(0_200000_000000L);
        _engine.getExecOrUserARegister(6).setW(0);

        var i = assertThrows(OperationTrapInterrupt.class, this::run);
        assertEquals(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow.getCode(), i.getShortStatusField());

        assertEquals(0_400000_000000L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0, _engine.getExecOrUserARegister(6).getW());
        assertTrue(_engine.getDesignatorRegister().isOverflow());
    }
}
