/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;

/**
 * expression item containing zero or more sub-expressions.
 * represents subfields coded as:
 *      (expression)
 *      (expression, ... , expression)
 * Can be used for literal generation and for arithmetic grouping.
 */
public class ExpressionGroupItem extends OperandItem {

    //  This should contain one or more sub-expressions.
    //  Syntactically, this represents zero or more expressions, inter-delimited by commas,
    //  presented inside parenthesis.
    //
    //  Case 1:
    //  If this exists inside a context of another expression where-in it is not the only entity,
    //  it is merely a sub-expression delimited by grouping symbols, which should be evaluated
    //  entirely before being used in the containing expression.
    //  In this context, it must contain exactly one expressionl
    //
    //  Case 2:
    //  If the containing expression has no binary operators, then this entity represents a value
    //  to be placed into the literal pool, and the address of that value is returned as the
    //  value of the evaluation.
    //
    //  This entity does not know which of the above two cases apply, nor does the creating entity.
    //  Prior to evaluation, the calling code must make that determination and set the sub-expression
    //  flag accordingly - it defaults to true.
    private final Expression[] _expressions;
    boolean _isSubExpression = true;        //  true if case 1 applies (above), false for case 2.

    /**
     * Represents a grouped sub-expression.
     * It must have exactly one expression, which we evaluate and propagate the return value
     * @param context assembler context
     * @return the resulting value
     * @throws ExpressionException if something is wrong with the expression
     */
    private Value resolveForCase1(
            final Context context
    ) throws ExpressionException {
        if (_expressions.length != 1) {
            context.appendDiagnostic(
                new ErrorDiagnostic(_locale,
                                    "Expected one expression inside the grouping symbols"));
            throw new ExpressionException();
        }

        return _expressions[0].evaluate(context);
    }

    /**
     * Represents a value to be placed into the literal pool, with the result being the
     * location-counter-relative address thereof.
     * If more than one expression exists, all expressions must be IntegerValue's, and
     * the result is made up of one bitfield per expression, the sizes of which are the
     * dividend of 36 bits by the number of fields.
     * @param context assembler context
     * @return the resulting value
     * @throws ExpressionException if something is wrong with the expression
     */
    private Value resolveForCase2(
            final Context context
    ) throws ExpressionException {
        if ((_expressions.length < 1) || (_expressions.length > 36)) {
            context.appendDiagnostic(
                new ErrorDiagnostic(_locale,
                                    "Expected one to thirty-six expressions inside the literal pool specification"));
            throw new ExpressionException();
        }

        int[] fieldSizes;
        IntegerValue[] values;
        if (_expressions.length == 1) {
            int[] fs = { 36 };
            IntegerValue[] ivs = { (IntegerValue) _expressions[0].evaluate(context) };
            fieldSizes = fs;
            values = ivs;
        } else {
            values = new IntegerValue[_expressions.length];
            for (int ex = 0; ex < _expressions.length; ++ex) {
                Value v = _expressions[ex].evaluate(context);
                if (!(v instanceof IntegerValue)) {
                    context.appendDiagnostic(
                        new ErrorDiagnostic(_locale,
                                            "Bit-field values inside the literal pool specifier must be integers."));
                    throw new ExpressionException();
                }
                values[ex] = (IntegerValue) v;
            }

            fieldSizes = new int[_expressions.length];
            int fieldSize = 36 / fieldSizes.length;
            for (int fx = 0; fx < fieldSizes.length; ++fx) {
                fieldSizes[fx] = fieldSize;
            }
        }

        Form form = new Form(fieldSizes);
        return context.generate(_locale.getLineSpecifier(), context.getCurrentLitLCIndex(), form, values);
    }

    /**
     * Constructor
     * @param locale location of this item
     * @param expressions expressions contained within this group
     */
    ExpressionGroupItem(
        final Locale locale,
        final Expression[] expressions
    ) {
        super(locale);
        _expressions = expressions;
    }

    /**
     * Resolves the value of this item.
     * @param context assembler context - we don't need this
     * @return a Value representing this operand
     * @throws ExpressionException if the underlying sub-expression throws it
     */
    @Override
    public Value resolve(
        final Context context
    ) throws ExpressionException {
        return (_isSubExpression ? resolveForCase1(context) : resolveForCase2(context));
    }
}
