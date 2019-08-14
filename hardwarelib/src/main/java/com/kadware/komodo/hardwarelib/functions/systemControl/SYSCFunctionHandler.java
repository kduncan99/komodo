/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.systemControl;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the SYSC instruction f=073 j=017 a=012
 */
public class SYSCFunctionHandler extends InstructionHandler {

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

        //  Retrieve U.  This could be troublesome in the future, as we need to retrieve maybe more than one value
        //  starting at U, but we don't know how many words until we know the subfunction, which is in U+0.
        //  But... due to storage lock logic, we're not allowed to ask multiple times.  Not a problem yet, but it will be...
        long operand = ip.getOperand(false, false, false, false);

        //  For now, we do not recognize any sub-functions, so we always throw a machine interrupt
        throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.UndefinedFunctionCode);

        //TODO
        //  Subfunction 020: Create dynamic memory block
        //      U+0,S1:     020
        //      U+0,S2:     UPI of target MSP
        //      U+0,S3:     Zeros
        //      U+0,H2:     Zeros
        //      U+1,W:      Zeros
        //      U+2,W:      Requested size of memory in words, range 0:0x7FFFFFF = 0_17777_777777 (31 bits)
        //      Upon return U+1,W contains the assigned segment index

        //TODO
        //  Subfunction 021: Release dynamic memory block
        //      U+0,S1:     021
        //      U+0,S2:     UPI of target MSP
        //      U+0,S3:     Zeros
        //      U+0,H2:     Zeros
        //      U+1,W:      Segment index of block to be released
        //      U+2,W:      Zeros

        //TODO
        //  Subfunction 022: Resize dynamic memory block
        //      U+0,S1:     022
        //      U+0,S2:     UPI of target MSP
        //      U+0,S3:     Zeros
        //      U+0,H2:     Zeros
        //      U+1,W:      Segment index of block to be resized
        //      U+2,W:      Requested size of memory in words, range 0:0x7FFFFFF = 0_17777_777777 (31 bits)

        //TODO
        //  Subfunction 030: Send system status console message
        //  U+0,S1:         030
        //  U+0,Bit6:       ASCII flag - message is in ASCII
        //  U+0,S3:         Length of first message in words
        //  U+0,S4:         Length of second message in words
        //  U+1,2:          Absolute address of buffer containing first message
        //  U+3,4:          Absolute address of buffer containing second message

        //TODO
        //  Subfunction 031: Send read-only console message
        //  U+0,S1          031
        //  U+0,Bit6:       ASCII flag - message is in ASCII
        //  U+0,S3:         Length of message in words
        //  U+1,2:          Absolute address of buffer containing message

        //TODO
        //  Subfunction 032: Send read-reply console message
        //  U+0,S1          032
        //  U+0,Bit6        send ASCII - message to be send is in ASCII
        //  U+0,Bit7        read ASCII - message to be read should be in ASCII
        //  U+0,S3          Length of message in words
        //  U+0,S4          Maximum accepted length of response in characters
        //  U+1,2:          Absolute address of buffer containing message
        //  U+3,4:          Absolute address of buffer where response should be placed

        //TODO
        //  Subfunction 033: Poll for unsolicited input
        //  U+0,S1          033
        //  U+0,Bit7        read ASCII - message to be read (if any) should be in ASCII
        //  U+0,Bit11       if set on return, a message was read - otherwise, this is clear
        //  U+0,S4          Maximum accepted length of input in words (should allow for 80 characters)
        //  U+0,S6          Number of words received if a message was read
        //  U+2:            Absolute address of buffer to receive response

        //TODO
        //  Subfunction 040: Start IO
        //  U+0,S1          040
        //  U+0,S2          UPI of IOP to be used
        //  U+0,S3          Channel Module index
        //  U+0,S4          Device index
        //  U+0,S6          Flags
        //                      Bit30: IOP should send a UPI interrupt when IO is complete
        //  U+1,S1          Operation
        //                      000: Write
        //                      001: Write EOF
        //                      002: Read
        //                      003: Skip
        //                      004: Skip EOF
        //                      005: Rewind
        //                      006: Rewind with Interlock
        //  U+1,Bits 6-7    Format: 0=type A, 1=type B, 2=type C, 3=type D
        //                      A is qword mode, 4 8-bit bytes per word
        //                      B is fd mode, 6 6-bit bytes per word
        //                      C is packed mode, 9 bytes per 2 words
        //                      D is similar to A, ignoring the stop bit
        //  U+1 Bit 8       Direction: 0=forward, 1=backward
        //  U+1,S3          Status of IO
        //                      000: IO completed successfully
        //                      001: IOP UPI does not correspond to an IOP
        //                      002: Channel module does not exist
        //                      003: Device does not exist
        //                      004: Device is not ready
        //                      005: Device is busy
        //                      006: End of file mark
        //                      007: End of tape mark
        //                      010: Address out of range
        //                      040: IO started successfully and is in progress
        //  U+1,S4          Non-integral residue count
        //  U+2,H1          Number of words to be transferred on output, buffer size on input
        //  U+2,H2          Number of words transferred
        //  U+3             Absolute address of IO buffer
        //  U+5             Device-relative address if applicable
    }

    @Override
    public Instruction getInstruction() { return Instruction.SYSC; }
}
