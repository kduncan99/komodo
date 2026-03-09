/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib;

public class InstructionWord {

    private InstructionWord() {}

    public static long compose(final int f,
                               final int j,
                               final int a,
                               final int x,
                               final int h,
                               final int i,
                               final int u) {
        long value = ((long)f & 0_77) << 30;
        value |= ((long)j & 0_17) << 26;
        value |= ((long)a & 0_17) << 22;
        value |= ((long)x & 0_17) << 18;
        value |= ((long)h & 0_1) << 17;
        value |= ((long)i & 0_1) << 16;
        value |= u & 0_177777;
        return value;
    }

    public static long compose(final int f,
                               final int j,
                               final int a,
                               final int x,
                               final int h,
                               final int i,
                               final int b,
                               final int d) {
        long value = ((long)f & 0_77) << 30;
        value |= ((long)j & 0_17) << 26;
        value |= ((long)a & 0_17) << 22;
        value |= ((long)x & 0_17) << 18;
        value |= ((long)h & 0_1) << 17;
        value |= ((long)i & 0_1) << 16;
        value |= ((long)b & 0_17) << 12;
        value |= d & 0_007777;
        return value;
    }

    public static long getF(final long value) { return (value & 0_770000_000000L) >> 30; }
    public static long getJ(final long value) { return (value & 0_007400_000000L) >> 26; }
    public static long getA(final long value) { return (value & 0_000360_000000L) >> 22; }
    public static long getX(final long value) { return (value & 0_000017_000000L) >> 18; }
    public static long getH(final long value) { return (value & 0_000000_400000L) >> 17; }
    public static long getI(final long value) { return (value & 0_000000_200000L) >> 16; }
    public static long getU(final long value) { return value & 0_000000_177777L; }
    public static long getHIU(final long value) { return value & 0_000000_777777L; }
    public static long getB(final long value) { return (value & 0_000000_170000L) >> 12; }
    public static long getD(final long value) { return value & 0_000000_007777L; }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return resulting value
     */
    public static long setF(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_007777_777777L) | ((partialValue & 0_77L) << 30);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return resulting value
     */
    public static long setJ(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_770377_777777L) | ((partialValue & 0_17L) << 26);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return resulting value
     */
    public static long setA(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_777417_777777L) | ((partialValue & 0_17L) << 22);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return resulting value
     */
    public static long setX(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_777760_777777L) | ((partialValue & 0_17L) << 18);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return resulting value
     */
    public static long setH(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_777777_377777L) | ((partialValue & 0_1) << 17);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return resulting value
     */
    public static long setI(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_777777_577777L) | ((partialValue & 0_1) << 16);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return resulting value
     */
    public static long setU(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_777777_600000L) | (partialValue & 0_177777);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return resulting value
     */
    public static long setHIU(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_777777_000000L) | (partialValue & 0_777777);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return resulting value
     */
    public static long setXHIU(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_777760_000000L) | (partialValue & 017_777777L);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return resulting value
     */
    public static long setB(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_777777_607777L) | ((partialValue & 0_17) << 12);
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param existingValue target of the injection
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return resulting value
     */
    public static long setD(
        final long existingValue,
        final long partialValue
    ) {
        return (existingValue & 0_777777_770000L) | (partialValue & 0_007777);
    }
}
