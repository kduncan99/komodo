/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.operators;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.ExpressionException;
import java.util.Stack;

/**
 * Base class for infix logical operators
 */
public abstract class LogicalOperator extends Operator {

    /**
     * Constructor
     * <p>
     * @param locale
     */
    public LogicalOperator(
        final Locale locale
    ) {
        super(locale);
    }

    /**
     * Evaluator
     * <p>
     * @param context current contextual information one of our subclasses might need to know
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @param diagnostics where we append diagnostics if necessary
     * <p>
     * @throws ExpressionException if something goes wrong with the process
     */
    @Override
    public abstract void evaluate(
        final Context context,
        Stack<Value> valueStack,
        Diagnostics diagnostics
    ) throws ExpressionException;

    /**
     * Retrieves the type of this operator
     * <p>
     * @return
     */
    @Override
    public final Type getType(
    ) {
        return Type.Infix;
    }

    /**
     * If both values have forms and the forms are the same, return that form.
     * If either value has a form and the other doesn't, return that form.
     * Otherwise return null
     * <p>
     * @param leftValue
     * @param rightValue
     * <p>
     * @return
     */
    protected static Form selectMatchingOrOnlyForm(
        final Value leftValue,
        final Value rightValue
    ) {
        Form leftForm = leftValue.getForm();
        Form rightForm = rightValue.getForm();
        if ((leftForm != null) && (rightForm != null)) {
            if (leftForm.equals(rightForm)) {
                return leftForm;
            }
        } else {
            if (leftForm != null) {
                return leftForm;
            } else if (rightForm != null) {
                return rightForm;
            }
        }

        return null;
    }
}
