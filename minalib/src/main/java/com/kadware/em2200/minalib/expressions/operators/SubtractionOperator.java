/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.operators;

import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import java.util.Stack;

/**
 * Class for subtraction operator
 */
public class SubtractionOperator extends ArithmeticOperator {

    /**
     * Constructor
     * <p>
     * @param locale
     */
    public SubtractionOperator(
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
        return 6;
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
            Value[] operands = getTransformedOperands(valueStack, true, diagnostics);
            Value opResult;

            if (operands[0].getType() == ValueType.Integer) {
                IntegerValue iopLeft = (IntegerValue)operands[0];
                IntegerValue iopRight = (IntegerValue)operands[1];

                //  check for relocatable incompatibility, and determine value (if any) for the result.
                //  The minuend (on the left) may have reloc info.  The subtrahend (on the right) may not.
                RelocationInfo relocInfo = iopLeft.getRelocationInfo();
                if (iopRight.getRelocationInfo() != null) {
                    diagnostics.append(new RelocationDiagnostic(getLocale(), "Subtrahend relocation info lost"));
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
                if (iopLeft.getSigned() != Signed.Negative) {
                    OnesComplement.negate72(opRight, opRight);
                }

                OnesComplement.Add72Result ocResult = new OnesComplement.Add72Result();
                OnesComplement.add72(opLeft, opRight, ocResult);

                //  check for truncation
                if (ocResult._overflow) {
                    diagnostics.append(new TruncationDiagnostic(getLocale(), "Arithmetic overflow"));
                } else if ((precision == Precision.Single) && (ocResult._sum[0] != 0)) {
                    diagnostics.append(new TruncationDiagnostic(getLocale(), "Result larger than 36 bits"));
                }

                opResult = new IntegerValue(ocResult._sum, false, Signed.None, precision, null, relocInfo);
            } else {
                //  must be floating point
                FloatingPointValue iopLeft = (FloatingPointValue)operands[0];
                FloatingPointValue iopRight = (FloatingPointValue)operands[1];

                //  Determine precision results
                Precision precision = Precision.None;
                if ((iopLeft.getPrecision() == Precision.Double) || (iopRight.getPrecision() == Precision.Double)) {
                    precision = Precision.Double;
                } else if ((iopLeft.getPrecision() == Precision.Single) || (iopRight.getPrecision() == Precision.Single)) {
                    precision = Precision.Single;
                }

                double opLeft = iopLeft.getSigned() == Signed.Negative ? (0 - iopLeft.getValue()) : iopLeft.getValue();
                double opRight = iopRight.getSigned() == Signed.Negative ? (0 - iopRight.getValue()) : iopRight.getValue();
                double result = opLeft - opRight;
                opResult = new FloatingPointValue(result, false, Signed.None, Precision.None);
            }

            valueStack.push(opResult);
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
