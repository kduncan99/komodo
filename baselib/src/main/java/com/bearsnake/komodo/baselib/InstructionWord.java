/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib;

public class InstructionWord {

    private long _value;

    public InstructionWord() {
        _value = 0;
    }

    public void compose(
        final int f,
        final int j,
        final int a,
        final int x,
        final int h,
        final int i,
        final int u
    ) {
        _value = ((long)f & 0_77) << 30;
        _value |= ((long)j & 0_17) << 26;
        _value |= ((long)a & 0_17) << 22;
        _value |= ((long)x & 0_17) << 18;
        _value |= ((long)h & 0_1) << 17;
        _value |= ((long)i & 0_1) << 16;
        _value |= u & 0_177777;
    }

    public void compose(
        final int f,
        final int j,
        final int a,
        final int x,
        final int h,
        final int i,
        final int b,
        final int d
    ) {
        _value = ((long)f & 0_77) << 30;
        _value |= ((long)j & 0_17) << 26;
        _value |= ((long)a & 0_17) << 22;
        _value |= ((long)x & 0_17) << 18;
        _value |= ((long)h & 0_1) << 17;
        _value |= ((long)i & 0_1) << 16;
        _value |= ((long)b & 0_17) << 12;
        _value |= d & 0_007777;
    }

    public long getF() { return (_value & 0_770000_000000L) >> 30; }
    public long getJ() { return (_value & 0_007400_000000L) >> 26; }
    public long getA() { return (_value & 0_000360_000000L) >> 22; }
    public long getX() { return (_value & 0_000017_000000L) >> 18; }
    public long getH() { return (_value & 0_000000_400000L) >> 17; }
    public long getI() { return (_value & 0_000000_200000L) >> 16; }
    public long getU() { return _value & 0_000000_177777L; }
    public long getHIU() { return _value & 0_000000_777777L; }
    public long getB() { return (_value & 0_000000_170000L) >> 12; }
    public long getD() { return _value & 0_000000_007777L; }
    public long getW() { return _value; }


    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return this object
     */
    public InstructionWord setF(
        final long partialValue
    ) {
        _value = (_value & 0_007777_777777L) | ((partialValue & 0_77L) << 30);
        return this;
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return this object
     */
    public InstructionWord setJ(
        final long partialValue
    ) {
        _value = (_value & 0_770377_777777L) | ((partialValue & 0_17L) << 26);
        return this;
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return this object
     */
    public InstructionWord setA(
        final long partialValue
    ) {
        _value = (_value & 0_777417_777777L) | ((partialValue & 0_17L) << 22);
        return this;
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return this object
     */
    public InstructionWord setX(
        final long partialValue
    ) {
        _value = (_value & 0_777760_777777L) | ((partialValue & 0_17L) << 18);
        return this;
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return this object
     */
    public InstructionWord setH(
        final long partialValue
    ) {
        _value = (_value & 0_777777_377777L) | ((partialValue & 0_1) << 17);
        return this;
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return this object
     */
    public InstructionWord setI(
        final long partialValue
    ) {
        _value = (_value & 0_777777_577777L) | ((partialValue & 0_1) << 16);
        return this;
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return this object
     */
    public InstructionWord setU(
        final long partialValue
    ) {
        _value = (_value & 0_777777_600000L) | (partialValue & 0_177777);
        return this;
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return this object
     */
    public InstructionWord setHIU(
        final long partialValue
    ) {
        _value = (_value & 0_777777_000000L) | (partialValue & 0_777777);
        return this;
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return this object
     */
    public InstructionWord setXHIU(
        final long partialValue
    ) {
        _value = (_value & 0_777760_000000L) | (partialValue & 017_777777L);
        return this;
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return this object
     */
    public InstructionWord setB(
        final long partialValue
    ) {
        _value = (_value & 0_777777_607777L) | ((partialValue & 0_17) << 12);
        return this;
    }

    /**
     * Injects a new value into a particular partial-value subset of a given existing value
     * @param partialValue the value to be replaced into a portion of the existing value
     * @return this object
     */
    public InstructionWord setD(
        final long partialValue
    ) {
        _value = (_value & 0_777777_770000L) | (partialValue & 0_007777);
        return this;
    }

    /**
     * Sets the entire word from the given value, masking out any bits not within range
     * @param value the new value
     * @return this object
     */
    public InstructionWord setW(final long value) {
        _value = value & 0_777777_777777L;
        return this;
    }
}
