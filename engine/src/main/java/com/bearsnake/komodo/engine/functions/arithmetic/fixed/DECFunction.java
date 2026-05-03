/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.fixed;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Decrement Storage instruction
 * (DEC) Decrements the operand by 1 under storage lock.
 * If the operand is +/- zero before or after the operation, execute NI else skip.
 * For Memory, j-field is interpreted as follows:
 *      W, XH1, XH2, T1, T2, T3 -> 1's complement with Carry/Overflow detection
 *      H1, H2, S1-S6, Q1-Q6 -> 2's complement (Carry/Overflow behavior is undefined)
 *      U, XU -> 1's complement with Carry/Overflow detection, but the result is not stored.
 * NOTE that all of the above will be accomplished automatically by just using normal operand load and store.
 * For Extended Mode with GRS reference,
 *      W -> 1's complement with Carry/Overflow detection
 *      01-015 is undefined
 *      U, XU -> 1's complement with Carry/Overflow detection, but the result is not stored.
 * For Basic Mode with GRS reference,
 *      W, XH1, XH2, T1, T2, T3 -> 1's complement FULL WORD with Carry/Overflow detection
 *      H1, H2, S1-S6, Q1-Q6 -> 2's complement FULL WORD (Carry/Overflow behavior is undefined)
 *      U, XU -> 1's complement with Carry/Overflow detection, but the result is not stored.
 * NOTE that this requires a bit of special code for the 2's complement variation.
 */
public class DECFunction extends FixedFunction {

    public static final DECFunction INSTANCE = new DECFunction();

    private DECFunction() {
        super("DEC");
        setBasicModeFunctionCode(new FunctionCode(0_05).setAField(0_011));
        setExtendedModeFunctionCode(new FunctionCode(0_05).setAField(0_011));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(true);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        // get full word operand (we might partial it out later)
        var operand = engine.getOperand(true, true, true, false, true);
        if (engine.spGetInstructionPoint() == InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        decrement(engine, operand, 1, true);
        engine.addressClearAllLocks();

        return true;
    }
}
