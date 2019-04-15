/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions;

import com.kadware.em2200.baselib.exceptions.NotFoundException;
import com.kadware.em2200.minalib.Context;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import com.kadware.em2200.minalib.diagnostics.UndefinedReferenceDiagnostic;
import com.kadware.em2200.minalib.dictionary.IntegerValue;
import com.kadware.em2200.minalib.dictionary.Value;
import com.kadware.em2200.minalib.exceptions.ExpressionException;

/**
 * Represents a dictionary reference within an expression
 */
public abstract class ReferenceItem extends OperandItem {

    private final String _reference;

    /**
     * constructor
     */
    public ReferenceItem(
        final String reference
    ) {
        _reference = reference;
    }

    /**
     * Evaluates the reference based on the dictionary
     * @param context
     * @param diagnostics
     * @return true if successful, false to discontinue evaluation
     * @throws ExpressionException
     */
    @Override
    public Value resolve(
        final Context context,
        Diagnostics diagnostics
    ) throws ExpressionException {
        try {
            return context._dictionary.getValue(_reference);
        } catch ( NotFoundException ex ) {
            //  This is an undefined reference - create an IntegerValue with a value of zero
            //  and a reference relocation item attached thereto.
            //  TODO fix reloc info object below
            return new IntegerValue.Builder().setRelocationInfo(null).build();
        }
    }
}
