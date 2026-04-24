/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.system;

import com.bearsnake.komodo.engine.AbsoluteAddress;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * System Control instruction
 * (SYSC) invokes various system or processor control functions.
 */
public class SYSCFunction extends Function {

    public static final SYSCFunction INSTANCE = new SYSCFunction();

    private SYSCFunction() {
        super("SYSC");
        setExtendedModeFunctionCode(new FunctionCode(0_73).setJField(0_17).setAField(0_12).setProcessorPrivilege(0));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getOperand(false, false, false, false, false);
        switch ((int)(operand >> 30)) {
            case 016 -> {
                var operands = engine.getConsecutiveOperandsFromCachedAddress(3);
                var address = AbsoluteAddress.construct((int)operands[0], (int)operands[1]);
                engine.addressLockAndWait(address);
            }
            case 017 -> {
                var operands = engine.getConsecutiveOperandsFromCachedAddress(3);
                var address = AbsoluteAddress.construct((int)operands[0], (int)operands[1]);
                engine.addressClearLock(address);
            }
            default -> throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.UndefinedFunctionCode);
        }

        return true;
    }
}
