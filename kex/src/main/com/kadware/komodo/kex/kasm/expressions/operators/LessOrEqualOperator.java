/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.operators;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.diagnostics.FormDiagnostic;
import com.kadware.komodo.kex.kasm.diagnostics.RelocationDiagnostic;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.exceptions.FormException;
import com.kadware.komodo.kex.kasm.exceptions.RelocationException;
import com.kadware.komodo.kex.kasm.exceptions.TypeException;
import java.util.Stack;

/**
 * Class for less-than-or-equal operator
 */
public class LessOrEqualOperator extends RelationalOperator {

    public LessOrEqualOperator(Locale locale) { super(locale); }

    /**
     * Evaluator
     * @param assembler context
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @throws ExpressionException if something goes wrong with the process
     */
    @Override
    public void evaluate(
        final Assembler assembler,
        final Stack<Value> valueStack
    ) throws ExpressionException {
        try {
            Value[] operands = getTransformedOperands(valueStack, assembler);
            int result = (operands[0].compareTo(operands[1]) <= 0) ? 1 : 0;
            valueStack.push(new IntegerValue.Builder().setValue(result).build());
        } catch (FormException ex) {
            //  thrown by compareTo() - we need to post a diag
            assembler.appendDiagnostic(new FormDiagnostic(_locale));
            throw new ExpressionException();
        } catch (RelocationException ex) {
            //  thrown by compareTo() - we need to post a diag
            assembler.appendDiagnostic(new RelocationDiagnostic(_locale));
            throw new ExpressionException();
        } catch (TypeException ex) {
            //  thrown by getTransformedOperands() - diagnostic already posted
            //  can be thrown by compareTo() - but won't be because we already prevented it in the previous call
            throw new ExpressionException();
        }
    }
}
