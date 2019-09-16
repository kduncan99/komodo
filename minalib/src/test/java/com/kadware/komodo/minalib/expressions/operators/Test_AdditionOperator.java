/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.baselib.FieldDescriptor;
import com.kadware.komodo.baselib.FloatingPointComponents;
import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.diagnostics.Diagnostics;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import org.junit.Test;
import static org.junit.Assert.*;
import java.math.BigInteger;
import java.util.Stack;

public class Test_AdditionOperator {

    @Test
    public void simple(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new IntegerValue.Builder().setValue(25).setFlagged(true).build());
        valueStack.push(new IntegerValue.Builder().setValue(10098).setFlagged(true).build());

        Context context = new Context(new Dictionary(), new String[0], "TEST");
        Diagnostics diags = new Diagnostics();

        LineSpecifier ls = new LineSpecifier(0, 12);
        Operator op = new AdditionOperator(new Locale(ls, 18));
        op.evaluate(context, valueStack);

        assertTrue(diags.getDiagnostics().isEmpty());
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.Integer, valueStack.peek().getType());

        IntegerValue vResult = (IntegerValue) valueStack.pop();
        assertFalse(vResult._flagged);
        assertEquals(BigInteger.valueOf(25 + 10098), vResult._value.get());
    }

    @Test
    public void simple_conversion(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new IntegerValue.Builder().setValue(25).setFlagged(true).build());
        valueStack.push(new FloatingPointValue.Builder().setValue(new FloatingPointComponents(2003.125)).build());

        Context context = new Context(new Dictionary(), new String[0], "TEST");
        Diagnostics diags = new Diagnostics();

        LineSpecifier ls = new LineSpecifier(0, 12);
        Operator op = new AdditionOperator(new Locale(ls, 18));
        op.evaluate(context, valueStack);

        assertTrue(diags.getDiagnostics().isEmpty());
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.FloatingPoint, valueStack.peek().getType());

        FloatingPointValue vResult = (FloatingPointValue) valueStack.pop();
        assertFalse(vResult._flagged);
        assertEquals(0, new FloatingPointComponents(25.0 + 2003.125).compare(vResult._value, 12));
    }

    @Test
    public void with_references(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        UndefinedReference[] refs1 = {
            new UndefinedReferenceToLocationCounter(new FieldDescriptor(0,18), false, 5),
            new UndefinedReferenceToLocationCounter(new FieldDescriptor(18,18), false, 7),
            new UndefinedReferenceToLocationCounter(new FieldDescriptor(18,18), false, 7),
            new UndefinedReferenceToLabel(new FieldDescriptor(0, 18),false,"FEE"),
            new UndefinedReferenceToLabel(new FieldDescriptor(18, 18),false,"FOO"),
            new UndefinedReferenceToLabel(FieldDescriptor.W, false, "OMNI"),
        };
        UndefinedReference[] refs2 = {
            new UndefinedReferenceToLocationCounter(new FieldDescriptor(0,18), false, 5),
            new UndefinedReferenceToLocationCounter(new FieldDescriptor(18,18), true, 7),
            new UndefinedReferenceToLabel(new FieldDescriptor(0, 18),false,"FEE"),
            new UndefinedReferenceToLabel(new FieldDescriptor(18, 18),true,"FOO"),
            new UndefinedReferenceToLabel(FieldDescriptor.W, false, "OMNI"),
        };
        UndefinedReference[] expectedRefs = {
            new UndefinedReferenceToLocationCounter(new FieldDescriptor(0,18), false, 5),
            new UndefinedReferenceToLocationCounter(new FieldDescriptor(0,18), false, 5),
            new UndefinedReferenceToLocationCounter(new FieldDescriptor(18,18), false, 7),
            new UndefinedReferenceToLabel(new FieldDescriptor(0, 18),false,"FEE"),
            new UndefinedReferenceToLabel(new FieldDescriptor(0, 18),false,"FEE"),
            new UndefinedReferenceToLabel(FieldDescriptor.W, false, "OMNI"),
            new UndefinedReferenceToLabel(FieldDescriptor.W, false, "OMNI"),
        };

        DoubleWord36 dwValue1 = new DoubleWord36(BigInteger.valueOf(25));
        DoubleWord36 dwValue2 = new DoubleWord36(DoubleWord36.getOnesComplement(-25));

        IntegerValue addend1 = new IntegerValue.Builder().setValue(dwValue1).setReferences(refs1).build();
        IntegerValue addend2 = new IntegerValue.Builder().setValue(dwValue2).setReferences(refs2).build();
        IntegerValue expected = new IntegerValue.Builder().setValue(0).setReferences(expectedRefs).build();

        valueStack.push(addend1);
        valueStack.push(addend2);

        Context context = new Context(new Dictionary(), new String[0], "TEST");
        Diagnostics diags = new Diagnostics();

        LineSpecifier ls = new LineSpecifier(0, 12);
        Operator op = new AdditionOperator(new Locale(ls, 18));
        op.evaluate(context, valueStack);

        assertTrue(diags.getDiagnostics().isEmpty());
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.Integer, valueStack.peek().getType());

        IntegerValue vResult = (IntegerValue) valueStack.pop();
        assertFalse(vResult._flagged);
        assertEquals(expected, vResult);
    }

    @Test
    public void with_forms(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new IntegerValue.Builder().setValue(002000200020L).setFlagged(true).build());
        valueStack.push(new IntegerValue.Builder().setValue(003000331111L).setFlagged(true).build());
        IntegerValue expected = new IntegerValue.Builder().setValue(005000531131L).build();

        Context context = new Context(new Dictionary(), new String[0], "TEST");
        Diagnostics diags = new Diagnostics();

        LineSpecifier ls = new LineSpecifier(0, 12);
        Operator op = new AdditionOperator(new Locale(ls, 18));
        op.evaluate(context, valueStack);

        assertTrue(diags.getDiagnostics().isEmpty());
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.Integer, valueStack.peek().getType());

        IntegerValue vResult = (IntegerValue) valueStack.pop();
        assertFalse(vResult._flagged);
        assertEquals(expected, vResult);
    }
}
