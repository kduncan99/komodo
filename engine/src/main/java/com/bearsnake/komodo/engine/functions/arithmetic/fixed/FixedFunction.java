package com.bearsnake.komodo.engine.functions.arithmetic.fixed;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.Register;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.interrupts.OperationTrapInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;

import static com.bearsnake.komodo.engine.Constants.*;
import static com.bearsnake.komodo.engine.Constants.JFIELD_T1;
import static com.bearsnake.komodo.engine.Constants.JFIELD_T2;
import static com.bearsnake.komodo.engine.Constants.JFIELD_T3;
import static com.bearsnake.komodo.engine.Constants.JFIELD_U;
import static com.bearsnake.komodo.engine.Constants.JFIELD_W;
import static com.bearsnake.komodo.engine.Constants.JFIELD_XU;

public abstract class FixedFunction extends Function {

    protected FixedFunction(String mnemonic) {
        super(mnemonic);
    }

    /**
     * Signed ones-complement 12 bit addition
     */
    protected int add12(
        final Engine engine,
        final short addend1,
        final short addend2
    ) {
        short result;

        if ((addend1 == 0_7777) && (addend2 == 0_7777)) {
            return 0_7777;
        } else {
            result = (short) (addend1 + addend2);
            if ((result & 0_010000) != 0) {
                result = (short) ((result & 0_007777) + 1);
                engine.getDesignatorRegister().setCarry(true);
            }
            if (result == 0_007777) {
                result = 0;
            }

            var aNeg = (addend1 & 0_004000) != 0;
            var opNeg = (addend2 & 0_004000) != 0;
            var resNeg = (result & 0_004000) != 0;
            if ((aNeg == opNeg) && (aNeg != resNeg)) {
                engine.getDesignatorRegister().setOverflow(true);
                if (engine.getDesignatorRegister().isOperationTrapEnabled()) {
                    engine.postInterrupt(new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow));
                }
            }
        }

        return result;
    }

    /**
     * Signed ones-complement 18 bit addition
     */
    protected int add18(
        final Engine engine,
        final int addend1,
        final int addend2
    ) {
        int result;

        if ((addend1 == 0_777777) && (addend2 == 0_777777)) {
            return 0_777777;
        } else {
            result = addend1 + addend2;
            if ((result & 01_000000) != 0) {
                result = (result & 0_777777) + 1;
                engine.getDesignatorRegister().setCarry(true);
            }
            if (result == 0_777777) {
                result = 0;
            }

            var aNeg = (addend1 & 0_400000) != 0;
            var opNeg = (addend2 & 0_400000) != 0;
            var resNeg = (result & 0_400000) != 0;
            if ((aNeg == opNeg) && (aNeg != resNeg)) {
                engine.getDesignatorRegister().setOverflow(true);
                if (engine.getDesignatorRegister().isOperationTrapEnabled()) {
                    engine.postInterrupt(new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow));
                }
            }
        }

        return result;
    }

    /**
     * Signed ones-complement 36 bit addition
     */
    protected long add36(
        final Engine engine,
        final long addend1,
        final long addend2
    ) {
        long result;

        if (Word36.isNegativeZero(addend1) && Word36.isNegativeZero(addend2)) {
            result = Word36.NEGATIVE_ZERO;
        } else {
            result = addend1 + addend2;
            if ((result & 01_000000_000000L) != 0) {
                result = (result & Word36.BIT_MASK) + 1;
                engine.getDesignatorRegister().setCarry(true);
            }
            if (result == Word36.NEGATIVE_ZERO) {
                result = Word36.POSITIVE_ZERO;
            }

            var aNeg = Word36.isNegative(addend1);
            var opNeg = Word36.isNegative(addend2);
            var resNeg = Word36.isNegative(result);
            if ((aNeg == opNeg) && (aNeg != resNeg)) {
                engine.getDesignatorRegister().setOverflow(true);
                if (engine.getDesignatorRegister().isOperationTrapEnabled()) {
                    engine.postInterrupt(new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow));
                }
            }
        }

        return result;
    }

    /**
     * Signed ones-complement 72 bit addition
     */
    protected void add72(
        final Engine engine,
        final Register register0,   // MSWord
        final Register register1,   // LSWord
        final long operand0,        // MSWord
        final long operand1         // LSWord
    ) {
        if (register0.isNegativeZero() && register1.isNegativeZero()
            && Word36.isNegativeZero(operand0) && Word36.isNegativeZero(operand1)) {
            // leave the registers as is - they're already negative zero.
            return;
        }

        var result1 = register1.getW() + operand1;
        var result0 = register0.getW() + operand0;
        if ((result1 & 01_000000_000000L) != 0) {
            result1 &= 0_777777_777777L;
            result0++;
        }
        if ((result0 & 01_000000_000000L) != 0) {
            result0 &= 0_777777_777777L;
            result1++;
            engine.getDesignatorRegister().setCarry(true);
        }

        if (result0 == Word36.NEGATIVE_ZERO && result1 == Word36.NEGATIVE_ZERO) {
            register0.setW(0);
            register1.setW(0);
        }

        var aNeg = register0.isNegative();
        var opNeg = Word36.isNegative(operand0);
        var resNeg = Word36.isNegative(result0);
        if ((aNeg == opNeg) && (aNeg != resNeg)) {
            engine.getDesignatorRegister().setOverflow(true);
            if (engine.getDesignatorRegister().isOperationTrapEnabled()) {
                engine.postInterrupt(new OperationTrapInterrupt(OperationTrapInterrupt.Reason.FixedPointBinaryIntegerOverflow));
            }
        }

        register0.setW(result0);
        register1.setW(result1);
    }

    /**
     * Performs decrement logic for SUB1, DEC, and DEC2 instructions.
     * @param engine reference to engine
     * @param fullOperand full fullOperand at the storage location (regardless of partial word indicator)
     * @param subtrahend amount to subtract from fullOperand
     * @param checkSkip true if we should check the partial fullOperand for +/- zero before and after incrementing (skipping if not)
     */
    protected void decrement(
        final Engine engine,
        final long fullOperand,
        final int subtrahend,
        final boolean checkSkip
    ) throws ReferenceViolationInterrupt {
        var dr = engine.getDesignatorRegister();
        var ci = engine.getCurrentInstruction();
        var jf = ci.getJ();

        long finalOperand = fullOperand;
        boolean initialZero = Word36.isZero(finalOperand);

        var ones = (jf == JFIELD_W)
                   || (jf == JFIELD_XH2)
                   || (!dr.isQuarterWordModeEnabled()
                       && ((jf == JFIELD_XH1) || (jf == JFIELD_T1) || (jf == JFIELD_T2) || (jf == JFIELD_T3)));
        var fullWord = dr.isBasicModeEnabled() && engine.spGetOperandIsGRS();

        if (!fullWord) {
            finalOperand = Engine.extractPartialWord(finalOperand, jf, dr.isQuarterWordModeEnabled());
        }

        if (ones) {
            finalOperand = add36(engine, finalOperand, subtrahend ^ Word36.BIT_MASK);
        } else {
            finalOperand = (finalOperand - subtrahend) & Word36.BIT_MASK;
        }

        if ((jf != JFIELD_U) && (jf != JFIELD_XU)) {
            engine.storeToCachedAddress(finalOperand,
                                        engine.spGetOperandIsGRS(),
                                        dr.isBasicModeEnabled() ? JFIELD_W : jf,
                                        false);
        }

        boolean finalZero = Word36.isZero(finalOperand);
        if (checkSkip && !initialZero && !finalZero) {
            engine.getProgramAddressRegister().incrementProgramCounter();
        }
    }

    /**
     * Performs increment logic for ADD1, INC, and INC2 instructions.
     * @param engine reference to engine
     * @param fullOperand full fullOperand at the storage location (regardless of partial word indicator)
     * @param addend amount to add to fullOperand
     * @param checkSkip true if we should check the partial fullOperand for +/- zero before and after incrementing (skipping if not)
     */
    protected void increment(
        final Engine engine,
        final long fullOperand,
        final int addend,
        final boolean checkSkip
    ) throws ReferenceViolationInterrupt {
        var dr = engine.getDesignatorRegister();
        var ci = engine.getCurrentInstruction();
        var jf = ci.getJ();

        long finalOperand = fullOperand;
        boolean initialZero = Word36.isZero(finalOperand);

        if (engine.spGetOperandIsGRS()) {
            // full word, maybe 1's, maybe 2's.
            var ones = (jf == JFIELD_W)
                       || (jf == JFIELD_XH2)
                       || (!dr.isQuarterWordModeEnabled()
                           && ((jf == JFIELD_XH1) || (jf == JFIELD_T1) || (jf == JFIELD_T2) || (jf == JFIELD_T3)));
            if (ones) {
                finalOperand = add36(engine, finalOperand, addend);
            } else {
                finalOperand = (finalOperand + addend) & Word36.BIT_MASK;
            }
        } else {
            // storage, partial word
            finalOperand = Engine.extractPartialWord(finalOperand, jf, dr.isQuarterWordModeEnabled());
            finalOperand = add36(engine, finalOperand, addend);
        }

        if ((jf != JFIELD_U) && (jf != JFIELD_XU)) {
            engine.storeToCachedAddress(finalOperand,
                                        engine.spGetOperandIsGRS(),
                                        dr.isBasicModeEnabled() ? JFIELD_W : jf,
                                        false);
        }

        boolean finalZero = Word36.isZero(finalOperand);
        if (checkSkip && !initialZero && !finalZero) {
            engine.getProgramAddressRegister().incrementProgramCounter();
        }
    }
}
