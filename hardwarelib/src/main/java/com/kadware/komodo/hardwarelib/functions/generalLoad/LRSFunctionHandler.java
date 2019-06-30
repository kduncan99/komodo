/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.generalLoad;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the LRS instruction f=072 j=017
 * This instruction loads one or two consecutive sets of values from a single buffer in memory to GRS locations.
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
 * The first set of transfers begins with the source set to effective U + 0, and the destination is the GRS register
 * at address 1.  {count1} words are transferred from consecutive locations in memory to consecutive GRS registers.
 * If at any point the GRS index reaches 0200, it is reset to 0, and the process continues.
 * The second set of transfers then begins with the source set to effective U + {count1} (or an appropriate value if
 * wraparound occurred at index 0200) and with the GRS register at address 2.  {count2} words are transferred just
 * as they were in the first set.
 *
 * If count1 or count2 are zero, the corresponding register transfer is effectively a NOP.
 *
 * A(a) may be included in the register transfer, so the content thereof will be pulled out and stored separately
 * in order to not be overwritten during the process.
 */
@SuppressWarnings("Duplicates")
public class LRSFunctionHandler extends InstructionHandler {

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

        //  Go get all the operands we need for both areas
        long[] operands = new long[count1 + count2];
        ip.getConsecutiveOperands(false, operands);

        //  If we got this far, we can start populating the GRS
        int ox = 0;
        int grsx = address1;
        for (int rx = 0; rx < count1; ++rx) {
            ip.setGeneralRegister(grsx++, operands[ox++]);
            if (grsx == 0200) {
                grsx = 0;
            }
        }

        grsx = address2;
        for (int rx = 0; rx < count2; ++rx) {
            ip.setGeneralRegister(grsx++, operands[ox++]);
            if (grsx == 0200) {
                grsx = 0;
            }
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.LRS; }
}
