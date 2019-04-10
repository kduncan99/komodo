/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;

/**
 * Handles f/j-field function codes which require looking up the a-field in a sub-tablel
 */
public class SubSubFunctionHandler extends FunctionHandler {

    private final FunctionHandler[] _table;

    public SubSubFunctionHandler(
        final FunctionHandler[] table
    ) {
        _table = table;
    }

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord instructionWord
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        FunctionHandler handler = _table[(int)instructionWord.getA()];
        if (handler == null) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.UndefinedFunctionCode);
        }
        handler.handle(ip, instructionWord);
    }


    /**
     * Retrieves ths sub-handler we know about which corresponds to the given a-field
     * <p>
     * @param aField
     * <p>
     * @return
     */
    public FunctionHandler getHandler(
        final int aField
    ) {
        return ((aField < 0) || (aField >= _table.length)) ? null : _table[aField];
    }
}
