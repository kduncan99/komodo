/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.items;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.Form;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.diagnostics.ErrorDiagnostic;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.expressions.Expression;
import java.util.Arrays;

/**
 * expression item containing zero or more sub-expressions.
 * represents subfields coded as:
 *      (expression)
 *      (expression, ... , expression)
 * Can be used for literal generation and for arithmetic grouping.
 */
public class ExpressionGroupItem extends OperandItem {

    //  This should contain one or more sub-expressions.
    //  Syntactically, this represents one or more expressions, inter-delimited by commas, presented inside parenthesis.
    //
    //  Case 1:
    //  If this exists inside a context of another expression where-in it is not the only entity,
    //  it is merely a sub-expression delimited by grouping symbols, which should be evaluated
    //  entirely before being used in the containing expression.
    //  In this context, it must contain exactly one expression.
    //
    //  Case 2:
    //  If the containing expression has no binary operators, then this entity represents a value
    //  to be placed into the current literal pool, and the address of that value is returned as the
    //  value of the evaluation (via a suitable UnresolvedReferenceToLiteral object).
    //
    //  This entity does not know which of the above two cases apply, nor does the creating entity.
    //  Prior to evaluation, the calling code must make that determination and set the sub-expression
    //  flag accordingly - it defaults to true.

    private final Expression[] _expressions;
    private boolean _isSubExpression = false;       //  true if case 1 applies (above), false for case 2.

    /**
     * Constructor
     * @param locale location of this item
     * @param expressions expressions contained within this group
     */
    public ExpressionGroupItem(
        final Locale locale,
        final Expression[] expressions
    ) {
        super(locale);
        _expressions = expressions;
    }

    /**
     * Resolves the value of this item.
     */
    @Override
    public Value resolve(
        final Assembler assembler
    ) throws ExpressionException {
        return (_isSubExpression ? resolveForCase1(assembler) : resolveForCase2(assembler));
    }

    /**
     * Represents a grouped sub-expression.
     * It must have exactly one expression, which we evaluate and propagate the return value
     */
    private Value resolveForCase1(
        final Assembler assembler
    ) throws ExpressionException {
        if (_expressions.length != 1) {
            assembler.appendDiagnostic(new ErrorDiagnostic(_locale,
                                                           "Expected one expression inside the grouping symbols"));
            throw new ExpressionException();
        }

        return _expressions[0].evaluate(assembler);
    }

    /**
     * Represents a value to be placed into the literal pool, with the result being the
     * location-counter-relative address thereof.
     * If more than one expression exists, all expressions must be IntegerValues, and
     * the result is made up of one bitfield per expression, the sizes of which are the
     * dividend of 36 bits by the number of fields.
     */
    private Value resolveForCase2(
        final Assembler assembler
    ) throws ExpressionException {
        if ((_expressions.length < 1) || (_expressions.length > 36)) {
            assembler.appendDiagnostic(
                new ErrorDiagnostic(_locale,
                                    "Expected one to thirty-six expressions inside the literal pool specification"));
            throw new ExpressionException();
        }

        int[] fieldSizes;
        IntegerValue[] values;
        if (_expressions.length == 1) {
            int[] fs = { 36 };
            IntegerValue[] ivs = { (IntegerValue) _expressions[0].evaluate(assembler) };
            fieldSizes = fs;
            values = ivs;
        } else {
            values = new IntegerValue[_expressions.length];
            for (int ex = 0; ex < _expressions.length; ++ex) {
                Value v = _expressions[ex].evaluate(assembler);
                if (!(v instanceof IntegerValue)) {
                    assembler.appendDiagnostic(
                        new ErrorDiagnostic(_locale,
                                            "Bit-field values inside the literal pool specifier must be integers."));
                    throw new ExpressionException();
                }
                values[ex] = (IntegerValue) v;
            }

            fieldSizes = new int[_expressions.length];
            int fieldSize = 36 / fieldSizes.length;
            Arrays.fill(fieldSizes, fieldSize);
        }

        Form form = new Form(fieldSizes);
        return assembler.getGeneratedPools().generateLiteral(assembler.getTopLevelTextLine(),
                                                             _locale,
                                                             assembler.getCurrentLiteralLCIndex(),
                                                             form,
                                                             values,
                                                             assembler);
    }

    /**
     * Determines the behavior of this object at resolution time
     */
    public void setIsSubExpression(
        final boolean value
    ) {
        _isSubExpression = value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Expression e : _expressions) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(e.toString());
        }
        return "ExpressionGroupItem:(" + sb.toString() + ")";
    }
}
