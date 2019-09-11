/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.addressSpaceManagement;

import com.kadware.komodo.baselib.IndexRegister;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.BankDescriptor;
import com.kadware.komodo.hardwarelib.DesignatorRegister;
import com.kadware.komodo.hardwarelib.VirtualAddress;

/**
 * Handles the LBN instruction f=075 j=014
 * Operand is a virtual address.
 * If the virtual address is between 0,0 and 0,31 (L,BDI) then that becomes the true bank name.
 * Otherwise L,BDI indicates a bank descriptor from which we get the true bank name,
 * subject to indirect and gate banks.
 */
public class LBNFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        DesignatorRegister dr = ip.getDesignatorRegister();
        if (dr.getBasicModeEnabled() && (dr.getProcessorPrivilege() > 0)) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        boolean skip = false;
        long bankName = 0;
        long operand = ip.getOperand(false, true, false, false);
        VirtualAddress va = new VirtualAddress(operand);
        int origLevel = va.getLevel();
        int origBDI = va.getBankDescriptorIndex();

        if (origLevel == 0 && (va.getBankDescriptorIndex() < 32)) {
            bankName = va.getH1();
            skip = true;
        } else {
            BankDescriptor bd = getBankDescriptor(ip, origLevel, origBDI, false);
            if (bd.getBankType() == BankDescriptor.BankType.QueueRepository) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.BDTypeInvalid,
                                                       origLevel,
                                                       origBDI);
            }

            bankName = origLevel << 15;
            bankName |= ((origBDI - bd.getDisplacement()) & 077777);
            if (bd.getBankType() != BankDescriptor.BankType.BasicMode) {
                skip = true;
            }
        }

        int ixReg = (int) iw.getA();
        IndexRegister xReg = ip.getExecOrUserXRegister(ixReg);
        ip.setExecOrUserXRegister(ixReg, bankName << 18);

        if (skip) {
            ip.setProgramCounter(ip.getProgramAddressRegister().getProgramCounter() + 1, false);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.LBN; }
}
