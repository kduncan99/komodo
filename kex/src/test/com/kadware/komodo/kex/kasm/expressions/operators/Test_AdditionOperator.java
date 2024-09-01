/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.operators;

import com.kadware.komodo.oldbaselib.FieldDescriptor;
import com.kadware.komodo.kex.kasm.*;
import com.kadware.komodo.kex.kasm.dictionary.*;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import java.math.BigInteger;
import java.util.Stack;
import org.junit.Test;
import static org.junit.Assert.*;

public class Test_AdditionOperator {

    @Test
    public void simple(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new IntegerValue.Builder().setValue(25).setFlagged(true).build());
        valueStack.push(new IntegerValue.Builder().setValue(10098).setFlagged(true).build());

        LineSpecifier ls = new LineSpecifier(0, 12);
        Operator op = new AdditionOperator(new Locale(ls, 18));
        Assembler asm = new Assembler.Builder().build();
        op.evaluate(asm, valueStack);

        assertTrue(asm.getDiagnostics().isEmpty());
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

        LineSpecifier ls = new LineSpecifier(0, 12);
        Operator op = new AdditionOperator(new Locale(ls, 18));
        Assembler asm = new Assembler.Builder().build();
        op.evaluate(asm, valueStack);

        assertTrue(asm.getDiagnostics().isEmpty());
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
        UnresolvedReference[] refs1 = {
            new UnresolvedReferenceToLocationCounter(new FieldDescriptor(0, 18), false, 5),
            new UnresolvedReferenceToLocationCounter(new FieldDescriptor(18, 18), false, 7),
            new UnresolvedReferenceToLocationCounter(new FieldDescriptor(18, 18), false, 7),
            new UnresolvedReferenceToLabel(new FieldDescriptor(0, 18), false, "FEE"),
            new UnresolvedReferenceToLabel(new FieldDescriptor(18, 18), false, "FOO"),
            new UnresolvedReferenceToLabel(FieldDescriptor.W, false, "OMNI"),
        };
        UnresolvedReference[] refs2 = {
            new UnresolvedReferenceToLocationCounter(new FieldDescriptor(0, 18), false, 5),
            new UnresolvedReferenceToLocationCounter(new FieldDescriptor(18, 18), true, 7),
            new UnresolvedReferenceToLabel(new FieldDescriptor(0, 18), false, "FEE"),
            new UnresolvedReferenceToLabel(new FieldDescriptor(18, 18), true, "FOO"),
            new UnresolvedReferenceToLabel(FieldDescriptor.W, false, "OMNI"),
        };
        UnresolvedReference[] expectedRefs = {
            new UnresolvedReferenceToLocationCounter(new FieldDescriptor(0, 18), false, 5),
            new UnresolvedReferenceToLocationCounter(new FieldDescriptor(0, 18), false, 5),
            new UnresolvedReferenceToLocationCounter(new FieldDescriptor(18, 18), false, 7),
            new UnresolvedReferenceToLabel(new FieldDescriptor(0, 18), false, "FEE"),
            new UnresolvedReferenceToLabel(new FieldDescriptor(0, 18), false, "FEE"),
            new UnresolvedReferenceToLabel(FieldDescriptor.W, false, "OMNI"),
            new UnresolvedReferenceToLabel(FieldDescriptor.W, false, "OMNI"),
        };

        DoubleWord36 dwValue1 = new DoubleWord36(BigInteger.valueOf(25));
        DoubleWord36 dwValue2 = new DoubleWord36(DoubleWord36.getOnesComplement(-25));

        IntegerValue addend1 = new IntegerValue.Builder().setValue(dwValue1).setReferences(refs1).build();
        IntegerValue addend2 = new IntegerValue.Builder().setValue(dwValue2).setReferences(refs2).build();
        IntegerValue expected = new IntegerValue.Builder().setValue(0).setReferences(expectedRefs).build();

        valueStack.push(addend1);
        valueStack.push(addend2);

        Assembler asm = new Assembler.Builder().build();

        LineSpecifier ls = new LineSpecifier(0, 12);
        Operator op = new AdditionOperator(new Locale(ls, 18));
        op.evaluate(asm, valueStack);

        assertTrue(asm.getDiagnostics().isEmpty());
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

        Assembler asm = new Assembler.Builder().build();

        LineSpecifier ls = new LineSpecifier(0, 12);
        Operator op = new AdditionOperator(new Locale(ls, 18));
        op.evaluate(asm, valueStack);

        assertTrue(asm.getDiagnostics().isEmpty());
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.Integer, valueStack.peek().getType());

        IntegerValue vResult = (IntegerValue) valueStack.pop();
        assertFalse(vResult._flagged);
        assertEquals(expected, vResult);
    }
}
