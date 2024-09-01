/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.oldbaselib.FieldDescriptor;

/**
 * Describes a reference to a generated literal.
 * We are one-pass, thus we generate things on the fly.
 * Literals are always placed at the end of their corresponding pools.
 * We don't know where the end of a pool is until the end of the assembly process.
 * So... when we generate a value which refers to the address of a literal,
 *      we don't yet know the lc offset of the literal we're generating.
 * So, we have to create one of these and attach it to the referring value,
 *      and resolve it after assembly is complete, but before the final location counter pools are created.
 */
public class UnresolvedReferenceToLiteral extends UnresolvedReference {

    public final int _locationCounterIndex;
    public final int _literalOffset;

    /**
     * constructor
     * @param fieldDescriptor describes the subset of the containing value, to which this reference applies
     * @param isNegative true if this value is to be arithmetically inverted when it is integrated into the containing value
     * @param locationCounterIndex index of the location counter which contains the literal of interest
     * @param literalOffset offset from the beginning of the literal sub-pool, where the literal of interest is located
     */
    public UnresolvedReferenceToLiteral(
        final FieldDescriptor fieldDescriptor,
        final boolean isNegative,
        final int locationCounterIndex,
        final int literalOffset
    ) {
        super(fieldDescriptor, isNegative);
        _locationCounterIndex = locationCounterIndex;
        _literalOffset = literalOffset;
    }

    @Override
    public UnresolvedReference copy(
        final boolean isNegative
    ) {
        return new UnresolvedReferenceToLiteral(_fieldDescriptor, isNegative, _locationCounterIndex, _literalOffset);
    }

    @Override
    public UnresolvedReference copy(
        final FieldDescriptor fieldDescriptor
    ) {
        return new UnresolvedReferenceToLiteral(fieldDescriptor, _isNegative, _locationCounterIndex, _literalOffset);
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof UnresolvedReferenceToLiteral) {
            UnresolvedReferenceToLiteral refObj = (UnresolvedReferenceToLiteral) obj;
            return (_isNegative == refObj._isNegative)
                   && (_fieldDescriptor.equals(refObj._fieldDescriptor))
                   && (_locationCounterIndex == refObj._locationCounterIndex)
                   && (_literalOffset == refObj._literalOffset);
        }
        return false;
    }

    @Override public int hashCode() {
        return (_locationCounterIndex << 12) | _literalOffset;
    }

    @Override
    public String toString() {
        return String.format("%slitPool(%d)+%d", _isNegative ? "-" : "+", _locationCounterIndex, _literalOffset);
    }
}
