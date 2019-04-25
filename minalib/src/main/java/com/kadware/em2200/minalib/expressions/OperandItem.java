/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import com.kadware.em2200.minalib.dictionary.Value;
import com.kadware.em2200.minalib.exceptions.ExpressionException;

/**
 * Base class for an expression item which represents an operand.
 * This could be a value, a built-in function reference, etc.
 */
public abstract class OperandItem implements IExpressionItem {

    final Locale _locale;

    OperandItem(
        final Locale locale
    ) {
        _locale = locale;
    }

    public abstract Value resolve(
        final Context context,
        Diagnostics diagnostics
    ) throws ExpressionException;
}
