/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.procControl;

import com.bearsnake.komodo.engine.AccessKey;
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

public class TestRTNFunction extends FunctionUnitTest {

    private long rtn() {
        return fjaxhibd(073, 017, 003, 0, 0, 0, 0, 0);
    }

    @BeforeEach
    public void setup() {
        _engine = new Engine(this, this);
    }

    @Test
    public void testRTN_Underflow() throws MachineInterrupt {
        var code = new long[] {
            rtn()
        };

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, 0, code);
        createReturnControlStack(0_2000, 32);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1000)
               .setBankDescriptorIndex(0_000004)
               .setBankLevel((short) 0_7);

        var interrupt = assertThrows(RCSGenericStackUnderflowOverflowInterrupt.class, this::run);
        assertEquals(RCSGenericStackUnderflowOverflowInterrupt.Reason.Underflow, interrupt.getReason());
        assertEquals(0_01000, _engine.getProgramAddressRegister().getProgramCounter());
        assertEquals(0_000004, _engine.getProgramAddressRegister().getBankDescriptorIndex());
        assertEquals(0_7, _engine.getProgramAddressRegister().getBankLevel());
    }

    @Test
    public void testRTN_to_EM() throws MachineInterrupt {
        // set up BDT, BD, and bank for the code bank we are returning to
        var destinationLevel = (short) 7;
        var destinationBDI = 0_000016;
        var destinationBD = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0_1).setUpperLimit(0_1777);
        var destinationAddress = 0_1010; // the return-to address

        var destinationBDTSegIndex = createBankDescriptorTable(1024);
        loadBankDescriptorTableToBaseRegister(destinationBDTSegIndex, destinationLevel);
        registerBankDescriptorViaLevelAndBDI(destinationLevel, destinationBDI, destinationBD);

        // set up BDT, BD, and bank for the code bank we are returning from
        var initialLevel = (short) 5;
        var initialBDI = 0_000004;
        var initialBD = new BankDescriptor().setBankType(BankType.ExtendedMode).setLowerLimit(0_1).setUpperLimit(0_1777);
        var initialBank = createBank(BankType.ExtendedMode, 0_1000, 1024, initialBD);
        initialBank[2] = rtn();

        var initialBDTSegIndex = createBankDescriptorTable(1024);
        loadBankDescriptorTableToBaseRegister(initialBDTSegIndex, initialLevel);
        registerBankDescriptorViaLevelAndBDI(initialLevel, initialBDI, initialBD);

        // set up RCS entry with a fake frame which appears to have been created by a CALL from the destination bank.
        createReturnControlStack(0_2000, 32);
        var db12to17 = 010; // !quantumTimer, !deferrable, pp=2, !basicMode, !execRegs
        _engine.allocateAndPopulateRCSFrame(destinationLevel,
                                            destinationBDI,
                                            destinationAddress,
                                            0,
                                            db12to17,
                                            new AccessKey((short)3, (short)12));

        loadBaseRegister((short) 0, false, 0_1000, 0_1777, initialBD.getBaseAddress(), initialBank);

        _engine.getDesignatorRegister()
               .setBasicModeEnabled(false)
               .setProcessorPrivilege((short) 3)
               .setExecRegisterSetSelected(false);
        _engine.getProgramAddressRegister()
               .setProgramCounter(0_1002) // arbitrary
               .setBankDescriptorIndex(initialBDI)
               .setBankLevel(initialLevel);

        run();

        assertEquals(destinationAddress, _engine.getProgramAddressRegister().getProgramCounter());
        assertEquals(destinationBDI, _engine.getProgramAddressRegister().getBankDescriptorIndex());
        assertEquals(destinationLevel, _engine.getProgramAddressRegister().getBankLevel());
    }

    // TODO need a test for returning to BM
    // TODO need a test for returning to gate bank (how does that make sense?)
    // TODO need some negative tests
}
