/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.generalStore;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the SRS instruction f=072 j=016
 * This instruction stores one or two consecutive sets of values from GRS locations to a single buffer in memory.
 * The two sets are contiguous in memory; they do not have to be contiguous in the GRS.
 *
 * A(a) contains a descriptor formatted as such:
 *  Bit2-8:     count2
 *  Bit11-17:   address2
 *  Bit20-26:   count1
 *  Bit29-35:   address1
 *
 * Effective u refers to the buffer in memory.  GRS access does not apply.
 * Standard address resolution for consecutive memory addresses does apply.
 * The size of the buffer must be at least count1 + count2 words in length.
 *
 * The first set of transfers begins with the destination set to effective U + 0, and the source is the GRS register
 * at address 1.  {count1} words are transferred to consecutive locations in memory from consecutive GRS registers.
 * If at any point the GRS index reaches 0200, it is reset to 0, and the process continues.
 * The second set of transfers then begins with the destination set to effective U + {count1} (or an appropriate value if
 * wraparound occurred at index 0200) and with the GRS register at address 2.  {count2} words are transferred just
 * as they were in the first set.
 *
 * If count1 or count2 are zero, the corresponding register transfer is effectively a NOP.
 */
@SuppressWarnings("Duplicates")
public class SRSFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Grab descriptor first
        int descriptorRegisterIndex = ip.getExecOrUserARegisterIndex((int)iw.getA());
        long descriptor = ip.getGeneralRegister(descriptorRegisterIndex).getW();

        int address1 = (int)descriptor & 0177;
        int count1 = (int)(descriptor >> 9) & 0177;
        int address2 = (int)(descriptor >> 18) & 0177;
        int count2 = (int)(descriptor >> 27) & 0177;

        //  Create and populate an operands array from the indicated registers
        long[] operands = new long[count1 + count2];
        int ox = 0;
        int grsx = address1;
        for (int rx = 0; rx < count1; ++rx) {
            operands[ox++] = ip.getGeneralRegister(grsx++).getW();
            if (grsx == 0200) {
                grsx = 0;
            }
        }

        grsx = address2;
        for (int rx = 0; rx < count2; ++rx) {
            operands[ox++] = ip.getGeneralRegister(grsx++).getW();
            if (grsx == 0200) {
                grsx = 0;
            }
        }

        //  Now store them
        ip.storeConsecutiveOperands(false, operands);
    }

    @Override
    public Instruction getInstruction() { return Instruction.SRS; }
}
