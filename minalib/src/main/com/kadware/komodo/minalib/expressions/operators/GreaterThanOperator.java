/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.Context;
import com.kadware.komodo.minalib.Locale;
import com.kadware.komodo.minalib.diagnostics.FormDiagnostic;
import com.kadware.komodo.minalib.diagnostics.RelocationDiagnostic;
import com.kadware.komodo.minalib.dictionary.IntegerValue;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import com.kadware.komodo.minalib.exceptions.FormException;
import com.kadware.komodo.minalib.exceptions.RelocationException;
import com.kadware.komodo.minalib.exceptions.TypeException;
import java.util.Stack;

/**
 * Class for greater-than operator
 */
public class GreaterThanOperator extends RelationalOperator {

    public GreaterThanOperator(Locale locale) { super(locale); }

    /**
     * Evaluator
     * @param context current contextual information one of our subclasses might need to know
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @throws ExpressionException if something goes wrong with the process
     */
    @Override
    public void evaluate(
        final Context context,
        Stack<Value> valueStack
    ) throws ExpressionException {
        try {
            Value[] operands = getTransformedOperands(valueStack, context.getDiagnostics());
            int result = (operands[0].compareTo(operands[1]) > 0) ? 1 : 0;
            valueStack.push(new IntegerValue.Builder().setValue(result).build());
        } catch (FormException ex) {
            //  thrown by compareTo() - we need to post a diag
            context.appendDiagnostic(new FormDiagnostic(_locale));
            throw new ExpressionException();
        } catch (RelocationException ex) {
            //  thrown by compareTo() - we need to post a diag
            context.appendDiagnostic(new RelocationDiagnostic(_locale));
            throw new ExpressionException();
        } catch (TypeException ex) {
            //  thrown by getTransformedOperands() - diagnostic already posted
            //  can be thrown by compareTo() - but won't be because we already prevented it in the previous call
            throw new ExpressionException();
        }
    }
}
