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
 * Class for bit-shift operator
 */
public class ShiftOperator extends ArithmeticOperator {

    /**
     * Constructor
     * <p>
     * @param locale
     */
    public ShiftOperator(
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
     * We do *NOT* flag T's on left shifts out of MSBit, contra MASM.
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

            IntegerValue iopLeft = (IntegerValue)operands[0];
            IntegerValue iopRight = (IntegerValue)operands[1];

            //  Relocation allowed only for the left-hand operand
            RelocationInfo relocInfo = iopLeft.getRelocationInfo();
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

            long[] result = new long[2];
            OnesComplement.copy72(iopLeft.getValue(), result);

            long[] count = new long[2];
            OnesComplement.copy72(iopRight.getValue(), count);

            boolean leftShift = true;
            if (OnesComplement.isNegative72(iopRight.getValue())) {
                //  negative shift - convert to positive shift right
                OnesComplement.negate72(count, count);
                leftShift = false;
            }

            if ((count[0] != 0) || (count[1] >= 72)) {
                result[0] = 0;
                result[1] = 0;
            } else {
                if (leftShift) {
                    OnesComplement.leftShiftLogical72(result, (int)count[1], result);
                } else {
                    OnesComplement.rightShiftLogical72(result, (int)count[1], result);
                }
            }

            valueStack.push(new IntegerValue.Builder().setValue(result)
                                                      .setPrecision(precision)
                                                      .setRelocationInfo(relocInfo)
                                                      .build());
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
