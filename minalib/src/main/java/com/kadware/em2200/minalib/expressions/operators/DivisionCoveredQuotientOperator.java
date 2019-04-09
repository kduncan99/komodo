/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.operators;

import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import java.math.BigInteger;
import java.util.Stack;

/**
 * Class for covered-quotient operator
 */
public class DivisionCoveredQuotientOperator extends ArithmeticOperator {

    /**
     * Constructor
     * <p>
     * @param locale
     */
    public DivisionCoveredQuotientOperator(
        final Locale locale
    ) {
        super(locale);
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public final int getPrecedence(
    ) {
        return 7;
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
    public void evaluate(
        final Context context,
        Stack<Value> valueStack,
        Diagnostics diagnostics
    ) throws ExpressionException {
        try {
            Value[] operands = getTransformedOperands(valueStack, false, diagnostics);
            Value opResult;

            IntegerValue iopLeft = (IntegerValue)operands[0];
            IntegerValue iopRight = (IntegerValue)operands[1];

            //  No relocation is allowed
            if (iopLeft.getRelocationInfo() != null) {
                diagnostics.append(new RelocationDiagnostic(getLocale()));
            }
            if (iopRight.getRelocationInfo() != null) {
                diagnostics.append(new RelocationDiagnostic(getLocale()));
            }

            //  Determine precision results
            Precision precision = Precision.None;
            if ((iopLeft.getPrecision() == Precision.Double) || (iopRight.getPrecision() == Precision.Double)) {
                precision = Precision.Double;
            } else if ((iopLeft.getPrecision() == Precision.Single) || (iopRight.getPrecision() == Precision.Single)) {
                precision = Precision.Single;
            }

            //  do the math
            long[] opLeft = iopLeft.getValue();
            if (iopLeft.getSigned() == Signed.Negative) {
                OnesComplement.negate72(opLeft, opLeft);
            }

            long[] opRight = iopRight.getValue();
            if (iopLeft.getSigned() == Signed.Negative) {
                OnesComplement.negate72(opRight, opRight);
            }

            if (OnesComplement.isZero72(opRight)) {
                diagnostics.append(new TruncationDiagnostic(getLocale(), "Division by zero"));
                throw new ExpressionException();
            }

            BigInteger biLeft = OnesComplement.getNative72(opLeft);
            BigInteger biRight = OnesComplement.getNative72(opRight);
            BigInteger biResult[] = biLeft.divideAndRemainder(biRight);
            if (!biResult[1].equals(BigInteger.ZERO)) {
                biResult[0].add(BigInteger.ONE);
            }

            OnesComplement.OnesComplement72Result ocResult = new OnesComplement.OnesComplement72Result();
            OnesComplement.getOnesComplement72(biResult[0], ocResult);

            //  check for truncation
            if (ocResult._overflow) {
                diagnostics.append(new TruncationDiagnostic(getLocale(), "Arithmetic overflow"));
            } else if ((precision == Precision.Single) && (ocResult._result[0] != 0)) {
                diagnostics.append(new TruncationDiagnostic(getLocale(), "Result larger than 36 bits"));
            }

            opResult = new IntegerValue(ocResult._result, false, Signed.None, precision, null, null);
            valueStack.push(opResult);
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
