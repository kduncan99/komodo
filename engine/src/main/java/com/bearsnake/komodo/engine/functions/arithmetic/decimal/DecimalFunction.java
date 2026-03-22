package com.bearsnake.komodo.engine.functions.arithmetic.decimal;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.functions.Function;

public abstract class DecimalFunction extends Function {

    // Single word decimal format consists of 9 4-bit cells.
    // Each cell is a digit from 0 to 9, most significant digit first.
    // The last cell indicates the sign of the value, while the first eight indicate the magnitude.
    // See isNegative() and isPositive() for values accepted for sign.

    // Specific values we use for specifying the sign for a decimal value.
    protected static final int NEGATIVE_SIGN = 015;
    protected static final int POSITIVE_SIGN = 014;

    protected DecimalFunction(
        final String name
    ) {
        super(name);
    }

    /**
     * Converts the decimal value in operand to a 64-bit two's complement binary value.
     * Negative zero decimal values are converted to positive zero binary.
     * @param operand decimal operand
     * @return 64-bit two's complement binary value
     */
    protected static long toBinary(
        final long operand
    ) {
        var value = ((operand >> 32) & 017) * 10000000L + ((operand >> 28) & 017) * 1000000L
                    + ((operand >> 24) & 017) * 100000L + ((operand >> 20) & 017) * 10000L
                    + ((operand >> 16) & 017) * 1000L + ((operand >> 12) & 017) * 100L
                    + ((operand >> 8) & 017) * 10 + ((operand >> 4) & 017);
        if (isNegative(operand) && (value != 0)) {
            value = -value;
        }
        return value;
    }

    /**
     * Converts the decimal value in double-word operand to a 64-bit two's complement binary value.
     * Negative zero decimal values are converted to positive zero binary.
     * @param operandHigh 9-digit high-value decimal operand
     * @param operandLow 8-digit low-value decimal operand with sign
     * @return 64-bit two's complement binary value
     */
    protected static long doubleToBinary(
        final long operandHigh,
        final long operandLow
    ) {
        var value0 = ((operandHigh >> 32) & 017) * 100000000L + ((operandHigh >> 28) & 017) * 10000000L
                     + ((operandHigh >> 24) & 017) * 1000000L + ((operandHigh >> 20) & 017) * 100000L
                     + ((operandHigh >> 16) & 017) * 10000L + ((operandHigh >> 12) & 017) * 1000L
                     + ((operandHigh >> 8) & 017) * 100 + ((operandHigh >> 4) & 017) * 10 + (operandHigh & 017);

        var value1 = ((operandLow >> 32) & 017) * 10000000L + ((operandLow >> 28) & 017) * 1000000L
                     + ((operandLow >> 24) & 017) * 100000L + ((operandLow >> 20) & 017) * 10000L
                     + ((operandLow >> 16) & 017) * 1000L + ((operandLow >> 12) & 017) * 100L
                     + ((operandLow >> 8) & 017) * 10 + ((operandLow >> 4) & 017);

        var value = (value0 * 100000000L) + value1;

        if (isNegative(operandLow) && (value != 0)) {
            value = -value;
        }

        return value;
    }

    /**
     * Converts the binary value in the operand to a decimal value, stored in the decimal parameter.
     * @param binary 64-bit two's complement binary value
     * @param decimal decimal value to store the result
     * @return true if the binary value cannot fit in the decimal value (overflow condition)
     */
    protected static boolean toDecimal(
        final long binary,
        final Word36 decimal
    ) {
        var sign = binary < 0 ? NEGATIVE_SIGN : POSITIVE_SIGN;
        var magnitude = Math.abs(binary);
        long value = (sign); // bottom 8 decimal digits and sign digit

        var shift = 4;
        var over = false;
        while (magnitude != 0) {
            var digit = magnitude % 10;
            magnitude /= 10;
            value |= (digit & 017) << shift;

            shift += 4;
            if (shift == 36) {
                over = true;
                break;
            }
        }

        decimal.setW(value);
        return over;
    }

    /**
     * Converts the binary value in the operand to a double-word decimal value, stored in the decimal parameter.
     * @param binary 64-bit two's complement binary value
     * @param decimalHigh decimal value to store the 9 high-order digits
     * @param decimalLow decimal value to store the 8 low-order digits with the sign
     * @return true if the binary value cannot fit in the decimal value (overflow condition)
     */
    protected static boolean doubleToDecimal(
        final long binary,
        final Word36 decimalHigh,
        final Word36 decimalLow
    ) {
        var sign = binary < 0 ? NEGATIVE_SIGN : POSITIVE_SIGN;
        var magnitude = Math.abs(binary);
        long high = 0; // top 9 digits
        long low = (sign); // bottom 8 decimal digits and sign digit

        var shift = 4;
        var over = false;
        var wx = 1;
        while (magnitude > 0) {
            var digit = magnitude % 10;
            magnitude /= 10;

            if (wx == 1) {
                low |= (digit & 017) << shift;
            } else {
                high |= (digit & 017) << shift;
            }

            shift += 4;
            if (shift == 36) {
                if (wx == 0) {
                    if (magnitude > 0) {
                        over = true;
                    }
                    break;
                }
                shift = 0;
                wx = 0;
            }
        }

        decimalHigh.setW(high);
        decimalLow.setW(low);
        return over;
    }

    protected static boolean isNegative(
        final long operand
    ) {
        return ((operand & 01) == 01) && ((operand & 017) != 017);
    }

    protected static boolean isPositive(
        final long operand
    ) {
        return ((operand & 01) == 0) || ((operand & 017) == 017);
    }
}
