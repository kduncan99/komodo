/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.operators;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.exceptions.*;
import org.junit.Test;
import static org.junit.Assert.*;

import com.kadware.em2200.minalib.dictionary.*;
import java.util.Stack;

/**
 * Unit tests for the addition operator
 */
public class Test_AdditionOperator {

    @Test
    public void simple_UnsPos_UnsPos(
    ) throws ExpressionException {
        long[] value1 = { 0, 01 };
        long[] value2 = { 0, 02 };
        long[] expValue = { 0, 03 };

        Value addend1 = new IntegerValue.Builder().setValue(value1).build();
        Value addend2 = new IntegerValue.Builder().setValue(value2).build();
        Value expected = new IntegerValue.Builder().setValue(expValue).build();

        Stack<Value> values = new Stack<>();
        values.push(addend1);
        values.push(addend2);

        Locale opLocale = new Locale(10, 20);
        Operator op = new AdditionOperator(opLocale);

        Context context = new Context();
        Diagnostics diags = new Diagnostics();
        op.evaluate(context, values, diags);

        assertEquals(1, values.size());
        Value result = values.pop();
        assertTrue(result instanceof IntegerValue);
        IntegerValue iresult = (IntegerValue)result;
        assertEquals(expected, result);
        assertFalse(result.getFlagged());
        assertEquals(Signed.None, result.getSigned());
        assertEquals(Precision.None, result.getPrecision());
        assertNull(result.getForm());
        assertNull(result.getRelocationInfo());
    }

    @Test
    public void simple_UnsPos_UnsNeg(
    ) throws ExpressionException {
        long[] value1 = { 0, 017 };
        long[] value2 = { 0_777777_777777l, 0_777777_777770l };
        long[] expValue = { 0, 010 };

        Value addend1 = new IntegerValue.Builder().setValue(value1).build();
        Value addend2 = new IntegerValue.Builder().setValue(value2).build();
        Value expected = new IntegerValue.Builder().setValue(expValue).build();

        Stack<Value> values = new Stack<>();
        values.push(addend1);
        values.push(addend2);

        Locale opLocale = new Locale(10, 20);
        Operator op = new AdditionOperator(opLocale);

        Context context = new Context();
        Diagnostics diags = new Diagnostics();
        op.evaluate(context, values, diags);

        assertEquals(1, values.size());
        Value result = values.pop();
        assertTrue(result instanceof IntegerValue);
        IntegerValue iresult = (IntegerValue)result;
        assertEquals(expected, result);
        assertFalse(result.getFlagged());
        assertEquals(Signed.None, result.getSigned());
        assertEquals(Precision.None, result.getPrecision());
        assertNull(result.getForm());
        assertNull(result.getRelocationInfo());
    }

    @Test
    public void simple_NegPos_NegNeg(
    ) throws ExpressionException {
        long[] value1 = { 0, 017 };
        long[] value2 = { 0_777777_777777l, 0_777777_777770l };
        long[] expValue = { 0_777777_777777l, 0_777777_777767l };

        Value addend1 = new IntegerValue.Builder().setValue(value1).setSigned(Signed.Negative).build();
        Value addend2 = new IntegerValue.Builder().setValue(value2).setSigned(Signed.Negative).build();
        Value expected = new IntegerValue.Builder().setValue(expValue).build();

        Stack<Value> values = new Stack<>();
        values.push(addend1);
        values.push(addend2);

        Locale opLocale = new Locale(10, 20);
        Operator op = new AdditionOperator(opLocale);

        Context context = new Context();
        Diagnostics diags = new Diagnostics();
        op.evaluate(context, values, diags);

        assertEquals(1, values.size());
        Value result = values.pop();
        assertTrue(result instanceof IntegerValue);
        IntegerValue iresult = (IntegerValue)result;
        assertEquals(expected, result);
        assertFalse(result.getFlagged());
        assertEquals(Signed.None, result.getSigned());
        assertEquals(Precision.None, result.getPrecision());
        assertNull(result.getForm());
        assertNull(result.getRelocationInfo());
    }
}
