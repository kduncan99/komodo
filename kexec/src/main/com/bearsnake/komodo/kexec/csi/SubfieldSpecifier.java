/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

public class SubfieldSpecifier {

    private final int _fieldIndex;
    private final int _subfieldIndex;

    public SubfieldSpecifier(
        final int fieldIndex,
        final int subfieldIndex
    ) {
        _fieldIndex = fieldIndex;
        _subfieldIndex = subfieldIndex;
    }

    public final int getFieldIndex() {
        return _fieldIndex;
    }

    public final int getSubfieldIndex() {
        return _subfieldIndex;
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        return ((obj instanceof SubfieldSpecifier ss)
            && (ss._subfieldIndex == _subfieldIndex)
            && (ss._fieldIndex == _fieldIndex));
    }

    @Override
    public int hashCode() {
        return _fieldIndex << 8 | _subfieldIndex;
    }

    @Override
    public String toString() {
        return "[fld=" + _fieldIndex + ", subFld=" + _subfieldIndex + "]";
    }
}
