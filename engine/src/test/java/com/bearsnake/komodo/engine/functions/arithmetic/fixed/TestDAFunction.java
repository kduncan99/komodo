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

public class TestDAFunction extends FunctionUnitTest {

    private long daBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(071, j, a, x, h, i, u);
    }

    private long daEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(071, j, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testDA_Zeros_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = daEM(010, 5, 0, 0, 0, 2, 0_1005);
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

        assertEquals(0, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0, _engine.getExecOrUserARegister(6).getW());
        assertFalse(_engine.getDesignatorRegister().isCarry());
        assertFalse(_engine.getDesignatorRegister().isOverflow());
    }

    @Test
    public void testDA_SimpleAddition_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = daEM(010, 5, 0, 0, 0, 2, 0_1005);
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

        assertEquals(0_000000_100001L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0_000000_200002L, _engine.getExecOrUserARegister(6).getW());
    }

    @Test
    public void testDA_InternalCarry_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = daEM(010, 5, 0, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = 0;
        data[6] = 0_400000_000000L;

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

        // 0_400000_000000 + 0_400000_000000 = 0_1000000_000000 (37 bits)
        // Carry to MSW
        assertEquals(1, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0, _engine.getExecOrUserARegister(6).getW());
        assertFalse(_engine.getDesignatorRegister().isCarry()); // Carry is from MSW (bit 71)
    }

    @Test
    public void testDA_EndAroundCarry_EM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = daEM(010, 5, 0, 0, 0, 2, 0_1005);
        code[1] = 0;
        data[5] = 0_777777_777777L;
        data[6] = 0_777777_777777L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 2, false, 0_1000, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserARegister(5).setW(0);
        _engine.getExecOrUserARegister(6).setW(0_000000_000001L);

        run();

        // 0:1 + 77...7:77...7 = 77...7:0 + carry from bit 35 of LSW to bit 0 of MSW
        // Wait, let's trace:
        // LSW: 1 + 777777777777 = 1000000000000 -> LSW = 0, Carry to MSW = 1
        // MSW: 0 + 777777777777 + 1 (carry) = 1000000000000 -> MSW = 0, End-around carry = 1
        // LSW = 0 + 1 (end-around) = 1
        // Result: MSW=0, LSW=1
        assertEquals(0, _engine.getExecOrUserARegister(5).getW());
        assertEquals(1, _engine.getExecOrUserARegister(6).getW());
        assertTrue(_engine.getDesignatorRegister().isCarry());
    }

    @Test
    public void testDA_Overflow_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = daBM(010, 5, 0, 0, 0, 0_2000);
        code[1] = 0;
        data[0] = 0_100000_000000L; // MSW
        data[1] = 0; // LSW

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

        // 0_300000_000000:0 + 0_100000_000000:0 = 0_400000_000000:0
        // Bit 35 of MSW (bit 71 of 72-bit) changed from 0 to 1 -> Overflow
        assertEquals(0_400000_000000L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0, _engine.getExecOrUserARegister(6).getW());
        assertTrue(_engine.getDesignatorRegister().isOverflow());
    }

    @Test
    public void testDA_NegativeZero_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = daBM(010, 5, 0, 0, 0, 0_2000);
        code[1] = 0;
        data[0] = 0_777777_777777L;
        data[1] = 0_777777_777777L;

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

        // -0 + -0 = -0 (in 72-bit, both words are -0)
        assertEquals(0_777777_777777L, _engine.getExecOrUserARegister(5).getW());
        assertEquals(0_777777_777777L, _engine.getExecOrUserARegister(6).getW());
    }

    @Test
    public void testDA_Indirect_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        // code[0] is DA, indirect, pointing to code[10]
        code[0] = daBM(010, 10, 0, 0, 1, 0_1010);
        // code[10] is the indirect word, pointing to data[2] (actually it will fetch data[2] and data[3])
        code[0_10] = fjaxhiu(0, 0, 0, 0, 0, 0, 0_22002);
        code[1] = 0;

        data[2] = 0_000000_000123L;
        data[3] = 0_000000_000456L;

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
    public void testDA_OperationTrap_BM() throws MachineInterrupt {
        var code = new long[0_1000];
        var data = new long[0_1000];

        code[0] = daBM(010, 5, 0, 0, 0, 0_2000);
        code[1] = 0;
        data[0] = 0_200000_000000L;
        data[1] = 0;

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
