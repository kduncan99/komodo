/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.operators;

import com.kadware.komodo.baselib.FieldDescriptor;
import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.LineSpecifier;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.UnresolvedReference;
import com.kadware.komodo.kex.kasm.UnresolvedReferenceToLabel;
import com.kadware.komodo.kex.kasm.UnresolvedReferenceToLocationCounter;
import com.kadware.komodo.kex.kasm.dictionary.*;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import java.math.BigInteger;
import java.util.Stack;
import org.junit.Test;
import static org.junit.Assert.*;

public class Test_SubtractionOperator {

    @Test
    public void simple(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new IntegerValue.Builder().setValue(10098).setFlagged(true).build());
        valueStack.push(new IntegerValue.Builder().setValue(512).setFlagged(true).build());

        Assembler asm = new Assembler.Builder().build();

        LineSpecifier ls = new LineSpecifier(0, 12);
        Operator op = new SubtractionOperator(new Locale(ls, 18));
        op.evaluate(asm, valueStack);

        assertTrue(asm.getDiagnostics().isEmpty());
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.Integer, valueStack.peek().getType());

        IntegerValue vResult = (IntegerValue) valueStack.pop();
        assertFalse(vResult._flagged);
        assertEquals(BigInteger.valueOf(10098 - 512), vResult._value.get());
    }

    @Test
    public void simple_conversion(
    ) throws ExpressionException {
        Stack<Value> valueStack = new Stack<>();
        valueStack.push(new IntegerValue.Builder().setValue(10000).setFlagged(true).build());
        valueStack.push(new FloatingPointValue.Builder().setValue(new FloatingPointComponents(22.22222)).build());

        Assembler asm = new Assembler.Builder().build();

        LineSpecifier ls = new LineSpecifier(0, 12);
        Operator op = new SubtractionOperator(new Locale(ls, 18));
        op.evaluate(asm, valueStack);

        assertTrue(asm.getDiagnostics().isEmpty());
        assertEquals(1, valueStack.size());
        assertEquals(ValueType.FloatingPoint, valueStack.peek().getType());

        FloatingPointValue vResult = (FloatingPointValue) valueStack.pop();
        assertFalse(vResult._flagged);
        assertEquals(0, new FloatingPointComponents(10000.0 - 22.22222).compare(vResult._value, 6));
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
}
