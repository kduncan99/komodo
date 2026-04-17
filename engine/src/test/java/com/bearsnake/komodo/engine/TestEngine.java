/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.interrupts.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InstructionWord class
 */
public class TestEngine extends EngineUnitTest {

    @Test
    public void testLoadBank_B0() throws HardwareCheckInterrupt, AddressingExceptionInterrupt {
        _engine = new Engine(this, this);

        var bankSize = 1024;
        var segIndex = allocateSegment(bankSize);
        var bank = getSegment(segIndex);
        bank.set(042, 0_1234);

        var bd = new BankDescriptor(false,
                                    new AccessLock(),
                                    AccessPermissions.ALL,
                                    AccessPermissions.ALL,
                                    new AbsoluteAddress(segIndex, 0),
                                    false,
                                    0,
                                    bankSize - 1,
                                    0);

        var bdtId = createBankDescriptorTable(32);
        var bankLevel = 1;
        var bdi = 05;

        loadBankDescriptorTableToBaseRegister(bdtId, bankLevel);
        registerBankDescriptorViaLevelAndBDI(bankLevel, bdi, bd);
        _engine.loadBank(0, bankLevel, bdi, 0);

        assertFalse(_engine.getBaseRegister(0).isVoid());
        assertEquals(bankSize - 1, _engine.getBaseRegister(0).getUpperLimitNormalized());
        assertEquals(0_1234, _engine.getBaseRegister(0).getStorage().get(0_42));
        assertEquals(bankLevel, _engine.getProgramAddressRegister().getBankLevel());
        assertEquals(bdi, _engine.getProgramAddressRegister().getBankDescriptorIndex());
    }

    @Test
    public void testLoadBank_B15() throws HardwareCheckInterrupt, AddressingExceptionInterrupt {
        _engine = new Engine(this, this);

        var bankSize = 1024;
        var segIndex = allocateSegment(bankSize);
        var bank = getSegment(segIndex);
        bank.set(042, 0_1234);

        var bd = new BankDescriptor(false,
                                    new AccessLock(),
                                    AccessPermissions.ALL,
                                    AccessPermissions.ALL,
                                    new AbsoluteAddress(segIndex, 0),
                                    false,
                                    0,
                                    bankSize - 1,
                                    0);

        var bdtId = createBankDescriptorTable(32);
        var bankLevel = 3;
        var bdi = 010;

        loadBankDescriptorTableToBaseRegister(bdtId, bankLevel);
        registerBankDescriptorViaLevelAndBDI(bankLevel, bdi, bd);
        _engine.loadBank(15, bankLevel, bdi, 0);

        assertFalse(_engine.getBaseRegister(15).isVoid());
        assertEquals(bankSize - 1, _engine.getBaseRegister(15).getUpperLimitNormalized());
        assertEquals(0_1234, _engine.getBaseRegister(15).getStorage().get(0_42));
        assertEquals(bankLevel, _engine.getActiveBaseTableEntry(15).getBankLevel());
        assertEquals(bdi, _engine.getActiveBaseTableEntry(15).getBankDescriptorIndex());
    }

    @Test
    public void testLoadBank_Fail_Level0_BDI0() throws HardwareCheckInterrupt {
        _engine = new Engine(this, this);

        var bankSize = 1024;
        var segIndex = allocateSegment(bankSize);
        var bd = new BankDescriptor(false,
                                    new AccessLock(),
                                    AccessPermissions.ALL,
                                    AccessPermissions.ALL,
                                    new AbsoluteAddress(segIndex, 0),
                                    false,
                                    0,
                                    bankSize - 1,
                                    0);

        var bdtId = createBankDescriptorTable(32);
        var bankLevel = 0;
        var bdi = 0;

        loadBankDescriptorTableToBaseRegister(bdtId, bankLevel);
        registerBankDescriptorViaLevelAndBDI(bankLevel, bdi, bd);
        assertThrows(AddressingExceptionInterrupt.class, () -> _engine.loadBank(0, bankLevel, bdi, 0));
    }

    @Test
    public void testLoadBank_Fail_Bad_BaseReg() throws HardwareCheckInterrupt {
        _engine = new Engine(this, this);

        var bankSize = 1024;
        var segIndex = allocateSegment(bankSize);
        var bd = new BankDescriptor(false,
                                    new AccessLock(),
                                    AccessPermissions.ALL,
                                    AccessPermissions.ALL,
                                    new AbsoluteAddress(segIndex, 0),
                                    false,
                                    0,
                                    bankSize - 1,
                                    0);

        var bdtId = createBankDescriptorTable(32);
        var bankLevel = 01;
        var bdi = 05;

        loadBankDescriptorTableToBaseRegister(bdtId, bankLevel);
        registerBankDescriptorViaLevelAndBDI(bankLevel, bdi, bd);
        assertThrows(AddressingExceptionInterrupt.class, () -> _engine.loadBank(32, bankLevel, bdi, 0));
    }

    @Test
    public void testInternalInterruptProcessing() throws HardwareCheckInterrupt {
        _engine = new Engine(this, null);

        var codeBankDescriptor = new BankDescriptor();
        createBank(BankType.ExtendedMode, 0_1000, 0_1000, codeBankDescriptor);
        // codeBank is all zeroes, so no matter where we jump to, we'll get an invalid instruction interrupt.

        var codeBankLevel = 0;
        var codeBankIndex = 32;
        var codeBankOffset = 0_20; // arbitrary choice for offset
        var codeBankVirtualAddress = ((long)codeBankLevel << 33) | ((long)codeBankIndex << 18) | codeBankOffset;

        // Set up level 0 BDT, then load an interrupt vector for the interrupt,
        // and a BDT for the code bank.
        var bdtID = createBankDescriptorTable(64);
        var bdt = getSegment(bdtID);
        loadBankDescriptorTableToBaseRegister(bdtID, 0);

        // Create an interrupt, use the interrupt class value to establish the interrupt vector,
        // register the code bank as the interrupt handler, then post the interrupt.
        var interrupt = new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidTargetInstruction);
        bdt.set(interrupt.getInterruptClass().getCode(), codeBankVirtualAddress);
        registerBankDescriptorViaLevelAndBDI(codeBankLevel, codeBankIndex, codeBankDescriptor);
        _engine.postInterrupt(new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidTargetInstruction));

        // Create an ICS
        createInterruptControlStack(0, 16, 256);

        // Now see what happens.
        _engine.cycle();
        assertFalse(_engine.isHalted());
        assertEquals(0_000040_000020L, _engine.getProgramAddressRegister().getCompositeValue());
    }

    // TODO we should do some interrupt processing testing where the process fails for various reasons
    //  Also need to check priority of processing

    @Test
    public void testEnsureInterruptsAreNotProcessed() throws HardwareCheckInterrupt {
        _engine = new Engine(this, null);

        var bdtID = createBankDescriptorTable(64);
        loadBankDescriptorTableToBaseRegister(bdtID, 0);

        MachineInterrupt interrupt = new UPINormalInterrupt(MachineInterrupt.Synchrony.Pended, 0);
        createInterruptControlStack(0, 16, 512);

        var code = new long[]{ 0 };
        var codeBank = new ArraySlice(code);
        _engine.getDesignatorRegister().setBasicModeEnabled(false).setDeferrableInterruptEnabled(false);
        _engine.postInterrupt(interrupt);
        loadBaseRegister(0, false, 0_1000, 0_1777, null, codeBank);

        _engine.cycle();
        assertEquals(HaltCode.NONE, _engine.getHaltCode());
    }
}
