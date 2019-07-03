/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.items;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.exceptions.ExpressionException;

/**
 * Base class for an expression item which represents an operand.
 * This could be a value, a built-in function reference, etc.
 */
public abstract class OperandItem implements IExpressionItem {

    public final Locale _locale;

    /**
     * constructor
     * @param locale where the item is found in the source code
     */
    OperandItem(
        final Locale locale
    ) {
        _locale = locale;
    }

    /**
     * Evaluates the function against the parameter list
     * @param context assembly context
     * @return true if successful, false to discontinue evaluation
     * @throws ExpressionException if something is wrong with the expression
     */
    public abstract Value resolve(
        final Context context
    ) throws ExpressionException;
}
