package com.bearsnake.komodo.engine.functions.arithmetic.fixed;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.interrupts.OperationTrapInterrupt;

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
}
