/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLXFunction extends FunctionUnitTest {

    private long lxImm(long j, long a, long x, long u) {
        return fjaxu(027, j, a, x, u);
    }

    private long lxBM(long j, long a, long x, long h, long i, long u) {
        return fjaxhiu(027, j, a, x, h, i, u);
    }

    private long lxEM(long j, long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(027, j, a, x, h, i, b, d);
    }

    @BeforeEach
    public void setup() {
        _engine =  new Engine(this, this);
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short) 0);
    }

    @Test
    public void testLXImmediate_BM() throws MachineInterrupt {
        var code = new long[] {
            lxImm(Constants.JFIELD_U, 0, 0, 0123),
            0
        };

        loadBaseRegister((short) 13, false, 0_22000, 0_22777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short) 07);

        run();

        assertEquals(0_123, _engine.getExecOrUserXRegister(0).getW());
    }

    @Test
    public void testLXImmediate_Large_BM() throws MachineInterrupt {
        var code = new long[0200000];
        code[0] = lxImm(Constants.JFIELD_U, 0, 0, 0200000);

        loadBaseRegister((short) 13, false, 0_22000, 0_277777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_200000, _engine.getExecOrUserXRegister(0).getW());
    }

    @Test
    public void testLXImmediate_EM() throws MachineInterrupt {
        var code = new long[] {
            lxImm(Constants.JFIELD_U, 0, 0, 0123),
            0
        };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_123, _engine.getExecOrUserXRegister(0).getW());
    }

    @Test
    public void testLX_W_EM() throws MachineInterrupt {
        var code = new long[] {
            lxEM(Constants.JFIELD_W, 2, 0, 0, 0, 1, 02),
            0,
            };

        var data = new long[] {
            0_1L,
            0_2L,
            0_3L,
            0_4L,
            0_5L
        };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 1, false, 0_0, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(3L, _engine.getExecOrUserXRegister(2).getW());
    }

    @Test
    public void testLX_fromGRS_EM() throws MachineInterrupt {
        var code = new long[] {
            // use Q1 to verify reg->reg is always full-word
            lxEM(Constants.JFIELD_Q1, 2, 0, 0, 0, 0, Constants.GRS_A3),
            0,
            };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserARegister(3).setW(0_400013_022100L);

        run();

        assertEquals(0_400013_022100L, _engine.getExecOrUserXRegister(2).getW());
    }

    @Test
    public void testLX_LongIndexed_EM() throws MachineInterrupt {
        var code = new long[] {
            lxEM(Constants.JFIELD_W, 7, 4, 1, 0, 1, 0),
            0
        };

        var data = new long[0_1000_1000];
        data[0_1000_0000] = 0_445544_667766L;

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 1, false, 0_0, 0_10000_0777, 0, data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setBaseAddress(AbsoluteAddress.construct(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_10000_0777)
                                      .setBaseAddress(AbsoluteAddress.construct(1, 0));

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)1)
               .setExecutive24BitIndexingEnabled(true)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getExecOrUserXRegister(4).setXI12(04).setXM24(0_1000_0000);

        run();

        assertEquals(0_445544_667766L, _engine.getExecOrUserXRegister(7).getW());
        assertEquals(04, _engine.getExecOrUserXRegister(4).getXI12());
        assertEquals(0_1000_0004, _engine.getExecOrUserXRegister(4).getXM24());
    }

    @Test
    public void testLX_Indexed_EM() throws MachineInterrupt {
        var code = new long[] {
            lxEM(Constants.JFIELD_W, 5, 3, 1, 0, 1, 01),
            0
        };

        var data = new long[] {
            0_11L,
            0_12L,
            0_13L,
            0_14L,
            0_15L
        };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 1, false, 0_0, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserXRegister(3).setXI(0_01).setXM(0_03);

        run();

        assertEquals(0_15L, _engine.getExecOrUserXRegister(5).getW());
        assertEquals(0_01L, _engine.getExecOrUserXRegister(3).getXI());
        assertEquals(0_04L, _engine.getExecOrUserXRegister(3).getXM());
    }

    @Test
    public void testLX_Tx_BM() throws MachineInterrupt {
        var code = new long[]{
            lxBM(Constants.JFIELD_T1, 0, 0, 0, 0, 040000),
            lxBM(Constants.JFIELD_T2, 1, 0, 0, 0, 040001),
            lxBM(Constants.JFIELD_T3, 2, 0, 0, 0, 040002),
            0
        };

        var data = new long[]{
            0_221111_111111L,
            0_113311_007766L,
            0_111144_675301L
        };

        loadBaseRegister((short) 14, false, 0_22000, 0_22777, 0, code);
        loadBaseRegister((short) 15, false, 0_40000, 0_40777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short) 3)
               .setQuarterWordModeEnabled(false)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short) 0_7);

        run();

        assertEquals(0_2211L, _engine.getExecOrUserXRegister(0).getW());
        assertEquals(0_1100L, _engine.getExecOrUserXRegister(1).getW());
        assertEquals(0_777777_775301L, _engine.getExecOrUserXRegister(2).getW());
    }

    @Test
    public void testLX_Qx_EM() throws MachineInterrupt {
        var code = new long[] {
            lxEM(Constants.JFIELD_Q1, 12, 0, 0, 0, 1, 0),
            lxEM(Constants.JFIELD_Q2, 13, 0, 0, 0, 1, 0),
            lxEM(Constants.JFIELD_Q3, 14, 0, 0, 0, 1, 0),
            lxEM(Constants.JFIELD_Q4, 15, 0, 0, 0, 1, 0),
            0
        };

        var data = new long[] { data(0_112, 0_233, 0_445, 0_566) };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        loadBaseRegister((short) 1, false, 0_0, 0_1777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setQuarterWordModeEnabled(true)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        run();

        assertEquals(0_112L, _engine.getExecOrUserXRegister(12).getW());
        assertEquals(0_233L, _engine.getExecOrUserXRegister(13).getW());
        assertEquals(0_445L, _engine.getExecOrUserXRegister(14).getW());
        assertEquals(0_566L, _engine.getExecOrUserXRegister(15).getW());
    }

    @Test
    public void testLX_Indirect_BM() throws MachineInterrupt {
        var code = new long[] {
            lxBM(Constants.JFIELD_W, 5, 0, 0, 1, 022002),
            0,
            lxBM(0, 0, 0, 0, 1, 022003),
            lxBM(0, 0, 0, 0, 0, 040000)
        };

        var data = new long[] { 0_221111_111111L };

        loadBaseRegister((short) 14, false, 0_22000, 0_22777, 0, code);
        loadBaseRegister((short) 15, false, 0_40000, 0_40777, 0, data);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setQuarterWordModeEnabled(true)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);
        _engine.getExecOrUserXRegister(3).setXI(0_01).setXM(0_040000);

        run();

        assertEquals(0_221111_111111L, _engine.getExecOrUserXRegister(5).getW());
    }

    @Test
    public void testLX_GRS040_Priv3_BM_Violation() throws MachineInterrupt {
        var code = new long[] {
            lxBM(Constants.JFIELD_W, 0, 0, 0, 0, 040),
            0
        };

        loadBaseRegister((short) 14, false, 0_22000, 0_22777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        try {
            run();
        } catch (ReferenceViolationInterrupt ex) {
            assertEquals(ReferenceViolationInterrupt.ErrorType.GRSViolation, ex._errorType);
            return;
        }
        throw new RuntimeException("Expected ReferenceViolationInterrupt");
    }

    @Test
    public void testLX_GRS0130_Priv0_BM_Success() throws MachineInterrupt {
        var code = new long[] {
            lxBM(Constants.JFIELD_W, 0, 0, 0, 0, 0130),
            0
        };

        loadBaseRegister((short) 14, false, 0_22000, 0_22777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_22000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getGeneralRegisterSet().getRegister(0130).setW(0_765432_123456L);

        run();

        assertEquals(0_765432_123456L, _engine.getExecOrUserXRegister(0).getW());
    }

    @Test
    public void testLX_GRS0130_Priv3_EM_Violation() throws MachineInterrupt {
        var code = new long[] {
            lxEM(Constants.JFIELD_W, 0, 0, 0, 0, 0, 0130),
            0
        };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        try {
            run();
        } catch (ReferenceViolationInterrupt ex) {
            assertEquals(ReferenceViolationInterrupt.ErrorType.GRSViolation, ex._errorType);
            return;
        }
        throw new RuntimeException("Expected ReferenceViolationInterrupt");
    }

    @Test
    public void testLX_GRS0130_Priv0_EM_Success() throws MachineInterrupt {
        var code = new long[] {
            lxEM(Constants.JFIELD_W, 0, 0, 0, 0, 0, 0130),
            0
        };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)0)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister().setProgramCounter(0_1000).setBankDescriptorIndex(0_000004).setBankLevel((short)0_7);

        _engine.getGeneralRegisterSet().getRegister(0130).setW(0_123456_765432L);

        run();

        assertEquals(0_123456_765432L, _engine.getExecOrUserXRegister(0).getW());
    }
}
