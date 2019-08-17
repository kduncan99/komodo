/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib;

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
        final boolean isNegative,
        final Type type,
        final String subjectLabel
    ) {
        super(isNegative);
        _type = type;
        _subjectLabel = subjectLabel;
    }

    @Override
    public UndefinedReference copy(
        final boolean isNegative
    ) {
        return new UndefinedReferenceSpecial(isNegative, _type, _subjectLabel);
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof UndefinedReferenceSpecial) {
            UndefinedReferenceSpecial refObj = (UndefinedReferenceSpecial) obj;
            return (_isNegative == refObj._isNegative)
                   && (_type == refObj._type)
                   && (_subjectLabel.equals(refObj._subjectLabel));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return _type.hashCode() & _subjectLabel.hashCode();
    }

    @Override
    public String toString(
    ) {
        return String.format("%s%s$->%s",
                             _isNegative ? "-" : "+",
                             _type.toString(),
                             _subjectLabel);
    }
}
