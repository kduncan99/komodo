/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions;

import com.kadware.em2200.minalib.Context;
import com.kadware.em2200.minalib.Locale;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import com.kadware.em2200.minalib.dictionary.Value;
import com.kadware.em2200.minalib.exceptions.*;

/**
 * expression item containing a sub-expression (something contained inside grouping symbols)
 */
public class SubExpressionItem extends OperandItem {

    public final Expression _subExpression;

    /**
     * Constructor
     * @param locale location of this item
     * @param subExpression expression we are representing
     */
    public SubExpressionItem(
        final Locale locale,
        final Expression subExpression
    ) {
        super(locale);
        _subExpression = subExpression;
    }

    /**
     * Resolves the value of this item
     * @param context assembler context - we don't need this
     * @param diagnostics where we store any diagnostics we need to generate - we don't need this either
     * @return a Value representing this operand
     * @throws ExpressionException if the underlying sub-expression throws it
     */
    @Override
    public Value resolve(
        final Context context,
        Diagnostics diagnostics
    ) throws ExpressionException {
        return _subExpression.evaluate(context, diagnostics);
    }
}
