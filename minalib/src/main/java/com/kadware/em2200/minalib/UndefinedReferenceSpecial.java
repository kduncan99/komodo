/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.baselib.FieldDescriptor;
import java.util.HashMap;
import java.util.Map;

/**
 * Describes a special undefined reference which the collector must resolve in a particular manner
 */
public class UndefinedReferenceSpecial extends UndefinedReference {

    public enum Type {
        LBDI,
        LBDICALL,
        LBDIREF,
    }

    public static Map<String, Type> TOKEN_TABLE = new HashMap<>();
    static {
        TOKEN_TABLE.put("LBDI$",        Type.LBDI);
        TOKEN_TABLE.put("LBDICALL$",    Type.LBDICALL);
        TOKEN_TABLE.put("LBDIREF$",     Type.LBDIREF);
    }

    public final Type _type;
    public final String _subjectLabel;

    public UndefinedReferenceSpecial(
        final FieldDescriptor fieldDescriptor,
        final boolean isNegative,
        final Type type,
        final String subjectLabel
    ) {
        super(fieldDescriptor, isNegative);
        _type = type;
        _subjectLabel = subjectLabel;
    }

    @Override
    public UndefinedReference copy(
        final boolean isNegative
    ) {
        return new UndefinedReferenceSpecial(_fieldDescriptor, isNegative, _type, _subjectLabel);
    }

    @Override
    public UndefinedReference copy(
        final FieldDescriptor newFieldDescriptor
    ) {
        return new UndefinedReferenceSpecial(newFieldDescriptor, _isNegative, _type, _subjectLabel);
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof UndefinedReferenceSpecial) {
            UndefinedReferenceSpecial refObj = (UndefinedReferenceSpecial) obj;
            return (_fieldDescriptor.equals(refObj._fieldDescriptor))
                   && (_isNegative == refObj._isNegative)
                   && (_type == refObj._type)
                   && (_subjectLabel.equals(refObj._subjectLabel));
        }
        return false;
    }

    @Override
    public String toString(
    ) {
        return String.format("[%d:%d]%s%s$->%s",
                             _fieldDescriptor._startingBit,
                             _fieldDescriptor._startingBit + _fieldDescriptor._fieldSize - 1,
                             _isNegative ? "-" : "+",
                             _type.toString(),
                             _subjectLabel);
    }
}
