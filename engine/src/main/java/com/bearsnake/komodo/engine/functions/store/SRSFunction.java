/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.GeneralRegisterSet;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;

/**
 * Store Register Set instruction
 * (SRS) Stores one or two subsets of GRS to the contents of U through U+n.
 * Specifically, the instruction defines two sets of ranges and lengths as follows:
 *      Aa[2:8]   = range 2 length
 *      Aa[11:17] = range 2 first GRS index
 *      Aa[20:26] = range 1 count
 *      Aa[29:35] = range 1 first GRS index
 * So we start storing registers from GRS index of range 1, for the number of registers in range 1 count,
 * to U[0] to U[range1count - 1], and then from GRS index of range 2, for the number of registers in range 2 count,
 * to U[range1count] to U[range1count + range2count - 1].
 * If either count is zero, then the associated range is not used.
 * If the GRS address exceeds 0177, it wraps around to zero.
 */
public class SRSFunction extends Function {

    public static final SRSFunction INSTANCE = new SRSFunction();

    private SRSFunction() {
        super("SRS");
        var fc = new FunctionCode(0_72).setJField(0_16);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.A_REGISTER);
        setImmediateMode(true);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var ci = engine.getCurrentInstruction();
        var dr = engine.getDesignatorRegister();
        var pPriv = dr.getProcessorPrivilege();
        var aVal = engine.getGeneralRegisterSet().getRegister(engine.getExecOrUserARegisterIndex(ci.getA()));

        var r2Length = aVal.getQ1() & 0177;
        var r2Index = aVal.getQ2() & 0177;
        var r1Count = aVal.getQ3() & 0177;
        var r1Index = aVal.getQ4() & 0177;

        int totalCount = r1Count + r2Length;
        if (totalCount == 0) {
            return true;
        }

        var operands = new long[totalCount];
        int opIdx = 0;

        for (int i = 0; i < r1Count; i++) {
            var grsIndex = (r1Index + i) & 0177;
            if (!GeneralRegisterSet.isAccessAllowed(grsIndex, pPriv, false)) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.GRSViolation, false);
            }
            operands[opIdx++] = engine.getGeneralRegisterSet().getRegister(grsIndex).getW();
        }

        for (int i = 0; i < r2Length; i++) {
            var grsIndex = (r2Index + i) & 0177;
            if (!GeneralRegisterSet.isAccessAllowed(grsIndex, pPriv, false)) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.GRSViolation, false);
            }
            operands[opIdx++] = engine.getGeneralRegisterSet().getRegister(grsIndex).getW();
        }

        return engine.storeConsecutiveOperands(true, operands);
    }
}
