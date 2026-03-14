/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions;

import com.bearsnake.komodo.baselib.InstructionWord;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.DesignatorRegister;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.interrupts.HardwareDefaultInterrupt;
import com.bearsnake.komodo.engine.interrupts.InvalidInstructionInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

import java.util.HashMap;
import java.util.NoSuchElementException;

import static com.bearsnake.komodo.engine.functions.Function.AFieldSemantics.UNUSED;

public abstract class Function {

    public enum AFieldSemantics {
        UNUSED,
        FUNCTION_CODE_EXTENSION,
        A_REGISTER,
        B_REGISTER,
        R_REGISTER,
        X_REGISTER,
    }

    private static final HashMap<Integer, Function> BM_FUNCTIONS_BY_F_CODE = new HashMap<>();
    private static final HashMap<Integer, Function> EM_FUNCTIONS_BY_F_CODE = new HashMap<>();

    private AFieldSemantics _aFieldSemantics = UNUSED;
    private FunctionCode _basicModeFunctionCode = null;
    private FunctionCode _extendedModeFunctionCode = null;
    private boolean _immediateMode = false;     // U and XU partial words are supported for this function
    private boolean _isGRS =  false;            // addresses < 0200 are GRS locations
    private final String _mnemonic;

    protected Function(
        final String mnemonic
    ) {
        _mnemonic = mnemonic;
    }

    public AFieldSemantics getAFieldSemantics() {
        return _aFieldSemantics;
    }

    public String getMnemonic() {
        return _mnemonic;
    }

    public boolean isJumpInstruction() {
        return false;
    }

    public boolean isShiftInstruction() {
        return false;
    }

    protected Function setAFieldSemantics(final AFieldSemantics semantics) {
        _aFieldSemantics = semantics;
        return this;
    }

    protected Function setBasicModeFunctionCode(final FunctionCode functionCode) {
        _basicModeFunctionCode = functionCode;
        return this;
    }

    public Function setExtendedModeFunctionCode(final FunctionCode functionCode) {
        _extendedModeFunctionCode = functionCode;
        return this;
    }

    public Function setImmediateMode(final boolean flag) {
        _immediateMode = flag;
        return this;
    }

    public Function setIsGRS(final boolean flag) {
        _isGRS = flag;
        return this;
    }

    public abstract boolean execute(
        final Engine engine
    ) throws MachineInterrupt;

    public FunctionCode getBasicModeFunctionCode() { return _basicModeFunctionCode; }
    public FunctionCode getExtendedModeFunctionCode() { return _extendedModeFunctionCode; }
    public boolean getImmediateMode() { return _immediateMode; }
    public boolean isGRS() { return _isGRS; }

    public static Function lookup(
        final DesignatorRegister designatorRegister,
        final InstructionWord instruction
    ) throws InvalidInstructionInterrupt,
             HardwareDefaultInterrupt {
        return FunctionTable.lookupFunction(designatorRegister, instruction);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Takes care of the various housekeeping tasks associated with actually taking a jump.
     * This is for transferring control within the current code bank, whether EM or BM.
     */
    protected void doJump(
        final Engine engine,
        final long jumpTarget
    ) {
        engine.preventPCUpdate();

        var par = engine.getProgramAddressRegister();
        var oldAddress = par.getProgramCounter();
        par.setProgramCounter((int)(jumpTarget & 0_777777));

        var dr = engine.getDesignatorRegister();
        if (dr.isBasicModeEnabled()) {
            engine.clearBMCachedBaseRegisterIndex();
        }

        engine.createJumpHistory(oldAddress);
    }

    // --------------------------------------------------------------------------------------------

    // Interprets a function to the extent one can do so given only a
    // designator register to indicate relevant modes, and the instruction word
    // which we are interpreting.
    public static String interpret(
        final DesignatorRegister dReg,
        final InstructionWord iWord
    ) {
        // TODO JGD, BT are weird
        Function func;
        try {
            func = lookup(dReg, iWord);
        } catch (InvalidInstructionInterrupt | HardwareDefaultInterrupt e) {
            return Word36.toOctal(iWord.getW());
        }

        FunctionCode funcCode;
        try {
            funcCode = dReg.isBasicModeEnabled()
                           ? func.getBasicModeFunctionCode()
                           : func.getExtendedModeFunctionCode();
        } catch (NoSuchElementException ex) {
            return Word36.toOctal(iWord.getW());
        }

        var sb = new StringBuilder();

        // first display field - mnemonic and optional j-field designation.
        // If there is a j-field value in the function coordinate, it is not used as a
        // partial-word designator, and thus is not displayed.
        sb.append(func.getMnemonic());
        if ((funcCode.getJField() == null) && (func.getImmediateMode())) {
            sb.append(",").append(getJFieldToken(iWord.getJ(), dReg));
        }
        while (sb.length() < 10) {
            sb.append(" ");
        }

        // Is there an a-field?
        switch (func.getAFieldSemantics()) {
            case AFieldSemantics.A_REGISTER -> sb.append("A").append(iWord.getA()).append(",");
            case AFieldSemantics.B_REGISTER -> sb.append("B").append(iWord.getA()).append(",");
            case AFieldSemantics.R_REGISTER -> sb.append("R").append(iWord.getA()).append(",");
            case AFieldSemantics.X_REGISTER -> sb.append("X").append(iWord.getA()).append(",");
        }

        if (dReg.isBasicModeEnabled()) {
            // Interpret u-field (I think there is always a u-field...)
            // If j-field is a partial word designator (jField is not set in the functionCode)
            // and is == 016 or 017, AND the x-field is zero, then the u-field includes the hiu bits.
            // Otherwise, it includes only the u-bits.
            // If we're only using u-bits, then the i-bit is indirect addressing (and gets an asterisk).
            // TODO interpret u < 0200 as GRS if not indirect addressing (and GRS allowed)
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
                sb.append(Integer.toOctalString((int)u));
            }
            if (iWord.getX() > 0) {
                sb.append(",");
                if (iWord.getH() > 0) {
                    sb.append("*");
                }
                sb.append("X").append(iWord.getX());
            }
        } else {
            // extended mode.
            // TODO interpret d as GRS for < 0200 if b is 0 (and GRS allowed)
            var jumpShiftImm = func.isJumpInstruction()
                               || func.isShiftInstruction()
                               || ((funcCode.getJField() == null) && (iWord.getJ() >= 016));
            if (jumpShiftImm) {
                // With x != 0, this is fjaxhiu. with x == 0, this is fjax[hiu].
                var x = iWord.getX();
                if (x > 0) {
                    var u = iWord.getU();
                    sb.append("0");
                    if (u != 0) {
                        sb.append(Integer.toOctalString(u));
                    }
                    sb.append(",");
                    if (iWord.getH() > 0) {
                        sb.append("*");
                    }
                    sb.append("X").append(x);
                } else {
                    var u = iWord.getU() | (iWord.getH() << 17) | (iWord.getI() << 16);
                    sb.append("0");
                    if (u != 0) {
                        sb.append(Integer.toOctalString(u));
                    }
                }
            } else {
                // Interpret d-field
                var d = iWord.getD();
                sb.append("0");
                if (d > 0) {
                    sb.append(Integer.toOctalString(d));
                }
                sb.append(",");

                // Interpret x-field
                var x = iWord.getX();
                if (x > 0) {
                    if (iWord.getH() > 0) {
                        sb.append("*");
                    }
                    sb.append("X").append(x);
                }
                sb.append(",");

                // Interpret b-field
                var breg = iWord.getB();
                if (!dReg.isBasicModeEnabled() && (dReg.getProcessorPrivilege() < 2)) {
                    breg |= (int) (iWord.getI() << 4);
                }
                sb.append("B").append(breg);
            }
        }

        return sb.toString();
    }

    protected static String getJFieldToken(
        final int jField,
        final DesignatorRegister designatorRegister
    ) {
        var qwMode = (designatorRegister != null) && designatorRegister.isQuarterWordModeEnabled();
        return switch (jField) {
            case 0_0 -> "W";
            case 0_1 -> "H2";
            case 0_2 -> "H1";
            case 0_3 -> "XH2";
            case 0_4 -> qwMode ? "Q2" : "XH1";
            case 0_5 -> qwMode ? "Q4" : "T3";
            case 0_6 -> qwMode ? "Q3" : "T2";
            case 0_7 -> qwMode ? "Q1" : "T1";
            case 0_010 -> "S6";
            case 0_011 -> "S5";
            case 0_012 -> "S4";
            case 0_013 -> "S3";
            case 0_014 -> "S2";
            case 0_015 -> "S1";
            case 0_016 -> "U";
            case 0_017 -> "XU";
            default -> "";
        };
    }
}
