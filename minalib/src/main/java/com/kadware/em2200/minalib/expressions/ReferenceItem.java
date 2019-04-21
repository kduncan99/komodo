/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions;

import com.kadware.em2200.baselib.exceptions.*;
import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;

/**
 * Represents a dictionary reference within an expression
 */
public class ReferenceItem extends OperandItem {

    private final String _reference;

    /**
     * constructor
     * @param locale location of this entity
     * @param reference reference for this entity
     */
    public ReferenceItem(
        final Locale locale,
        final String reference
    ) {
        super(locale);
        _reference = reference;
    }

    /**
     * Evaluates the reference based on the dictionary
     * @param context context of execution
     * @param diagnostics where we post diagnostics if necessary
     * @return true if successful, false to discontinue evaluation
     * @throws ExpressionException if something goes wrong with the process (we presume something has been posted to diagnostics)
     */
    @Override
    public Value resolve(
        final Context context,
        Diagnostics diagnostics
    ) throws ExpressionException {
        try {
            //  Look up the reference in the dictionary.
            //  It must be a particular type of value, else we have an expression exception.
            Value v = context._dictionary.getValue(_reference);
            switch (v.getType()) {
                case Integer:
                case FloatingPoint:
                case String:
                    return v;

                default:
                    diagnostics.append( new ValueDiagnostic( _locale, "Wrong value type referenced" ));
                    throw new ExpressionException();
            }
        } catch ( NotFoundException ex ) {
            //  reference not found - create an IntegerValue with a value of zero
            //  and an attached positive UndefinedReference.
            IntegerValue.UndefinedReference[] refs = { new IntegerValue.UndefinedReference(  _reference, false ) };
            return new IntegerValue(false, 0, refs);
        }
    }
}
