/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.system;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Instruction Processor Control instruction
 * (IPC) invokes various processor control functions.
 */
public class IPCFunction extends Function {

    public static final IPCFunction INSTANCE = new IPCFunction();

    private IPCFunction() {
        super("IPC");
        setExtendedModeFunctionCode(new FunctionCode(0_73).setJField(0_17).setAField(0_10).setProcessorPrivilege(0));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getOperand(false, false, false, false, false);
        switch ((int) (operand >> 30)) {
            case 0 -> engine.clearReset();
            case 1 -> engine.enableJumpHistoryInterrupt(true);
            case 2 -> engine.enableJumpHistoryInterrupt(false);
            case 4 -> engine.setBroadcastInterruptEligible(false);
            case 5 -> engine.setBroadcastInterruptEligible(true);
            case 070 -> {
                // allocate a memory segment
                // TODO
            }
            case 071 -> {
                // resize a memory segment
                // TODO
            }
            case 072 -> {
                // release a memory segment
                // TODO
            }
            default -> throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.UndefinedFunctionCode);
        }

        return true;
    }
}
