/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.builtInFunctions;

import com.kadware.em2200.baselib.FieldDescriptor;
import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import com.kadware.em2200.minalib.expressions.*;

/**
 * Establishes an external reference which the Linker will replace
 * with the BDI of the given location counter index.
 */
public class BDIFunction extends BuiltInFunction {

    /**
     * Constructor
     * @param locale location of the text for the function
     * @param argumentExpressions argument expressions
     */
    public BDIFunction(
        final Locale locale,
        final Expression[] argumentExpressions
    ) {
        super(locale, argumentExpressions);
    }

    /**
     * Getter
     * @return the function name
     */
    @Override
    public String getFunctionName(
    ) {
        return "$BDI";
    }

    /**
     * Getter
     * @return max arguments we expect
     */
    @Override
    public int getMaximumArguments(
    ) {
        return 1;
    }

    /**
     * Getter
     * @return min arguments we expect
     */
    @Override
    public int getMinimumArguments(
    ) {
        return 1;
    }

    /**
     * Evaluator
     * @param context evaluation-time contextual information
     * @return Value object representing the result of the evaluation
     * @throws ExpressionException if something goes wrong with the evaluation process
     */
    @Override
    public Value evaluate(
        final Context context
    ) throws ExpressionException {
        Value[] arguments = evaluateArguments(context);
        if (!(arguments[0] instanceof IntegerValue)) {
            context.appendDiagnostic(this.getValueDiagnostic(1));
            throw new ExpressionException();
        }

        int lcIndex = (int)((IntegerValue) arguments[0])._value;
        if ((lcIndex < 0) || (lcIndex > 063)) {
            context.appendDiagnostic(this.getValueDiagnostic(1));
            throw new ExpressionException();
        }

        String ref = String.format("%s_LC$BDI_%d", context.getModuleName(), lcIndex);
        UndefinedReference[] refs = {
            new UndefinedReferenceToLabel(new FieldDescriptor(0, 26), false, ref),
        };

        return new IntegerValue(false, 0, refs);
    }
}
