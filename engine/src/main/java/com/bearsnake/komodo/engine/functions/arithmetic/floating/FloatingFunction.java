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
 * ---
 * Note that a negative floating point number is represented by the ones-complement of its magnitude.
 * Hence, negative zero is all ones (as it is in fixed point).
 */
public abstract class FloatingFunction extends Function {

    protected FloatingFunction(String mnemonic) {
        super(mnemonic);
    }

    // single precision ------------------------------------------------------------------------------------------------------------

    protected static long constructSinglePrecision(
        final long signBit,
        final long absoluteCharacteristic,
        final long absoluteMantissa
    ) {
        var result = ((absoluteCharacteristic & 0_377) << 27) | (absoluteMantissa & 0_000777_777777L);
        if (signBit == 1) {
            result ^= Word36.BIT_MASK;
        }
        return result;
    }

    protected static long getSinglePrecisionCharacteristic(
        final long value
    ) {
        return (value >> 27) & 0_0377;
    }

    protected static long getSinglePrecisionCharacteristicFromExponent(
        final long exponent
    ) {
        return exponent + 0200;
    }

    protected static long getSinglePrecisionExponent(
        final long value
    ) {
        return getSinglePrecisionCharacteristic(value) - 0200;
    }

    protected static long getSinglePrecisionMantissa(
        final long value
    ) {
        return value & 0_000777_777777L;
    }

    protected static long getSinglePrecisionSign(
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
            var sign = getSinglePrecisionSign(value);
            var characteristic = getSinglePrecisionCharacteristic(value);
            var mantissa = getSinglePrecisionMantissa(value);
            while (mantissa >> 23 == sign) {
                mantissa = (mantissa << 1) & 0_77_777777L;
                characteristic--;
            }
            result = constructSinglePrecision(sign, characteristic, mantissa);
        }
        return result;
    }

    // double precision ------------------------------------------------------------------------------------------------------------

    protected static void constructDoublePrecision(
        final long[] result,
        final long signBit,
        final long absoluteCharacteristic,
        final long absoluteMantissa
    ) {
        result[0] = ((signBit & 01) << 35) | ((absoluteCharacteristic & 0_3777) << 24) | (absoluteMantissa >> 36);
        result[1] = absoluteMantissa & 0_000077_777777_777777_777777L;
    }

    protected static long getDoublePrecisionCharacteristic(
        final long[] value
    ) {
        return (value[0] >> 24) & 0_03777;
    }

    protected static long getDoublePrecisionCharacteristicFromExponent(
        final long exponent
    ) {
        return exponent + 02000;
    }

    protected static long getDoublePrecisionExponent(
        final long[] value
    ) {
        return getDoublePrecisionCharacteristic(value) - 02000;
    }

    protected static long getDoublePrecisionMantissa(
        final long[] value
    ) {
        return ((value[0] & 0_7777_7777) << 36) | (value[1] & 0_777777_777777L);
    }

    protected static long getDoublePrecisionSign(
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
            constructDoublePrecision(value, sign, characteristic, mantissa);
        }
    }
}
