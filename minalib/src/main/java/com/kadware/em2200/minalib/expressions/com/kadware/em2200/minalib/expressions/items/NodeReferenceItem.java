/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.com.kadware.em2200.minalib.expressions.items;

import com.kadware.em2200.baselib.FieldDescriptor;
import com.kadware.em2200.baselib.exceptions.NotFoundException;
import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.ValueDiagnostic;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.expressions.*;
import com.kadware.em2200.minalib.exceptions.ExpressionException;

/**
 * Represents a dictionary reference within an expression
 */
@SuppressWarnings("Duplicates")
public class NodeReferenceItem extends ReferenceItem {

    /**
     * constructor
     * @param locale location of this entity
     * @param reference reference for this entity
     * @param nodeSelectorExpressions expressions defining the node selectors for this reference
     */
    public NodeReferenceItem(
        final Locale locale,
        final String reference,
        final Expression[] nodeSelectorExpressions
    ) {
        super(locale, reference);
    }

    /**
     * Evaluates the reference based on the dictionary
     * @param context context of execution
     * @return true if successful, false to discontinue evaluation
     * @throws ExpressionException if something goes wrong with the process (we presume something has been posted to diagnostics)
     */
    @Override
    public Value resolve(
        final Context context
    ) throws ExpressionException {
        try {
            //  Look up the reference in the dictionary.
            //  It must be a particular type of value, else we have an expression exception.
            Value v = context.getDictionary().getValue(getReference());
            switch (v.getType()) {
                case Node:
                    return v;//TODO much more than this

                default:
                    //  This is an internal error - we shouldn't get here if the parser worked right
                    context.appendDiagnostic(new ValueDiagnostic( _locale, "Wrong value type referenced"));
                    throw new ExpressionException();
            }
        } catch (NotFoundException ex) {
            //  Shouldn't get here either - if the parser worked right.
            context.appendDiagnostic(new ValueDiagnostic( _locale, "Wrong value type referenced"));
            throw new ExpressionException();
        }
    }
}
