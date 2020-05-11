/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.diagnostics.Diagnostics;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import java.util.HashSet;
import java.util.Stack;
import org.junit.Test;
import static org.junit.Assert.*;

public class Test_LeftJustificationOperator {

    @Test
    public void simple(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new StringValue.Builder().setValue("ABC").build());

        Context context = new Context(new Dictionary(), new String[0], new HashSet<>());
        Diagnostics diags = new Diagnostics();

        LineSpecifier ls = new LineSpecifier(0, 12);
        Operator op = new LeftJustificationOperator(new Locale(ls, 18));
        op.evaluate(context, valueStack);

        assertTrue(diags.getDiagnostics().isEmpty());
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.String, valueStack.peek().getType());

        StringValue vResult = (StringValue) valueStack.pop();
        assertFalse(vResult._flagged);
        assertEquals("ABC", vResult._value);
        assertEquals(ValueJustification.Left, vResult._justification);
    }
}
