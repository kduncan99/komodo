/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.addressSpaceManagement;

import com.kadware.em2200.baselib.IndexRegister;
import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.functions.FunctionHandler;
import com.kadware.em2200.hardwarelib.interrupts.AddressingExceptionInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.misc.BankDescriptor;
import com.kadware.em2200.hardwarelib.misc.DesignatorRegister;
import com.kadware.em2200.hardwarelib.misc.VirtualAddress;
import sun.jvm.hotspot.debugger.Address;

/**
 * Handles the LBN instruction f=075 j=014
 * Operand is a virtual address.
 * If the virtual address is between 0,0 and 0,31 (L,BDI) then that becomes the true bank name.
 * Otherwise L,BDI indicates a bank descriptor from which we get the true bank name,
 * subject to indirect and gate banks.
 */
public class LBNFunctionHandler extends FunctionHandler {

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
        int bankName = 0;
        long operand = ip.getOperand(false, true, false, false);
        VirtualAddress va = new VirtualAddress(operand);
        int origLevel = (int) va.getLevel();
        int origBDI = (int) va.getBankDescriptorIndex();

        if (origLevel == 0 && (va.getBankDescriptorIndex() < 32)) {
            bankName = (int) va.getH1();
            skip = true;
        } else {
            BankDescriptor bd = ip.findBankDescriptor(origLevel, origBDI);
            if (bd.getBankType() == BankDescriptor.BankType.QueueRepository) {
                throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.BDTypeInvalid,
                                                       origLevel,
                                                       origBDI);
            }

            bankName = origLevel << 15;
            bankName |= ((origBDI - bd.getDisplacement()) & 077777);
        }

        IndexRegister xReg = ip.getExecOrUserXRegister((int) iw.getA());
        xReg.setXI(bankName);
        xReg.setXM(0);

        if (skip) {
            ip.setProgramCounter(ip.getProgramAddressRegister().getProgramCounter() + 1, false);
        }
    }
}
