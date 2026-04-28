package com.bearsnake.komodo.engine.functions.arithmetic.fixed;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;

public abstract class FixedFunction extends Function {

    protected FixedFunction(String mnemonic) {
        super(mnemonic);
    }

    protected long add36(
        final Engine engine,
        final long addend1,
        final long addend2
    ) {
        long result;
        boolean carry = false;
        boolean overflow = false;

        if (Word36.isNegativeZero(addend1) && Word36.isNegativeZero(addend2)) {
            result = Word36.NEGATIVE_ZERO;
        } else {
            result = addend1 + addend2;
            if ((result & 01_000000_000000L) != 0) {
                result = (result & Word36.BIT_MASK) + 1;
                carry = true;
            }
            if (result == Word36.NEGATIVE_ZERO) {
                result = Word36.POSITIVE_ZERO;
            }

            var aNeg = Word36.isNegative(addend1);
            var opNeg = Word36.isNegative(addend2);
            var resNeg = Word36.isNegative(result);
            overflow = (aNeg == opNeg) && (aNeg != resNeg);
        }

        var dr = engine.getDesignatorRegister();
        dr.setCarry(carry);
        dr.setOverflow(overflow);

        return result;
    }
}
