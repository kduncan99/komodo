/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.floating;

import com.bearsnake.komodo.baselib.DoubleWord36;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.functions.Function;

/**
 * Floating point representation arithmetic functions
 * Floats are represented as a signed fraction and an exponent biased across zero.
 * We refer to the signed fraction as the mantisaa, and the exponent as the characteristic.
 * The mantissa is normalized when the left-most bit is not equal to the sign bit.
 * ---
 * Single precision floating point values are arranged as follows:
 *      Bit 0: Sign Bit (1 == negative, 0 == positive)
 *      Bits 1-8: Characteristic, biased by 0200 (exponent is characteristic - 0200)
 *      Bits 9-35: Mantissa (27 bits)
 * ---
 * Double precision floating point values are arranged as follows:
 *      Bit 0: Sign Bit (1 == negative, 0 == positive)
 *      Bits 1-11: Characteristic, biased by 02000 (exponent is characteristic - 02000)
 *      Bits 12-71: Mantissa (60 bits)
 */
public abstract class FloatingFunction extends Function {

    protected FloatingFunction(String mnemonic) {
        super(mnemonic);
    }

    // single precision ------------------------------------------------------------------------------------------------------------

    protected static long construct(
        final long sign,
        final long characteristic,
        final long mantissa
    ) {
        return ((sign & 01) << 35) | ((characteristic & 0_377) << 27) | (mantissa & 0_000777_777777L);
    }

    protected static long getCharacteristic(
        final long value
    ) {
        return (value >> 27) & 0_0377;
    }

    protected static long getSinglePrecisionCharacteristicFromExponent(
        final long exponent
    ) {
        return exponent + 0200;
    }

    protected static long getExponent(
        final long value
    ) {
        return getCharacteristic(value) - 0200;
    }

    protected static long getMantissa(
        final long value
    ) {
        return value & 0_000777_777777L;
    }

    protected static long getSign(
        final long value
    ) {
        return value >> 35;
    }

    protected static boolean isNormalized(
        final long value
    ) {
        // Positive and Negative zero are considered normalized
        if (Word36.isZero(value)) {
            return true;
        }
        // else the leading bit of the mantissa must not be identical to the sign bit
        var check = value & 0_400400_000000L;
        return (check != 0) && (check != 0_400400_000000L);
    }

    protected static long normalize(
        final long value
    ) {
        var result = value;
        if (!Word36.isZero(value)) {
            var sign = getSign(value);
            var characteristic = getCharacteristic(value);
            var mantissa = getMantissa(value);
            while (mantissa >> 23 == sign) {
                mantissa = (mantissa << 1) & 0_77_777777L;
                characteristic--;
            }
            result = construct(sign, characteristic, mantissa);
        }
        return result;
    }

    // double precision ------------------------------------------------------------------------------------------------------------

    protected static void construct(
        final long[] result,
        final long sign,
        final long characteristic,
        final long mantissa
    ) {
        result[0] = ((sign & 01) << 35) | ((characteristic & 0_3777) << 24) | (mantissa >> 36);
        result[1] = mantissa & 0_000077_777777_777777_777777L;
    }

    protected static long getCharacteristic(
        final long[] value
    ) {
        return (value[0] >> 24) & 0_03777;
    }

    protected static long getDoublePrecisionCharacteristicFromExponent(
        final long exponent
    ) {
        return exponent + 02000;
    }

    protected static long getExponent(
        final long[] value
    ) {
        return getCharacteristic(value) - 02000;
    }

    protected static long getMantissa(
        final long[] value
    ) {
        return value[1] & 0_000077_777777_777777_777777L;
    }

    protected static long getSign(
        final long[] value
    ) {
        return value[0] >> 35;
    }

    protected static boolean isNormalized(
        final long[] value
    ) {
        if (DoubleWord36.isZero(value[0], value[1])) {
            return true;
        }
        var check = value[0] & 0_400040_000000L;
        return (check != 0) && (check != 0_400040_000000L);
    }

    protected static void normalize(
        final long[] value
    ) {
        if (!DoubleWord36.isZero(value[0], value[1])) {
            var sign = value[0] >> 35;
            var characteristic = (value[0] >> 24) & 0_03777;
            var mantissa = ((value[0] & 0_77_777777L) << 36) | value[1];
            while (mantissa >> 59 == sign) {
                mantissa = (mantissa << 1) & 0_77_777777_777777_777777L;
                characteristic--;
            }
            construct(value, sign, characteristic, mantissa);
        }
    }
}
