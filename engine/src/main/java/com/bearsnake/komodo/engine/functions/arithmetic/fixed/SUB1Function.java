/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.fixed;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

import static com.bearsnake.komodo.engine.Constants.*;

/**
 * Subtract One from Storage instruction
 * (SUB1) Decrements the operand by 1 under storage lock.
 * For Memory, j-field is interpreted as follows:
 *      W, XH1, XH2, T1, T2, T3 -> 1's complement with Carry/Overflow detection
 *      H1, H2, S1-S6, Q1-Q6 -> 2's complement (Carry/Overflow behavior is undefined)
 *      U, XU -> 1's complement with Carry/Overflow detection, but the result is not stored.
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
public class SUB1Function extends FixedFunction {

    public static final SUB1Function INSTANCE = new SUB1Function();

    private SUB1Function() {
        super("SUB1");
        setBasicModeFunctionCode(new FunctionCode(0_05).setAField(0_016).setProcessorPrivilege(0));
        setExtendedModeFunctionCode(new FunctionCode(0_05).setAField(0_016));

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

        var dr = engine.getDesignatorRegister();
        var ci = engine.getCurrentInstruction();
        var jf = ci.getJ();

        var ones = (jf == JFIELD_W)
                   || (jf == JFIELD_XH2)
                   || (!dr.isQuarterWordModeEnabled()
                       && ((jf == JFIELD_XH1) || (jf == JFIELD_T1) || (jf == JFIELD_T2) || (jf == JFIELD_T3)));
        var fullWord = dr.isBasicModeEnabled() && engine.spGetOperandIsGRS();

        if (!fullWord) {
            operand = Engine.extractPartialWord(operand, jf, dr.isQuarterWordModeEnabled());
        }

        if (ones) {
            operand = add36(engine, operand, Word36.NEGATIVE_ONE);
        } else {
            operand = (operand - 1) & Word36.BIT_MASK;
        }

        if ((jf != JFIELD_U) && (jf != JFIELD_XU)) {
            engine.storeToCachedAddress(operand,
                                        engine.spGetOperandIsGRS(),
                                        dr.isBasicModeEnabled() ? JFIELD_W : jf,
                                        false);
        }
        engine.addressClearAllLocks();

        return true;
    }
}
