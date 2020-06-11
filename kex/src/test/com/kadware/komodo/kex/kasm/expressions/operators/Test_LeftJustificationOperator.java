/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.operators;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.LineSpecifier;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.dictionary.StringValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.dictionary.ValueJustification;
import com.kadware.komodo.kex.kasm.dictionary.ValueType;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import java.util.Stack;
import org.junit.Test;
import static org.junit.Assert.*;

public class Test_LeftJustificationOperator {

    @Test
    public void simple(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new StringValue.Builder().setValue("ABC").build());

        Assembler asm = new Assembler.Builder().build();

        LineSpecifier ls = new LineSpecifier(0, 12);
        Operator op = new LeftJustificationOperator(new Locale(ls, 18));
        op.evaluate(asm, valueStack);

        assertTrue(asm.getDiagnostics().isEmpty());
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.String, valueStack.peek().getType());

        StringValue vResult = (StringValue) valueStack.pop();
        assertFalse(vResult._flagged);
        assertEquals("ABC", vResult._value);
        assertEquals(ValueJustification.Left, vResult._justification);
    }
}
