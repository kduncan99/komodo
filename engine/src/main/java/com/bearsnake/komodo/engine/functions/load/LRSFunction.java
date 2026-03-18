/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;

/**
 * Load Register Set instruction
 * (LRS) Loads the GRS (or one or two subsets thereof) from the contents of U through U+n.
 * Specifically, the instruction defines two sets of ranges and lengths as follows:
 *      Aa[2:8]   = range 2 length
 *      Aa[11:17] = range 2 first GRS index
 *      Aa[20:26] = range 1 count
 *      Aa[29:35] = range 1 first GRS index
 * So we start loading registers from GRS index of range 1, for the number of registers in range 1 count,
 * from U[0] to U[range1count - 1], and then from GRS index of range 2, for the number of registers in range 2 count,
 * from U[range1count] to U[range1count + range2count - 1].
 * If either count is zero, then the associated range is not used.
 * If the GRS address exceeds 0177, it wraps around to zero.
 */
public class LRSFunction extends Function {

    public static final LRSFunction INSTANCE = new LRSFunction();

    private LRSFunction() {
        super("LRS");
        var fc = new FunctionCode(0_72).setJField(0_17);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.A_REGISTER);
        setImmediateMode(false);
        setIsGRS(false);
    }

    private void doSubset(
        final Engine engine,
        final short pPriv,
        int grsIndex,
        int grsCount,
        final long[] operands,
        int opIndex
    ) throws ReferenceViolationInterrupt {
        while (grsCount > 0) {
            if (grsIndex > 0177) {
                grsIndex = 0;
            }

            if (!Engine.isGRSAccessAllowed(grsIndex, pPriv, true)) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.GRSViolation, false);
            }

            engine.getGeneralRegisterSet().getRegister(grsIndex++).setW(operands[opIndex++]);
            grsCount--;
        }
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var ci = engine.getCurrentInstruction();
        var dr = engine.getDesignatorRegister();
        var aReg = engine.getExecOrUserARegister(ci.getA());

        var grscount2 = aReg.getQ1() & 0177;
        var grsx2 = aReg.getQ2() & 0177;
        var grscount1 = aReg.getQ3() & 0177;
        var grsx1 = aReg.getQ4() & 0177;
        var count = grscount1 + grscount2;

        var operands = engine.getConsecutiveOperands(true, count);
        doSubset(engine, dr.getProcessorPrivilege(), grsx1, grscount1, operands, 0);
        doSubset(engine, dr.getProcessorPrivilege(), grsx2, grscount2, operands, grscount1);
        return true;
    }
}
