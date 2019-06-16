/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.addressSpaceManagement;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.functions.InstructionHandler;
import com.kadware.em2200.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.ReferenceViolationInterrupt;
import com.kadware.em2200.hardwarelib.misc.*;

/**
 * Handles the TRARS instruction f=072 j=00
 */
public class TRARSFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        if (ip.getDesignatorRegister().getProcessorPrivilege() > 0) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        //  Read the 23 word packet indicated by U...
        //  Packet format:
        //  +0  Reserved
        //  +1 to +15 Active Base Table (ABT)
        //  +16 to +22 Activity Save Packet (ASP)
        //  ASP Format is:
        //      +0:     Program Address Register
        //      +1:     Designator Register
        //      +2:     Indicator / Key Register
        //      +3:     Quantum Timer
        //      +4:     F0 (instruction)
        //      +5:     ISW0 (Interrupt Status Word)
        //      +6:     ISW1 (Interrupt Status Word)
        long[] operands = new long[23];
        ip.getConsecutiveOperands(false, operands);

        int abtx = 1;   //  index of first ABT word in the packet
        int aspx = 16;  //  index of first ASP word in the packet
        DesignatorRegister packetDesignatorRegister = new DesignatorRegister(operands[aspx + 2]);
        IndicatorKeyRegister packetIndicatorKeyRegister = new IndicatorKeyRegister(operands[aspx + 3]);

        int[] candidates =
            packetDesignatorRegister.getBasicModeBaseRegisterSelection() ?
                InstructionProcessor.BASE_REGISTER_CANDIDATES_TRUE
                : InstructionProcessor.BASE_REGISTER_CANDIDATES_FALSE;

        //  We're taking relative addresses from X(a) and X(a+1) as lower and upper limits, then checking
        //  that against the basic mode banks (B12-B15) specified in the packet, in the context specified
        //  by the packet.  See TRA - we're basically doing that, but with specified instead of extant
        //  state, and with a range of addresses rather than a single address.
        //  Docs state the relative address is X(a) (and +1) sans top 3 bits.
        //  This is still 33 bits, which is more than an int, and all our relative addresses are expected
        //  to fit in an int (which might be wrong, but there it is).  We use 31, not 33 bits for relative
        //  address (because ints are signed in idiot Java)
        IndexRegister xReg1 = ip.getExecOrUserXRegister((int) iw.getA());
        IndexRegister xReg2 = ip.getExecOrUserXRegister((int) iw.getA() + 1);
        int lowerRelAddr = (int) (xReg1.getW() & 0x7FFF);
        int upperRelAddr = (int) (xReg2.getW() & 0x7FFF);
        AccessInfo accessKey = packetIndicatorKeyRegister.getAccessInfo();

        int count = upperRelAddr - lowerRelAddr + 1;
        if (count >= 0) {
            for (int cx = 0; cx < 4; ++cx) {
                int brIndex = candidates[cx];
                long abtValue = operands[abtx + brIndex - 1];
                if (abtValue != 0) {
                    ActiveBaseTableEntry abte = new ActiveBaseTableEntry(abtValue);
                    if ((abte.getLevel() > 0) || (abte.getBDI() > 31)) {
                        BaseRegister bdtBaseRegister = ip.getBaseRegister(abte.getLevel() + 16);
                        int bdtBaseRegisterOffset = 8 * abte.getBDI();
                        try {
                            bdtBaseRegister.checkAccessLimits(bdtBaseRegisterOffset + 7, false);
                            BankDescriptor bd = new BankDescriptor(bdtBaseRegister._storage, bdtBaseRegisterOffset);
                            BaseRegister bReg = new BaseRegister(bd);

                            bReg.checkAccessLimits(lowerRelAddr, count, false, false, accessKey);

                            long value = 0_400000_000000L;
                            value |= ((long) (brIndex & 03)) << 33;
                            xReg1.setW(value);

                            try {
                                //  If we have read/write access, skip next instruction.
                                bReg.checkAccessLimits(false, true, true, accessKey);
                                ip.setProgramCounter(ip.getProgramAddressRegister().getProgramCounter() + 1,
                                                     false);
                            } catch (ReferenceViolationInterrupt ex) {
                                //  we don't have read/write access, so do not skip
                            }

                            return;
                        } catch (ReferenceViolationInterrupt ex) {
                            //  docs are unclear as to this contingency if we are checking the BDT,
                            //  so we move on to the next thing as if this was a zero or void bank
                            //  just like we do if address limits don't match a proper bank descriptor
                        }
                    }
                }
            }
        }

       xReg1.setW(0);
    }

    @Override
    public Instruction getInstruction() { return Instruction.TRARS; }
}
