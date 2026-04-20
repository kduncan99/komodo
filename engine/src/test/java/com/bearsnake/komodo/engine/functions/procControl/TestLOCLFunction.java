/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.procControl;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.BankDescriptor;
import com.bearsnake.komodo.engine.BankType;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionUnitTest;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.RCSGenericStackUnderflowOverflowInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestLOCLFunction extends FunctionUnitTest {

    private long locl(
        long x,
        long h,
        long i,
        long u
    ) {
        return fjaxhiu(007, 016, 000, x, h, i, u);
    }

    private long rtn() {
        return fjaxhibd(073, 017, 003, 0, 0, 0, 0, 0);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testLOCL_Overflow() throws MachineInterrupt {
        var code = new long[]{
            locl(0, 0, 0, 0_1012),
            0,
            0
        };

        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, 0, bank0);
        createReturnControlStack(0_2000, 32);
        var stackReg = _engine.getGeneralRegister(Engine.RCS_STACK_POINTER, true);
        stackReg.setXM(0_2000);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        var interrupt = assertThrows(RCSGenericStackUnderflowOverflowInterrupt.class, this::run);
        assertEquals(RCSGenericStackUnderflowOverflowInterrupt.Reason.Overflow, interrupt.getReason());
        assertEquals(0_01000, _engine.getProgramAddressRegister()
                                     .getProgramCounter());
        assertEquals(0_000004, _engine.getProgramAddressRegister()
                                      .getBankDescriptorIndex());
        assertEquals(0_7, _engine.getProgramAddressRegister()
                                 .getBankLevel());
    }

    @Test
    public void testLOCL() throws MachineInterrupt {
        var code = new long[]{
            locl(0, 0, 0, 0_1005),
            0,
            0,
            0,
            0,
            0
        };

        var bank0 = new ArraySlice(code);
        loadBaseRegister(0, false, 0_1000, 0_1777, 0, bank0);
        createReturnControlStack(0_2000, 32);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        run();

        assertEquals(0_01005, _engine.getProgramAddressRegister()
                                     .getProgramCounter());
        assertEquals(0_000004, _engine.getProgramAddressRegister()
                                      .getBankDescriptorIndex());
        assertEquals(0_7, _engine.getProgramAddressRegister()
                                 .getBankLevel());
    }

    // TODO need test with index
    // TODO need some negative tests

    @Test
    public void testLOCLandRTN() throws MachineInterrupt {
        // set up BDT, BD, and bank for the code bank we are returning to
        var codeLevel = 7;
        var codeBDI = 0_000016;
        var codeBD = new BankDescriptor();
        var codeBank = createBank(BankType.ExtendedMode, 0_1000, 1024, codeBD);
        codeBank.set(0, locl(0, 0, 0, 0_1005));
        codeBank.set(5, rtn());

        var codeBDTSegIndex = createBankDescriptorTable(1024);
        loadBankDescriptorTableToBaseRegister(codeBDTSegIndex, codeLevel);
        registerBankDescriptorViaLevelAndBDI(codeLevel, codeBDTSegIndex, codeBD);

        // set up RCS entry with a fake frame which appears to have been created by a CALL from the destination bank.
        createReturnControlStack(0_2000, 32);

        loadBaseRegister(0, false, 0_1000, 0_1777, codeBD.getBaseAddress(), codeBank);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(codeBDI)
               .setBankLevel((short) codeLevel);

        run();

        assertEquals(0_01001, _engine.getProgramAddressRegister().getProgramCounter());
        assertEquals(codeBDI, _engine.getProgramAddressRegister().getBankDescriptorIndex());
        assertEquals(codeLevel, _engine.getProgramAddressRegister().getBankLevel());
    }
}
