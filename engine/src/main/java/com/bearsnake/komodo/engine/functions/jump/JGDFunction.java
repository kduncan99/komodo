/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.baselib.InstructionWord;
import com.bearsnake.komodo.engine.Constants;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.GeneralRegisterSet;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;

/**
 * Jump Greater and Decrement instruction
 * (JGD) If the GRS register indicated by the concatenation of the j-field and the a-field
 * is greater than zero, we jump to the address indicated by resolving U.
 * Regardless of whether we jump, the GRS register is decremented.
 */
public class JGDFunction extends Function {

    public static final JGDFunction INSTANCE = new JGDFunction();

    private JGDFunction() {
        super("JGD");
        setBasicModeFunctionCode(new FunctionCode(0_70));
        setExtendedModeFunctionCode(new FunctionCode(0_70));

        setAFieldSemantics(AFieldSemantics.GRS_INDEX);
        setImmediateMode(false);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getJumpOperand();
        if (engine.getInstructionPoint() == Engine.InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        var ci = engine.getCurrentInstruction();
        var grsx = (ci.getJ() << 4) | ci.getA();
        if (!GeneralRegisterSet.isAccessAllowed(grsx, engine.getDesignatorRegister().getProcessorPrivilege(), true)) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.GRSViolation, false);
        }

        var reg = engine.getGeneralRegisterSet().getRegister(grsx);
        if (reg.isPositive() && !reg.isZero()) {
            doJump(engine, operand);
        }

        reg.decrement();
        return true;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }

    /**
     * Special version of interpret() for JGD
     */
    public static String interpret(
        final Engine engine,
        final InstructionWord iWord
    ) {
        var funcCode = new FunctionCode(070);
        var sb = new StringBuilder("JGD       ");
        var grsx = ((iWord.getJ() << 4) | iWord.getA()) & 0177;
        sb.append(Constants.GRS_REGISTER_NAMES[grsx])
          .append(",");

        // Interpret u-field
        var u18 = (funcCode.getJField() == null) && (iWord.getJ() >= 016) && (iWord.getX() == 0);
        if (!u18 && (iWord.getI() > 0)) {
            sb.append("*");
        }
        var u = iWord.getU();
        if (u18) {
            u |= (iWord.getI() << 16);
            u |= (iWord.getH() << 17);
        }
        sb.append("0");
        if (u != 0) {
            sb.append(Integer.toOctalString(u));
        }

        if (iWord.getX() > 0) {
            sb.append(",");
            if (iWord.getH() > 0) {
                sb.append("*");
            }
            sb.append("X")
              .append(iWord.getX());

            while (sb.length() < 25) {
                sb.append(" ");
            }
            sb.append("");
            var xReg = engine.getGeneralRegisterSet()
                             .getRegister(engine.getExecOrUserXRegisterIndex(iWord.getX()));
            sb.append(String.format("X%d=%06o:%06o", iWord.getX(), xReg.getXI(), xReg.getXM()));
        }

        return sb.toString();
    }
}
