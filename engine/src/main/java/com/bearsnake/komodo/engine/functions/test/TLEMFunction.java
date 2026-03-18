/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Test Less Than or Equal Alphanumeric instruction (TLEM)
 * Also known as Test Not Greater Alphanumeric (TNGM).
 * Function code 047.
 * The contents of U are fetched under F0.j control, its high-order 18 bits are truncated,
 * and it is alphanumerically compared with the right half (bits 18–35) of Xa.
 * bits 0–17 of Xa are added to bits 18–35 of Xa and the sum is stored into bits 18–35 of Xa.
 * Bits 0–17 of Xa remain unchanged.
 * If the results of the comparison indicate that the operand is less than or equal to Xa,
 * the next instruction (NI) is skipped.
 */
public class TLEMFunction extends Function {

    public static final TLEMFunction INSTANCE = new TLEMFunction();

    private TLEMFunction() {
        super("TLEM");
        var fc = new FunctionCode(047);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.X_REGISTER);
        setImmediateMode(true);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        // Fetch the operand U under F0.j control.
        var operand = engine.getOperand(false, true, true, true, false);
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var xReg = engine.getExecOrUserXRegister(ci.getA());

        // Extract low 18 bits of U and Xa (right half).
        long uLow = operand & 0777777L;
        long xaLow = xReg.getXM();
        long xaHigh = xReg.getXI();

        // Alphanumeric comparison (unsigned 18-bit).
        boolean skip = uLow <= xaLow;

        // bits 0–17 of Xa are added to bits 18–35 of Xa and the sum is stored into bits 18–35 of Xa.
        // This is 18-bit ones-complement addition.
        long sum = xaLow + xaHigh;
        if (sum > 0777777L) {
            sum = (sum + 1) & 0777777L;
        }
        // Special case: ones-complement 18-bit 0777777 + 0 is 0777777 (-0 + 0 = -0),
        // but typically simple additions in this context handle -0 as 0.
        // Actually, for X-register modifiers, 0777777 is usually handled as -0.
        // Let's ensure if it was 0777777 before and we added 0, it stays 0777777.
        // Wait, 18-bit ones complement: -0 is 0777777.
        // If we add 0 (000000) to 0777777, it's 0777777.
        // If we add 1 to 0777777, it's 1000000 -> 000001.

        xReg.setXM(sum);

        if (skip) {
            engine.getProgramAddressRegister().incrementProgramCounter();
        }

        return true;
    }
}
