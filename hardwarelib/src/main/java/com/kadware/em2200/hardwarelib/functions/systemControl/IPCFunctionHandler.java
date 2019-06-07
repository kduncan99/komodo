/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.systemControl;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.baselib.Word36;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the IPC instruction f=073 j=017 a=010
 */
public class IPCFunctionHandler extends InstructionHandler {

    @Override
    public synchronized void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  PP has to be 0
        int procPriv = ip.getDesignatorRegister().getProcessorPrivilege();
        if (procPriv > 0) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        //  Retrieve U to get the subfunction
        long operand = ip.getOperand(false, false, false, false);
        int subFunc = (int)Word36.getS1(operand);

        //  We do recognize a few subfunctions...
        switch (subFunc) {
            case 0: //  clear reset designator
                //???? don't really know what this is yet...
                break;

            case 1: //  enable conditionalJump-history-full interrupt
                ip.setJumpHistoryFullInterruptEnabled(true);
                break;

            case 2: //  disable conditionalJump-history-full interrupt
                ip.setJumpHistoryFullInterruptEnabled(false);
                break;

            case 3: //  synchronize page invalidates (we don't do this, so it's a NOP)
                //????
                break;

            case 4: //  clear broadcast interrupt eligibility
                ip.setBroadcastInterruptEligibility(false);
                break;

            case 5: //  set broadcast interrupt eligibility
                ip.setBroadcastInterruptEligibility(true);
                break;

            default:
                throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.UndefinedFunctionCode);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.IPC; }
}
