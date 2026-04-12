/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.arithmetic.decimal;

import com.bearsnake.komodo.engine.functions.FunctionUnitTest;

public abstract class TestDecimalFunction extends FunctionUnitTest {

    // Single word decimal format consists of 9 4-bit cells.
    // Each cell is a digit from 0 to 9, most significant digit first.
    // The last cell indicates the sign of the value, while the first eight indicate the magnitude.
    // See isNegative() and isPositive() for values accepted for sign.

    // Specific values we use for specifying the sign
    protected static final int NEGATIVE_SIGN = 015;
    protected static final int POSITIVE_SIGN = 014;

    protected static long decWord(
        final long c1,
        final long c2,
        final long c3,
        final long c4,
        final long c5,
        final long c6,
        final long c7,
        final long c8,
        final long c9
    ) {
        return ((c1 & 017) << 32) | ((c2 & 017) << 28) | ((c3 & 017) << 24)
               | ((c4 & 017) << 20) | ((c5 & 017) << 16) | ((c6 & 017) << 12)
               | ((c7 & 017) << 8) | ((c8 & 017) << 4) | (c9 & 017);
    }
}
