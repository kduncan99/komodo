/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions;

import com.kadware.em2200.minalib.Context;
import com.kadware.em2200.minalib.Locale;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import com.kadware.em2200.minalib.dictionary.Value;
import com.kadware.em2200.minalib.dictionary.ValueType;
import com.kadware.em2200.minalib.exceptions.*;

/**
 * expression item containing zero or more sub-expressions.
 * represents subfields coded as:
 *      (expression)
 *      (expression, ... , expression)
 * Can be used for literal generation and for arithmetic grouping.
 */
public class SubGroupItem extends OperandItem {

    //  This should contain one or more sub-expressions.
    //  If it contains multiple entities they should all resolve to integer values.
    //  The resulting values will represent equally-sized bit fields in a single word integer value result.
    //  If it contains a single entity, that entity can be any type.
    public final Expression[] _subExpressions;

    /**
     * Constructor
     * @param locale location of this item
     * @param subExpressions expressions contained within this group
     */
    public SubGroupItem(
        final Locale locale,
        final Expression[] subExpressions
    ) {
        super(locale);
        _subExpressions = subExpressions;
    }

    /**
     * Resolves the value of this item.
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
//        int entityCount = _subExpressions.length;
//        if (entityCount == 1) {
//            return
//        }
//        Value[] result = new Value[entityCount];
//        for (int vx = 0; vx < entityCount; ++vx) {
//            result[vx] = _subExpressions[vx].evaluate(context, diagnostics);
//            if ((result[vx].getType() != ValueType.Integer) && (entityCount > 1)) {
//
//            }
//        }
//
//        return result;
        return null;//TODO
    }
}
