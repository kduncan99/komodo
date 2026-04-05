/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.*;
import com.bearsnake.komodo.engine.functions.TestFunction;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Conditional Replace instruction
 * (CR) Compares (U) with A(a), if equal, A(a+1) is stored in U and the next instruction is skipped.
 * Otherwise, the next instruction is executed.
 * f=075, j=015.
 */
public class TestCRFunction extends TestFunction {

    @BeforeEach
    public void setup() {
        _engine = new Engine();
        _engine.getDesignatorRegister().clear();
        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
        _engine.getDesignatorRegister().setQuarterWordModeEnabled(false);
    }

    private long crEM(long a, long x, long h, long i, long b, long d) {
        return fjaxhibd(0_75, 0_15, a, x, h, i, b, d);
    }

    private long crBM(long a, long x, long u) {
        return fjaxu(0_75, 0_15, a, x, u);
    }

    @Test
    public void testCR_Success_EM() throws MachineInterrupt {
        var code = new long[] {
            crEM(2, 0, 0, 0, 2, 42),      // CR (U) == A(2)? Yes. Replace (U) with A(3), Skip.
            0,                           // Skipped
            0,                           // Normal stop
        };

        var data = new long[50];
        data[42] = 0_123456_777777L;        // (U) initial value

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
        _engine.getExecOrUserARegister(2).setW(0_123456_777777L); // A(2) matches (U)
        _engine.getExecOrUserARegister(3).setW(0_777777_654321L); // A(3) new value
        _engine.getExecOrUserXRegister(0).setXM(0);

        run();

        // Check if (U) was replaced
        assertEquals(0_777777_654321L, bank1.get(42));
        // Check if next instruction was skipped
        assertEquals(0_2, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testCR_Failure_EM() throws MachineInterrupt {
        var code = new long[] {
            crEM(2, 0, 0, 0, 2, 0),      // CR (U) == A(2)? No. Don't Replace, Don't Skip.
            0,                           // Executed (Normal stop)
            0,
        };

        var data = new long[] {
            0_123456_777777L,            // (U) initial value
        };

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.ExtendedMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(0).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(2).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short)3)
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);
        _engine.getExecOrUserARegister(2).setW(0_000000_000000L); // A(2) does NOT match (U)
        _engine.getExecOrUserARegister(3).setW(0_777777_654321L); // A(3) new value
        _engine.getExecOrUserXRegister(0).setXM(0);

        run();

        // Check if (U) was NOT replaced
        assertEquals(0_123456_777777L, bank1.get(0));
        // Check if next instruction was NOT skipped
        assertEquals(0_1, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testCR_Success_BM() throws MachineInterrupt {
        var code = new long[] {
            crBM(2, 0, 0_24000),          // CR (U) == A(2)? Yes. Replace (U) with A(3), Skip.
            0,                           // Skipped
            0,                           // Normal stop
        };

        var data = new long[0_3000];
        data[0_2000] = 0_123456_777777L; // (U) initial value

        var bank0 = new ArraySlice(code);
        var bank1 = new ArraySlice(data);

        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_1)
                                      .setUpperLimit(0_1777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));
        var bd1 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0_22)
                                      .setUpperLimit(0_24777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);
        _engine.getBaseRegister(14).setBankDescriptor(bd1).setStorage(bank1).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setBasicModeBaseRegisterSelection(false)
               .setProcessorPrivilege((short)0) // Required for CR in Basic Mode
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(01000).setBankDescriptorIndex(0).setBankLevel((short)0);
        _engine.getExecOrUserARegister(2).setW(0_123456_777777L); // A(2) matches (U)
        _engine.getExecOrUserARegister(3).setW(0_777777_654321L); // A(3) new value

        run();

        // Check if (U) was replaced
        assertEquals(0_777777_654321L, bank1.get(0_2000));
        // Check if next instruction was skipped
        assertEquals(0_1002, _engine.getProgramAddressRegister().getProgramCounter());
    }

    @Test
    public void testCR_InvalidInstruction_BM_Privilege1() {
        var code = new long[] {
            crBM(2, 0, 0_2000),
            0,
        };

        var bank0 = new ArraySlice(code);
        var bd0 = new BankDescriptor().setBankType(BankType.BasicMode)
                                      .setLowerLimit(0)
                                      .setUpperLimit(0_777777)
                                      .setGeneralAccessPermissions(AccessPermissions.ALL)
                                      .setBaseAddress(new AbsoluteAddress(0, 0));

        _engine.getBaseRegister(12).setBankDescriptor(bd0).setStorage(bank0).setSubsetting(0);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(true)
               .setBasicModeBaseRegisterSelection(false)
               .setProcessorPrivilege((short)1) // CR requires privilege 0 in Basic Mode
               .setExecRegisterSetSelected(false);

        _engine.getProgramAddressRegister().setProgramCounter(0).setBankDescriptorIndex(0).setBankLevel((short)0);

        var ex = assertThrows(InvalidInstructionInterrupt.class, this::run);
        assertEquals(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege, ex._reason);
    }
}
